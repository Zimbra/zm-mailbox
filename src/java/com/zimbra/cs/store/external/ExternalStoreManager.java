/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store.external;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destItemId, int destRevision)
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
        for (MailboxBlob.MailboxBlobInfo mbinfo : blobs) {
            delete(getMailboxBlob(mbox, mbinfo.itemId, mbinfo.revision, mbinfo.locator));
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
        FileCache.Item cached = localCache.get(mblob.getLocator());
        if (cached != null) {
            return new FileInputStream(cached.file);
        }

        InputStream is = readStreamFromStore(mblob.getLocator(), mblob.getMailbox());
        if (is == null) {
            throw new IOException("Store " + this.getClass().getName() +" returned null for locator " + mblob.getLocator());
        } else {
            return is;
        }
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        return new BlobInputStream(blob);
    }

    protected Blob getLocalBlob(Mailbox mbox, String locator) throws IOException {
        FileCache.Item cached = localCache.get(locator);
        if (cached != null) {
            return new ExternalBlob(cached);
        }

        InputStream is = readStreamFromStore(locator, mbox);
        if (is == null) {
            throw new IOException("Store " + this.getClass().getName() +" returned null for locator " + locator);
        } else {
            cached = localCache.put(locator, is);
            return new ExternalBlob(cached);
        }
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator) throws ServiceException {
        return new ExternalMailboxBlob(mbox, itemId, revision, locator);
    }

    @Override
    public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision) throws IOException,
    ServiceException {
        // link is a noop
        return renameTo(src, destMbox, destMsgId, destRevision);
    }

    @Override
    public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision) throws IOException,
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
