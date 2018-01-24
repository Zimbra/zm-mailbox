/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.util.JMSession;

public class TestMailSender {

    @Rule
    public TestName testInfo = new TestName();

    static final int TEST_SMTP_PORT = 6025;
    private static final String NAME_PREFIX = TestMailSender.class.getSimpleName();
    private static String SENDER_NAME = "user1";
    private static String RECIPIENT_NAME = "user2";
    private String mOriginalSmtpPort = null;
    private String mOriginalSmtpSendPartial;

    @Before
    public void setUp()
    throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        SENDER_NAME = prefix + "sender";
        RECIPIENT_NAME = prefix + "recipient";
        cleanUp();
        mOriginalSmtpPort = Provisioning.getInstance().getLocalServer().getSmtpPortAsString();
        mOriginalSmtpSendPartial = TestUtil.getServerAttr(Provisioning.A_zimbraSmtpSendPartial);
        TestUtil.createAccount(SENDER_NAME);
        TestUtil.createAccount(RECIPIENT_NAME);
    }

    @After
    public void tearDown()
    throws Exception {
        cleanUp();
        TestUtil.setServerAttr(Provisioning.A_zimbraSmtpPort, mOriginalSmtpPort);
        TestUtil.setServerAttr(Provisioning.A_zimbraSmtpSendPartial, mOriginalSmtpSendPartial);
    }
    private void cleanUp()
    throws Exception {
        TestUtil.deleteAccountIfExists(SENDER_NAME);
        TestUtil.deleteAccountIfExists(RECIPIENT_NAME);
    }

    @Test
    public void testRejectRecipient()
    throws Exception {
        String errorMsg = "Sender address rejected: User unknown in relay recipient table";
        String bogusAddress = TestUtil.getAddress("bogus");
        startDummySmtpServer(bogusAddress, errorMsg);
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSmtpPort(TEST_SMTP_PORT);

        String content = TestUtil.getTestMessage(NAME_PREFIX + " testRejectSender", bogusAddress, SENDER_NAME, null);
        MimeMessage msg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        Mailbox mbox = TestUtil.getMailbox(SENDER_NAME);

        // Test reject first recipient, get partial send value from LDAP.
        boolean sendFailed = false;
        server.setSmtpSendPartial(false);
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_ABORTED_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        Assert.assertTrue(sendFailed);

        // Test reject first recipient, set partial send value explicitly.
        startDummySmtpServer(bogusAddress, errorMsg);
        sendFailed = false;
        server.setSmtpSendPartial(true);
        MailSender sender = mbox.getMailSender().setSendPartial(false);

        try {
            sender.sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_ABORTED_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        Assert.assertTrue(sendFailed);

        // Test reject second recipient, get partial send value from LDAP.
        startDummySmtpServer(bogusAddress, errorMsg);
        sendFailed = false;
        String validAddress = TestUtil.getAddress(RECIPIENT_NAME);
        InternetAddress[] recipients = new InternetAddress[2];
        recipients[0] = new JavaMailInternetAddress(validAddress);
        recipients[1] = new JavaMailInternetAddress(bogusAddress);
        msg.setRecipients(MimeMessage.RecipientType.TO, recipients);
        server.setSmtpSendPartial(false);
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_ABORTED_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        Assert.assertTrue(sendFailed);

        // Test partial send, get value from LDAP.
        startDummySmtpServer(bogusAddress, errorMsg);
        server.setSmtpSendPartial(true);
        sendFailed = false;
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE, bogusAddress, null);
            sendFailed = true;
        }
        Assert.assertTrue(sendFailed);

        // Test partial send, specify value explicitly.
        server.setSmtpSendPartial(false);
        startDummySmtpServer(bogusAddress, errorMsg);
        sendFailed = false;
        sender = mbox.getMailSender().setSendPartial(true);
        try {
            sender.sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            // Don't check error message.  JavaMail does not give us the SMTP protocol error in the
            // partial send case.
            validateException(e, MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE, bogusAddress, null);
            sendFailed = true;
        }
        Assert.assertTrue(sendFailed);
    }

    @Test
    public void testRestrictEnvelopeSender()
    throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSmtpPort(TEST_SMTP_PORT);

        Mailbox mbox = TestUtil.getMailbox(SENDER_NAME);
        Account account = mbox.getAccount();

        // Create a message with a different From header value.
        String from = TestUtil.getAddress("testRestrictEnvelopeSender");
        String subject = NAME_PREFIX + " testRestrictEnvelopeSender";
        MessageBuilder builder = new MessageBuilder().withFrom(from).withToRecipient(RECIPIENT_NAME)
            .withSubject(subject).withBody("Who are you?");
        String content = builder.create();
        MimeMessage msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));

        account.setSmtpRestrictEnvelopeFrom(true);

        // Restrict envelope sender, disallow custom from.
        account.setAllowAnyFromAddress(false);
        DummySmtpServer smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        Assert.assertEquals(account.getName(), smtp.getMailFrom());
        // Test contains to handle personal name
        Assert.assertTrue(getHeaderValue(smtp.getDataLines(), "From").contains(account.getName()));

        // Restrict envelope sender, allow custom from.
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        account.setAllowAnyFromAddress(true);
        smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        Assert.assertEquals(account.getName(), smtp.getMailFrom());
        Assert.assertEquals(from, getHeaderValue(smtp.getDataLines(), "From"));

        account.setSmtpRestrictEnvelopeFrom(false);

        // Don't restrict envelope sender, disallow custom from.
        account.setAllowAnyFromAddress(false);
        smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        Assert.assertEquals(account.getName(), smtp.getMailFrom());
        Assert.assertTrue(getHeaderValue(smtp.getDataLines(), "From").contains(account.getName()));

        // Don't restrict envelope sender, allow custom from.
        msg = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        account.setAllowAnyFromAddress(true);
        smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        Assert.assertEquals(from, smtp.getMailFrom());
        Assert.assertEquals(from, getHeaderValue(smtp.getDataLines(), "From"));
    }

    private String getHeaderValue(List<String> dataLines, String headerName) {
        if (dataLines == null) {
            return null;
        }

        Pattern pat = Pattern.compile(headerName + ":\\s+(.*)");
        for (String line : dataLines) {
            Matcher m = pat.matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    private DummySmtpServer startDummySmtpServer(String rejectedRecipient, String errorMsg) {
        DummySmtpServer smtp = new DummySmtpServer(TEST_SMTP_PORT);
        smtp.setRejectedRecipient(rejectedRecipient, errorMsg);
        Thread smtpServerThread = new Thread(smtp);
        smtpServerThread.start();
        return smtp;
    }

    private void validateException(MailServiceException e, String expectedCode, String invalidRecipient, String errorSubstring) {
        Assert.assertEquals(expectedCode, e.getCode());
        if (errorSubstring != null) {
            Assert.assertTrue("Error did not contain '" + errorSubstring + "': " + e.getMessage(), e.getMessage().contains(errorSubstring));
        }

        boolean foundRecipient = false;
        for (Argument arg : e.getArgs()) {
            if (arg.name.equals("invalid")) {
                Assert.assertEquals(invalidRecipient, arg.value);
                foundRecipient = true;
            }
        }
        Assert.assertTrue(foundRecipient);
    }

    public static void main(String[] args)
    throws Exception {
        // Simply starts the test SMTP server for ad-hoc testing.  Doesn't
        // run unit tests, since they need to run on the server side.
        DummySmtpServer smtp = new DummySmtpServer(TEST_SMTP_PORT);
        if (args.length >= 2) {
            smtp.setRejectedRecipient(args[0], args[1]);
        }
        Thread thread = new Thread(smtp, DummySmtpServer.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }
}
