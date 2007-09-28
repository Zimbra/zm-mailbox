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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.zclient.ZMailbox;

public class TestMaxMessageSize
extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestMaxMessageSize.class.getSimpleName();
    private static final int TEST_MAX_MESSAGE_SIZE = 2000;
    
    private int mOrigMaxMessageSize;
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        Config config = Provisioning.getInstance().getConfig();
        mOrigMaxMessageSize = config.getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, -1);
        assertTrue("Unexpected max message size: " + mOrigMaxMessageSize, mOrigMaxMessageSize >= 0);
    }
    
    public void testMaxMessageSize()
    throws Exception {
        setMaxMessageSize(TEST_MAX_MESSAGE_SIZE);
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        attachments.put("file1.exe", new byte[500]);
        attachments.put("file2.exe", new byte[600]);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String aid = mbox.uploadAttachments(attachments, 5000);
        TestUtil.sendMessage(mbox, USER_NAME, NAME_PREFIX, "Message size below threshold", aid);
        
        attachments.put("file3.exe", new byte[1000]);
        aid = mbox.uploadAttachments(attachments, 5000);
        try {
            TestUtil.sendMessage(mbox, USER_NAME, NAME_PREFIX, "Message size above threshold", aid);
            fail("sendMessage() should not have succeeded");
        } catch (SoapFaultException e) {
            // Message send was not allowed, as expected.
            assertEquals("Message exceeded allowed size", e.getReason());
            assertEquals(MailServiceException.MESSAGE_TOO_BIG, e.getCode());
            assertEquals(Integer.toString(TEST_MAX_MESSAGE_SIZE), e.getArgumentValue("maxSize"));
        }
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
        setMaxMessageSize(mOrigMaxMessageSize);
    }
    
    private void setMaxMessageSize(int numBytes)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMtaMaxMessageSize, Integer.toString(numBytes));
        prov.modifyAttrs(config, attrs);
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestMaxMessageSize.class));
    }
}
