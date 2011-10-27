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
    private IoBuffer buf;

    NioOutputStream(IoSession session, int chunkSize) {
        this.session = session;
        this.buf = IoBuffer.allocate(chunkSize);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        // If the request is larger than the capacity, flush the buffer and write it directly.
        if (len > buf.capacity()) {
            flush();
            session.write(IoBuffer.wrap(b, off, len));
        } else {
            if (len > buf.remaining()) { // If not enough space left, flush the buffer first.
                flush();
            }
            buf.put(b, off, len);
        }
    }

    public synchronized void write(String s) throws IOException {
        int len = s.length();
        // If the request is larger than the capacity, flush the buffer and write it directly.
        if (len > buf.capacity()) {
            flush();
            session.write(IoBuffer.allocate(len).putString(s, Charsets.UTF_8.newEncoder()).flip());
        } else {
            if (len > buf.remaining()) { // If not enough space left, flush the buffer first.
                flush();
            }
            buf.putString(s, Charsets.UTF_8.newEncoder());
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (!buf.hasRemaining()) { // If not enough space left, flush the buffer first.
            flush();
        }
        buf.put((byte) b);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (buf.position() > 0) {
            buf.flip();
            session.write(buf);
            buf = IoBuffer.allocate(buf.capacity());
        }
    }

    @Override
    public synchronized void close() throws IOException {
        flush();
        buf.free();
        session.close(false);
    }
}
