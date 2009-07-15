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
package com.zimbra.cs.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;

public class VolumeBlobBuilder extends BlobBuilder {

    VolumeBlobBuilder(Blob targetBlob) {
        super(targetBlob);
    }

    private short getVolumeId() {
        return ((VolumeBlob) blob).getVolumeId();
    }

    @Override protected boolean useCompression(long size) throws ServiceException {
        if (isCompressionDisabled())
            return false;

        Volume volume = Volume.getById(getVolumeId());
        return volume.getCompressBlobs() &&
               (size <= 0 || size > volume.getCompressionThreshold());
    }

    @Override public Blob finish() throws IOException, ServiceException {
        if (isFinished())
            return blob;

        super.finish();

        // If sizeHint wasn't given we may have compressed a blob that was under
        // the compression threshold. Let's uncompress it. This isn't really
        // necessary, but uncompressing results in behavior consistent with
        // earlier ZCS releases.
        Volume volume = Volume.getById(getVolumeId());
        if (blob.isCompressed() && getTotalBytes() < volume.getCompressionThreshold()) {
            try {
                uncompressBlob(blob);
            } catch (IOException e) {
                dispose();
                throw e;
            }
        }

        return blob;
    }

    static void uncompressBlob(Blob blob) throws IOException {
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

    @Override public String toString() {
        return super.toString() + ", volume=" + getVolumeId();
    }
}
