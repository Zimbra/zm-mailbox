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

import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buf;

    public ByteBufferInputStream(ByteBuffer bb) {
        buf = bb.duplicate();
        buf.rewind().mark();
    }

    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || off > b.length || len < 0 ||
            off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (buf.remaining() == 0) {
            return -1;
        }
        if (len > buf.remaining()) {
            len = buf.remaining();
        }
        buf.get(b, off, len);
        return len;
    }

    public int read() {
        if (buf.remaining() == 0) {
            return -1;
        }
        return (int) buf.get() & 0xff;
    }

    public long skip(long n) {
        if (n < 0) {
            return 0;
        }
        if (n > buf.remaining()) {
            n = buf.remaining();
        }
        buf.position(buf.position() + (int) n);
        return n;
    }

    public int available() {
        return buf.remaining();
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int limit) {
        buf.mark();
    }

    public void reset() {
        buf.reset();
    }
}
