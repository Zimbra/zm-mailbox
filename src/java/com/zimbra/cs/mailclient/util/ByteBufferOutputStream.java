/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.util;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer buf;

    public ByteBufferOutputStream(int size) {
        buf = ByteBuffer.allocate(size);
    }

    public ByteBufferOutputStream() {
        this(32);
    }

    public void write(int b) throws IOException {
        ensureCapacity(1);
        buf.put((byte) b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }
        if (off < 0 || off > b.length || len < 0 ||
            off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(len);
        buf.put(b, off, len);
    }

    private void ensureCapacity(int count) throws IOException {
        if (buf.remaining() < count) {
            int size = Math.max(buf.capacity() * 2, buf.capacity() + count);
            if (size < 0) {
                throw new IOException("buffer limit exceeded");
            }
            ByteBuffer tmp = ByteBuffer.allocate(size);
            buf.flip();
            tmp.put(buf);
            buf = tmp;
        }
    }

    public ByteBuffer getByteBuffer() {
        return buf;
    }
}
