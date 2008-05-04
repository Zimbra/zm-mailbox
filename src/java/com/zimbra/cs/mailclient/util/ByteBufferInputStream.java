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
