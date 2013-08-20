/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.server;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.google.common.base.Charsets;
import com.zimbra.common.util.ZimbraLog;

public final class NioOutputStream extends OutputStream {
    private final IoSession session;
    private IoBuffer buf;
    private int maxScheduledBytes;
    private int maxWritePause;

    NioOutputStream(IoSession session, int chunkSize, int maxScheduleBytes, int maxWritePause) {
        this.session = session;
        this.buf = IoBuffer.allocate(chunkSize);
        this.maxScheduledBytes = maxScheduleBytes;
        this.maxWritePause = maxWritePause;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        // If the request is larger than the capacity, flush the buffer and write it directly.
        if (len > buf.capacity()) {
            flush();
            writeToSession(IoBuffer.wrap(b, off, len));
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
            writeToSession(IoBuffer.allocate(len).putString(s, Charsets.UTF_8.newEncoder()).flip());
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
            writeToSession(buf);
            buf = IoBuffer.allocate(buf.capacity());
        }
    }

    private synchronized void writeToSession(Object output) throws IOException {
        long writeBytes = session.getScheduledWriteBytes();
        WriteFuture future = session.write(output);
        if (writeBytes > maxScheduledBytes) {
            ZimbraLog.nio.debug("IOSession has %d scheduled write bytes; waiting for buffer to catch up", writeBytes);
            long start = System.currentTimeMillis();
            if (maxWritePause > 0) {
                boolean done = future.awaitUninterruptibly(maxWritePause);
                if (!done) {
                    throw new IOException("Write stalled, client may have gone away");
                }
            } else {
                future.awaitUninterruptibly();
            }
            if (ZimbraLog.nio.isDebugEnabled()) {
                ZimbraLog.nio.debug("waited %d for %d scheduled bytes", (System.currentTimeMillis()-start), writeBytes);
                ZimbraLog.nio.debug("now have %d scheduled bytes, %d messages; %d written bytes %d messages", session.getScheduledWriteBytes(), session.getScheduledWriteMessages(), session.getWrittenBytes(), session.getWrittenMessages());
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        flush();
        buf.free();
        session.close(false);
    }
}
