/*
 * Created on 2005. 6. 7.
 */
package com.liquidsys.coco.store;

import java.io.File;

/**
 * @author jhahm
 * 
 * Represents a blob in blob store incoming directory.  An incoming blob
 * does not belong to any mailbox.  When a message is delivered to a mailbox,
 * message is saved in the incoming directory and a link to it is created
 * in the mailbox's directory.  The linked blob in mailbox directory
 * is represented by a MailboxBlob object.
 */
public class Blob {

    private File mFile;
    private String mPath;
    private short mVolumeId;

    public Blob(File file, short volumeId) {
        mFile = file;
        mPath = file.getAbsolutePath();
        mVolumeId = volumeId;
    }

    public File getFile() {
        return mFile;
    }

    public String getPath() {
    	return mPath;
    }

    public short getVolumeId() {
    	return mVolumeId;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("path=").append(mPath);
        sb.append(", vol=").append(mVolumeId);
        return sb.toString();
    }
}
