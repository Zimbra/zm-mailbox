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

public class SaslOutputBuffer {
    private final int mMaxSize;
    private ByteBuffer mBuffer;

    private static final int MINSIZE = 512;
    
    public SaslOutputBuffer(int maxSize) {
        this(Math.min(MINSIZE, maxSize), maxSize);
    }
    
    public SaslOutputBuffer(int minSize, int maxSize) {
        if (minSize > maxSize) {
            throw new IllegalArgumentException("minSize > maxSize");
        }
        mBuffer = ByteBuffer.allocate(minSize);
        mMaxSize = maxSize;
    }

    public void write(org.apache.mina.common.ByteBuffer bb) {
        write(bb.buf());
    }
    
    public void write(ByteBuffer bb) {
        if (isFull()) return;
        if (bb.remaining() > mBuffer.remaining()) {
            int minSize = Math.min(bb.remaining(), mMaxSize);
            mBuffer = MinaUtil.expand(mBuffer, minSize, mMaxSize);
        }
        int len = Math.min(mBuffer.remaining(), bb.remaining());
        int pos = mBuffer.position();
        bb.get(mBuffer.array(), pos, len);
        mBuffer.position(pos + len);
    }

    public boolean isFull() {
        return mBuffer.position() >= mMaxSize;
    }

    public byte[] wrap(SaslServer server) throws SaslException {
        return server.wrap(mBuffer.array(), 0, mBuffer.position());
    }

    public void clear() {
        mBuffer.clear();
    }
}
