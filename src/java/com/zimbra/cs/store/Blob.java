/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
 * Represents a blob in blob store incoming directory.  An incoming blob
 * does not belong to any mailbox.  When a message is delivered to a mailbox,
 * message is saved in the incoming directory and a link to it is created
 * in the mailbox's directory.  The linked blob in mailbox directory
 * is represented by a MailboxBlob object.
 */
public class Blob {

    private File mFile;
    private String mPath;
    private Boolean mIsCompressed = null;
    private String mDigest;
    private Long mRawSize;

    protected Blob(final File file) {
        if (file == null)
            throw new NullPointerException("file cannot be null");

        mFile = file;
        mPath = file.getAbsolutePath();
    }

    public File getFile() {
        return mFile;
    }

    public String getPath() {
        return mPath;
    }

    public InputStream getInputStream() throws IOException {
        InputStream in = new FileInputStream(mFile);
        if (isCompressed())
            in = new GZIPInputStream(in);
        return in;
    }

    public boolean isCompressed() throws IOException {
        if (mIsCompressed == null) {
            if (mRawSize != null && mRawSize.longValue() == mFile.length())
                mIsCompressed = Boolean.FALSE;
            else
                mIsCompressed = FileUtil.isGzipped(mFile);
        }
        return mIsCompressed;
    }

    /** Returns the SHA1 digest of this blob's uncompressed data,
     *  encoded in base64. */
    public String getDigest() throws IOException {
        if (mDigest == null)
            initializeSizeAndDigest();
        return mDigest;
    }

    /** Returns the size of the blob's data.  If the blob is compressed,
     *  returns the uncompressed size. */
    public long getRawSize() throws IOException {
        if (mRawSize == null) {
            if (!isCompressed())
                mRawSize = mFile.length();
            else
                initializeSizeAndDigest();
        }
        return mRawSize;
    }

    private void initializeSizeAndDigest() throws IOException {
        InputStream in = null;
        try {
            // Get the stream using the local method.  FileBlobStore.getContent()
            // can call getDigest(), which could result in an infinite loop.
            in = getInputStream();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] buffer = new byte[1024];
            int numBytes;
            long totalBytes = 0;
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
            ByteUtil.closeStream(in);
        }
    }

    public Blob setCompressed(final boolean isCompressed) {
        mIsCompressed = isCompressed;
        return this;
    }

    public Blob setDigest(final String digest) {
        mDigest = digest;
        return this;
    }

    public Blob setRawSize(final long rawSize) {
        mRawSize = rawSize;
        return this;
    }

    public Blob copyCachedDataFrom(final Blob other) {
        if (mIsCompressed == null && other.mIsCompressed != null)
            mIsCompressed = other.mIsCompressed;
        if (mDigest == null && other.mDigest != null)
            mDigest = other.mDigest;
        if (mRawSize == null && other.mRawSize != null)
            mRawSize = other.mRawSize;
        return this;
    }

    public boolean renameTo(final String path) {
        if (mPath.equals(path))
            return false;

        File newFile = new File(path);
        if (mFile.renameTo(newFile)) {
            mPath = path;
            mFile = newFile;
            return true;
        } else {
            return false;
        }
    }

    @Override public String toString() {
        return "Blob: { path=" + mPath + ", size=" + mRawSize + ", compressed=" + mIsCompressed + " }";
    }
}
