/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.store.external;

import java.io.IOException;

import com.zimbra.cs.store.Blob;

/**
 * Blob which has been streamed to a remote store while also being written to local incoming cache. Used during streaming upload to optimize the stage operation.
 */
public class ExternalUploadedBlob extends Blob {
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
