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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A data structure that gathers data from multiple ByteBuffer objectsinto a
 * single byte array. Synchronization is entirely upto the user of this class -
 * concurrent adds will cause severe internal damage.
 */
public final class OzByteBufferGatherer {

    private byte[] mBuffer;
    private int mPosition;
    private int mLimit;
    private boolean mOverflowed;
    
    /**
     * Create a gatherer of specified initial size and limit.
     * 
     * @param limit
     *            After limit number of bytes are added, the rest are discarded
     *            and the overflow bit is toggled.
     */
    public OzByteBufferGatherer(int initialSize, int limit) {
        mBuffer = new byte[initialSize];
        mPosition = 0;
        mOverflowed = false;
        
        if (mLimit < 0) {
            throw new IllegalArgumentException("can not specify negative limit");
        }
        mLimit = limit;
    }
    
    public void add(ByteBuffer buffer) {
        System.out.println("new bytes=" + OzUtil.toString(buffer));
        int numNewBytes = buffer.remaining();

        if ((mPosition + numNewBytes) > mLimit) {
            mOverflowed = true;
            numNewBytes = mLimit - mPosition;
        }
        ensureCapacity(numNewBytes);
        buffer.get(mBuffer, mPosition, numNewBytes);
        if (buffer.hasRemaining()) {
            // discard whatever is left - this is the stuff that
            // has overflown.
            buffer.position(buffer.limit());
        }
        System.out.println("after read=" + OzUtil.toString(buffer));
        mPosition += numNewBytes;
    }
    
    public static void main(String[] args) {
        OzByteBufferGatherer bbg = new OzByteBufferGatherer(4, 8);
        bbg.add(ByteBuffer.allocate(4));
        bbg.add(ByteBuffer.allocate(4));
        bbg.add(ByteBuffer.allocate(4));
    }
    
    private void ensureCapacity(int forThisManyMore) {
        assert(mPosition <= mBuffer.length);
        int spaceLeft = mBuffer.length - mPosition;
        if (forThisManyMore > spaceLeft) {
            byte newbuf[] = new byte[Math.max(mBuffer.length << 1, (mPosition+forThisManyMore))];
            System.arraycopy(mBuffer, 0, newbuf, 0, mPosition);
            mBuffer= newbuf;
        }
    }
    
    /**
     * @return an InputStream view of this buffer
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(mBuffer, 0, mPosition);
    }
    
    public byte[] array() {
        return mBuffer;
    }
    
    public byte get(int i) {
        return mBuffer[i];
    }
    
    public String toAsciiString() {
        StringBuilder sb = new StringBuilder(mPosition);
        for (int i = 0; i < mPosition; i++) {
            sb.append((char)mBuffer[i]);
        }
        return sb.toString();
    }

    public boolean overflowed() {
        return mOverflowed;
    }
    
    public int size() {
        return mPosition;
    }
    
    public void clear() {
        mPosition = 0;
        mOverflowed = false;
    }

    public void limit(int newLimit) {
        mLimit = newLimit;
    }
    
    public int limit() {
        return mLimit;
    }
    
    public void trim(int n) {
        if (n > mPosition) {
            throw new IllegalArgumentException("Can not trim " + n + " bytes from " + mPosition + " total bytes");
        }
        mPosition -= n;
    }
}
