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

package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.lmtpserver.LmtpMessageInputStream;
import com.zimbra.cs.lmtpserver.MinaLmtpDataRequest;
import com.zimbra.cs.mina.LineBuffer;
import com.zimbra.cs.mina.MinaOutputStream;
import com.zimbra.cs.mina.MinaUtil;

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
        assertEquals(LINE, MinaUtil.toString(bb));
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
        lb.parse(MinaUtil.toByteBuffer(LINE));
        assertFalse(lb.isComplete());
        ByteBuffer bb = MinaUtil.toByteBuffer(CR+CRLF+LINE);
        lb.parse(bb);
        assertTrue(lb.isComplete());
        assertEquals(LINE, MinaUtil.toString(lb.getByteBuffer()));
        assertEquals(LINE.length(), bb.remaining());
    }

    public void testLineBuffer2() {
        LineBuffer lb = new LineBuffer();
        lb.parse(MinaUtil.toByteBuffer(LINE));
        lb.parse(MinaUtil.toByteBuffer(CR));
        lb.parse(MinaUtil.toByteBuffer(LF));
        assertTrue(lb.isComplete());
        assertEquals(LINE.length(), lb.toString().length());
    }
    
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
    
    private static ByteBuffer testData(int size) {
        ByteBuffer bb = ByteBuffer.allocate(size);
        for (int i = 0; i < size; i++) {
            bb.put((byte) i);
        }
        bb.flip();
        return bb;
    }

    private static final int CHUNK_SIZE = 16;

    public void testBigData() throws Exception {
        byte[] data = getBigData().getBytes();
        MinaLmtpDataRequest lmtpData = new MinaLmtpDataRequest();
        for (int off = 0; off < data.length; off += CHUNK_SIZE) {
            int len = Math.min(CHUNK_SIZE, data.length - off);
            ByteBuffer bb = ByteBuffer.wrap(data, off, len);
            lmtpData.parse(bb);
            assertFalse(lmtpData.isComplete());
            assertFalse(bb.hasRemaining());
        }
        lmtpData.parse(MinaUtil.toByteBuffer(EOM));
        assertTrue(lmtpData.isComplete());
    }

    private static String getBigData() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1024*1024; i++) {
            sb.append('0' + (i % 10));
            if (i % 80 == 0) sb.append(CRLF);
        }
        return sb.toString();
    }
    
    public void testMsgData1() throws Exception {
        testMsgData(MSG_1);
    }

    public void testMsgData2() throws Exception {
        testMsgData(MSG_2);
    }

    public void testMsgData3() throws Exception {
        testMsgData(MSG_3);
    }

    public void testMsgData4() throws Exception {
        testMsgData(MSG_4);
    }

    // Test MinaLmtpDataRequest for given input data. Test compares result
    // against what is produced by LmtpInputStream as a reference.
    private void testMsgData(String data) throws IOException {
        MinaLmtpDataRequest lmtpData = new MinaLmtpDataRequest();
        ByteBuffer bb = MinaUtil.toByteBuffer(data);
        lmtpData.parse(bb);
        assertTrue(lmtpData.isComplete());
        assertFalse(bb.hasRemaining());
        byte[] resultData = lmtpData.getBytes();
        byte[] refData = getLmtpInputStreamResult(data, null);
        assertEquals(refData, resultData);
    }

    private static void assertEquals(byte[] b1, byte[] b2) {
        assertEquals(b1.length, b2.length);
        for (int i = 0; i < b1.length; i++) {
            assertEquals("at index " + i, b1[i], b2[i]);
        }
    }

    // Return result bytes using LmtpInputStream as a reference
    private static byte[] getLmtpInputStreamResult(String data, String prefix)
            throws IOException {
        byte[] b = data.getBytes();
        // XXX bburtin: get rid of byte array
        LmtpMessageInputStream is = new LmtpMessageInputStream(new ByteArrayInputStream(b), prefix);
        int size = b.length;
        if (prefix != null) {
            size += prefix.length();
        }
        return ByteUtil.getContent(is, size);
    }
}
