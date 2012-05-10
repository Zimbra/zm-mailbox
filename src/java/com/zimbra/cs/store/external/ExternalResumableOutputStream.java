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

import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.BlobBuilderOutputStream;

/**
 * OutputStream used to write to an external store during resumable upload.
 *
 */
public abstract class ExternalResumableOutputStream extends BlobBuilderOutputStream {

    protected ExternalResumableOutputStream(BlobBuilder blobBuilder) {
        super(blobBuilder);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeToExternal(b, off, len);
        super.write(b, off, len);
    }

    /**
     * Append data to remote upload location
     * @param b: byte array holding the data to upload
     * @param off: offset to start the upload from
     * @param len: length of the data to copy from the byte array
     * @throws IOException
     */
    protected abstract void writeToExternal(byte[] b, int off, int len) throws IOException;
}
