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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.znative.IO;

public class FileBlobStore extends StoreManager {

    private static final int BUFLEN = Math.max(LC.zimbra_store_copy_buffer_size_kb.intValue(), 1) * 1024;
    private static int sDiskStreamingThreshold;
    private UncompressedFileCache<String> mUncompressedFileCache;
    private FileDescriptorCache mFileDescriptorCache;

    private UniqueFileNameGenerator mUniqueFilenameGenerator;

    FileBlobStore() throws Exception {
        mUniqueFilenameGenerator = new UniqueFileNameGenerator();
    }

    public FileDescriptorCache getFileDescriptorCache() {
        return mFileDescriptorCache;
    }

    @Override public void startup()
    throws IOException, ServiceException {
        long sweepMaxAgeMS = LC.zimbra_store_sweeper_max_age.intValue() * 60 * 1000;
        mSweeper = new IncomingDirectorySweeper(SWEEP_INTERVAL_MS, sweepMaxAgeMS);
        mSweeper.start();

        // Initialize file uncompressed file cache and file descriptor cache.
        String uncompressedPath = LC.zimbra_tmp_directory.value() + "/uncompressed";
        FileUtil.ensureDirExists(uncompressedPath);
        mUncompressedFileCache = new UncompressedFileCache<String>(uncompressedPath);
        mFileDescriptorCache = new FileDescriptorCache(mUncompressedFileCache);

        loadSettings();
        mUncompressedFileCache.startup();

    }

    @Override public void shutdown() {
        if (mSweeper != null) {
            mSweeper.signalShutdown();
            try {
                mSweeper.join();
            } catch (InterruptedException e) {}
        }
    }

    private static boolean onWindows() {
        String os = System.getProperty("os.name").toLowerCase();         
        return os.startsWith("win");     
    }     

    private static final boolean sOnWindows = onWindows();

    public static int getDiskStreamingThreshold() {
        return sDiskStreamingThreshold;
    }

    public static void loadSettings()
    throws ServiceException {
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

    private Blob getUniqueIncomingBlob() throws IOException {
        Volume volume = Volume.getCurrentMessageVolume();
        String incomingDir = volume.getIncomingMsgDir();
        String fname = mUniqueFilenameGenerator.getFilename();
        StringBuilder sb = new StringBuilder(incomingDir.length() + 1 + fname.length());
        sb.append(incomingDir).append(File.separator).append(fname);

        File f = new File(sb.toString());
        ensureParentDirExists(f);
        String volumeId = Short.toString(volume.getId());
        return new Blob(f, volumeId);
    }

    @Override public BlobBuilder getBlobBuilder()
    throws IOException, ServiceException {
        Blob blob = getUniqueIncomingBlob();
        return new BlobBuilder(blob);
    }

    @Override public Blob storeIncoming(InputStream in, int sizeHint,
            StorageCallback callback, boolean storeAsIs)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();

        Blob blob = getUniqueIncomingBlob();
        File file = blob.getFile();

        // Initialize streams and digest calculator.
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("", e);
        }

        DigestInputStream digestStream = new DigestInputStream(in, digest);
        FileOutputStream fos = null;
        OutputStream out = null;
        boolean compress =
            !storeAsIs && volume.getCompressBlobs() &&
            (sizeHint > volume.getCompressionThreshold() || sizeHint <= 0);
        int numRead = -1;
        int totalRead = 0;

        try {
            fos = new FileOutputStream(file);
            out = fos;

            if (compress) {
                out = new GZIPOutputStream(fos);
                blob.setCompressed(true);
            }

            // Write to the file.
            byte[] buffer = new byte[BUFLEN];
            while ((numRead = digestStream.read(buffer)) >= 0) {
                out.write(buffer, 0, numRead);
                if (callback != null) {
                    callback.wrote(blob, buffer, numRead);
                }
                totalRead += numRead;
            }
            if (!DebugConfig.disableMessageStoreFsync) {
                out.flush();
                fos.getChannel().force(true);
            }
        } finally {
            ByteUtil.closeStream(out);
        }

        // Set the blob's digest and size.
        blob.setDigest(ByteUtil.encodeFSSafeBase64(digest.digest()));
        blob.setRawSize(totalRead);

        // If sizeHint wasn't given we may have compressed a blob that was under the compression
        // threshold.  Let's uncompress it.  This isn't really necessary, but uncompressing results
        // in behavior consistent with earlier ZCS releases.
        if (compress && totalRead <= volume.getCompressionThreshold()) {
            File temp = File.createTempFile(file.getName(), ".unzip.tmp", file.getParentFile());
            InputStream fin = null;
            OutputStream fout = null;
            try {
                fin = new GZIPInputStream(new FileInputStream(file));
                fout = new FileOutputStream(temp);
                ByteUtil.copy(fin, false, fout, false);
            } finally {
                ByteUtil.closeStream(fin);
                ByteUtil.closeStream(fout);
            }
            boolean deleted = file.delete();
            if (!deleted)
                throw new IOException("Unable to delete temp file " + file.getAbsolutePath());
            String srcPath = temp.getAbsolutePath();
            boolean renamed = temp.renameTo(file);
            if (!renamed)
                throw new IOException("Unable to rename " + srcPath + " to " + file.getAbsolutePath());
            blob.setCompressed(false);
        }

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug("Stored blob: data size=%d bytes, file size=%d bytes, volumeId=%d, isCompressed=%b",
                    totalRead, file.length(), volume.getId(), blob.isCompressed());
        }
        return blob;
    }

    @Override public MailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return copy(src, destMbox, destMsgId, destRevision, volume);
    }

    public MailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        Volume volume = Volume.getById(destVolumeId);
        return copy(src, destMbox, destMsgId, destRevision, volume);
    }

    private MailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision, Volume destVolume)
    throws IOException, ServiceException {
        String srcPath = src.getPath();
        File dest = getBlobFile(destMbox, destMsgId, destRevision, destVolume.getLocator(), false);
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

        String destVolumeId = Short.toString(destVolume.getId());
        Blob newBlob = new Blob(dest, destVolumeId);
        newBlob.setCompressed(destCompressed);
        return new MailboxBlob(destMbox, destMsgId, destRevision, newBlob);
    }

    @Override public MailboxBlob link(Blob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        return link(src, destMbox, destMsgId, destRevision, volume.getId());
    }

    public MailboxBlob link(Blob src, Mailbox destMbox, int destMsgId, int destRevision, short destVolumeId)
    throws IOException, ServiceException {
        String destLocator = Short.toString(destVolumeId);
        File dest = getBlobFile(destMbox, destMsgId, destRevision, destLocator, false);
        String srcPath = src.getPath();
        String destPath = dest.getAbsolutePath();
        ensureParentDirExists(dest);

        short srcVolumeId = Short.parseShort(src.getLocator());
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

        return new MailboxBlob(destMbox, destMsgId, destRevision, new Blob(dest, destLocator));
    }

    @Override public MailboxBlob renameTo(Blob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        Volume volume = Volume.getCurrentMessageVolume();
        File srcFile = src.getFile();
        File destFile = getBlobFile(destMbox, destMsgId, destRevision, volume.getLocator(), false);
        String srcPath = srcFile.getAbsolutePath();
        String destPath = destFile.getAbsolutePath();
        ensureParentDirExists(destFile);

        short srcVolumeId = Short.parseShort(src.getLocator());
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

        return new MailboxBlob(destMbox, destMsgId, destRevision, new Blob(destFile, volume.getLocator()));
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
        File file = getBlobFile(mbox, msgId, revision, locator, true);
        if (file == null)
            return null;
        return new MailboxBlob(mbox, msgId, revision, new Blob(file, locator));
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

    private File getBlobFile(Mailbox mbox, int msgId, int revision, String locator, boolean check)
    throws ServiceException {
        File file = new File(getBlobPath(mbox, msgId, revision, locator));
        if (check && !file.exists())
            file = new File(getBlobPath(mbox, msgId, -1, locator));
        return (!check || file.exists() ? file : null);
    }

    private static String getBlobPath(Mailbox mbox, int msgId, int revision, String locator)
    throws ServiceException {
        Volume vol = Volume.getById(locator);
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



    private static class UniqueFileNameGenerator {
        private long mTime;
        private long mSeq;

        public UniqueFileNameGenerator() {
            reset();
        }

        private void reset() {
            mTime = System.currentTimeMillis();
            mSeq = 0;
        }

        public String getFilename() {
            long time, seq;
            synchronized (this) {
                if (mSeq >= 1000) {
                    reset();
                }
                time = mTime;
                seq = mSeq++;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(time).append("-").append(seq).append(".msg");
            return sb.toString();
        }
    }


    private static final long SWEEP_INTERVAL_MS = 60 * 1000;  // 1 minute

    private IncomingDirectorySweeper mSweeper;

    private static class IncomingDirectorySweeper extends Thread {
        private Log sLog = LogFactory.getLog(IncomingDirectorySweeper.class);

        private boolean mShutdown = false;
        private long mSweepIntervalMS;
        private long mMaxAgeMS;

        public IncomingDirectorySweeper(long sweepIntervalMS,
                long maxAgeMS) {
            super("IncomingDirectorySweeper");
            setDaemon(true);
            mSweepIntervalMS = sweepIntervalMS;
            mMaxAgeMS = maxAgeMS;
        }

        public synchronized void signalShutdown() {
            mShutdown = true;
            wakeup();
        }

        public synchronized void wakeup() {
            notify();
        }

        @Override public void run() {
            sLog.info(getName() + " thread starting");

            boolean shutdown = false;
            long startTime = System.currentTimeMillis();

            while (!shutdown) {
                // Sleep until next scheduled wake-up time, or until notified.
                synchronized (this) {
                    if (!mShutdown) {
                        long now = System.currentTimeMillis();
                        long until = startTime + mSweepIntervalMS;
                        if (until > now) {
                            try {
                                wait(until - now);
                            } catch (InterruptedException e) {}
                        }
                    }
                    shutdown = mShutdown;
                    if (shutdown) break;
                }

                int numDeleted = 0;
                startTime = System.currentTimeMillis();

                // Delete old files in incoming directory of each volume.
                List<Volume> allVolumes = Volume.getAll();
                for (Volume volume : allVolumes) {
                    short volType = volume.getType();
                    if (volType != Volume.TYPE_MESSAGE &&
                            volType != Volume.TYPE_MESSAGE_SECONDARY)
                        continue;
                    File directory = new File(volume.getIncomingMsgDir());
                    if (!directory.exists()) continue;
                    File[] files = directory.listFiles();
                    if (files == null) continue;
                    for (int i = 0; i < files.length; i++) {
                        // Check for shutdown after every 100 files.
                        if (i % 100 == 0) {
                            synchronized (this) {
                                shutdown = mShutdown;
                            }
                            if (shutdown) break;
                        }

                        File file = files[i];
                        if (file.isDirectory()) continue;
                        long lastMod = file.lastModified();
                        // lastModified() returns 0L if file doesn't exist (i.e. deleted by another thread
                        // after this thread did directory.listFiles())
                        if (lastMod > 0L) {
                            long age = startTime - lastMod;
                            if (age >= mMaxAgeMS) {
                                boolean deleted = file.delete();
                                if (!deleted) {
                                    // Let's warn only if delete failure wasn't caused by file having been
                                    // deleted by someone else already.
                                    if (file.exists())
                                        sLog.warn("Sweeper unable to delete " + file.getAbsolutePath());
                                } else if (sLog.isDebugEnabled()) {
                                    sLog.debug("Sweeper deleted " +
                                            file.getAbsolutePath());
                                    numDeleted++;
                                }
                            }
                        }
                    }
                    synchronized (this) {
                        shutdown = mShutdown;
                    }
                    if (shutdown) break;
                }

                long elapsed = System.currentTimeMillis() - startTime;

                sLog.debug("Incoming directory sweep deleted " + numDeleted +
                        " files in " + elapsed + "ms");
            }

            sLog.info(getName() + " thread exiting");
        }
    }
}
