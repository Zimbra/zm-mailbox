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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import junit.framework.TestCase;
import com.zimbra.cs.lmtpserver.utils.LmtpData;
import com.zimbra.cs.lmtpserver.LmtpInputStream;

import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TestLmtpData extends TestCase {
    private static final String CRLF = "\r\n";
    private static final String EOM = CRLF + "." + CRLF;

    private static final String DATA_1 =
        "01234" + CRLF + "0123456789" + EOM;
    private static final String DATA_2 =
        "01234" + CRLF + "..foo" + ".0123456789" + EOM;
    private static final String DATA_3 =
        "." + CRLF;
    private static final String DATA_4 = EOM;

    public void testData1() throws Exception {
        testData(DATA_1);
    }

    public void testData2() throws Exception {
        testData(DATA_2);
    }

    public void testData3() throws Exception {
        testData(DATA_3);
    }

    public void testData4() throws Exception {
        testData(DATA_4);
    }

    private static final int CHUNK_SIZE = 16;
    
    public void testBigData() throws Exception {
        byte[] data = getBigData().getBytes();
        LmtpData lmtpData = new LmtpData();
        for (int off = 0; off < data.length; off += CHUNK_SIZE) {
            int len = Math.min(CHUNK_SIZE, data.length - off);
            ByteBuffer bb = ByteBuffer.wrap(data, off, len);
            lmtpData.parse(bb);
            assertFalse(lmtpData.isComplete());
            assertFalse(bb.hasRemaining());
        }
        lmtpData.parse(getByteBuffer(EOM));
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
    
    // Test LmtpData for given input data. Test compares result against
    // what is produced by LmtpInputStream as a reference.
    private void testData(String data) throws IOException {
        LmtpData lmtpData = new LmtpData();
        ByteBuffer bb = getByteBuffer(data);
        lmtpData.parse(bb);
        assertTrue(lmtpData.isComplete());
        assertFalse(bb.hasRemaining());
        byte[] resultData = lmtpData.getBytes();
        byte[] refData = getLmtpInputStreamResult(data, null);
        assertEquals(refData, resultData);
    }
    
    private static ByteBuffer getByteBuffer(String s) {
        return ByteBuffer.wrap(s.getBytes());
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
        LmtpInputStream is = new LmtpInputStream(new ByteArrayInputStream(b));
        return is.readMessage(b.length, prefix);
    }
}
