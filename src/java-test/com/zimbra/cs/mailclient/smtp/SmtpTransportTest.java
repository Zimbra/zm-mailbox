/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.smtp;

import static com.google.common.base.Charsets.ISO_8859_1;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.common.mime.shim.JavaMailMimeMessage;
import com.zimbra.cs.util.MockTcpServer;
import com.zimbra.cs.util.JMSession;

/**
 * Unit test for {@link SmtpTransport}.
 *
 * @author ysasaki
 */
public final class SmtpTransportTest {
    private static final int PORT = 9025;
    private MockTcpServer server;

    @After
    public void tearDown() {
        Properties props = JMSession.getSession().getProperties();
        props.remove("mail.smtp.sendpartial");
        props.remove("mail.smtp.from");
        JMSession.getSession().getProperties().remove("mail.smtp.sendpartial");
        if (server != null) {
            server.destroy();
        }
    }

    @Test(timeout = 3000)
    public void send() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("354 OK")
        .swallowUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .sendLine("221 bye")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void sendPartially() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO 1
        .sendLine("250 OK")
        .recvLine() // RCPT TO 2
        .sendLine("550 not found")
        .recvLine() // RCPT TO 3
        .sendLine("550 not found")
        .recvLine() // DATA
        .sendLine("354 OK")
        .swallowUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .sendLine("221 bye")
        .build().start(PORT);

        Session session = JMSession.getSession();
        session.getProperties().setProperty("mail.smtp.sendpartial", "true");
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\n" +
            "To: rcpt1@zimbra.com, rcpt2@zimbra.com, rcpt3@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        try {
            transport.sendMessage(msg, msg.getAllRecipients());
        } catch (SendFailedException e) {
            Assert.assertEquals(1, e.getValidSentAddresses().length);
            Assert.assertEquals(0, e.getValidUnsentAddresses().length);
            Assert.assertEquals(2, e.getInvalidAddresses().length);
        } finally {
            transport.close();
        }

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt1@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt2@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt3@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void mailFromError() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("451 error")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\n" +
            "Subject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        try {
            transport.sendMessage(msg, msg.getAllRecipients());
            Assert.fail();
        } catch (SendFailedException e) {
            Assert.assertEquals(0, e.getValidSentAddresses().length);
            Assert.assertEquals(0, e.getValidUnsentAddresses().length);
            Assert.assertEquals(0, e.getInvalidAddresses().length);
        } finally {
            transport.close();
        }

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void mailSmtpFrom() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("354 OK")
        .swallowUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .sendLine("221 bye")
        .build().start(PORT);

        Session session = JMSession.getSession();
        session.getProperties().setProperty("mail.smtp.from", "from@zimbra.com");
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<from@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void bracketsInMailAndRcpt() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("354 OK")
        .swallowUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .sendLine("221 bye")
        .build().start(PORT);

        Session session = JMSession.getSession();
        session.getProperties().setProperty("mail.smtp.from", "<>");
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(ISO_8859_1)));
        transport.sendMessage(msg, new Address[] { new InternetAddress("<rcpt@zimbra.com>") });
        transport.close();

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void auth() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250-smtp.zimbra.com")
        .sendLine("250 AUTH LOGIN")
        .recvLine() // AUTH
        .sendLine("334 OK")
        .recvLine() // USER
        .sendLine("334")
        .recvLine() // PASSWORD
        .sendLine("235 Authentication successful")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("354 OK")
        .swallowUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .sendLine("221 bye")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, "zimbra", "secret");
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("AUTH LOGIN\r\n", server.replay());
        Assert.assertEquals(base64("zimbra") + "\r\n", server.replay());
        Assert.assertEquals(base64("secret") + "\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void noAuth() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        try {
            transport.connect("localhost", PORT, "zimbra", "secret");
            Assert.fail();
        } catch (MessagingException e) {
            Assert.assertEquals("The server doesn't support authentication.", e.getMessage());
        }
        server.shutdown(1000);
    }

    @Test(timeout = 3000)
    public void noAuthMechansims() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250-OK")
        .sendLine("250 AUTH PLAIN NTLM")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        try {
            transport.connect("localhost", PORT, "zimbra", "secret");
            Assert.fail();
        } catch (MessagingException e) {
            Assert.assertEquals("No authentication mechansims supported: [NTLM, PLAIN]", e.getMessage());
        }
        server.shutdown(1000);
    }

    private String base64(String src) {
        return new String(Base64.encodeBase64(src.getBytes()));
    }

    @Test(timeout = 3000)
    public void rcptToError() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("550 error")
        .build().start(PORT);

        Session session = JMSession.getSession();
        session.getProperties().setProperty("mail.smtp.sendpartial", "true");
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        try {
            transport.sendMessage(msg, msg.getAllRecipients());
            Assert.fail();
        } catch (SendFailedException e) {
            Assert.assertEquals(0, e.getValidSentAddresses().length);
            Assert.assertEquals(0, e.getValidUnsentAddresses().length);
            Assert.assertEquals(1, e.getInvalidAddresses().length);
        } finally {
            transport.close();
        }

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void endOfData() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("354 OK")
        .recvUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .sendLine("221 bye")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\n" +
            ".\n" +
            "..\n" +
            ".\n";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("From: sender@zimbra.com\r\nTo: rcpt@zimbra.com\r\nSubject: test\r\n\r\n" +
                "..\r\n" +
                "...\r\n" +
                "..\r\n" +
                ".\r\n",
                server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void dataError() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("451 error")
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        try {
            transport.sendMessage(msg, msg.getAllRecipients());
            Assert.fail();
        } catch (SendFailedException e) {
            Assert.assertEquals(1, e.getValidSentAddresses().length);
            Assert.assertEquals(0, e.getValidUnsentAddresses().length);
            Assert.assertEquals(0, e.getInvalidAddresses().length);
        } finally {
            transport.close();
        }

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void rset() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RSET
        .sendLine("250 OK")
        .build().start(PORT);

        Session session = JMSession.getSession();
        SmtpTransport transport = (SmtpTransport) session.getTransport("smtp");
        try {
            transport.connect("localhost", PORT, null, null);
            transport.mail("sender@zimbra.com");
            transport.rset();
        } finally {
            transport.close();
        }

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RSET\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

    @Test(timeout = 3000)
    public void quitNoResponse() throws Exception {
        server = MockTcpServer.scenario()
        .sendLine("220 test ready")
        .recvLine() // EHLO
        .sendLine("250 OK")
        .recvLine() // MAIL FROM
        .sendLine("250 OK")
        .recvLine() // RCPT TO
        .sendLine("250 OK")
        .recvLine() // DATA
        .sendLine("354 OK")
        .swallowUntil("\r\n.\r\n")
        .sendLine("250 OK")
        .recvLine() // QUIT
        .build().start(PORT);

        Session session = JMSession.getSession();
        Transport transport = session.getTransport("smtp");
        transport.connect("localhost", PORT, null, null);
        String raw = "From: sender@zimbra.com\nTo: rcpt@zimbra.com\nSubject: test\n\ntest";
        MimeMessage msg = new JavaMailMimeMessage(session, new ByteArrayInputStream(raw.getBytes(Charsets.ISO_8859_1)));
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

        server.shutdown(1000);
        Assert.assertEquals("EHLO localhost\r\n", server.replay());
        Assert.assertEquals("MAIL FROM:<sender@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("RCPT TO:<rcpt@zimbra.com>\r\n", server.replay());
        Assert.assertEquals("DATA\r\n", server.replay());
        Assert.assertEquals("QUIT\r\n", server.replay());
        Assert.assertNull(server.replay());
    }

}
