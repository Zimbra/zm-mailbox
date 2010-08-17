/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.cs.zclient.ZMessage.ZMimePart;

public class TestSendAndReceive extends TestCase {

    private static final String NAME_PREFIX = TestSendAndReceive.class.getSimpleName();
    private static final String USER_NAME = "user1";
    private static final Pattern PAT_RECEIVED = Pattern.compile("Received: .*from.*LHLO.*");
    private static final Pattern PAT_RETURN_PATH = Pattern.compile("Return-Path: (.*)");
    
    private String mOriginalSmtpSendAddAuthenticatedUser;
    private String mOriginalDomainSmtpPort;
    private String[] mOriginalSmtpHostname;
    
    public void setUp()
    throws Exception {
        cleanUp();
        mOriginalSmtpSendAddAuthenticatedUser = TestUtil.getConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser);
        mOriginalDomainSmtpPort = TestUtil.getDomainAttr(USER_NAME, Provisioning.A_zimbraSmtpPort);
        mOriginalSmtpHostname = Provisioning.getInstance().getLocalServer().getSmtpHostname();
    }
    
    /**
     * Verifies that we set the Return-Path and Received headers
     * for incoming messages.
     */
    public void testReceivedHeaders()
    throws Exception {
        // Send message from user2 to user1
        String sender = TestUtil.getAddress("user2");
        String recipient = TestUtil.getAddress(USER_NAME);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testReceivedHeaders()", recipient, sender);
        
        // Search
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, NAME_PREFIX);
        assertEquals("Unexpected message count", 1, messages.size());
        
        // Get the message content, since a search won't return the content
        ZGetMessageParams params = new ZGetMessageParams();
        params.setId(messages.get(0).getId());
        params.setRawContent(true);
        ZMessage message = mbox.getMessage(params);
        String content = message.getContent();
        
        // Check headers
        boolean foundReceived = false;
        boolean foundReturnPath = false;
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = PAT_RECEIVED.matcher(line);
            if (matcher.matches()) {
                ZimbraLog.test.debug("Found " + line);
                foundReceived = true;
            }
            
            matcher = PAT_RETURN_PATH.matcher(line);
            if (matcher.matches()) {
                foundReturnPath = true;
                assertEquals("Sender doesn't match", sender, matcher.group(1));
                ZimbraLog.test.debug("Found " + line);
            }
            line = reader.readLine();
        }
        reader.close();
        
        assertTrue("Received header not found.  Content=\n" + content, foundReceived);
        assertTrue("Return-Path header not found.  Content=\n" + content, foundReturnPath);
    }
    
    /**
     * Confirms that the message received date is set to the value of the
     * <tt>X-Zimbra-Received</tt> header.
     */
    public void testZimbraReceivedHeader()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, "subject:\"Test Phone Number Formats\"");
        assertEquals("Unexpected message count", 1, messages.size());
        ZMessage msg = messages.get(0);
        Calendar cal = Calendar.getInstance(mbox.getPrefs().getTimeZone());
        cal.setTimeInMillis(msg.getReceivedDate());
        assertEquals(2005, cal.get(Calendar.YEAR));
        assertEquals(1, cal.get(Calendar.MONTH));
        assertEquals(27, cal.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * Confirms that <tt>X-Authenticated-User</tt> is set on outgoing messages when
     * <tt>zimbraSmtpSendAddAuthenticatedUser</tt> is set to <tt>TRUE</tt>.
     */
    public void testAuthenticatedUserHeader()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        
        // X-Authenticated-User not sent.
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, LdapUtil.LDAP_FALSE);
        String subject = NAME_PREFIX + " testAuthenticatedUserHeader false";
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        assertNull(TestUtil.getHeaderValue(mbox, msg, MailSender.X_AUTHENTICATED_USER));
        
        // X-Authenticated-User sent.
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, LdapUtil.LDAP_TRUE);
        subject = NAME_PREFIX + " testAuthenticatedUserHeader true";
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        assertEquals(mbox.getName(), TestUtil.getHeaderValue(mbox, msg, MailSender.X_AUTHENTICATED_USER));
    }
    
    /**
     * Confirms that domain SMTP settings override server settings (bug 28442).
     */
    public void testDomainSmtpSettings()
    throws Exception {
        // Send a message using the user's default SMTP settings.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " testDomainSmtpSettings 1";
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        
        // Set domain SMTP port to a bogus value and confirm that the send fails.
        TestUtil.setDomainAttr(USER_NAME, Provisioning.A_zimbraSmtpPort, "35");
        subject = NAME_PREFIX + " testDomainSmtpSettings 2";
        boolean sendFailed = false;
        try {
            TestUtil.sendMessage(mbox, USER_NAME, subject);
        } catch (SoapFaultException e) {
            assertEquals(MailServiceException.TRY_AGAIN, e.getCode());
            sendFailed = true;
        }
        assertTrue("Message send should have failed", sendFailed);
    }
    
    public void testBogusSmtpHostname()
    throws Exception {
        // Create a list that contains the original valid SMTP host
        // and a bunch of bogus ones.
        List<String> smtpHosts = new ArrayList<String>();
        Collections.addAll(smtpHosts, mOriginalSmtpHostname);
        for (int i = 1; i <= 10; i++) {
            smtpHosts.add("bogushost" + i);
        }
        String[] hostsArray = new String[smtpHosts.size()];
        smtpHosts.toArray(hostsArray);
        Provisioning.getInstance().getLocalServer().setSmtpHostname(hostsArray);
        
        // Send a message and make sure it arrives.
        String subject = NAME_PREFIX + " testBogusSmtpHostname";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
    }
    
    /**
     * Confirms that we can forward attachments with a malformed content type (bug 42452).
     */
    public void testMalformedContentType()
    throws Exception {
        // Generate original message.
        String subject = NAME_PREFIX + " testMalformedContentType";
        MessageBuilder builder = new MessageBuilder().withFrom(USER_NAME).withToRecipient(USER_NAME)
            .withSubject(subject).withAttachment("This is an attachment", "test.txt", MimeConstants.CT_TEXT_PLAIN);
        
        // Hack Content-Type so that it's invalid.
        BufferedReader reader = new BufferedReader(new StringReader(builder.create()));
        StringBuilder msgBuf = new StringBuilder();
        String line = reader.readLine();
        boolean replaced = false;
        while (line != null) {
            if (line.matches("Content-Type.*test.txt.*")) {
                line = line.replace("Content-Type: text/plain;", "Content-Type: text/plain;;");
                assertTrue("Unexpected line: " + line, line.contains(";;"));
                replaced = true;
            }
            msgBuf.append(line).append("\r\n");
            line = reader.readLine();
        }
        assertTrue("Could not find text/plain attachment.", replaced);
        
        // Add message to the mailbox.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgBuf.toString());
        
        // Forward the attachment in a new message.
        ZMessage srcMsg = TestUtil.getMessage(mbox, "subject:\"" + subject + "\"");
        ZMimePart srcAttachPart = srcMsg.getMimeStructure().getChildren().get(1);
        assertEquals("test.txt", srcAttachPart.getFileName());
        
        ZOutgoingMessage outgoing = new ZOutgoingMessage();
        outgoing.setMessagePart(new MessagePart(MimeConstants.CT_TEXT_PLAIN, "Forwarding attachment."));
        outgoing.setMessagePartsToAttach(Arrays.asList(new AttachedMessagePart(srcMsg.getId(), srcAttachPart.getPartName(), null)));
        String address = TestUtil.getAddress(USER_NAME);
        ZEmailAddress sender = new ZEmailAddress(address, null, null, ZEmailAddress.EMAIL_TYPE_FROM);
        ZEmailAddress recipient = new ZEmailAddress(address, null, null, ZEmailAddress.EMAIL_TYPE_TO);
        outgoing.setAddresses(Arrays.asList(sender, recipient));
        String fwdSubject = NAME_PREFIX + " testMalformedContentType forward";
        outgoing.setSubject(fwdSubject);
        mbox.sendMessage(outgoing, null, false);
        
        // Make sure the forwarded message arrives.
        ZMessage fwd = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + fwdSubject + "\"");
        ZMimePart fwdAttachPart = fwd.getMimeStructure().getChildren().get(1);
        assertEquals("test.txt", fwdAttachPart.getFileName());
    }
    
    /**
     * Confirms that we preserve line endings of attached text files (bug 45858).
     */
    public void testTextAttachmentLineEnding()
    throws Exception {
        // Test simple send.
        String content = "I used to think that the day would never come,\n" +
            "I'd see the light in the shade of the morning sun\n";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String attachId = mbox.uploadAttachment("text.txt", content.getBytes(), MimeConstants.CT_TEXT_PLAIN, 5000);
        String subject = NAME_PREFIX + " testTextAttachmentLineEnding 1";
        TestUtil.sendMessage(mbox, USER_NAME, subject, "Testing text attachment", attachId);

        ZMessage msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        InputStream in = mbox.getRESTResource("?id=" + msg.getId() + "&part=2");
        String attachContent = new String(ByteUtil.getContent(in, content.length()));
        assertEquals(content, attachContent);
        
        // Test save draft and send.
        attachId = mbox.uploadAttachment("text.txt", content.getBytes(), MimeConstants.CT_TEXT_PLAIN, 5000);
        subject = NAME_PREFIX + " testTextAttachmentLineEnding 2";
        TestUtil.saveDraftAndSendMessage(mbox, USER_NAME, subject, "Testing text attachment", attachId);

        msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        in = mbox.getRESTResource("?id=" + msg.getId() + "&part=2");
        attachContent = new String(ByteUtil.getContent(in, content.length()));
        assertEquals(content, attachContent);
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, mOriginalSmtpSendAddAuthenticatedUser);
        TestUtil.setDomainAttr(USER_NAME, Provisioning.A_zimbraSmtpPort, mOriginalDomainSmtpPort);
        Provisioning.getInstance().getLocalServer().setSmtpHostname(mOriginalSmtpHostname);
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSendAndReceive.class);
    }
}
