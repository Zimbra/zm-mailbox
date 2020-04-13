/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store.external;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileCache;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.FileDescriptorCache;
import com.zimbra.cs.store.IncomingDirectory;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;

/**
 * Abstract base class for external store integration.
 * Uses local incoming directory during blob creation and maintains local file cache of retrieved blobs to minimize remote round-trips
 */
public abstract class ExternalStoreManager extends StoreManager implements ExternalBlobIO {

    private final IncomingDirectory incoming = new IncomingDirectory(LC.zimbra_tmp_directory.value() + File.separator + "incoming");
    protected FileCache<String> localCache;

    @Override
    public void startup() throws IOException, ServiceException {
        FileUtil.mkdirs(new File(incoming.getPath()));
        IncomingDirectory.setSweptDirectories(incoming);
        IncomingDirectory.startSweeper();

        // create a local cache for downloading remote blobs
        File tmpDir = new File(LC.zimbra_tmp_directory.value());
        File localCacheDir = new File(tmpDir, "blobs");
        FileUtil.deleteDir(localCacheDir);
        FileUtil.ensureDirExists(localCacheDir);
        localCache = FileCache.Builder.createWithStringKey(localCacheDir, false)
            .maxFiles(LC.external_store_local_cache_max_files.intValue())
            .maxBytes(LC.external_store_local_cache_max_bytes.longValue())
            .minLifetime(LC.external_store_local_cache_min_lifetime.longValue())
            .removeCallback(new MessageCacheChecker()).build();

        // initialize file uncompressed file cache and file descriptor cache
        File ufCacheDir = new File(tmpDir, "uncompressed");
        FileUtil.ensureDirExists(ufCacheDir);
        FileCache<String> ufCache = FileCache.Builder.createWithStringKey(ufCacheDir, false)
            .maxFiles(LC.external_store_local_cache_max_files.intValue())
            .maxBytes(LC.external_store_local_cache_max_bytes.longValue())
            .minLifetime(LC.external_store_local_cache_min_lifetime.longValue())
            .removeCallback(new MessageCacheChecker()).build();
        BlobInputStream.setFileDescriptorCache(new FileDescriptorCache(ufCache).loadSettings());

    }

    private class MessageCacheChecker implements FileCache.RemoveCallback {
        MessageCacheChecker()  { }

        @Override
        public boolean okToRemove(FileCache.Item item) {
            // Don't remove blobs that are being referenced by a cached message.
            return !MessageCache.contains(item.digest);
        }
    };

    @Override
    public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destItemId, long destRevision)
    throws IOException, ServiceException {
        //default implementation does not handle de-duping
        //stores which de-dupe need to override this method appropriately
        InputStream is = getContent(src);
        try {
            StagedBlob staged = stage(is, src.getSize(), destMbox);
            return link(staged, destMbox, destItemId, destRevision);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    @Override
    public boolean delete(Blob blob) throws IOException {
        return blob.getFile().delete();
    }

    @Override
    public boolean delete(StagedBlob staged) throws IOException {
        ExternalStagedBlob blob = (ExternalStagedBlob) staged;
        // we only delete a staged blob if it hasn't already been added to the mailbox
        if (blob == null || blob.isInserted()) {
            return true;
        }
        return deleteFromStore(blob.getLocator(), blob.getMailbox());
    }

    @Override
    public boolean delete(MailboxBlob mblob) throws IOException {
        if (mblob == null) {
            return true;
        }
        localCache.remove(mblob.getLocator());
        return deleteFromStore(mblob.getLocator(), mblob.getMailbox());
    }

    @Override
    public boolean deleteStore(Mailbox mbox, Iterable<MailboxBlob.MailboxBlobInfo> blobs) throws IOException, ServiceException {
        // the default implementation iterates through the mailbox's blobs and deletes them one by one
        IOException ioException = null;
        int consecutiveIoExceptions = 0;
        for (MailboxBlob.MailboxBlobInfo mbinfo : blobs) {
            try {
                delete(getMailboxBlob(mbox, mbinfo.itemId, mbinfo.revision, mbinfo.locator, false));
                consecutiveIoExceptions = 0;
            } catch (IOException ioe) {
                if (ioException == null) {
                    ioException = ioe;
                }
                consecutiveIoExceptions++;
                ZimbraLog.store.warn("IOException during deleteStore() for mbox [%d] item [%d] revision [%d] locator [%s]"
                    , mbox.getId(), mbinfo.itemId, mbinfo.revision, mbinfo.locator, ioe);
                if (consecutiveIoExceptions > LC.external_store_delete_max_ioexceptions.intValue()) {
                    ZimbraLog.store.error("too many consecutive IOException during delete store, bailing");
                    break;
                }
            }
        }
        if (ioException != null) {
            throw new IOException("deleteStore failed due to IOException", ioException);
        }
        return true;
    }

    @Override
    public BlobBuilder getBlobBuilder() throws IOException, ServiceException {
        return new ExternalBlobBuilder(new ExternalBlob(incoming.getNewIncomingFile()));
    }

    @Override
    public InputStream getContent(MailboxBlob mblob) throws IOException {
        if (mblob == null) {
            return null;
        }
        Blob blob = getLocalBlob(mblob.getMailbox(), mblob.getLocator(), true);
        return blob.getInputStream();
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        return new ExternalBlobInputStream(blob);
    }

    protected Blob getLocalBlob(Mailbox mbox, String locator, boolean fromCache) throws IOException {
        FileCache.Item cached = null;
        if (fromCache) {
            cached = localCache.get(locator);
            if (cached != null) {
                ExternalBlob blob = new ExternalBlob(cached);
                blob.setLocator(locator);
                blob.setMbox(mbox);
                return blob;
            }
        }

        InputStream is = readStreamFromStore(locator, mbox);
        if (is == null) {
            throw new IOException("Store " + this.getClass().getName() +" returned null for locator " + locator);
        } else {
            cached = localCache.put(locator, is);
            ExternalBlob blob = new ExternalBlob(cached);
            blob.setLocator(locator);
            blob.setMbox(mbox);
            return blob;
        }
    }

    protected Blob getLocalBlob(Mailbox mbox, String locator) throws IOException {
        return getLocalBlob(mbox, locator, true);
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, long revision, String locator, boolean validate) throws ServiceException {
        ExternalMailboxBlob mblob = new ExternalMailboxBlob(mbox, itemId, revision, locator);
        return (!validate || mblob.validateBlob()) ? mblob : null;
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, boolean validate) throws ServiceException {
        return this.getMailboxBlob(mbox, itemId, (long)revision, locator, validate);
    }

    @Override
    public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, long destRevision) throws IOException,
    ServiceException {
        // link is a noop
        return renameTo(src, destMbox, destMsgId, destRevision);
    }

    @Override
    public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, long destRevision) throws IOException,
    ServiceException {
        // rename is a noop
        ExternalStagedBlob staged = (ExternalStagedBlob) src;
        staged.markInserted();

        MailboxBlob mblob = new ExternalMailboxBlob(destMbox, destMsgId, destRevision, staged.getLocator());
        return mblob.setSize(staged.getSize()).setDigest(staged.getDigest());
    }

    @Override
    public void shutdown() {
        IncomingDirectory.stopSweeper();
    }

    @Override
    public StagedBlob stage(Blob blob, Mailbox mbox) throws IOException, ServiceException {
        if (supports(StoreFeature.RESUMABLE_UPLOAD) && blob instanceof ExternalUploadedBlob) {
            ZimbraLog.store.debug("blob already uploaded, just need to commit");
            String locator = ((ExternalResumableUpload) this).finishUpload((ExternalUploadedBlob) blob);
            if (locator != null) {
                ZimbraLog.store.debug("wrote to locator %s",locator);
                localCache.put(locator, getContent(blob));
            } else {
                ZimbraLog.store.warn("blob staging returned null locator");
            }
            return new ExternalStagedBlob(mbox, blob.getDigest(), blob.getRawSize(), locator);
        } else {
            InputStream is = getContent(blob);
            try {
                StagedBlob staged = stage(is, blob.getRawSize(), mbox);
                if (staged != null && staged.getLocator() != null) {
                    localCache.put(staged.getLocator(), getContent(blob));
                }
                return staged;
            } finally {
                ByteUtil.closeStream(is);
            }
        }
    }

    @Override
    public StagedBlob stage(InputStream in, long actualSize, Mailbox mbox) throws ServiceException, IOException {
        if (actualSize < 0) {
            Blob blob = storeIncoming(in);
            try {
                return stage(blob, mbox);
            } finally {
                quietDelete(blob);
            }
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("SHA-256 digest not found", e);
        }
        ByteUtil.PositionInputStream pin = new ByteUtil.PositionInputStream(new DigestInputStream(in, digest));

        try {
            String locator = writeStreamToStore(pin, actualSize, mbox);
            if (locator != null) {
                ZimbraLog.store.debug("wrote to locator %s",locator);
            } else {
                ZimbraLog.store.warn("blob staging returned null locator");
            }
            return new ExternalStagedBlob(mbox, ByteUtil.encodeFSSafeBase64(digest.digest()), pin.getPosition(), locator);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to stage blob", e);
        }
    }


    @Override
    public Blob storeIncoming(InputStream data, boolean storeAsIs) throws IOException,
    ServiceException {
        BlobBuilder builder = getBlobBuilder();
        // if the blob is already compressed, *don't* calculate a digest/size from what we write
        builder.disableCompression(storeAsIs).disableDigest(storeAsIs);
        return builder.init().append(data).finish();
    }

    /**
     * Get a set of all blobs which exist in the store associated with a mailbox
     * Optional operation used to find orphaned blobs
     * If the blob store does not partition based on mailbox this method should not be overridden
     * @throws IOException
     */
    public List<String> getAllBlobPaths(Mailbox mbox) throws IOException {
        return new ArrayList<String>();
    }

    @Override
    public boolean supports(StoreFeature feature, String locator) {
      return supports(feature);
    }

    @Override
    public boolean supports(StoreFeature feature) {
        switch (feature) {
            case BULK_DELETE:                   return false;
            case CENTRALIZED:                   return true;
            case SINGLE_INSTANCE_SERVER_CREATE: return false;
            case RESUMABLE_UPLOAD:              return this instanceof ExternalResumableUpload;
            default:                            return false;
        }
    }

    @VisibleForTesting
    public void clearCache() {
        localCache.removeAll();
    }
}
