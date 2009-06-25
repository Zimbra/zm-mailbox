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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.localconfig.DebugConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

public class BlobBuilder {
    private Blob blob;
    private int sizeHint;
    private boolean disableCompression;
    private MessageDigest digest;
    private OutputStream out;
    private FileChannel fc;
    private int totalBytes;
    private boolean finished;

    BlobBuilder(Blob blob) {
        this.blob = blob;
    }

    public void setSizeHint(int sizeHint) {
        this.sizeHint = sizeHint;
    }
    
    public void disableCompression(boolean disableCompression) {
        this.disableCompression = disableCompression;
    }

    public void init() throws IOException, ServiceException {
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("SHA1 digest not found", e);
        }
        FileOutputStream fos = new FileOutputStream(blob.getFile());
        fc = fos.getChannel();
        if (!useCompression(sizeHint)) {
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
    }
    
    private boolean useCompression(int sizeHint) throws ServiceException {
        if (disableCompression) return false;
        Volume volume = Volume.getById(blob.getLocator());
        return volume.getCompressBlobs() &&
               (sizeHint <= 0 || sizeHint > volume.getCompressionThreshold());
    }

    public void update(byte[] b, int off, int len)
        throws IOException, ServiceException {
        if (finished) {
            throw new IllegalStateException("Blob builder is finished");
        }
        if (out == null) {
            init(); // First time initialization
        }
        digest.update(b, off, len);
        try {
            out.write(b, off, len);
        } catch (IOException e) {
            dispose();
            throw e;
        }
        totalBytes += len;
    }

    public void finish() throws IOException, ServiceException {
        if (finished) return;
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

        // Set the blob's digest and size.
        blob.setDigest(ByteUtil.encodeFSSafeBase64(digest.digest()));
        blob.setRawSize(totalBytes);
        
        // If sizeHint wasn't given we may have compressed a blob that was under
        // the compression threshold. Let's uncompress it. This isn't really
        // necessary, but uncompressing results in behavior consistent with
        // earlier ZCS releases.
        Volume volume = Volume.getById(blob.getLocator());
        if (blob.isCompressed() && totalBytes < volume.getCompressionThreshold()) {
            try {
                uncompressBlob(blob);
            } catch (IOException e) {
                dispose();
                throw e;
            }
        }

        if (ZimbraLog.store.isDebugEnabled()) {
            ZimbraLog.store.debug(
                "Stored %s: data size=%d bytes, file size=%d bytes, volumeId=%d, isCompressed=%b",
                blob.getFile().getAbsolutePath(), totalBytes, blob.getFile().length(),
                blob.getLocator(), blob.isCompressed());
        }
    }

    private static void uncompressBlob(Blob blob) throws IOException {
        File file = blob.getFile();
        File tmp = File.createTempFile(file.getName(), ".unzip.tmp", file.getParentFile());
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new FileInputStream(file));
            os = new FileOutputStream(tmp);
            ByteUtil.copy(is, false, os, false);
        } finally {
            ByteUtil.closeStream(is);
            ByteUtil.closeStream(os);
        }
        if (!file.delete()) {
            throw new IOException("Unable to delete file: " + file.getAbsolutePath());
        }
        if (!tmp.renameTo(file)) {
            throw new IOException(
                String.format("Unable to rename '%s' to '%s'",
                              tmp.getAbsolutePath(), file.getAbsolutePath()));
        }
        blob.setCompressed(false);
    }

    public Blob getBlob() {
        if (!finished) {
            throw new IllegalStateException("Blob builder not finished");
        }
        return blob;
    }

    public boolean isFinished() {
        return finished;
    }

    // Clean up and dispose of blob file
    public void dispose() {
        finished = true;
        ByteUtil.closeStream(out);
        File file = blob.getFile();
        if (file.exists()) {
            file.delete();
        }
        blob = null;
    }

}
