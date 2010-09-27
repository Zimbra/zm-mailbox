/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store.file;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;

public class VolumeBlobBuilder extends BlobBuilder {

    VolumeBlobBuilder(Blob targetBlob) {
        super(targetBlob);
    }

    private short getVolumeId() {
        return ((VolumeBlob) blob).getVolumeId();
    }

    @Override protected boolean useCompression() throws IOException {
        if (disableCompression)
            return false;

        try {
            Volume volume = Volume.getById(getVolumeId());
            return volume.getCompressBlobs();
        } catch (ServiceException e) {
            throw new IOException("Unable to determine volume compression flag", e);
        }
    }

    
    @Override
    protected int getCompressionThreshold() {
        try {
            Volume volume = Volume.getById(getVolumeId());
            return (int) volume.getCompressionThreshold();
        } catch (ServiceException e) {
            ZimbraLog.store.error("Unable to determine volume compression threshold", e);
        }
        return 0;
    }

    @Override public Blob finish() throws IOException, ServiceException {
        if (isFinished())
            return blob;

        super.finish();
        return blob;
    }

    @Override public String toString() {
        return super.toString() + ", volume=" + getVolumeId();
    }
}
