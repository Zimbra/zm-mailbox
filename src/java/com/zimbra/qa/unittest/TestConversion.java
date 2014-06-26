/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.File;

import junit.framework.TestCase;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessage.ZMimePart;

public class TestConversion extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestConversion.class.getSimpleName();

    @Override
    public void setUp()
    throws Exception {
        cleanUp();
    }

    /**
     * Tests downloading attachments from a TNEF message (bug 44263).
     */
    public void testTnef()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // Add the TNEF message
        String msgContent = new String(ByteUtil.getContent(new File(
            LC.zimbra_home.value() + "/unittest/tnef.msg")));
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgContent);

        // Test downloading attachments.
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + NAME_PREFIX + " Rich text (TNEF) test\"");
        byte[] data = TestUtil.getContent(mbox, msg.getId(), "upload.gif");
        assertEquals(73, data.length);
        data = TestUtil.getContent(mbox, msg.getId(), "upload2.gif");
        assertEquals(851, data.length);

        ZMimePart part = TestUtil.getPart(msg, "upload.gif");
        checkPartSize(73, part.getSize());
        part = TestUtil.getPart(msg, "upload2.gif");
        checkPartSize(851, part.getSize());
    }

    /**
     * The part size is calculated from the base64 content, so it may be off by a few bytes.
     */
    private void checkPartSize(long expected, long actual) {
        assertTrue("expected " + expected + " +/- 4 bytes, got " + actual, Math.abs(expected - actual) <= 4);
    }

    @Override
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestConversion.class);
    }
}
