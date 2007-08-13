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

package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.mina.MinaUtils;

import java.nio.ByteBuffer;

public class TestMinaUtils extends TestCase {
    private static final String CR = "\r";
    private static final String CRLF = "\r\n";
    private static final String LINE_1 = "This is line 1.";
    private static final String LINE_2 = "This is line 2.";

    private static final int SIZE = 64 * 1024;
    
    public void testExpand() {
        ByteBuffer bb = ByteBuffer.allocate(10);
        for (int i = 0; i < SIZE; i++) {
            bb = MinaUtils.expand(bb, 1);
            bb.put((byte) i);
        }
        assertEquals(SIZE, bb.position());
        bb.flip();
        for (int i = 0; i < SIZE; i++) {
            assertEquals((byte) i, bb.get());
        }
    }

    public void testGetLine() {
        assertNull(MinaUtils.getLine(toByteBuffer(LINE_1)));
        assertEquals(LINE_1, MinaUtils.getLine(toByteBuffer(LINE_1 + CRLF)));
        assertEquals("", MinaUtils.getLine(toByteBuffer(CRLF)));
        assertEquals("", MinaUtils.getLine(toByteBuffer(CR + CR + CRLF)));
    }
    
    public void testCopyLine() {
        ByteBuffer bb = MinaUtils.copyLine(null, toByteBuffer(LINE_1));


    }
    private ByteBuffer toByteBuffer(String s) {
        return ByteBuffer.wrap(s.getBytes());
    }

    private String toString(ByteBuffer bb) {
        byte[] b = new byte[bb.remaining()];
        bb.get(b, 0, b.length);
        return new String(b);
    }
}
