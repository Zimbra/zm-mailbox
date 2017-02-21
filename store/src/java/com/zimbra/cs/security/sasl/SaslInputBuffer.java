/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.security.sasl;

import javax.security.sasl.SaslException;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.ByteBuffer;

public class SaslInputBuffer {
    private final int mMaxSize;
    private final ByteBuffer mLenBuffer;
    private ByteBuffer mDataBuffer;

    public SaslInputBuffer(int maxSize) {
        mMaxSize = maxSize;
        mLenBuffer = ByteBuffer.allocate(4);
    }

    public void put(IoBuffer buf) throws SaslException {
        put(buf.buf());
    }

    public void put(ByteBuffer bb) throws SaslException {
        if (isComplete()) return;
        if (mLenBuffer.hasRemaining() && !readLength(bb)) return;
        int len = Math.min(mDataBuffer.remaining(), bb.remaining());
        int pos = mDataBuffer.position();
        bb.get(mDataBuffer.array(), pos, len);
        mDataBuffer.position(pos + len);
    }

    public boolean isComplete() {
        return mDataBuffer != null && !mDataBuffer.hasRemaining();
    }

    public int getLength() {
        return mDataBuffer != null ? mDataBuffer.limit() : -1;
    }

    public int getRemaining() {
        return mDataBuffer != null ? mDataBuffer.remaining() : -1;
    }

    public byte[] unwrap(SaslSecurityLayer securityLayer) throws SaslException {
        if (!isComplete()) {
            throw new IllegalStateException("input not complete");
        }
        return securityLayer.unwrap(mDataBuffer.array(), 0,
                                    mDataBuffer.position());
    }

    public void clear() {
        mLenBuffer.clear();
        if (mDataBuffer != null) mDataBuffer.clear();
    }

    private boolean readLength(ByteBuffer bb) throws SaslException {
        // Copy rest of length bytes
        while (mLenBuffer.hasRemaining()) {
            if (!bb.hasRemaining()) return false;
            mLenBuffer.put(bb.get());
        }
        int len = mLenBuffer.getInt(0);
        if (len < 0 || len > mMaxSize) {
            throw new SaslException(
                "Invalid receive buffer size '" + len + "' (max size = " +
                 mMaxSize + ")");
        }
        if (mDataBuffer == null || mDataBuffer.capacity() < len) {
            mDataBuffer = ByteBuffer.allocate(len);
        }
        mDataBuffer.limit(len);
        return true;
    }
}
