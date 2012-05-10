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
