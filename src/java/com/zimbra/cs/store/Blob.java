/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 7.
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;

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
    private Boolean mIsCompressed = null;
    private String mDigest;
    private Integer mRawSize;

    public Blob(File file, short volumeId) {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }
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
    
    /**
     * Returns an <tt>InputStream</tt> to this blob's uncompressed data.
     */
    public InputStream getInputStream()
    throws IOException {
        InputStream in = new FileInputStream(mFile);
        if (isCompressed()) {
            in = new GZIPInputStream(in);
        }
        return in;
    }
    
    public boolean isCompressed()
    throws IOException {
        if (mIsCompressed == null) {
            mIsCompressed = FileUtil.isGzipped(mFile);
        }
        return mIsCompressed;
    }
    
    /**
     * Returns the SHA1 digest of this blob's uncompressed data, encoded in base64.
     */
    public String getDigest()
    throws IOException {
        if (mDigest == null) {
            initializeSizeAndDigest();
        }
        return mDigest;
    }
    
    /**
     * Returns the size of the blob's data.  If the blob is compressed,
     * returns the uncompressed size.
     */
    public int getRawSize()
    throws IOException {
        if (mRawSize == null) {
            if (!isCompressed()) {
                mRawSize = (int) mFile.length();
            } else {
                initializeSizeAndDigest();
            }
        }
    	return mRawSize;
    }
    
    private void initializeSizeAndDigest()
    throws IOException {
        InputStream in = null;
        try {
            in = getInputStream();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] buffer = new byte[1024];
            int numBytes;
            int totalBytes = 0;
            while ((numBytes = in.read(buffer)) >= 0) {
                md.update(buffer, 0, numBytes);
                totalBytes += numBytes;
            }
            byte[] digest = md.digest();
            mDigest = ByteUtil.encodeFSSafeBase64(digest);
            mRawSize = totalBytes;
        } catch (NoSuchAlgorithmException e) {
            // this should never happen unless the JDK is foobar
            //  e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }
    
    public void setCompressed(boolean isCompressed) {
        mIsCompressed = isCompressed;
    }

    public void setDigest(String digest) {
        mDigest = digest;
    }
    
    public void setRawSize(int rawSize) {
    	mRawSize = rawSize;
    }
    
    public boolean renameTo(String path) {
        if (mPath.equals(path)) {
            return false;
        }
        File newFile = new File(path);
        if (mFile.renameTo(newFile)) {
            mPath = path;
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return String.format("Blob: { path=%s, vol=%d, isCompressed=%b }", mPath, mVolumeId, mIsCompressed);
    }
}
