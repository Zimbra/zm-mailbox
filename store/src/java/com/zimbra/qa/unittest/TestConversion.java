/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.File;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessage.ZMimePart;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;

public class TestConversion {

    @Rule
    public TestName testInfo = new TestName();
    private String testName = null;
    private String USER_NAME = null;
    private static final String NAME_PREFIX = TestConversion.class.getSimpleName();

    @Before
    public void setUp() throws Exception {
        testName = testInfo.getMethodName();
        USER_NAME = NAME_PREFIX + "-" + testName + "-user";
        TestUtil.createAccount(USER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }

    /**
     * Tests downloading attachments from a TNEF message (bug 44263).
     */
    @Test
    public void downloadAttachmentsFromTNEFmsg()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String msgName = "/unittest/tnef.msg";
        File tnefMsg = new File(LC.zimbra_home.value() + msgName);
        Assert.assertTrue(String.format("To run this test copy data%1$s to /opt/zimbra%1$s", msgName),
                tnefMsg.exists() && tnefMsg.canRead());

        // Add the TNEF message
        String msgContent = new String(ByteUtil.getContent(tnefMsg));
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgContent);

        // Test downloading attachments.
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"" + NAME_PREFIX + " Rich text (TNEF) test\"");
        byte[] data = TestUtil.getContent(mbox, msg.getId(), "upload.gif");
        Assert.assertEquals(73, data.length);
        data = TestUtil.getContent(mbox, msg.getId(), "upload2.gif");
        Assert.assertEquals(851, data.length);

        ZMimePart part = TestUtil.getPart(msg, "upload.gif");
        checkPartSize(73, part.getSize());
        part = TestUtil.getPart(msg, "upload2.gif");
        checkPartSize(851, part.getSize());
    }

    /**
     * The part size is calculated from the base64 content, so it may be off by a few bytes.
     */
    private void checkPartSize(long expected, long actual) {
        Assert.assertTrue("expected " + expected + " +/- 4 bytes, got " + actual, Math.abs(expected - actual) <= 4);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestConversion.class);
    }
}
