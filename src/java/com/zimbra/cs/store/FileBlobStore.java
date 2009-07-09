/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 10. 13.
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.znative.IO;

public class FileBlobStore extends StoreManager {

    private static final int BUFLEN = Math.max(LC.zimbra_store_copy_buffer_size_kb.intValue(), 1) * 1024;
    private static int sDiskStreamingThreshold;
    private UncompressedFileCache<String> mUncompressedFileCache;
    private FileDescriptorCache mFileDescriptorCache;

    public FileDescriptorCache getFileDescriptorCache() {
        return mFileDescriptorCache;
    }

    @Override public void startup() throws IOException, ServiceException {
        Volume.reloadVolumes();
        IncomingDirectory.startSweeper();

        // Initialize file uncompressed file cache and file descriptor cache.
        String uncompressedPath = LC.zimbra_tmp_directory.value() + "/uncompressed";
        FileUtil.ensureDirExists(uncompressedPath);
        mUncompressedFileCache = new UncompressedFileCache<String>(uncompressedPath);
        mFileDescriptorCache = new FileDescriptorCache(mUncompressedFileCache);

        loadSettings();
        mUncompressedFileCache.startup();

    }

    @Override public void shutdown() {
        IncomingDirectory.stopSweeper();
    }

    private static boolean onWindows() {
        String os = System.getProperty("os.name").toLowerCase();         
        return os.startsWith("win");     
    }     

    private static final boolean sOnWindows = onWindows();

    public static int getDiskStreamingThreshold() {
        return sDiskStreamingThreshold;
    }

    @SuppressWarnings("static-access")
    public static void loadSettings() throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer(); 
        sDiskStreamingThreshold = server.getMailDiskStreamingThreshold();
        int uncompressedMaxFiles = server.getMailUncompressedCacheMaxFiles();
        long uncompressedMaxBytes = server.getMailUncompressedCacheMaxBytes();
        int fileDescriptorCacheSize = server.getMailFileDescriptorCacheSize();

        ZimbraLog.store.info("Loading %s settings: %s=%d, %s=%d, %s=%d, %s=%d.", FileBlobStore.class.getSimpleName(),
                Provisioning.A_zimbraMailDiskStreamingThreshold, sDiskStreamingThreshold,
                Provisioning.A_zimbraMailUncompressedCacheMaxFiles, uncompressedMaxFiles,
                Provisioning.A_zimbraMailUncompressedCacheMaxBytes, uncompressedMaxBytes,
                Provisioning.A_zimbraMailFileDescriptorCacheSize, fileDescriptorCacheSize);

        FileBlobStore store = (FileBlobStore) getInstance();
        store.mUncompressedFileCache.setMaxBytes(uncompressedMaxBytes);
        store.mUncompressedFileCache.setMaxFiles(uncompressedMaxFiles);
        store.mFileDescriptorCache.setMaxSize(fileDescriptorCacheSize);
    }

    private Blob getUniqueIncomingBlob() throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        IncomingDirectory incdir = volume.getIncomingDirectory();
        if (incdir == null)
            throw ServiceException.FAILURE("storing blob to volume without incoming directory: " + volume.getName(), null);
        File f = incdir.getNewIncomingFile();
        ensureParentDirExists(f);
        return new VolumeBlob(f, volume.getId());
    }

    @Override public BlobBuilder getBlobBuilder()
    throws IOException, ServiceException {
        Blob blob = getUniqueIncomingBlob();
        return new VolumeBlobBuilder(blob);
    }

    @Override public Blob storeIncoming(InputStream in, int sizeHint, StorageCallback callback, boolean storeAsIs)
    throws IOException, ServiceException {
        BlobBuilder builder = getBlobBuilder().setSizeHint(sizeHint).disableCompression(storeAsIs).setStorageCallback(callback).init();

        byte[] buffer = new byte[BUFLEN];
        int numRead;
        while ((numRead = in.read(buffer)) >= 0)
            builder.update(buffer, 0, numRead);

        return builder.finish();
    }

    @Override public StagedBlob stage(Blob blob, Mailbox mbox) {
        // mailbox store is on the same volume as incoming directory, so no need to stage the blob
        return new StagedBlob(blob);
    }

    @Override public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return copy(src.getBlob(), destMbox, destMsgId, destRevision, volume);
    }

    public MailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        Volume volume = Volume.getById(destVolumeId);
        return copy(src, destMbox, destMsgId, destRevision, volume);
    }

    private MailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision, Volume destVolume)
    throws IOException, ServiceException {
        String srcPath = src.getPath();
        File dest = getMailboxBlobFile(destMbox, destMsgId, destRevision, destVolume.getId(), false);
        String destPath = dest.getAbsolutePath();
        ensureParentDirExists(dest);

        boolean srcCompressed = FileUtil.isGzipped(src.getFile());
        boolean destCompressed = false;
        if (destVolume.getCompressBlobs()) {
            if (srcCompressed || src.getFile().length() <= destVolume.getCompressionThreshold()) {
                FileUtil.copy(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
                destCompressed = srcCompressed;
            } else {
                FileUtil.compress(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
                destCompressed = true;
            }
        } else {
            if (srcCompressed)
                FileUtil.uncompress(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
            else
                FileUtil.copy(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
        }

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug("Copied id=" + destMsgId +
                    " mbox=" + destMbox.getId() +
                    " oldpath=" + srcPath + 
                    " newpath=" + destPath);
        }

        Blob newBlob = new VolumeBlob(dest, destVolume.getId());
        newBlob.setCompressed(destCompressed);
        return new MailboxBlob(destMbox, destMsgId, destRevision, destVolume.getLocator(), newBlob);
    }

    @Override public MailboxBlob link(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return link(src.getBlob(), destMbox, destMsgId, destRevision, volume.getId());
    }

    @Override public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return link(src.getLocalBlob(), destMbox, destMsgId, destRevision, volume.getId());
    }

    public MailboxBlob link(Blob src, Mailbox destMbox, int destMsgId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        File dest = getMailboxBlobFile(destMbox, destMsgId, destRevision, destVolumeId, false);
        String srcPath = src.getPath();
        String destPath = dest.getAbsolutePath();
        ensureParentDirExists(dest);

        short srcVolumeId = ((VolumeBlob) src).getVolumeId();
        if (srcVolumeId == destVolumeId) {
            try {
                IO.link(srcPath, destPath);
            } catch (IOException e) {
                // Did it fail because the destination file already exists?
                // This can happen if we stored a file (or link), and we failed to
                // commit (say because of a server crash), and a subsequent new
                // message gets the ID of uncommited message
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
            FileUtil.copy(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
        }

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug("Linked id=" + destMsgId +
                    " mbox=" + destMbox.getId() +
                    " oldpath=" + srcPath + 
                    " newpath=" + destPath);
        }

        String destLocator = Short.toString(destVolumeId);
        return new MailboxBlob(destMbox, destMsgId, destRevision, destLocator, new VolumeBlob(dest, destVolumeId));
    }

    @Override public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        Blob blob = src.getLocalBlob();
        File srcFile = blob.getFile();
        String srcPath = srcFile.getAbsolutePath();

        File destFile = getMailboxBlobFile(destMbox, destMsgId, destRevision, volume.getId(), false);
        String destPath = destFile.getAbsolutePath();
        ensureParentDirExists(destFile);

        short srcVolumeId = ((VolumeBlob) blob).getVolumeId();
        if (srcVolumeId == volume.getId()) {
            boolean renamed = srcFile.renameTo(destFile);
            if (sOnWindows) {
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

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug("Renamed id=" + destMsgId +
                    " mbox=" + destMbox.getId() +
                    " oldpath=" + srcPath + 
                    " newpath=" + destPath);
        }

        return new MailboxBlob(destMbox, destMsgId, destRevision, volume.getLocator(), new VolumeBlob(destFile, volume.getId()));
    }

    @Override public boolean delete(MailboxBlob mboxBlob) throws IOException {
        if (mboxBlob == null)
            return false;
        return deleteFile(mboxBlob.getBlob().getFile());
    }

    @Override public boolean delete(Blob blob) throws IOException {
        if (blob == null)
            return false;
        return deleteFile(blob.getFile());
    }

    private boolean deleteFile(File file) throws IOException {
        if (file == null)
            return false;
        ZimbraLog.store.debug("Deleting %s.", file.getPath());
        boolean deleted = file.delete();
        if (deleted)
            return true;
        if (!file.exists()) {
            // File wasn't there to begin with.
            return false;
        }
        throw new IOException("Unable to delete blob file " + file.getAbsolutePath());
    }

    @Override public MailboxBlob getMailboxBlob(Mailbox mbox, int msgId, int revision, String locator)
    throws ServiceException {
        short volumeId = Short.parseShort(locator);
        File file = getMailboxBlobFile(mbox, msgId, revision, volumeId, true);
        if (file == null)
            return null;
        return new MailboxBlob(mbox, msgId, revision, locator, new VolumeBlob(file, volumeId));
    }

    @Override public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        if (mboxBlob == null)
            return null;
        return getContent(mboxBlob.getBlob());
    }

    @Override public InputStream getContent(Blob blob) throws IOException {
        if (blob == null)
            return null;
        return new BlobInputStream(blob.getFile());
    }

    @Override public boolean deleteStore(Mailbox mbox)
    throws IOException {
        for (Volume vol : Volume.getAll()) {
            FileUtil.deleteDir(new File(vol.getMessageRootDir(mbox.getId())));
        }
        return true;
    }

    private File getMailboxBlobFile(Mailbox mbox, int msgId, int revision, short volumeId, boolean check)
    throws ServiceException {
        File file = new File(getBlobPath(mbox, msgId, revision, volumeId));
        if (check && !file.exists())
            file = new File(getBlobPath(mbox, msgId, -1, volumeId));
        return (!check || file.exists() ? file : null);
    }

    private static String getBlobPath(Mailbox mbox, int msgId, int revision, short volumeId)
    throws ServiceException {
        Volume vol = Volume.getById(volumeId);
        String path = vol.getBlobDir(mbox.getId(), msgId);
        int buflen = path.length() + 15;
        if (revision >= 0)
            buflen += 11;
        StringBuffer sb = new StringBuffer(buflen);
        sb.append(path).append(File.separator).append(msgId);
        if (revision >= 0)
            sb.append('-').append(revision);
        sb.append(".msg");
        return sb.toString();
    }

    private static void ensureDirExists(File dir) throws IOException {
        if (!FileUtil.mkdirs(dir))
            throw new IOException("Unable to create blob store directory " +
                    dir.getAbsolutePath());
    }

    private static void ensureParentDirExists(File file) throws IOException {
        File dir = file.getParentFile();
        ensureDirExists(dir);
    }
}
