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

import java.util.List;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;

import junit.framework.TestCase;
import junit.framework.TestSuite;


public class TestIndex extends TestCase {

    private static final String NAME_PREFIX = TestIndex.class.getSimpleName();
    private static final String USER_NAME = "user1";

    private int mOriginalTextLimit;
    
    public void setUp()
    throws Exception {
        mOriginalTextLimit = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit));
        cleanUp();
    }
    
    public void testIndexedTextLimit()
    throws Exception {
        // Test text attachment
        StringBuilder body = new StringBuilder();
        for (int i = 1; i < 100; i++) {
            body.append("Walrus walrus walrus walrus walrus walrus walrus.\n");
        }
        body.append("Goo goo goo joob.\n");

        // Test text truncated
        setTextLimit(50);
        String subject = NAME_PREFIX + " text attachment 1";
        String msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", Mime.CT_TEXT_PLAIN).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", null);
        
        // Test HTML truncated
        subject = NAME_PREFIX + " HTML attachment 1";
        String htmlBody = "<html>\n" + body + "</html>";
        msgId = sendMessage(subject, htmlBody.getBytes(), "attachment.html", Mime.CT_TEXT_HTML).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", null);
        
        // Test text not truncated
        setTextLimit(100000);
        subject = NAME_PREFIX + " text attachment 2";
        msgId = sendMessage(subject, body.toString().getBytes(), "attachment.txt", Mime.CT_TEXT_PLAIN).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", msgId);

        // Test HTML not truncated
        subject = NAME_PREFIX + " HTML attachment 2";
        msgId = sendMessage(subject, htmlBody.getBytes(), "attachment.html", Mime.CT_TEXT_HTML).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" walrus", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" joob", msgId);
        
        // Test attached message subject truncated
        subject = NAME_PREFIX + " subject";
        String attachedMsg = TestUtil.getTestMessage("Pigs from a gun", "recipient", "sender", null);
        setTextLimit(4);
        msgId = sendMessage(subject, attachedMsg.getBytes(), "attachment.msg", Mime.CT_MESSAGE_RFC822).getId();
        checkQuery("in:inbox subject:\"" + subject + "\" pigs", msgId);
        checkQuery("in:inbox subject:\"" + subject + "\" gun", null);
    }
    
    /**
     * Sends a message with the specified attachment, waits for the message to
     * arrives, and runs a query.
     * @param subject the subject of the message
     * @param attData attachment data 
     * @param attName attachment name
     * @param attContentType attachment content type
     * @param query query to run after message arrives
     * @return <tt>true</tt> if the query returns the message
     */
    private ZMessage sendMessage(String subject, byte[] attData, String attName, String attContentType)
    throws Exception {
        
        // Send message
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String attachmentId = mbox.uploadAttachment(attName, attData, attContentType, 5000);
        TestUtil.sendMessage(mbox, USER_NAME, subject, "Cranberry sauce", attachmentId);
        String query = "in:inbox subject:\"" + subject + "\"";
        return TestUtil.waitForMessage(mbox, query);
    }
    
    private void checkQuery(String query, String msgId)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, query);
        if (msgId == null) {
            assertEquals(0, messages.size());
        } else {
            assertEquals(1, messages.size());
            assertEquals(msgId, messages.get(0).getId());
        }
    }
    
    private void setTextLimit(int numBytes)
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, Integer.toString(numBytes));
    }
    
    public void tearDown()
    throws Exception {
        setTextLimit(mOriginalTextLimit);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestIndex.class));
    }
}
