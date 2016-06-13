/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import junit.framework.TestCase;

import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessage.ZMimePart;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.JMSession;


public class TestMessageIntercept
extends TestCase {

    private static String RECIPIENT_NAME = "testmessageintercept-user1";
    private static String SENDER_NAME = "TestMessageInterceptSender1";
    private static String INTERCEPTOR1_NAME = "TestMessageInterceptUser2";
    private static String INTERCEPTOR2_NAME = "TestMessageInterceptUser3";

    private static String[] ALL_TEST_USERS = { RECIPIENT_NAME, INTERCEPTOR1_NAME, INTERCEPTOR2_NAME };

    private static String NAME_PREFIX = TestMessageIntercept.class.getSimpleName();

    @Override
    protected void setUp() throws Exception
    {
        cleanUp();

        // tapped account
        TestUtil.createAccount(RECIPIENT_NAME);

        // interceptor accounts
        TestUtil.createAccount(INTERCEPTOR1_NAME);
        TestUtil.createAccount(INTERCEPTOR2_NAME);
    }

    public void testIntercept()
    throws Exception {
        // Turn on legal intercept for recipient account
        String interceptorAddress = TestUtil.getAddress(INTERCEPTOR1_NAME);
        String senderAddress = TestUtil.getAddress(SENDER_NAME);
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraInterceptAddress, interceptorAddress);
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraInterceptSendHeadersOnly, LdapConstants.LDAP_FALSE);

        // Send message to recipient account and make sure it's intercepted
        ZMailbox interceptorMbox = TestUtil.getZMailbox(INTERCEPTOR1_NAME);
        ZMailbox tappedMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        String tappedAddress = TestUtil.getAddress(RECIPIENT_NAME);
        String subject = NAME_PREFIX + " testIntercept-receive";
        TestUtil.addMessageLmtp(subject, tappedAddress, senderAddress);

        ZMessage tappedMsg = TestUtil.getMessage(tappedMbox, "subject:\"" + subject + "\"");
        ZMessage interceptMsg = TestUtil.waitForMessage(interceptorMbox, "subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg, "add message", "Inbox", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        compareContent(tappedMbox, tappedMsg, interceptorMbox, interceptMsg);

        // Confirm that saving a draft is intercepted.  The first draft calls Mailbox.addMessage().
        ZOutgoingMessage outgoing = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses
                .add(new ZEmailAddress(TestUtil.getAddress(INTERCEPTOR1_NAME),
            null, null, ZEmailAddress.EMAIL_TYPE_TO));
        outgoing.setAddresses(addresses);
        subject = NAME_PREFIX + " testIntercept-draft-1";
        outgoing.setSubject(subject);
        outgoing.setMessagePart(new MessagePart("text/plain", "I always feel like somebody's watching me."));
        tappedMbox.saveDraft(outgoing, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        tappedMsg = TestUtil.getMessage(tappedMbox, "in:drafts subject:\"" + subject + "\"");
        interceptMsg = TestUtil.waitForMessage(interceptorMbox, "subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg, "add message", "Drafts", Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        compareContent(tappedMbox, tappedMsg, interceptorMbox, interceptMsg);

        // Save draft again.  This time Mailbox.saveDraft() gets called.
        ZMessage draft = TestUtil.getMessage(tappedMbox, "in:drafts subject:\"" + subject + "\"");
        subject = NAME_PREFIX + " testIntercept-draft-2";
        outgoing.setSubject(subject);
        tappedMbox.saveDraft(outgoing, draft.getId(), null);
        tappedMsg = TestUtil.getMessage(tappedMbox, "in:drafts subject:\"" + subject + "\"");
        interceptMsg = TestUtil.waitForMessage(interceptorMbox, "subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg, "save draft", "Drafts", Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        compareContent(tappedMbox, tappedMsg, interceptorMbox, interceptMsg);

        // Send message with save-to-sent turned on.
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraPrefSaveToSent, LdapConstants.LDAP_TRUE);
        subject = NAME_PREFIX + " testIntercept-send-1";
        TestUtil.sendMessage(tappedMbox, INTERCEPTOR1_NAME, subject);
        tappedMsg = TestUtil.waitForMessage(tappedMbox, "in:sent subject:\"" + subject + "\"");
        interceptMsg = TestUtil.waitForMessage(interceptorMbox, "subject:intercepted subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg, "add message", "Sent", Integer.toString(Mailbox.ID_FOLDER_SENT));
        compareContent(tappedMbox, tappedMsg, interceptorMbox, interceptMsg);

        // Send message with save-to-sent turned off.
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraPrefSaveToSent, LdapConstants.LDAP_FALSE);
        subject = NAME_PREFIX + " testIntercept-send-2";
        TestUtil.sendMessage(tappedMbox, INTERCEPTOR1_NAME, subject);
        interceptMsg = TestUtil.waitForMessage(interceptorMbox, "subject:intercepted subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg, "send message", "none", "none");

        // Check intercepting headers only.
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraInterceptSendHeadersOnly, LdapConstants.LDAP_TRUE);
        subject = NAME_PREFIX + " testIntercept-headers-only";
        TestUtil.sendMessage(interceptorMbox, RECIPIENT_NAME, subject);
        tappedMsg = TestUtil.waitForMessage(tappedMbox, "in:inbox subject:\"" + subject + "\"");
        interceptMsg = TestUtil.waitForMessage(interceptorMbox, "subject:intercepted subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg, "add message", "Inbox", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        compareContent(tappedMbox, tappedMsg, interceptorMbox, interceptMsg);
    }

    /**
     * Confirms that legal intercept works with multiple interceptor addresses (bug 30961).
     */
    public void testInterceptMultiValue()
    throws Exception {
        // Turn on legal intercept for recipient account.
        String interceptor1Address = TestUtil.getAddress(INTERCEPTOR1_NAME);
        String interceptor2Address = TestUtil.getAddress(INTERCEPTOR2_NAME);
        String senderAddress = TestUtil.getAddress(SENDER_NAME);
        String[] interceptorAddresses = new String[] { interceptor1Address, interceptor2Address };

        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraInterceptAddress, interceptorAddresses);
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraInterceptSendHeadersOnly, LdapConstants.LDAP_FALSE);

        // Send message to recipient account.
        ZMailbox tappedMbox = TestUtil.getZMailbox(RECIPIENT_NAME);
        String tappedAddress = TestUtil.getAddress(RECIPIENT_NAME);
        String subject = NAME_PREFIX + " testIntercept-receive";
        TestUtil.addMessageLmtp(subject, tappedAddress, senderAddress);

        // Make sure both interceptor accounts intercepted it.
        ZMailbox interceptor1Mbox = TestUtil.getZMailbox(INTERCEPTOR1_NAME);
        ZMailbox interceptor2Mbox = TestUtil.getZMailbox(INTERCEPTOR2_NAME);
        ZMessage tappedMsg = TestUtil.getMessage(tappedMbox, "subject:\"" + subject + "\"");
        ZMessage interceptMsg1 = TestUtil.waitForMessage(interceptor1Mbox, "subject:\"" + subject + "\"");
        ZMessage interceptMsg2 = TestUtil.waitForMessage(interceptor2Mbox, "subject:\"" + subject + "\"");
        verifyInterceptMessage(interceptMsg1, "add message", "Inbox", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        verifyInterceptMessage(interceptMsg2, "add message", "Inbox", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        compareContent(tappedMbox, tappedMsg, interceptor1Mbox, interceptMsg1);
        compareContent(tappedMbox, tappedMsg, interceptor2Mbox, interceptMsg2);
    }

    /**
     * Verifies the structure of the intercept message.
     */
    private void verifyInterceptMessage(ZMessage interceptMsg, String operation, String folderName, String folderId)
    throws Exception {
        // Check structure
        ZMimePart part = interceptMsg.getMimeStructure();
        assertEquals(MimeConstants.CT_MULTIPART_MIXED, part.getContentType());
        List<ZMimePart> children = part.getChildren();
        assertEquals(2, children.size());

        // Check body
        ZMimePart bodyPart = children.get(0);
        assertEquals(MimeConstants.CT_TEXT_PLAIN, bodyPart.getContentType());
        String body = bodyPart.getContent();
        String context = "Unexpected body: \n" + body;
        assertTrue(context, body.contains("Intercepted message for " + RECIPIENT_NAME));
        assertTrue(context, body.contains("Operation=" + operation));
        assertTrue(context, body.contains("folder=" + folderName));
        assertTrue(context, body.contains("folder ID=" + folderId));

        // Compare to original message
        ZMimePart interceptedPart = children.get(1);
        assertEquals(MimeConstants.CT_MESSAGE_RFC822, interceptedPart.getContentType());
    }

    /**
     * Confirm that the message attached to the intercept message matches the original.
     */
    private void compareContent(ZMailbox tappedMbox, ZMessage tappedMsg, ZMailbox interceptorMbox, ZMessage interceptMsg)
    throws Exception {
        String relativeUrl = String.format("?id=%s&part=2", interceptMsg.getId());
        InputStream in = interceptorMbox.getRESTResource(relativeUrl);
        String interceptedMsgContent = new String(ByteUtil.getContent(in, -1)).trim();
        String tappedMsgContent = TestUtil.getContent(tappedMbox, tappedMsg.getId()).trim();

        Account account = TestUtil.getAccount(RECIPIENT_NAME);

        // Compare headers
        MimeMessage tappedMimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(tappedMsgContent.getBytes()));
        MimeMessage interceptedMimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(interceptedMsgContent.getBytes()));

        boolean headersOnly = account.getBooleanAttr(Provisioning.A_zimbraInterceptSendHeadersOnly, false);
        Set<String> tappedHeaderLines = getHeaderLines(tappedMimeMsg);
        Set<String> interceptedHeaderLines = getHeaderLines(interceptedMimeMsg);
        tappedHeaderLines.removeAll(getHeaderLines(interceptedMimeMsg));
        interceptedHeaderLines.removeAll(getHeaderLines(tappedMimeMsg));
        String context = "Unexpected headers found.  tapped: " +
            StringUtil.join(",", tappedHeaderLines) + ".  intercepted: " +
            StringUtil.join(",", interceptedHeaderLines) + ".";
        assertTrue(context, tappedHeaderLines.size() == 0 && interceptedHeaderLines.size() == 0);

        // Compare body
        if (headersOnly) {
            String interceptedBody = new String(ByteUtil.getContent(interceptedMimeMsg.getInputStream(), 0));
            if (interceptedBody != null) {
                interceptedBody = interceptedBody.trim();
            }
            assertTrue("Unexpected body: '" + interceptedBody + "'", interceptedBody == null || interceptedBody.length() == 0);
        } else {
            TestUtil.assertMessageContains(tappedMsgContent, interceptedMsgContent);
        }
    }

    private Set<String> getHeaderLines(MimeMessage msg)
    throws MessagingException {
        Set<String> headerLines = new HashSet<String>();
        Enumeration<?> e = msg.getAllHeaderLines();
        while (e.hasMoreElements()) {
            headerLines.add((String) e.nextElement());
        }
        return headerLines;
    }

    @Override
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        // Clean up temporary data
        for (String userName : ALL_TEST_USERS) {
            if (TestUtil.accountExists(userName)) {
                TestUtil.deleteAccount(userName);
            }
        }
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestMessageIntercept.class);
    }
}
