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

import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import java.nio.ByteBuffer;

/**
 * OutputStream implementation for writing bytes to a MINA IoSession. The bytes
 * are buffered in order to minimize the number of MINA requests passed to the
 * I/O processor for output.
 */
public class MinaIoSessionOutputStream extends MinaOutputStream {
    private final IoSession mSession;
    private WriteFuture mFuture;

    /**
     * Creates a new output stream for writing output bytes using a specified
     * buffer size.
     *  
     * @param session the IoSession to which bytes are written
     * @param size the size of the output buffer
     */
    public MinaIoSessionOutputStream(IoSession session, int size) {
        super(size);
        mSession = session;
    }

    /**
     * Creates a new output stream for writing output bytes with a default
     * buffer size of 1024 bytes.
     * 
     * @param session the IoSession to which bytes are written
     */
    public MinaIoSessionOutputStream(IoSession session) {
        this(session, 1024);
    }

    @Override
    protected void flushBytes(ByteBuffer bb) {
        mFuture = mSession.write(MinaUtil.toMinaByteBuffer(bb));
    }

    @Override
    public boolean join(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout");
        }
        if (mFuture == null) return true;
        if (timeout > 0) {
            mFuture.join(timeout);
        } else {
            mFuture.join();
        }
        return mFuture.isWritten();
    }
}

