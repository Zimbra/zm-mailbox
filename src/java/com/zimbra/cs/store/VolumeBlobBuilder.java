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

import java.io.IOException;

import com.zimbra.common.service.ServiceException;

public class VolumeBlobBuilder extends BlobBuilder {

    VolumeBlobBuilder(Blob blob) {
        super(blob);
    }

    private short getVolumeId() {
        return ((VolumeBlob) super.getBlob()).getVolumeId();
    }

    @Override boolean useCompression(int size) throws ServiceException {
        if (isCompressionDisabled())
            return false;

        Volume volume = Volume.getById(getVolumeId());
        return volume.getCompressBlobs() &&
               (size <= 0 || size > volume.getCompressionThreshold());
    }

    @Override public void finish() throws IOException, ServiceException {
        super.finish();

        // If sizeHint wasn't given we may have compressed a blob that was under
        // the compression threshold. Let's uncompress it. This isn't really
        // necessary, but uncompressing results in behavior consistent with
        // earlier ZCS releases.
        Blob blob = getBlob();
        Volume volume = Volume.getById(getVolumeId());
        if (blob.isCompressed() && getTotalBytes() < volume.getCompressionThreshold()) {
            try {
                uncompressBlob(blob);
            } catch (IOException e) {
                dispose();
                throw e;
            }
        }
    }

    @Override public String toString() {
        return super.toString() + ", volume=" + getVolumeId();
    }
}
