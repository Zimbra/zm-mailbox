/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009 Zimbra, Inc.
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
package com.zimbra.cs.store;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.localconfig.DebugConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPOutputStream;

public class BlobBuilder {
    protected Blob blob;
    private long sizeHint;
    protected boolean disableCompression;
    private boolean disableDigest;
    private StorageCallback storageCallback;
    private MessageDigest digest;
    private OutputStream out;
    private FileChannel fc;
    private long totalBytes;
    private boolean finished;

    protected BlobBuilder(Blob targetBlob) {
        this.blob = targetBlob;
    }

    public BlobBuilder setSizeHint(long size) {
        this.sizeHint = size;
        return this;
    }

    public long getSizeHint() {
        return sizeHint;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public BlobBuilder setStorageCallback(StorageCallback callback) {
        this.storageCallback = callback;
        return this;
    }

    public BlobBuilder disableCompression(boolean disable) {
        this.disableCompression = disable;
        return this;
    }

    public BlobBuilder disableDigest(boolean disable) {
        this.disableDigest = disable;
        return this;
    }

    public BlobBuilder init() throws IOException, ServiceException {
        if (!disableDigest) {
            try {
                digest = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                throw ServiceException.FAILURE("SHA1 digest not found", e);
            }
        }

        FileOutputStream fos = new FileOutputStream(blob.getFile());
        fc = fos.getChannel();
        if (useCompression(sizeHint)) {
            try {
                out = new GZIPOutputStream(fos);
            } catch (IOException e) {
                dispose();
                throw e;
            }
            blob.setCompressed(true);
        } else {
            out = fos;
            blob.setCompressed(false);
        }

        return this;
    }

    @SuppressWarnings("unused")
    protected boolean useCompression(long size) throws ServiceException {
        return false;
    }

    private static final int BUFLEN = Math.max(LC.zimbra_store_copy_buffer_size_kb.intValue(), 1) * 1024;

    public BlobBuilder append(InputStream in) throws IOException {
        byte[] buffer = new byte[BUFLEN];
        int numRead;
        while ((numRead = in.read(buffer)) >= 0)
            append(buffer, 0, numRead);

        return this;
    }

    public BlobBuilder append(byte[] b, int off, int len) throws IOException {
        if (finished)
            throw new IllegalStateException("BlobBuilder is finished");

        checkInitialized();

        try {
            out.write(b, off, len);
            if (storageCallback != null)
                storageCallback.wrote(blob, b, off, len);
            if (digest != null)
                digest.update(b, off, len);
            totalBytes += len;
        } catch (IOException e) {
            dispose();
            throw e;
        }

        return this;
    }

    private void checkInitialized() throws IOException {
        if (out == null) {
            try {
                init();
            } catch (Exception e) {
                throw (IOException) new IOException("Unable to initialize BlobBuilder").initCause(e);
            }
        }
    }

    @SuppressWarnings("unused")
    public Blob finish() throws IOException, ServiceException {
        if (finished)
            return blob;

        try {
            if (!DebugConfig.disableMessageStoreFsync) {
                out.flush();
                fc.force(true);
            }
        } catch (IOException e) {
            dispose();
            throw e;
        } finally {
            ByteUtil.closeStream(out);
        }

        // set the blob's digest and size
        if (digest != null) {
            blob.setDigest(ByteUtil.encodeFSSafeBase64(digest.digest()));
            blob.setRawSize(totalBytes);
        }

        if (ZimbraLog.store.isDebugEnabled())
            ZimbraLog.store.debug("stored " + this);

        finished = true;
        return blob;
    }

    @Override public String toString() {
        File file = blob.getFile();

        String compressed = "???";
        try {
            compressed = Boolean.toString(blob.isCompressed());
        } catch (IOException ioe) { }
        return file.getAbsolutePath() + ": data size=" + totalBytes + ", file size=" + file.length() + ", isCompressed=" + compressed;
    }

    public Blob getBlob() {
        if (!finished)
            throw new IllegalStateException("Blob builder not finished");
        return blob;
    }

    public boolean isFinished() {
        return finished;
    }

    // Clean up and dispose of blob file
    public void dispose() {
        if (blob != null) {
            finished = true;
            ByteUtil.closeStream(out);
            StoreManager.getInstance().quietDelete(blob);
            blob = null;
        }
    }
}
