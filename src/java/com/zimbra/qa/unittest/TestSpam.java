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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
