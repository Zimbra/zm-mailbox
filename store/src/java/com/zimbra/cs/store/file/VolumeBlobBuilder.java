/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store.file;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.volume.VolumeManager;

public final class VolumeBlobBuilder extends BlobBuilder {

    VolumeBlobBuilder(Blob targetBlob) {
        super(targetBlob);
    }

    private short getVolumeId() {
        return ((VolumeBlob) blob).getVolumeId();
    }

    @Override
    protected boolean useCompression() throws IOException {
        if (disableCompression) {
            return false;
        }
        try {
            return VolumeManager.getInstance().getVolume(getVolumeId()).isCompressBlobs();
        } catch (ServiceException e) {
            throw new IOException("Unable to determine volume compression flag", e);
        }
    }


    @Override
    protected int getCompressionThreshold() {
        try {
            return (int) VolumeManager.getInstance().getVolume(getVolumeId()).getCompressionThreshold();
        } catch (ServiceException e) {
            ZimbraLog.store.error("Unable to determine volume compression threshold", e);
        }
        return 0;
    }

    @Override
    public Blob finish() throws IOException, ServiceException {
        if (isFinished()) {
            return blob;
        }
        super.finish();
        return blob;
    }

    @Override
    public String toString() {
        return super.toString() + ", volume=" + getVolumeId();
    }
}
