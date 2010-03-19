/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.mina.LineBuffer;
import com.zimbra.cs.mina.MinaOutputStream;
import com.zimbra.cs.mina.MinaUtil;
import com.zimbra.cs.imap.LiteralInfo;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class TestMina extends TestCase {
    private static final String LINE = "This is a line";
    private static final String CR = "\r";
    private static final String LF = "\n";
    private static final String CRLF = CR + LF;
    private static final String EOM = CRLF + "." + CRLF;

    private static final int BIG_DATA_SIZE = 64 * 1024;

    private static final String MSG_1 = "01234" + CRLF + "0123456789" + EOM;
    private static final String MSG_2 =
        "01234" + CRLF + "..foo" + ".0123456789" + EOM;
    private static final String MSG_3 = "." + CRLF;
    private static final String MSG_4 = EOM;

    public void testExpand() {
        ByteBuffer bb = ByteBuffer.allocate(100);
        bb.position(99);
        bb = MinaUtil.expand(bb, 1001);
        assertTrue(bb.remaining() >= 1001);
    }

    public void testExpandBigData() {
        ByteBuffer bb = ByteBuffer.allocate(10);
        for (int i = 0; i < BIG_DATA_SIZE; i++) {
            bb = MinaUtil.expand(bb, 1);
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
        assertEquals(LINE, MinaUtil.toAsciiString(bb));
    }

    public void testGetBytes() {
        byte[] b = new byte[100];
        ByteBuffer bb = ByteBuffer.wrap(b);
        assertEquals(b, MinaUtil.getBytes(bb));
        assertNotSame(b, MinaUtil.getBytes(bb.put((byte) 1)));
        bb = ByteBuffer.wrap(b, 1, b.length - 1);
        assertNotSame(b, MinaUtil.getBytes(bb));
    }
    
    public void testLineBuffer() {
        LineBuffer lb = new LineBuffer();
        assertFalse(lb.isComplete());
        lb.parse(MinaUtil.toAsciiBytes(LINE));
        assertFalse(lb.isComplete());
        ByteBuffer bb = MinaUtil.toAsciiBytes(CRLF+LINE);
        lb.parse(bb);
        assertTrue(lb.isComplete());
        assertEquals(LINE+CRLF, lb.toString());
        assertEquals(LINE.length(), bb.remaining());
        lb = new LineBuffer();
        for (int i = 0; i < LINE.length(); i++) {
            lb.parse(ByteBuffer.wrap(new byte[] { (byte) LINE.charAt(i) }));
            assertFalse(lb.isComplete());
        }
        lb.parse(MinaUtil.toAsciiBytes(CRLF));
        assertTrue(lb.isComplete());
        assertEquals(LINE+CRLF, lb.toString());
    }

    public void testLineBuffer2() {
        LineBuffer lb = new LineBuffer();
        lb.parse(MinaUtil.toAsciiBytes(LINE));
        lb.parse(MinaUtil.toAsciiBytes(CR));
        lb.parse(MinaUtil.toAsciiBytes(LF));
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

    /*
    public void testMinaOutputStream() throws IOException {
        TestMinaOutputStream tos = new TestMinaOutputStream();
        ByteBuffer data = testData(BIG_DATA_SIZE);
        byte[] b = new byte[BIG_DATA_SIZE];
        int len = 1;
        while (data.hasRemaining()) {
            data.get(b, 0, len);
            tos.write(b, 0, len);
            len = Math.min(len * 2, data.remaining());
        }
        tos.close();
        assertEquals(data.position(), tos.buf.position());
        for (int i = 0; i < BIG_DATA_SIZE; i++) {
            assertEquals("pos=" + i, data.get(i), tos.buf.get(i));
        }
    }


    private static class TestMinaOutputStream extends MinaOutputStream {
        ByteBuffer buf;
        TestMinaOutputStream() { super(23); }
        public void flushBytes(ByteBuffer bb) {
            buf = MinaUtil.expand(buf, bb.remaining()).put(bb);
        }
        public boolean join(long ms) {
            return true;
        }
    }

    */
    
    private static ByteBuffer testData(int size) {
        ByteBuffer bb = ByteBuffer.allocate(size);
        for (int i = 0; i < size; i++) {
            bb.put((byte) i);
        }
        bb.flip();
        return bb;
    }

    public void testTaskUtil1() throws Exception {
        final long timeout = 100;
        try {
            TaskUtil.call(new Callable<Object>() {
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
