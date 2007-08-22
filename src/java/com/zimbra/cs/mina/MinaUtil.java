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

import java.nio.ByteBuffer;

public final class MinaUtil {
    public static final byte CR = '\r';
    public static final byte LF = '\n';
    public static final byte DOT = '.';

    public static final int INITIAL_CAPACITY = 32;

    /**
     * Ensure that specified buffer has at least enough capacity to accomodate
     * 'minSize' additional bytes, but not more than 'maxSize' additional bytes.
     * If 'maxSize' is 0, then there is no limit.
     * 
     * @param bb the ByteBuffer to expand, or null to create a new one
     * @param minSize minimum additional capacity for resulting byte buffer
     * @param maxSize maximum additional capacity, or -1 for no maximum
     * @return the resulting, possible expanded, ByteBuffer
     */
    public static ByteBuffer expand(ByteBuffer bb, int minSize, int maxSize) {
        if (maxSize != -1 && maxSize < minSize) {
            throw new IllegalArgumentException("maxSize < minSize");
        }
        if (bb == null) {
            int size = Math.max(minSize, INITIAL_CAPACITY);
            if (maxSize != -1 && size > maxSize) size = maxSize;
            return ByteBuffer.allocate(size);
        }
        if (bb.remaining() >= minSize) return bb;
        int capacity = Math.max((bb.capacity() * 3) / 2 + 1,
                                bb.position() + minSize);
        if (maxSize != -1) {
            capacity = Math.max(capacity, bb.position() + maxSize);
        }
        ByteBuffer tmp = ByteBuffer.allocate(capacity);
        bb.flip();
        return tmp.put(bb);
    }

    public static ByteBuffer expand(ByteBuffer bb, int minSize) {
        return expand(bb, minSize, -1);
    }
    
    public static String toString(ByteBuffer bb) {
        int len = bb.remaining();
        char[] cs = new char[len];
        for (int i = 0; i < len; i++) {
            cs[i] = (char) ((int) bb.get(i) & 0xff);
        }
        return new String(cs);
    }

    public static ByteBuffer toByteBuffer(String s) {
        return put(ByteBuffer.allocate(s.length()), s);
    }
    
    public static ByteBuffer put(ByteBuffer bb, String s) {
        bb = expand(bb, s.length());
        for (int i = 0; i < s.length(); i++) {
            bb.put(i, (byte) s.charAt(i));
        }
        return bb;
    }

    public static byte[] getBytes(ByteBuffer bb) {
        if (bb.hasArray() && bb.arrayOffset() == 0 && bb.position() == 0) {
            byte[] b = bb.array();
            if (b.length == bb.limit()) return b;
        }
        byte[] b = new byte[bb.limit() - bb.position()];
        bb.duplicate().get(b);
        return b;
    }
}
