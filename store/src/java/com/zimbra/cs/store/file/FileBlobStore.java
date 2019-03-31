/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileCache;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.FileDescriptorCache;
import com.zimbra.cs.store.IncomingDirectory;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.znative.IO;

/**
 * @since 2004.10.13
 */
public final class FileBlobStore extends StoreManager {
    private static final VolumeManager MANAGER = VolumeManager.getInstance();

    @Override
    public void startup() throws IOException, ServiceException {
        IncomingDirectory.startSweeper();

        // initialize file uncompressed file cache and file descriptor cache
        File tmpDir = new File(LC.zimbra_tmp_directory.value());
        File ufCacheDir = new File(tmpDir, "uncompressed");
        FileUtil.ensureDirExists(ufCacheDir);
        FileCache<String> ufCache = FileCache.Builder.createWithStringKey(ufCacheDir, false)
            .minLifetime(LC.uncompressed_cache_min_lifetime.longValue()).build();
        BlobInputStream.setFileDescriptorCache(new FileDescriptorCache(ufCache).loadSettings());
    }

    @Override
    public void shutdown() {
        IncomingDirectory.stopSweeper();
        BlobInputStream.getFileDescriptorCache().shutdown();
    }

    @Override
    public boolean supports(StoreFeature feature, String locator) {
        return supports(feature);
    }

    @Override
    public boolean supports(StoreFeature feature) {
        switch (feature) {
            case BULK_DELETE:  return true;
            case CENTRALIZED:  return false;
            case SINGLE_INSTANCE_SERVER_CREATE : return false;
            case RESUMABLE_UPLOAD : return false;
            default:           return false;
        }
    }

    private Blob getUniqueIncomingBlob() throws IOException, ServiceException {
        Volume volume = MANAGER.getCurrentMessageVolume();
        IncomingDirectory incdir = volume.getIncomingDirectory();
        if (incdir == null) {
            throw ServiceException.FAILURE("storing blob to volume without incoming directory: " + volume.getName(), null);
        }
        File f = incdir.getNewIncomingFile();
        ensureParentDirExists(f);
        return new VolumeBlob(f, volume.getId());
    }

    @Override
    public BlobBuilder getBlobBuilder() throws IOException, ServiceException {
        Blob blob = getUniqueIncomingBlob();
        return new VolumeBlobBuilder(blob);
    }

    @Override
    public Blob storeIncoming(InputStream in, boolean storeAsIs)
    throws IOException, ServiceException {
        BlobBuilder builder = getBlobBuilder();
        // if the blob is already compressed, *don't* calculate a digest/size from what we write
        builder.disableCompression(storeAsIs).disableDigest(storeAsIs);

        return builder.init().append(in).finish();
    }

    @Override
    public VolumeStagedBlob stage(InputStream in, long actualSize, Mailbox mbox)
    throws IOException, ServiceException {
        // mailbox store is on the same volume as incoming directory, so just storeIncoming() and wrap it
        Blob blob = storeIncoming(in);
        return new VolumeStagedBlob(mbox, (VolumeBlob) blob).markStagedDirectly();
    }

    @Override
    public VolumeStagedBlob stage(Blob blob, Mailbox mbox) throws IOException {
        // mailbox store is on the same volume as incoming directory, so no need to stage the blob
        return new VolumeStagedBlob(mbox, (VolumeBlob) blob);
    }

    @Override
    public VolumeMailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destItemId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = MANAGER.getCurrentMessageVolume();
        //FileBlobStore optimizes copy by using link where possible
        return link(src.getLocalBlob(), destMbox, destItemId, destRevision, volume.getId());
    }

    /**
     * Create a copy of a blob in volume/path specified by dest* parameters.
     * Note this method is not part of the StoreManager interface
     * It is only to be used for FileBlobStore specific code such as BlobMover
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @param destVolumeId mail_item.volume_id for message in dest Mbox
     * @return MailboxBlob object representing the linked blob
     * @throws IOException
     * @throws ServiceException
     */
    public VolumeMailboxBlob copy(Blob src, Mailbox destMbox, int destItemId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        Volume volume = MANAGER.getVolume(destVolumeId);
        return copy(src, destMbox, destItemId, destRevision, volume);
    }

    private VolumeMailboxBlob copy(Blob src, Mailbox destMbox, int destItemId, int destRevision, Volume destVolume)
    throws IOException, ServiceException {
        File srcFile = src.getFile();
        if (!srcFile.exists()) {
            throw new IOException(srcFile.getPath() + " does not exist");
        }

        String srcPath = src.getPath();
        File dest = getMailboxBlobFile(destMbox, destItemId, destRevision, destVolume.getId(), false);
        BlobInputStream.getFileDescriptorCache().remove(dest.getPath());  // Prevent stale cache read.
        String destPath = dest.getAbsolutePath();

        if (ZimbraLog.store.isDebugEnabled()) {
            long srcSize = srcFile.length();
            long srcRawSize = src.getRawSize();
            ZimbraLog.store.debug("Copying %s (size=%d, raw size=%d) to %s for mailbox %d, id %d.",
                srcPath, srcSize, srcRawSize, destPath, destMbox.getId(), destItemId);
        }

        ensureParentDirExists(dest);

        boolean destCompressed;
        if (destVolume.isCompressBlobs()) {
            if (src.isCompressed() || srcFile.length() <= destVolume.getCompressionThreshold()) {
                FileUtil.copy(srcFile, dest, !DebugConfig.disableMessageStoreFsync);
                destCompressed = src.isCompressed();
            } else {
                FileUtil.compress(srcFile, dest, !DebugConfig.disableMessageStoreFsync);
                destCompressed = true;
            }
        } else {
            if (src.isCompressed()) {
                FileUtil.uncompress(srcFile, dest, !DebugConfig.disableMessageStoreFsync);
            } else {
                FileUtil.copy(srcFile, dest, !DebugConfig.disableMessageStoreFsync);
            }
            destCompressed = false;
        }

        VolumeBlob newBlob = (VolumeBlob) new VolumeBlob(dest, destVolume.getId()).copyCachedDataFrom(src).setCompressed(destCompressed);
        return new VolumeMailboxBlob(destMbox, destItemId, destRevision, destVolume.getLocator(), newBlob);
    }

    @Override
    public VolumeMailboxBlob link(StagedBlob src, Mailbox destMbox, int destItemId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = MANAGER.getCurrentMessageVolume();
        VolumeBlob blob = ((VolumeStagedBlob) src).getLocalBlob();
        return link(blob, destMbox, destItemId, destRevision, volume.getId());
    }

    public VolumeMailboxBlob link(Blob src, Mailbox destMbox, int destItemId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        File srcFile = src.getFile();
        if (!srcFile.exists()) {
            throw new IOException(srcFile.getPath() + " does not exist.");
        }

        File dest = getMailboxBlobFile(destMbox, destItemId, destRevision, destVolumeId, false);
        String srcPath = src.getPath();
        String destPath = dest.getAbsolutePath();
        BlobInputStream.getFileDescriptorCache().remove(destPath);  // Prevent stale cache read.

        if (ZimbraLog.store.isDebugEnabled()) {
            long srcSize = srcFile.length();
            long srcRawSize = src.getRawSize();
            ZimbraLog.store.debug("Linking %s (size=%d, raw size=%d) to %s for mailbox %d, id %d.",
                srcPath, srcSize, srcRawSize, destPath, destMbox.getId(), destItemId);
        }

        ensureParentDirExists(dest);

        short srcVolumeId = ((VolumeBlob) src).getVolumeId();
        if (srcVolumeId == destVolumeId) {
            try {
                IO.link(srcPath, destPath);
            } catch (IOException e) {
                // Did it fail because the destination file already exists?
                // This can happen if we stored a file (or link), and we failed to
                // commit (say because of a server crash), and a subsequent new
                // item gets the ID of the uncommitted item
                if (dest.exists()) {
                    File destBak = new File(destPath + ".bak");
                    ZimbraLog.store.warn("Destination file exists.  Backing up to " + destBak.getAbsolutePath());
                    if (destBak.exists()) {
                        String bak = destBak.getAbsolutePath();
                        ZimbraLog.store.warn(bak + " already exists.  Deleting to make room for new backup file");
                        if (!destBak.delete()) {
                            ZimbraLog.store.warn("Unable to delete " + bak);
                            throw e;
                        }
                    }
                    File destTmp = new File(destPath);
                    if (!destTmp.renameTo(destBak)) {
                        ZimbraLog.store.warn("Can't rename " + destTmp.getAbsolutePath() + " to .bak");
                        throw e;
                    }
                    // Existing file is now renamed to <file>.bak.
                    // Retry link creation.
                    IO.link(srcPath, destPath);
                } else {
                    throw e;
                }
            }
        } else {
            // src and dest are on different volumes and can't be hard linked.
            // Do a copy instead.
            FileUtil.copy(srcFile, dest, !DebugConfig.disableMessageStoreFsync);
        }
        String destLocator = Short.toString(destVolumeId);
        VolumeBlob vblob = (VolumeBlob) new VolumeBlob(dest, destVolumeId).copyCachedDataFrom(src);
        return new VolumeMailboxBlob(destMbox, destItemId, destRevision, destLocator, vblob);
    }

    @Override
    public VolumeMailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destItemId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = MANAGER.getCurrentMessageVolume();
        VolumeBlob blob = ((VolumeStagedBlob) src).getLocalBlob();
        File srcFile = blob.getFile();
        String srcPath = srcFile.getAbsolutePath();
        if (!srcFile.exists()) {
            throw new IOException(srcFile.getPath() + " does not exist.");
        }

        File destFile = getMailboxBlobFile(destMbox, destItemId, destRevision, volume.getId(), false);
        String destPath = destFile.getAbsolutePath();
        BlobInputStream.getFileDescriptorCache().remove(destPath);  // Prevent stale cache read.
        ensureParentDirExists(destFile);

        if (ZimbraLog.store.isDebugEnabled()) {
            long srcSize = srcFile.length();
            long srcRawSize = blob.getRawSize();
            ZimbraLog.store.debug("Renaming %s (size=%d, raw size=%d) to %s for mailbox %d, id %d.",
                srcPath, srcSize, srcRawSize, destPath, destMbox.getId(), destItemId);
        }

        short srcVolumeId = blob.getVolumeId();
        if (srcVolumeId == volume.getId()) {
            boolean renamed = srcFile.renameTo(destFile);
            if (SystemUtil.ON_WINDOWS) {
                // On Windows renameTo fails if the dest already exists.  So delete
                // the destination and try the rename again
                if (!renamed && destFile.exists()) {
                    destFile.delete();
                    renamed = srcFile.renameTo(destFile);
                }
            }
            if (!renamed)
                throw new IOException("Unable to rename " + srcPath + " to " + destPath);
        } else {
            // Can't rename across volumes.  Copy then delete instead.
            FileUtil.copy(srcFile, destFile, !DebugConfig.disableMessageStoreFsync);
            srcFile.delete();
        }

        VolumeBlob vblob = (VolumeBlob) new VolumeBlob(destFile, volume.getId()).copyCachedDataFrom(blob);
        return new VolumeMailboxBlob(destMbox, destItemId, destRevision, volume.getLocator(), vblob);
    }

    @Override
    public boolean delete(MailboxBlob mblob) throws IOException {
        if (mblob == null) {
            return false;
        }
        return deleteFile(mblob.getLocalBlob().getFile());
    }

    @Override
    public boolean delete(StagedBlob staged) throws IOException {
        VolumeStagedBlob vsb = (VolumeStagedBlob) staged;
        // we only delete a staged blob if the caller never saw it as an incoming blob
        //  (this prevents killing the incoming blob in a multi-user delivery, for instance)
        if (staged == null || !vsb.wasStagedDirectly()) {
            return false;
        }
        return deleteFile(vsb.getLocalBlob().getFile());
    }

    @Override
    public boolean delete(Blob blob) throws IOException {
        if (blob == null) {
            return false;
        }
        return deleteFile(blob.getFile());
    }

    private boolean deleteFile(File file) throws IOException {
        if (file == null) {
            return false;
        }
        ZimbraLog.store.debug("Deleting %s.", file.getPath());
        BlobInputStream.getFileDescriptorCache().remove(file.getPath());  // Prevent stale cache read.
        boolean deleted = file.delete();
        if (deleted) {
            return true;
        }
        if (!file.exists()) {
            // File wasn't there to begin with.
            return false;
        }
        throw new IOException("Unable to delete blob file " + file.getAbsolutePath());
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, boolean validate) throws ServiceException {
        short volumeId = Short.valueOf(locator);
        File file = getMailboxBlobFile(mbox, itemId, revision, volumeId, validate);
        if (file == null) {
            return null;
        }
        return new VolumeMailboxBlob(mbox, itemId, revision, locator, new VolumeBlob(file, volumeId));
    }

    @Override
    public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        if (mboxBlob == null) {
            return null;
        }
        return getContent(mboxBlob.getLocalBlob());
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        if (blob == null) {
            return null;
        }
        return new BlobInputStream(blob);
    }

    @Override
    public boolean deleteStore(Mailbox mbox, Iterable<MailboxBlob.MailboxBlobInfo> blobs) throws IOException, ServiceException {
        assert blobs == null : "should not be passed a blob list since we support bulk blob delete";
        for (Volume vol : MANAGER.getAllVolumes()) {
            FileUtil.deleteDir(new File(vol.getMessageRootDir(mbox.getId())));
        }
        return true;
    }

    private File getMailboxBlobFile(Mailbox mbox, int itemId, int revision, short volumeId, boolean check)
    throws ServiceException {
        File file = new File(getBlobPath(mbox, itemId, revision, volumeId));
        if (!check || file.exists()) {
            return file;
        }
        // fallback for very very *very* old installs where blob paths were based on item id only
        file = new File(getBlobPath(mbox, itemId, -1, volumeId));
        return (file.exists() ? file : null);
    }

    public static String getBlobPath(Mailbox mbox, int itemId, int revision, short volumeId) throws ServiceException {
        return getBlobPath(mbox.getId(), itemId, revision, volumeId);
    }

    public static String getBlobPath(int mboxId, int itemId, int revision, short volumeId) throws ServiceException {
        Volume vol = MANAGER.getVolume(volumeId);
        String path = vol.getBlobDir(mboxId, itemId);
        int buflen = path.length() + 15 + (revision < 0 ? 0 : 11);

        StringBuilder sb = new StringBuilder(buflen);
        sb.append(path).append(File.separator);
        appendFilename(sb, itemId, revision);
        return sb.toString();
    }

    public static void appendFilename(StringBuilder sb, int itemId, int revision) {
        sb.append(itemId);
        if (revision >= 0) {
            sb.append('-').append(revision);
        }
        sb.append(".msg");
    }

    public static String getFilename(int itemId, int revision) {
        StringBuilder buf = new StringBuilder();
        appendFilename(buf, itemId, revision);
        return buf.toString();
    }

    private static void ensureDirExists(File dir) throws IOException {
        if (!FileUtil.mkdirs(dir)) {
            throw new IOException("Unable to create blob store directory " + dir.getAbsolutePath());
        }
    }

    private static void ensureParentDirExists(File file) throws IOException {
        ensureDirExists(file.getParentFile());
    }
}
