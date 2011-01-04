/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import com.zimbra.common.util.TaskUtil;
import com.zimbra.cs.tcpserver.NioLineBuffer;
import com.zimbra.cs.tcpserver.NioUtil;
import com.zimbra.cs.imap.LiteralInfo;
import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class TestNioTcpServer extends TestCase {
    private static final String LINE = "This is a line";
    private static final String CR = "\r";
    private static final String LF = "\n";
    private static final String CRLF = CR + LF;

    private static final int BIG_DATA_SIZE = 64 * 1024;

    public void testExpand() {
        ByteBuffer bb = ByteBuffer.allocate(100);
        bb.position(99);
        bb = NioUtil.expand(bb, 1001);
        assertTrue(bb.remaining() >= 1001);
    }

    public void testExpandBigData() {
        ByteBuffer bb = ByteBuffer.allocate(10);
        for (int i = 0; i < BIG_DATA_SIZE; i++) {
            bb = NioUtil.expand(bb, 1);
            assertTrue(bb.hasRemaining());
            bb.put((byte) i);
        }
        assertEquals(BIG_DATA_SIZE, bb.position());
        bb.flip();
        for (int i = 0; i < BIG_DATA_SIZE; i++) {
            assertEquals((byte) i, bb.get());
        }
    }

    public void testToString() {
        ByteBuffer bb = ByteBuffer.wrap(LINE.getBytes());
        assertEquals(LINE, NioUtil.toAsciiString(bb));
    }

    public void testGetBytes() {
        byte[] b = new byte[100];
        ByteBuffer bb = ByteBuffer.wrap(b);
        assertEquals(b, NioUtil.getBytes(bb));
        assertNotSame(b, NioUtil.getBytes(bb.put((byte) 1)));
        bb = ByteBuffer.wrap(b, 1, b.length - 1);
        assertNotSame(b, NioUtil.getBytes(bb));
    }

    public void testLineBuffer() {
        NioLineBuffer lb = new NioLineBuffer();
        assertFalse(lb.isComplete());
        lb.parse(NioUtil.toAsciiBytes(LINE));
        assertFalse(lb.isComplete());
        ByteBuffer bb = NioUtil.toAsciiBytes(CRLF+LINE);
        lb.parse(bb);
        assertTrue(lb.isComplete());
        assertEquals(LINE+CRLF, lb.toString());
        assertEquals(LINE.length(), bb.remaining());
        lb = new NioLineBuffer();
        for (int i = 0; i < LINE.length(); i++) {
            lb.parse(ByteBuffer.wrap(new byte[] { (byte) LINE.charAt(i) }));
            assertFalse(lb.isComplete());
        }
        lb.parse(NioUtil.toAsciiBytes(CRLF));
        assertTrue(lb.isComplete());
        assertEquals(LINE+CRLF, lb.toString());
    }

    public void testLineBuffer2() {
        NioLineBuffer lb = new NioLineBuffer();
        lb.parse(NioUtil.toAsciiBytes(LINE));
        lb.parse(NioUtil.toAsciiBytes(CR));
        lb.parse(NioUtil.toAsciiBytes(LF));
        assertTrue(lb.isComplete());
        assertEquals(LINE+CRLF, lb.toString());
    }

    public void testLiteralInfo() throws Exception {
        LiteralInfo li = LiteralInfo.parse(". append {10+}");
        assertNotNull(li);
        assertEquals(10, li.getCount());
        assertFalse(li.isBlocking());
        assertNull(LiteralInfo.parse(". login foo bar}"));
        try {
            LiteralInfo.parse(". append {-10}");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testTaskUtil1() throws Exception {
        final long timeout = 100;
        try {
            TaskUtil.call(new Callable<Object>() {
                @Override
                public Object call() throws InterruptedException {
                    Thread.sleep(timeout * 4);
                    return null;
                }
            }, timeout);
        } catch (TimeoutException e) {
            return;
        }
        throw new AssertionError("Task should have timed out");
    }

    public void testTaskUtil2() throws Exception {
        final long timeout = 500;
        try {
            TaskUtil.call(new Callable<Object>() {
                @Override
                public Object call() throws InterruptedException {
                    return null;
                }
            }, timeout);
        } catch (TimeoutException e) {
            throw new AssertionError("Task should not have timed out");
        }
    }

    private static void assertEquals(byte[] b1, byte[] b2) {
        assertEquals(b1.length, b2.length);
        for (int i = 0; i < b1.length; i++) {
            assertEquals("at index " + i, b1[i], b2[i]);
        }
    }
}
