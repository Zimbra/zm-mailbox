/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.File;

import junit.framework.TestCase;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMessage.ZMimePart;

public class TestConversion extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestConversion.class.getSimpleName();
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Tests downloading attachments from a TNEF message (bug 44263).
     */
    // Disabled until bug 55859 is fixed.
    public void disabledTestTnef()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // Add the TNEF message
        String msgContent = new String(ByteUtil.getContent(new File(
            LC.zimbra_home.value() + "/unittest/tnef.msg")));
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgContent);
        
        // Test downloading attachments.
        ZMessage msg = TestUtil.getMessage(mbox, "in:inbox subject:\"Rich text (TNEF) test\"");
        byte[] data = TestUtil.getContent(mbox, msg.getId(), "upload.gif");
        assertEquals(73, data.length);
        data = TestUtil.getContent(mbox, msg.getId(), "upload2.gif");
        assertEquals(851, data.length);
        
        ZMimePart part = TestUtil.getPart(msg, "upload.gif");
        assertEquals(73, part.getSize());
        part = TestUtil.getPart(msg, "upload2.gif");
        assertEquals(851, part.getSize());
    }
    
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
