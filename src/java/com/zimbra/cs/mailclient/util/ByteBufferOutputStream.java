/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
