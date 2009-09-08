/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * OutputStream implementation for writing bytes to a MINA IoSession. The bytes
 * are buffered in order to minimize the number of MINA requests passed to the
 * I/O processor for output.
 */
public class MinaIoSessionOutputStream extends MinaOutputStream {
    private final IoSession session;
    private WriteFuture lowWatermarkFuture;
    private WriteFuture lastFuture;
    private int lowWatermark;
    private int highWatermark;
    private long timeout;

    /**
     * Creates a new output stream for writing output bytes using a specified
     * buffer size.
     *  
     * @param session the IoSession to which bytes are written
     * @param size the size of the output buffer
     */
    public MinaIoSessionOutputStream(IoSession session, int size) {
        super(size);
        this.session = session;
    }

    /**
     * Creates a new output stream for writing output bytes with a default
     * buffer size of 8192 bytes.
     *
     * @param session the IoSession to which bytes are written
     */
    public MinaIoSessionOutputStream(IoSession session) {
        this(session, 8192);
    }

    public MinaIoSessionOutputStream setLowWatermark(int lowWatermark) {
        if (lowWatermark > highWatermark) {
            throw new IllegalArgumentException();
        }
        this.lowWatermark = lowWatermark;
        return this;
    }

    public MinaIoSessionOutputStream setHighWatermark(int highWatermark) {
        if (highWatermark < lowWatermark) {
            throw new IllegalArgumentException();
        }
        this.highWatermark = highWatermark;
        return this;
    }

    public MinaIoSessionOutputStream setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    protected void flushBytes(ByteBuffer bb) throws IOException {
        if (highWatermark > 0 && session.getScheduledWriteBytes() > highWatermark) {
            // Wait up to set timeout for pending writes to fall below low watermark
            if (!join(lowWatermarkFuture, timeout)) {
                throw new IOException("Max write queue size exceeded - closing connection");
            }
            lowWatermarkFuture = null;
        }
        lastFuture = session.write(MinaUtil.toMinaByteBuffer(bb));
        if (lowWatermark > 0 && session.getScheduledWriteBytes() > lowWatermark) {
            lowWatermarkFuture = lastFuture;
        }
    }

    public boolean join() {
        return join(timeout);
    }
    
    @Override
    public boolean join(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout");
        }
        return join(lastFuture, timeout);
    }

    private boolean join(WriteFuture future, long timeout) {
        if (future == null) {
            return true;
        }
        if (timeout > 0) {
            future.join(timeout);
        } else {
            future.join();
        }
        return future.isWritten();
    }
}

