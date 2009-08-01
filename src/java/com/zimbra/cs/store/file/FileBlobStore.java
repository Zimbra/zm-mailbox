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
package com.zimbra.cs.store.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.*;
import com.zimbra.znative.IO;

public class FileBlobStore extends StoreManager {

    @Override public void startup() throws IOException, ServiceException {
        Volume.reloadVolumes();
        IncomingDirectory.startSweeper();

        // initialize file uncompressed file cache and file descriptor cache
        String uncompressedPath = LC.zimbra_tmp_directory.value() + File.separator + "uncompressed";
        FileUtil.ensureDirExists(uncompressedPath);
        UncompressedFileCache<String> ufcache = new UncompressedFileCache<String>(uncompressedPath).startup();
        BlobInputStream.setFileDescriptorCache(new FileDescriptorCache(ufcache).loadSettings());
    }

    @Override public void shutdown() {
        IncomingDirectory.stopSweeper();
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

    @Override public Blob storeIncoming(InputStream in, long sizeHint, StorageCallback callback, boolean storeAsIs)
    throws IOException, ServiceException {
        BlobBuilder builder = getBlobBuilder().setSizeHint(sizeHint).setStorageCallback(callback);
        // if the blob is already compressed, *don't* calculate a digest/size from what we write
        builder.disableCompression(storeAsIs).disableDigest(storeAsIs);

        return builder.init().append(in).finish();
    }

    @Override public VolumeStagedBlob stage(InputStream in, long actualSize, StorageCallback callback, Mailbox mbox)
    throws IOException, ServiceException {
        // mailbox store is on the same volume as incoming directory, so just storeIncoming() and wrap it
        Blob blob = storeIncoming(in, actualSize, callback);
        return new VolumeStagedBlob(mbox, (VolumeBlob) blob).markStagedDirectly();
    }

    @Override public VolumeStagedBlob stage(Blob blob, Mailbox mbox) throws IOException {
        // mailbox store is on the same volume as incoming directory, so no need to stage the blob
        return new VolumeStagedBlob(mbox, (VolumeBlob) blob);
    }

    @Override public VolumeMailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return copy(src.getLocalBlob(), destMbox, destMsgId, destRevision, volume);
    }

    public VolumeMailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        Volume volume = Volume.getById(destVolumeId);
        return copy(src, destMbox, destMsgId, destRevision, volume);
    }

    private VolumeMailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision, Volume destVolume)
    throws IOException, ServiceException {
        String srcPath = src.getPath();
        File dest = getMailboxBlobFile(destMbox, destMsgId, destRevision, destVolume.getId(), false);
        String destPath = dest.getAbsolutePath();
        ensureParentDirExists(dest);

        boolean destCompressed;
        if (destVolume.getCompressBlobs()) {
            if (src.isCompressed() || src.getFile().length() <= destVolume.getCompressionThreshold()) {
                FileUtil.copy(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
                destCompressed = src.isCompressed();
            } else {
                FileUtil.compress(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
                destCompressed = true;
            }
        } else {
            if (src.isCompressed())
                FileUtil.uncompress(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
            else
                FileUtil.copy(src.getFile(), dest, !DebugConfig.disableMessageStoreFsync);
            destCompressed = false;
        }

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug("copied id=" + destMsgId +
                                      " mbox=" + destMbox.getId() +
                                   " oldpath=" + srcPath + 
                                   " newpath=" + destPath);
        }

        VolumeBlob newBlob = (VolumeBlob) new VolumeBlob(dest, destVolume.getId()).copyCachedDataFrom(src).setCompressed(destCompressed);
        return new VolumeMailboxBlob(destMbox, destMsgId, destRevision, destVolume.getLocator(), newBlob);
    }

    @Override public VolumeMailboxBlob link(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return link(src.getLocalBlob(), destMbox, destMsgId, destRevision, volume.getId());
    }

    @Override public VolumeMailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        VolumeBlob blob = ((VolumeStagedBlob) src).getLocalBlob();
        return link(blob, destMbox, destMsgId, destRevision, volume.getId());
    }

    public VolumeMailboxBlob link(Blob src, Mailbox destMbox, int destMsgId, int destRevision, short destVolumeId)
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
            ZimbraLog.store.debug("linked id=" + destMsgId +
                                      " mbox=" + destMbox.getId() +
                                   " oldpath=" + srcPath + 
                                   " newpath=" + destPath);
        }

        String destLocator = Short.toString(destVolumeId);
        VolumeBlob vblob = (VolumeBlob) new VolumeBlob(dest, destVolumeId).copyCachedDataFrom(src);
        return new VolumeMailboxBlob(destMbox, destMsgId, destRevision, destLocator, vblob);
    }

    @Override public VolumeMailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        VolumeBlob blob = ((VolumeStagedBlob) src).getLocalBlob();
        File srcFile = blob.getFile();
        String srcPath = srcFile.getAbsolutePath();

        File destFile = getMailboxBlobFile(destMbox, destMsgId, destRevision, volume.getId(), false);
        String destPath = destFile.getAbsolutePath();
        ensureParentDirExists(destFile);

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

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug("renamed id=" + destMsgId +
                                       " mbox=" + destMbox.getId() +
                                    " oldpath=" + srcPath +
                                    " newpath=" + destPath);
        }

        VolumeBlob vblob = (VolumeBlob) new VolumeBlob(destFile, volume.getId()).copyCachedDataFrom(blob);
        return new VolumeMailboxBlob(destMbox, destMsgId, destRevision, volume.getLocator(), vblob);
    }

    @Override public boolean delete(MailboxBlob mblob) throws IOException {
        if (mblob == null)
            return false;
        return deleteFile(mblob.getLocalBlob().getFile());
    }

    @Override public boolean delete(StagedBlob staged) throws IOException {
        VolumeStagedBlob vsb = (VolumeStagedBlob) staged;
        // we only delete a staged blob if the caller never saw it as an incoming blob
        //  (this prevents killing the incoming blob in a multi-user delivery, for instance)
        if (staged == null || !vsb.wasStagedDirectly())
            return false;
        return deleteFile(vsb.getLocalBlob().getFile());
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
        return new VolumeMailboxBlob(mbox, msgId, revision, locator, new VolumeBlob(file, volumeId));
    }

    @Override public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        if (mboxBlob == null)
            return null;
        return getContent(mboxBlob.getLocalBlob());
    }

    @Override public InputStream getContent(Blob blob) throws IOException {
        if (blob == null)
            return null;
        return new BlobInputStream(blob);
    }

    @Override public boolean deleteStore(Mailbox mbox) throws IOException {
        for (Volume vol : Volume.getAll())
            FileUtil.deleteDir(new File(vol.getMessageRootDir(mbox.getId())));
        return true;
    }

    private File getMailboxBlobFile(Mailbox mbox, int msgId, int revision, short volumeId, boolean check)
    throws ServiceException {
        File file = new File(getBlobPath(mbox, msgId, revision, volumeId));
        if (!check || file.exists())
            return file;

        // fallback for very very *very* old installs where blob paths were based on item id only
        file = new File(getBlobPath(mbox, msgId, -1, volumeId));
        return (file.exists() ? file : null);
    }

    private static String getBlobPath(Mailbox mbox, int msgId, int revision, short volumeId)
    throws ServiceException {
        Volume vol = Volume.getById(volumeId);
        String path = vol.getBlobDir(mbox.getId(), msgId);
        int buflen = path.length() + 15 + (revision < 0 ? 0 : 11);

        StringBuffer sb = new StringBuffer(buflen);
        sb.append(path).append(File.separator).append(msgId);
        if (revision >= 0)
            sb.append('-').append(revision);
        sb.append(".msg");
        return sb.toString();
    }

    private static void ensureDirExists(File dir) throws IOException {
        if (!FileUtil.mkdirs(dir))
            throw new IOException("Unable to create blob store directory " + dir.getAbsolutePath());
    }

    private static void ensureParentDirExists(File file) throws IOException {
        ensureDirExists(file.getParentFile());
    }
}
