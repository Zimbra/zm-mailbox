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
package com.zimbra.cs.store;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream which writes to a BlobBuilder
 *
 */
public class BlobBuilderOutputStream extends OutputStream {
    protected BlobBuilder blobBuilder;

    protected BlobBuilderOutputStream(BlobBuilder blobBuilder) {
        super();
        this.blobBuilder = blobBuilder;
    }

    @Override
    public void write(int b) throws IOException {
        // inefficient, but we don't expect this to be used
        byte[] tmp = new byte[1];
        tmp[0] = (byte) b;
        this.write(tmp);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        blobBuilder.append(b, off, len);
    }
}
