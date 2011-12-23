/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZGetMessageParams;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessage.ZMimePart;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class TestSendAndReceive extends TestCase {

    private static final String NAME_PREFIX = TestSendAndReceive.class.getSimpleName();
    private static final String USER_NAME = "user1";
    private static final String REMOTE_USER_NAME = "user2";
    private static final Pattern PAT_RECEIVED = Pattern.compile("Received: .*from.*LHLO.*");
    private static final Pattern PAT_RETURN_PATH = Pattern.compile("Return-Path: (.*)");

    private String mOriginalSmtpSendAddAuthenticatedUser;
    private String mOriginalDomainSmtpPort;
    private String[] mOriginalSmtpHostname;

    @Override
    public void setUp() throws Exception {
        cleanUp();
        mOriginalSmtpSendAddAuthenticatedUser = TestUtil.getConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser);
        mOriginalDomainSmtpPort = TestUtil.getDomainAttr(USER_NAME, Provisioning.A_zimbraSmtpPort);
        mOriginalSmtpHostname = Provisioning.getInstance().getLocalServer().getSmtpHostname();
    }

    /**
     * Verifies "send-message-later" functionality.
     */
    public void testAutoSendDraft() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        ZMailbox.ZOutgoingMessage outgoingMsg = new ZMailbox.ZOutgoingMessage();
        List<ZEmailAddress> addrs = new LinkedList<ZEmailAddress>();
        String senderAddr = TestUtil.getAddress(USER_NAME);
        addrs.add(new ZEmailAddress(senderAddr, null, null, ZEmailAddress.EMAIL_TYPE_FROM));
        String rcptAddr = TestUtil.getAddress(REMOTE_USER_NAME);
        addrs.add(new ZEmailAddress(rcptAddr, null, null, ZEmailAddress.EMAIL_TYPE_TO));
        outgoingMsg.setAddresses(addrs);
        String subject = NAME_PREFIX + " autoSendDraft";
        outgoingMsg.setSubject(subject);

        // auto-send after 0.5 sec
        mbox.saveDraft(outgoingMsg, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS), System.currentTimeMillis() + 500);

        // make sure message has arrived in the Sent folder
        TestUtil.waitForMessage(mbox, "in:Sent " + subject);

        // make sure message is no longer in the Drafts folder
        assertTrue("message is still in the Drafts folder",
                   TestUtil.search(mbox, "in:Drafts " + subject).isEmpty());
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

        // Add message.
        String msgContent = new String(ByteUtil.getContent(new File(
            LC.zimbra_home.value() + "/unittest/testZimbraReceivedHeader.msg")));
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgContent);

        // Test date.
        List<ZMessage> messages = TestUtil.search(mbox, "subject:testZimbraReceivedHeader");
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
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, LdapConstants.LDAP_FALSE);
        String subject = NAME_PREFIX + " testAuthenticatedUserHeader false";
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        assertNull(TestUtil.getHeaderValue(mbox, msg, MailSender.X_AUTHENTICATED_USER));

        // X-Authenticated-User sent.
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, LdapConstants.LDAP_TRUE);
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
     * Confirms that we preserve line endings of attached text files (bugs 45858 and 53405).
     */
    public void testTextAttachmentLineEnding()
    throws Exception {
        String content = "I used to think that the day would never come,\n" +
            "I'd see the light in the shade of the morning sun\n";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        verifyTextAttachmentLineEnding(mbox, content, MimeConstants.CT_TEXT_PLAIN);
        verifyTextAttachmentLineEnding(mbox, content, "application/x-shellscript");
    }

    private class SenderThread
    extends Thread {
        private String subject;
        private String attachId;
        private Throwable error;

        SenderThread(String subject, String attachId) {
            this.subject = subject;
            this.attachId = attachId;
        }

        @Override
        public void run() {
            try {
                ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
                TestUtil.sendMessage(mbox, USER_NAME, subject, "", attachId);
            } catch (Throwable e) {
                this.error = e;
            }
        }
    }

    /**
     * Confirms that the server gracefully handles the case where multiple threads
     * simultaneously send a message, using the same attachment upload id (bug 57222).
     */
    public void testReuseUploadId()
    throws Exception {
        // Upload attachment.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String attachContent = "The leaves that are green turn to brown.";
        String attachId = mbox.uploadAttachment("lyrics.txt", attachContent.getBytes(), "text/plain", 0);

        // Run 5 threads that all send with the same upload id.
        SenderThread[] threads = new SenderThread[5];
        String subject = NAME_PREFIX + " testReuseUploadId";
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new SenderThread(subject + " " + i, attachId);
            threads[i].start();
        }
        for (SenderThread thread : threads) {
            thread.join();
        }

        // Make sure only 1 succeeded.
        int numFailures = 0;
        for (SenderThread sender : threads) {
            if (sender.error != null) {
                numFailures++;
            }
        }
        assertEquals(threads.length - 1, numFailures);
    }

    private void verifyTextAttachmentLineEnding(ZMailbox mbox, String content, String contentType)
    throws Exception {
        // Test simple send.
        String attachId = mbox.uploadAttachment("text.txt", content.getBytes(), contentType, 5000);
        String subject = NAME_PREFIX + " testTextAttachmentLineEnding " + contentType + " 1";
        TestUtil.sendMessage(mbox, USER_NAME, subject, "Testing text attachment", attachId);

        ZMessage msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        InputStream in = mbox.getRESTResource("?id=" + msg.getId() + "&part=2");
        String attachContent = new String(ByteUtil.getContent(in, content.length()));
        assertEquals(content, attachContent);

        // Test save draft and send.
        attachId = mbox.uploadAttachment("text.txt", content.getBytes(), contentType, 5000);
        subject = NAME_PREFIX + " testTextAttachmentLineEnding " + contentType + " 2";
        TestUtil.saveDraftAndSendMessage(mbox, USER_NAME, subject, "Testing text attachment", attachId);

        msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        in = mbox.getRESTResource("?id=" + msg.getId() + "&part=2");
        attachContent = new String(ByteUtil.getContent(in, content.length()));
        assertEquals(content, attachContent);
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, mOriginalSmtpSendAddAuthenticatedUser);
        TestUtil.setDomainAttr(USER_NAME, Provisioning.A_zimbraSmtpPort, mOriginalDomainSmtpPort);
        Provisioning.getInstance().getLocalServer().setSmtpHostname(mOriginalSmtpHostname);
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(REMOTE_USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSendAndReceive.class);
    }
}
