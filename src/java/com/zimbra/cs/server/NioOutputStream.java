/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.server;

import com.google.common.base.Charsets;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

public final class NioOutputStream extends OutputStream {
    private final IoSession session;
    private final int chunkSize;
    private IoBuffer buf;

    public NioOutputStream(IoSession session, int chunkSize) {
        this.session = session;
        this.chunkSize = chunkSize;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        while (len > 0) {
            if (buf == null) {
                buf = IoBuffer.allocate(chunkSize);
            }
            int count = Math.min(len, buf.remaining());
            buf.put(b, off, count);
            if (!buf.hasRemaining()) {
                flush();
            }
            len -= count;
            off += count;
        }
    }

    public void write(String s) throws IOException {
        assert s.length() <= chunkSize : s;
        if (buf == null) {
            buf = IoBuffer.allocate(chunkSize);
        } else if (buf.remaining() < s.length()) {
            flush();
            buf = IoBuffer.allocate(chunkSize);
        }
        buf.putString(s, Charsets.UTF_8.newEncoder());
    }

    @Override
    public void write(int b) throws IOException {
        if (buf == null) {
            buf = IoBuffer.allocate(chunkSize);
        }
        buf.put((byte) b);
    }

    @Override
    public void flush() throws IOException {
        if (buf != null && buf.position() > 0) {
            buf.flip();
            session.write(buf);
            buf = null;
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        if (buf != null) {
            buf.free();
            buf = null;
        }
        session.close(false);
    }
}
