/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.ToXML.EmailType;

public class TestSendAndReceive {

    @Rule
    public TestName testInfo = new TestName();
    private static final String NAME_PREFIX = TestSendAndReceive.class.getSimpleName();
    private static String USER_NAME = null;
    private static String REMOTE_USER_NAME = null;
    private static final Pattern PAT_RECEIVED = Pattern.compile("Received: .*from.*LHLO.*");
    private static final Pattern PAT_RETURN_PATH = Pattern.compile("Return-Path: (.*)");
    private static String mOriginalSmtpSendAddAuthenticatedUser;
    private static String[] mOriginalSmtpHostname;
    private static String[] originalCustomMimeHeaderNameAllowed;
    private static Domain originalDefaultDomain;
    private static String originalDefaultDomainSmtpPort;
    public static final String PUBLIC_LIST_HEADER = "X-ZTest-PublicFile";
    private static TestSendMailListener listener;
    private static Provisioning prov;

    @BeforeClass
    public static void beforeClassSetup() throws ServiceException {
        prov = Provisioning.getInstance();
        mOriginalSmtpHostname = prov.getLocalServer().getSmtpHostname();
        mOriginalSmtpSendAddAuthenticatedUser =
                TestUtil.getConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser);
        originalCustomMimeHeaderNameAllowed = prov.getConfig().getCustomMimeHeaderNameAllowed();
        afterClassCleanup();
        prov.getConfig().addCustomMimeHeaderNameAllowed(MailSender.PRE_SEND_HEADER);
        prov.getConfig().addCustomMimeHeaderNameAllowed(PUBLIC_LIST_HEADER);
    }

    @AfterClass
    public static void afterClassCleanup() throws ServiceException {
        prov.getConfig().setCustomMimeHeaderNameAllowed(originalCustomMimeHeaderNameAllowed);
    }

    @Before
    public void setUp() throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user1";
        REMOTE_USER_NAME = prefix + "user2";
        originalDefaultDomain = prov.getDefaultDomain();
        originalDefaultDomainSmtpPort = originalDefaultDomain.getAttr(Provisioning.A_zimbraSmtpPort);
        cleanUp();
        listener = new TestSendMailListener();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        MailSender.unregisterPreSendMailListener(listener);
        listener = null;
    }

    private void cleanUp() throws ServiceException {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(REMOTE_USER_NAME);
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraSmtpPort, originalDefaultDomainSmtpPort);
        prov.modifyAttrs(originalDefaultDomain, attrs);
        prov.getLocalServer().setSmtpHostname(mOriginalSmtpHostname);
        TestUtil.setConfigAttr(
                Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, mOriginalSmtpSendAddAuthenticatedUser);
    }

    /**
     * Verifies that we set the Return-Path and Received headers
     * for incoming messages.
     */
    @Test
    public void testReceivedHeaders()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        // Send message from user2 to user1
        String sender = TestUtil.getAddress(REMOTE_USER_NAME);
        String recipient = TestUtil.getAddress(USER_NAME);
        TestUtil.addMessageLmtp(NAME_PREFIX + " testReceivedHeaders()", recipient, sender);

        // Search
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZMessage> messages = TestUtil.search(mbox, NAME_PREFIX);
        Assert.assertEquals("Unexpected message count", 1, messages.size());

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
                Assert.assertEquals("Sender doesn't match", "<" + sender + ">", matcher.group(1));
                ZimbraLog.test.debug("Found " + line);
            }
            line = reader.readLine();
        }
        reader.close();

        Assert.assertTrue("Received header not found.  Content=\n" + content, foundReceived);
        Assert.assertTrue("Return-Path header not found.  Content=\n" + content, foundReturnPath);
    }

    /**
     * Confirms that the message received date is set to the value of the
     * <tt>X-Zimbra-Received</tt> header.
     */
    @Test
    public void testZimbraReceivedHeader()
    throws Exception {
        Account acct = TestUtil.createAccount(USER_NAME);
        String[] tzs = { "America/Los_Angeles" };
        acct.setPrefTimeZoneId(tzs);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // Add message.
        String msgContent = new String(ByteUtil.getContent(new File(
            LC.zimbra_home.value() + "/unittest/testZimbraReceivedHeader.msg")));
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgContent);

        // Test date.
        List<ZMessage> messages = TestUtil.search(mbox, "subject:testZimbraReceivedHeader");
        Assert.assertEquals("Unexpected message count", 1, messages.size());
        ZMessage msg = messages.get(0);
        TimeZone tz = mbox.getPrefs().getTimeZone();
        ZimbraLog.test.debug("Timezone ID for mbox %s is %s", mbox.getName(), tz.getID());
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(msg.getReceivedDate());
        Assert.assertEquals("Year wrong for received date", 2005, cal.get(Calendar.YEAR));
        Assert.assertEquals("Month wrong for received date", 1, cal.get(Calendar.MONTH));
        Assert.assertEquals("Day wrong for received date", 27, cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Confirms that <tt>X-Authenticated-User</tt> is set on outgoing messages when
     * <tt>zimbraSmtpSendAddAuthenticatedUser</tt> is set to <tt>TRUE</tt>.
     */
    @Test
    public void testAuthenticatedUserHeader()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        // X-Authenticated-User not sent.
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, LdapConstants.LDAP_FALSE);
        String subject = NAME_PREFIX + " testAuthenticatedUserHeader false";
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        Assert.assertNull(TestUtil.getHeaderValue(mbox, msg, MailSender.X_AUTHENTICATED_USER));

        // X-Authenticated-User sent.
        TestUtil.setConfigAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, LdapConstants.LDAP_TRUE);
        subject = NAME_PREFIX + " testAuthenticatedUserHeader true";
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        Assert.assertEquals(mbox.getName(), TestUtil.getHeaderValue(mbox, msg, MailSender.X_AUTHENTICATED_USER));
    }
    /**
     * Confirms that domain SMTP settings override server settings (bug 28442).
     */
    @Test
    public void testDomainSmtpSettings()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
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
            Assert.assertEquals(MailServiceException.TRY_AGAIN, e.getCode());
            sendFailed = true;
        }
        Assert.assertTrue("Message send should have failed", sendFailed);
    }

    @Test
    public void testBogusSmtpHostname()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        // Create a list that contains the original valid SMTP host
        // and a bunch of bogus ones.
        List<String> smtpHosts = new ArrayList<String>();
        Collections.addAll(smtpHosts, mOriginalSmtpHostname);
        for (int i = 1; i <= 10; i++) {
            smtpHosts.add("bogushost" + i);
        }
        String[] hostsArray = new String[smtpHosts.size()];
        smtpHosts.toArray(hostsArray);
        prov.getLocalServer().setSmtpHostname(hostsArray);

        // Send a message and make sure it arrives.
        String subject = NAME_PREFIX + " testBogusSmtpHostname";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.sendMessage(mbox, USER_NAME, subject);
        TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
    }

    /**
     * Confirms that we can forward attachments with a malformed content type (bug 42452).
     */
    @Test
    public void testMalformedContentType()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
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
                Assert.assertTrue("Unexpected line: " + line, line.contains(";;"));
                replaced = true;
            }
            msgBuf.append(line).append("\r\n");
            line = reader.readLine();
        }
        Assert.assertTrue("Could not find text/plain attachment.", replaced);

        // Add message to the mailbox.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessageLmtp(new String[] { USER_NAME }, USER_NAME, msgBuf.toString());

        // Forward the attachment in a new message.
        ZMessage srcMsg = TestUtil.getMessage(mbox, "subject:\"" + subject + "\"");
        ZMimePart srcAttachPart = srcMsg.getMimeStructure().getChildren().get(1);
        Assert.assertEquals("test.txt", srcAttachPart.getFileName());

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
        Assert.assertEquals("test.txt", fwdAttachPart.getFileName());
    }

    /**
     * Test inline attachment. See bug #88933
     */
    @Test
    public void testInlineAttachment()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        //create inline image attachment
        String imgName = "synacore.jpg";
        String content = "<content><img src=\"cid:" + imgName +"\"></content>";
        List<AttachedMessagePart> inlineAttachments = new ArrayList<AttachedMessagePart>();
        String attachId = mbox.uploadAttachment(imgName, new byte[1024], MimeConstants.CT_IMAGE_JPEG, 5000);
        AttachedMessagePart inlineImg = new AttachedMessagePart(attachId, imgName);
        inlineAttachments.add(inlineImg);
        MessagePart html = new MessagePart(MimeConstants.CT_TEXT_HTML, content, inlineAttachments);

        //create outgoing message
        String subject = NAME_PREFIX + " testInlineAttachment " + MimeConstants.CT_TEXT_PLAIN + " 1";
        ZOutgoingMessage outgoingMsg = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses.add(new ZEmailAddress(TestUtil.addDomainIfNecessary(USER_NAME),
            null, null, ZEmailAddress.EMAIL_TYPE_TO));
        outgoingMsg.setAddresses(addresses);
        outgoingMsg.setSubject(subject);
        MessagePart related = new MessagePart(MimeConstants.CT_MULTIPART_RELATED, html);
        outgoingMsg.setMessagePart(related);
        mbox.sendMessage(outgoingMsg, null, false);

        //verify that received message has the attachment
        ZMessage incoming = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        Assert.assertTrue("this message should have an attachment", incoming.hasAttachment());
        Assert.assertNotNull("this message should have mime parts", incoming.getMimeStructure());
        Assert.assertNotNull("this message should have two subparts", incoming.getMimeStructure().getChildren());
        List<ZMimePart> parts = incoming.getMimeStructure().getChildren();
        Assert.assertEquals("this message should have 2 subparts", 2,parts.size());
        Assert.assertTrue("one of the parts should have a content id", parts.get(0).getContentId() != null || parts.get(1).getContentId() != null);
        Assert.assertTrue("one of the parts should be " + imgName, imgName.equalsIgnoreCase(parts.get(0).getFileName())
                || imgName.equalsIgnoreCase(parts.get(1).getFileName()));
    }

    /**
     * Confirms that we preserve line endings of attached text files (bugs 45858 and 53405).
     */
    @Test
    public void testTextAttachmentLineEnding()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        String content = "I used to think that the day would never come,\n" +
            "I'd see the light in the shade of the morning sun\n";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        verifyTextAttachmentLineEnding(mbox, content, MimeConstants.CT_TEXT_PLAIN);
        verifyTextAttachmentLineEnding(mbox, content, "application/x-shellscript");
    }

    @Test
    public void testSendDraftWithData() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        MailSender.registerPreSendMailListener(listener);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String subj = "Thorin";
        String body = "far over the misty mountains cold";
        String customHeaderValue = "https://zss.server/file2";
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(zmbox.getAuthToken());

        //create a draft with a custom mime header
        Element request = new Element.JSONElement(MailConstants.SAVE_DRAFT_REQUEST);
        Element el = request.addUniqueElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, subj);
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, body);
        el.addNonUniqueElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, TestUtil.addDomainIfNecessary(REMOTE_USER_NAME));
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, MailSender.PRE_SEND_HEADER)
            .setText("custom");
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, PUBLIC_LIST_HEADER)
            .setText(customHeaderValue);

        ZMessage draft = new ZMessage(transport.invoke(request).getElement(MailConstants.E_MSG),zmbox);

        //Send the draft and save to Sent folder
        request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        request.addUniqueElement(MailConstants.E_MSG)
                .addAttribute(MailConstants.A_DRAFT_ID, draft.getId())
                    .addAttribute(MailConstants.A_SEND_FROM_DRAFT, true)
                        .addAttribute(MailConstants.A_NO_SAVE_TO_SENT, false);

        transport.invoke(request);

        Assert.assertTrue("Listener was not triggered",listener.isTriggered());
        Assert.assertEquals("listener should ahve been triggered only once", 1,listener.getTriggerCounter());
        String[] listenersData = listener.getData();
        Assert.assertNotNull("Listener did not get data from custom headers", listenersData);
        Assert.assertTrue("wrong number of elements in custom header data",listenersData.length == 1);
        Assert.assertTrue(String.format("wrong value in custom header: %s",listenersData[0]), customHeaderValue.equalsIgnoreCase(listenersData[0].replace("\"", "")));
    }

    @Test
    public void testSendDraftWithCustomDataNoSave() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        MailSender.registerPreSendMailListener(listener);
        MailSender.registerPreSendMailListener(listener); //register second time to test that it will be triggered once
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String subj = "Dwalin";
        String body = "To dungeons deep, and caverns old";
        String customHeaderValue = "http://zss.server/file1";
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(zmbox.getAuthToken());

        //create a draft with a custom mime header
        Element request = new Element.JSONElement(MailConstants.SAVE_DRAFT_REQUEST);
        Element el = request.addUniqueElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, subj);
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, body);
        el.addNonUniqueElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, TestUtil.addDomainIfNecessary(REMOTE_USER_NAME));
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, MailSender.PRE_SEND_HEADER)
            .setText("custom");
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, PUBLIC_LIST_HEADER)
            .setText(customHeaderValue);

        ZMessage draft = new ZMessage(transport.invoke(request).getElement(MailConstants.E_MSG),zmbox);

        //send the draft but don't save to Sent folder
        request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        request.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_DRAFT_ID,  Integer.parseInt(draft.getId()))
            .addAttribute(MailConstants.A_SEND_FROM_DRAFT, true)
                .addAttribute(MailConstants.A_NO_SAVE_TO_SENT, true);

        transport.invoke(request);
        Assert.assertTrue("Listener was not triggered",listener.isTriggered());
        Assert.assertEquals("listener should ahve been triggered only once", 1,listener.getTriggerCounter());
        String[] listenersData = listener.getData();
        Assert.assertNotNull("Listener did not get data from custom headers", listenersData);
        Assert.assertTrue("wrong number of elements in custom header data",listenersData.length == 1);
        Assert.assertTrue(String.format("wrong value in custom header: %s",listenersData[0]), customHeaderValue.equalsIgnoreCase(listenersData[0].replace("\"", "")));
        Assert.assertNotNull("Listener did not get data from custom headers", listenersData);
    }

    @Test
    public void testSendNoDraft() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        MailSender.registerPreSendMailListener(listener);
        MailSender.registerPreSendMailListener(listener); //register second time to test that it will be triggered once
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String subj = "Dwalin";
        String body = "To dungeons deep, and caverns old";
        String customHeaderValue = "http://zss.server/file1";
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(zmbox.getAuthToken());

        //create a draft with a custom mime header
        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        Element el = request.addUniqueElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, subj);
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, body);
        el.addNonUniqueElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, TestUtil.addDomainIfNecessary(REMOTE_USER_NAME));
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, MailSender.PRE_SEND_HEADER)
            .setText("custom");
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, PUBLIC_LIST_HEADER)
            .setText(customHeaderValue);

        transport.invoke(request);

        Assert.assertTrue("Listener was not triggered",listener.isTriggered());
        Assert.assertEquals("listener should ahve been triggered only once", 1,listener.getTriggerCounter());
        String[] listenersData = listener.getData();
        Assert.assertNotNull("Listener did not get data from custom headers", listenersData);
        Assert.assertTrue("wrong number of elements in custom header data",listenersData.length == 1);
        Assert.assertTrue("wrong value in custom header", customHeaderValue.equalsIgnoreCase(listenersData[0].replace("\"", "")));
    }

    @Test
    public void testMultipleHeaders() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        MailSender.registerPreSendMailListener(listener);
        MailSender.registerPreSendMailListener(listener); //register second time to test that it will be triggered once
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String subj = "Dwalin";
        String body = "To dungeons deep, and caverns old";
        String customHeaderValue1 = "http://zss.server/file1";
        String customHeaderValue2 = "http://zss.server/file2";
        String customHeaderValue3 = "http://zss.server/file3";

        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(zmbox.getAuthToken());

        //create a draft with a custom mime header
        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        Element el = request.addUniqueElement(MailConstants.E_MSG);
        el.addAttribute(MailConstants.E_SUBJECT, subj);
        el.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
            .addAttribute(MailConstants.E_CONTENT, body);
        el.addNonUniqueElement(MailConstants.E_EMAIL)
            .addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString())
            .addAttribute(MailConstants.A_ADDRESS, TestUtil.addDomainIfNecessary(REMOTE_USER_NAME));
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, MailSender.PRE_SEND_HEADER)
            .setText("custom");
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, PUBLIC_LIST_HEADER)
            .setText(customHeaderValue1);
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, PUBLIC_LIST_HEADER)
            .setText(customHeaderValue2);
        el.addNonUniqueElement(MailConstants.E_HEADER)
            .addAttribute(MailConstants.A_NAME, PUBLIC_LIST_HEADER)
            .setText(customHeaderValue3);

        transport.invoke(request);

        Assert.assertTrue("Listener was not triggered",listener.isTriggered());
        Assert.assertEquals("listener should ahve been triggered only once", 1,listener.getTriggerCounter());
        String[] listenersData = listener.getData();
        Assert.assertNotNull("Listener did not get data from custom headers", listenersData);
        Assert.assertEquals("wrong number of elements in custom header data",3,listenersData.length);
        List<String> valueList = Arrays.asList(listenersData);
        Assert.assertTrue(customHeaderValue1 + " is missing", valueList.contains(String.format("\"%s\"",customHeaderValue1)));
        Assert.assertTrue(customHeaderValue2 + " is missing", valueList.contains(String.format("\"%s\"",customHeaderValue2)));
        Assert.assertTrue(customHeaderValue3 + " is missing", valueList.contains(String.format("\"%s\"",customHeaderValue3)));
    }

    @Test
    public void testSendNoHeaders() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        MailSender.registerPreSendMailListener(listener);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String subj = "Balin";
        String body = "We must away ere break of day";
        TestUtil.sendMessage(zmbox, REMOTE_USER_NAME, subj, body);
        Assert.assertFalse("Listener was not supposed to be triggered",listener.isTriggered());
    }

    @Test
    public void testSendDraftNoHeader() throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        MailSender.registerPreSendMailListener(listener);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        String subj = "Thorin";
        String body = "far over the misty mountains cold";
        ZOutgoingMessage msg = TestUtil.getOutgoingMessage(REMOTE_USER_NAME, subj, body,null);

        ZMessage draft = zmbox.saveDraft(msg, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS));

        Element request = new Element.JSONElement(MailConstants.SEND_MSG_REQUEST);
        request.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_DRAFT_ID, draft.getId()).addAttribute(MailConstants.A_SEND_FROM_DRAFT, true);
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        transport.setAuthToken(zmbox.getAuthToken());
        transport.invoke(request);

        Assert.assertFalse("Listener was not supposed to be triggered",listener.isTriggered());
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
        Assert.assertEquals(content, attachContent);

        // Test save draft and send.
        attachId = mbox.uploadAttachment("text.txt", content.getBytes(), contentType, 5000);
        subject = NAME_PREFIX + " testTextAttachmentLineEnding " + contentType + " 2";
        TestUtil.saveDraftAndSendMessage(mbox, USER_NAME, subject, "Testing text attachment", attachId);

        msg = TestUtil.waitForMessage(mbox, "in:inbox subject:\"" + subject + "\"");
        in = mbox.getRESTResource("?id=" + msg.getId() + "&part=2");
        attachContent = new String(ByteUtil.getContent(in, content.length()));
        Assert.assertEquals(content, attachContent);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSendAndReceive.class);
    }

    private static class TestSendMailListener implements MailSender.PreSendMailListener {
        private boolean triggered = false;
        private String[] data = null;
        private int triggerCounter = 0;
        public TestSendMailListener() {
            triggerCounter++;
        }

        @Override
        public void handle(Mailbox mbox, Address[] sentAddresses,
                MimeMessage sentMessage) {
            ZimbraLog.test.debug("Handling sent mail notification");
            triggered = true;
            try {
                data = sentMessage.getHeader(PUBLIC_LIST_HEADER);
                if(data == null) {
                    ZimbraLog.test.error("Could not find " +  PUBLIC_LIST_HEADER);
                }
            }  catch (MessagingException e) {
                ZimbraLog.test.error("failed to extract custom mime header",e);
            }
        }

        public String[]  getData() {
            return data;
        }

        @Override
        public String getName() {
            return TestSendAndReceive.TestSendMailListener.class.getSimpleName();
        }

        public boolean isTriggered() {
            return triggered;
        }

        public int getTriggerCounter() {
            return triggerCounter;
        }
    }
}
