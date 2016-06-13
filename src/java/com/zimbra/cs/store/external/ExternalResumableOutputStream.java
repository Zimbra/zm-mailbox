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
