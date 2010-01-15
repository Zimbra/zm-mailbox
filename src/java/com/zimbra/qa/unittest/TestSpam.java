/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.ByteArrayInputStream;

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.util.JMSession;

public class TestSpam extends TestCase {

    private static final String NAME_PREFIX = TestCase.class.getSimpleName();
    private static final String USER_NAME = "user1";
    
    private String mOriginalSpamHeaderValue;

    public void setUp()
    throws Exception {
        mOriginalSpamHeaderValue = Provisioning.getInstance().getConfig().getSpamHeaderValue();
    }
    
    /**
     * Tests {@link Mime#isSpam}.
     */
    public void testSpam()
    throws Exception {
        String coreContent = TestUtil.getTestMessage(NAME_PREFIX + " testSpam", USER_NAME, USER_NAME, null);
        MimeMessage msg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(coreContent.getBytes()));
        assertFalse(SpamHandler.isSpam(msg));
        
        // Test single-line spam header (common case)
        String headerName = Provisioning.getInstance().getConfig().getSpamHeader();
        String singleLineSpamContent = headerName + ": YES\r\n" + coreContent;
        msg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(singleLineSpamContent.getBytes()));
        assertTrue(SpamHandler.isSpam(msg));
        
        // Test folded spam header (bug 24954).
        Provisioning.getInstance().getConfig().setSpamHeaderValue("spam.*");
        String folderSpamContent = headerName + ": spam, SpamAssassin (score=5.701, required 5,\r\n" +
            "   DCC_CHECK 1.37, FH_RELAY_NODNS 1.45, RATWARE_RCVD_PF 2.88)\r\n" + coreContent;
        msg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(folderSpamContent.getBytes()));
        assertTrue(SpamHandler.isSpam(msg));
    }
    
    public void tearDown()
    throws Exception {
        Provisioning.getInstance().getConfig().setSpamHeaderValue(mOriginalSpamHeaderValue);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSpam.class);
    }
}
