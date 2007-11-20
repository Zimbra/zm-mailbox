/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.mina;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A buffered output stream for writing data as a sequence of ByteBuffer
 * objects. Subclasses override the flushBytes() method to specify how
 * the ByteBuffer should be written.
 */
public abstract class MinaOutputStream extends OutputStream {
    private final int mSize;
    private ByteBuffer mBuffer;
    private boolean mClosed;

    /**
     * Creates a new output stream using the specified buffer size.
     * 
     * @param size the size of the output buffer.
     */
    public MinaOutputStream(int size) {
        mSize = size;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len)
            throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (mClosed) throw new IOException("Stream has been closed");
        while (len > 0) {
            if (mBuffer == null) {
                mBuffer = ByteBuffer.allocate(Math.max(len, mSize));
            }
            int count = Math.min(len, mBuffer.remaining());
            mBuffer.put(b, off, count);
            if (!mBuffer.hasRemaining()) flushBytes();
            len -= count;
            off += count;
        }
    }

    public synchronized void write(String s) throws IOException {
        if (mClosed) throw new IOException("Stream has been closed");
        int off = 0;
        int len = s.length();
        while (len > 0) {
            if (mBuffer == null) {
                mBuffer = ByteBuffer.allocate(Math.max(len, mSize));
            }
            int count = Math.min(len, mBuffer.remaining());
            len -= count;
            while (count-- > 0) {
                mBuffer.put((byte) s.charAt(off++));
            }
            if (!mBuffer.hasRemaining()) flushBytes();
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    @Override
    public synchronized void flush() throws IOException {
        if (!mClosed) flushBytes();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!mClosed) {
            flushBytes();
            mClosed = true;
        }
    }

    private void flushBytes() throws IOException {
        if (mBuffer == null || mBuffer.position() <= 0) return;
        mBuffer.flip();
        flushBytes(mBuffer);
        mBuffer = null;
    }

    /**
     * Flushes specified bytes. Implementation can assume that byte buffer will
     * not be subsequently reused, so this is safe for asynchronous writes.
     *
     * @param bb the bytes to be written
     * @throws IOException if an I/O error occurs
     */
    protected abstract void flushBytes(ByteBuffer bb) throws IOException;

    /**
     * Waits up to 'timeout' milliseconds for all remaining bytes to be
     * written. Typically called after close() to make sure that all data
     * has been written to the output stream. Return true if successful
     * otherwise false if timeout was reached before last write was complete,
     * in which case the method can be called again if desired.
     *
     * @param timeout max number of milliseconds to wait, or 0 to wait forever
     * @return true if all bytes have been written, false otherwise
     */
    public abstract boolean join(long timeout);
}
