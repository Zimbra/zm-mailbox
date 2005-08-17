/*
 * Created on 2004. 10. 13.
 */
package com.zimbra.cs.store;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.FileUtil;
import com.liquidsys.os.IO;

/**
 * @author jhahm
 */
public class FileBlobStore extends StoreManager {

    private static Log mLog = LogFactory.getLog(FileBlobStore.class);

    private static boolean sCompressionEnabled;

    static {
        sCompressionEnabled = Config.getBoolean(Config.C_STORE_COMPRESS_BLOBS,
                                                Config.D_STORE_COMPRESS_BLOBS);
    }

	// TODO: This value is per-volume, and should come from config/ldap.
	private static final int FS_BLOCK_SIZE = 4096;

    private UniqueFileNameGenerator mUniqueFilenameGenerator;

	FileBlobStore() throws Exception {
        mUniqueFilenameGenerator = new UniqueFileNameGenerator();
	}

    public void shutdown() {
        // nothing to do
    }

    public Blob storeIncoming(byte[] data, String digest,
                              String path, short volumeId)
    throws IOException, ServiceException {
        byte[] writeData;

        if (sCompressionEnabled && data.length > FS_BLOCK_SIZE) {
            writeData = ByteUtil.compress(data);
        } else {
            writeData = data;
        }

        if (path == null) {
            Volume volume = Volume.getById(volumeId);
            String incomingDir = volume.getIncomingMsgDir();
            String fname = mUniqueFilenameGenerator.getFilename();
            StringBuffer sb = new StringBuffer(incomingDir.length() + 1 + fname.length());
            sb.append(incomingDir).append(File.separator).append(fname);
            path = sb.toString();
        }
        File file = new File(path);
        ensureParentDirExists(file);

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(writeData);
        fos.flush();
        fos.getFD().sync();
        fos.close();

        if (mLog.isInfoEnabled()) {
            mLog.info("Stored size=" + data.length +
                      " wrote=" + writeData.length +
                      " path=" + path +
                      " vol=" + volumeId +
                      " digest=" + digest);
        }

        Blob blob = new Blob(file, volumeId);
        return blob;
    }

	public MailboxBlob link(Blob src, Mailbox destMbox,
                            int destMsgId, int destRevision,
                            short destVolumeId)
    throws IOException, ServiceException {
        String srcPath = src.getPath();
		File dest = getBlobFile(destMbox, destMsgId, destRevision,
                                destVolumeId, false);
        String destPath = dest.getAbsolutePath();
		ensureParentDirExists(dest);

        if (destVolumeId == src.getVolumeId()) {
            try {
                IO.link(srcPath, destPath);
            } catch (IOException e) {
                // Did it fail because the destination file already exists?
                // This can happen if we stored a file (or link), and we failed to
                // commit (say because of a server crash), and a subsequent new
                // message gets the ID of uncommited message
                if (dest.exists()) {
                    File destBak = new File(destPath + ".bak");
                	mLog.warn("Destination file exists.  Backing up to " + destBak.getAbsolutePath());
                    if (destBak.exists()) {
                        String bak = destBak.getAbsolutePath();
                    	mLog.warn(bak + " already exists.  Deleting to make room for new backup file");
                        if (!destBak.delete()) {
                        	mLog.warn("Unable to delete " + bak);
                            throw e;
                        }
                    }
                    File destTmp = new File(destPath);
                    if (!destTmp.renameTo(destBak)) {
                        mLog.warn("Can't rename " + destTmp.getAbsolutePath() + " to .bak");
                        throw e;
                    }
                    // Existing file is now renamed to <file>.bak.
                    // Retry link creation.
                    IO.link(srcPath, destPath);
                } else
                    throw e;
            }
        } else {
        	// src and dest are on different volumes and can't be hard linked.
            // Do a copy instead.
            copy(src.getFile(), dest);
        }

        if (mLog.isInfoEnabled()) {
            mLog.info("Linked id=" + destMsgId +
                      " mbox=" + destMbox.getId() +
                      " oldpath=" + srcPath + 
                      " newpath=" + destPath);
        }

		MailboxBlob destMboxBlob =
            new MailboxBlob(destMbox, destMsgId, destRevision,
                            new Blob(dest, destVolumeId));
		return destMboxBlob;
	}

    public MailboxBlob renameTo(Blob src, Mailbox destMbox,
                                int destMsgId, int destRevision,
                                short destVolumeId)
    throws IOException, ServiceException {
        File srcFile = src.getFile();
        File destFile = getBlobFile(destMbox, destMsgId, destRevision,
                                    destVolumeId, false);
        String srcPath = srcFile.getAbsolutePath();
        String destPath = destFile.getAbsolutePath();
        ensureParentDirExists(destFile);

        if (destVolumeId == src.getVolumeId()) {
            boolean renamed = srcFile.renameTo(destFile);
            if (!renamed)
            	throw new IOException("Unable to rename " + srcPath + " to " + destPath);
        } else {
            // Can't rename across volumes.  Copy then delete instead.
        	copy(srcFile, destFile);
            srcFile.delete();
        }

        if (mLog.isInfoEnabled()) {
            mLog.info("Renamed id=" + destMsgId +
                      " mbox=" + destMbox.getId() +
                      " oldpath=" + srcPath + 
                      " newpath=" + destPath);
        }

        MailboxBlob destMboxBlob =
            new MailboxBlob(destMbox, destMsgId, destRevision,
                            new Blob(destFile, destVolumeId));
        return destMboxBlob;
    }

    private static void copy(File src, File dest) throws IOException {
        try {
        	FileUtil.copy(src, dest);
        } catch (IOException e) {
            if (dest.exists())
                dest.delete();
            throw e;
        }
    }

    public boolean delete(MailboxBlob mboxBlob) throws IOException {
        if (mboxBlob == null)
            return false;
        if (mLog.isInfoEnabled())
            mLog.info("deleting blob " + mboxBlob.getMessageId() +
                      " in mailbox " + mboxBlob.getMailbox().getId());
        return deleteFile(mboxBlob.getBlob().getFile());
    }

    public boolean delete(Blob blob) throws IOException {
        if (blob == null)
            return false;
        if (mLog.isInfoEnabled())
            mLog.info("deleting blob file (" + blob.toString() + ")");
        return deleteFile(blob.getFile());
    }

    private boolean deleteFile(File file) throws IOException {
        if (file == null)
            return false;
        boolean deleted = file.delete();
        if (deleted)
            return true;
        if (!file.exists()) {
            // File wasn't there to begin with.
            return false;
        }
        throw new IOException("Unable to delete blob file " + file.getAbsolutePath());
    }

    public MailboxBlob getMailboxBlob(Mailbox mbox,
                                      int msgId, int revision,
                                      short volumeId)
    throws ServiceException {
        File file = getBlobFile(mbox, msgId, revision, volumeId, true);
        if (file == null)
            return null;
        return new MailboxBlob(mbox, msgId, revision, new Blob(file, volumeId));
    }

    public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        if (mboxBlob == null)
            return null;
        return getContent(mboxBlob.getBlob());
    }

    public InputStream getContent(Blob blob) throws IOException {
        if (blob == null)
            return null;
        byte[] data = ByteUtil.getContent(blob.getFile());
        if (data != null) {
            if (ByteUtil.isGzipped(data))
                data = ByteUtil.uncompress(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            return bais;
        }
        return null;
    }

    /* (non-Javadoc)
	 * @see com.zimbra.cs.store.StoreManager#deleteStore(com.zimbra.cs.mailbox.Mailbox, int)
	 */
	public boolean deleteStore(Mailbox mbox, int volume)
    throws IOException, ServiceException {
        List volumes = Volume.getAll();
        for (Iterator iter = volumes.iterator(); iter.hasNext(); ) {
        	Volume vol = (Volume) iter.next();
            FileUtil.deleteDir(new File(vol.getMailboxDir(mbox.getId(), Volume.TYPE_MESSAGE)));
        }
        return true;
	}

    private File getBlobFile(Mailbox mbox, int msgId, int revision,
                             short volumeId, boolean check)
    throws ServiceException {
        File file = new File(getBlobPath(mbox, msgId, revision, volumeId));
        if (check && !file.exists())
            file = new File(getBlobPath(mbox, msgId, -1, volumeId));
        return (!check || file.exists() ? file : null);
    }

	private static String getBlobPath(Mailbox mbox,
                                      int msgId, int revision,
                                      short volumeId)
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
}
