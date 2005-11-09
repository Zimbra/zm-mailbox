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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

/**
 * Build ByteBuffers into a single byte array - synchronization is entirely upto
 * the user of this class - two concurrent adds will blow up.
 */
public class OzByteBufferGatherer {

    private byte[] mBuffer;
    private int mPosition;

    public OzByteBufferGatherer() {
        this(32);
    }
    
    public OzByteBufferGatherer(int initialSize) {
        mBuffer = new byte[initialSize];
        mPosition = 0;
    }
    
    public void add(ByteBuffer buffer) {
        int remaining = buffer.remaining();
        ensureCapacity(remaining);
        buffer.get(mBuffer, mPosition, remaining);
        mPosition += remaining;
    }
    
    private void ensureCapacity(int forThisManyMore) {
        assert(mPosition <= mBuffer.length);
        int spaceLeft = mBuffer.length - mPosition;
        if (forThisManyMore > spaceLeft) {
            byte newbuf[] = new byte[Math.max(mBuffer.length << 1, forThisManyMore)];
            System.arraycopy(mBuffer, 0, newbuf, 0, mPosition);
            mBuffer= newbuf;
        }
    }
    
    public byte[] array() {
        return mBuffer;
    }
    
    public int size() {
        return mPosition;
    }
}
