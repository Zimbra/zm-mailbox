/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.external;

import java.io.IOException;

import com.zimbra.cs.store.Blob;

/**
 * Blob which has been streamed to a remote store while also being written to local incoming cache. Used during streaming upload to optimize the stage operation.
 */
public class ExternalUploadedBlob extends ExternalBlob {
    protected final String uploadId;

    /**
     * Create a new ExternalUploadedBlob from data which was written directly to remote server during upload
     * @param blob: The local Blob which was created inline with upload
     * @param uploadId: The remote system's identifier for the upload
     * @throws IOException
     */
    protected ExternalUploadedBlob(Blob blob, String uploadId) throws IOException {
        super(blob.getFile(), blob.getRawSize(), blob.getDigest());
        this.uploadId = uploadId;
    }

    /**
     * @return the remote system's identifier for this uploaded blob
     */
    public String getUploadId() {
        return uploadId;
    }
}
