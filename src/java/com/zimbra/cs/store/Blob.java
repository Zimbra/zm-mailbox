/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import org.apache.commons.io.FileUtils;

import com.google.common.base.Objects;
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
    private File file;
    private String path;
    private Boolean compressed = null;
    private String digest;
    private Long rawSize;

    protected Blob(final File file) {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }

        this.file = file;
        this.path = file.getAbsolutePath();
    }

    protected Blob(File file, long rawSize, String digest) {
        this(file);
        this.rawSize = rawSize;
        this.digest = digest;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return path;
    }

    public InputStream getInputStream() throws IOException {
        InputStream in = new FileInputStream(file);
        if (isCompressed()) {
            in = new GZIPInputStream(in);
        }
        return in;
    }

    public boolean isCompressed() throws IOException {
        if (compressed == null) {
            if (rawSize != null && rawSize.longValue() == file.length()) {
                this.compressed = Boolean.FALSE;
            } else {
                this.compressed = FileUtil.isGzipped(file);
            }
        }
        return compressed;
    }

    /** Returns the SHA1 digest of this blob's uncompressed data,
     *  encoded in base64. */
    public String getDigest() throws IOException {
        if (digest == null) {
            initializeSizeAndDigest();
        }
        return digest;
    }

    /** Returns the size of the blob's data.  If the blob is compressed,
     *  returns the uncompressed size. */
    public long getRawSize() throws IOException {
        if (rawSize == null) {
            if (!isCompressed()) {
                this.rawSize = file.length();
            } else {
                initializeSizeAndDigest();
            }
        }
        return rawSize;
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
            this.digest = ByteUtil.encodeFSSafeBase64(md.digest());
            this.rawSize = totalBytes;
        } catch (NoSuchAlgorithmException e) {
            // this should never happen unless the JDK is foobar
            //  e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ByteUtil.closeStream(in);
        }
    }

    public Blob setCompressed(final boolean isCompressed) {
        this.compressed = isCompressed;
        return this;
    }

    public Blob setDigest(final String digest) {
        this.digest = digest;
        return this;
    }

    public Blob setRawSize(final long rawSize) {
        this.rawSize = rawSize;
        return this;
    }

    public Blob copyCachedDataFrom(final Blob other) {
        if (compressed == null && other.compressed != null) {
            this.compressed = other.compressed;
        }
        if (digest == null && other.digest != null) {
            this.digest = other.digest;
        }
        if (rawSize == null && other.rawSize != null) {
            this.rawSize = other.rawSize;
        }
        return this;
    }

    public void renameTo(final String newPath) throws IOException {
        if (!path.equals(newPath)) {
            File newFile = new File(newPath);
            FileUtils.moveFile(file, newFile);
            this.path = newPath;
            this.file = newFile;
        }
    }

    @Override public String toString() {
        return Objects.toStringHelper(this)
            .add("path", path)
            .add("size", rawSize)
            .add("compressed", compressed).toString();
    }
}
