/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.nio.ByteBuffer;

public class SaslInputBuffer {
    private final ByteBuffer mLenBuffer;
    private ByteBuffer mDataBuffer;

    public SaslInputBuffer() {
        mLenBuffer = ByteBuffer.allocate(4);
    }
    
    public void read(org.apache.mina.common.ByteBuffer bb)
            throws SaslException {
        read(bb.buf());
    }
    
    public void read(ByteBuffer bb) throws SaslException {
        if (isComplete()) return;
        if (mLenBuffer.hasRemaining() && !parseLength(bb)) return;
        int len = Math.min(mDataBuffer.remaining(), bb.remaining());
        int pos = mDataBuffer.position();
        bb.get(mDataBuffer.array(), pos, len);
        mDataBuffer.position(pos + len);
    }

    public boolean isComplete() {
        return mDataBuffer != null && !mDataBuffer.hasRemaining();
    }

    public byte[] unwrap(SaslServer server) throws SaslException {
        if (!isComplete()) {
            throw new IllegalStateException("Buffer is not complete");
        }
        return server.unwrap(mDataBuffer.array(), 0,
                             mDataBuffer.position());
    }

    public void clear() {
        mLenBuffer.clear();
        if (mDataBuffer != null) mDataBuffer.clear();
    }
    
    private boolean parseLength(ByteBuffer bb) throws SaslException {
        // Copy rest of length bytes
        while (mLenBuffer.hasRemaining()) {
            if (!bb.hasRemaining()) return false;
            mLenBuffer.put(bb.get());
        }
        int len = mLenBuffer.getInt(0);
        if (len < 0) {
            throw new SaslException(
                "Invalid length " + len + " in received buffer");
        }
        if (mDataBuffer == null || mDataBuffer.capacity() < len) {
            mDataBuffer = ByteBuffer.allocate(len);
        }
        return true;
    }
}
