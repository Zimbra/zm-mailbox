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

import com.zimbra.cs.datasource.imap.ImapAppender;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import org.junit.*;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import static org.junit.Assert.*;

import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ResponseHandler;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.ImapUtil;
import com.zimbra.cs.mailclient.util.SSLUtil;
import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.util.JMSession;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Date;
import java.util.Random;
import java.net.Socket;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

public class TestImapClient {
    private ImapConfig config;
    private ImapConnection connection;

    private static final Logger LOG = Logger.getLogger(TestImapClient.class);
    
    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final int SSL_PORT = 7993;
    private static final String USER = "user1";
    private static final String PASS = "test123";

    private static final String MESSAGE =
        "Return-Path: dac@zimbra.com\r\n" +
        "Date: Fri, 27 Feb 2004 15:24:43 -0800 (PST)\r\n" +
        "From: dac <dac@zimbra.com>\r\n" +
        "To: bozo <bozo@foo.com>\r\n" +
        "\r\n" +
        "This is a test message.\r\n";

    private static final boolean DEBUG = true;

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
        config = null;
        connection = null;
    }

    @Test
    public void testLogin() throws Exception {
        connect();
        connection.login(PASS);
    }

    @Test
    public void testSSLLogin() throws Exception {
        connect(MailConfig.Security.SSL);
        connection.login(PASS);
    }

    @Test
    public void testStartTls() throws Exception {
        connect(MailConfig.Security.TLS);
        connection.login(PASS);
    }

    @Test
    public void testPlainAuth() throws Exception {
        try {
        connect();
        connection.authenticate(PASS);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBadAuth() throws Exception {
        connect();
        try {
            connection.authenticate("foobaz");
        } catch (CommandFailedException e) {
            return;
        }
        throw new Exception("Expected auth failure");
    }

    @Test
    public void testSelect() throws Exception {
        login();
        MailboxInfo mb = connection.getMailboxInfo();
        assertNotNull(mb);
        assertTrue(mb.isReadWrite());
        assertTrue(mb.getUidValidity() > 0);
        assertTrue(mb.getUidNext() > 0);
    }

    @Test
    public void testList() throws Exception {
        login();
        List<ListData> lds = connection.list("", "*");
        for (ListData ld : lds) {
            assertEquals('/', ld.getDelimiter());
            assertNotNull(ld.getMailbox());
            assertTrue(ld.getFlags().size() > 0);
        }
    }

    @Test
    public void testIdle() throws Exception {
        login();
        assertFalse(connection.isIdling());
        final AtomicLong exists = new AtomicLong(-1);
        // Start IDLE...
        connection.idle(new ResponseHandler() {
            public void handleResponse(ImapResponse res) {
                System.out.println("XXX res = " + res);
                if (res.getCCode() == CAtom.EXISTS) {
                    synchronized (exists) {
                        exists.set(res.getNumber());
                    }
                }
            }
        });
        assertTrue(connection.isIdling());
        // Send test message
        sendTestMessage();
        // Wait for message delivery...
        synchronized (exists) {
            while (exists.get() <= 0) {
                exists.wait();
            }
        }
        // Stop IDLE...
        connection.stopIdle();
        // Check mailbox status
        MailboxInfo mb = connection.getMailboxInfo();
        assertEquals(mb.getExists(), exists.get());
    }

    private void sendTestMessage() throws IOException {
        Socket sock = new Socket("localhost", 7025);
        Writer out = new OutputStreamWriter(sock.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        smtpSend(in, out, "LHLO localhost");
        smtpSend(in, out, "MAIL FROM: <user1@localhost>");
        smtpSend(in, out, "RCPT TO: <user1>");
        smtpSend(in, out, "DATA");
        smtpSend(in, out, "Hello, world\r\n.\r\n");
        smtpSend(in, out, "QUIT");
    }

    private static void smtpSend(BufferedReader in, Writer out, String cmd) throws IOException {
        System.out.println("SMTP C: " + cmd);
        out.write(cmd);
        out.write("\r\n");
        out.flush();
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("SMTP S: " + line);
            if (line.matches("5.. .*")) {
                throw new IOException("SMTP command failed: " + line);
            }
            if (!line.matches("2..-.*")) {
                return;
            }
        }
    }
    
    @Test
    public void testAppend() throws Exception {
        login();
        MailboxInfo mb = connection.select("INBOX");
        long exists = mb.getExists();
        Date date = new Date((System.currentTimeMillis() / 1000) * 1000);
        Flags flags = Flags.fromSpec("fs");
        AppendResult res = connection.append("INBOX", Flags.fromSpec("fs"), date,
            new Literal(Ascii.getBytes(MESSAGE)));
        assertNotNull(res);
        mb = connection.select("INBOX");
        assertEquals(1, mb.getExists() - exists);
        MessageData md = connection.uidFetch(res.getUid(), "(FLAGS BODY.PEEK[] INTERNALDATE)");
        assertNotNull(md);
        assertEquals(date, md.getInternalDate());
        assertEquals(res.getUid(), md.getUid());
        assertEquals(flags, md.getFlags());
        Body[] parts = md.getBodySections();
        assertNotNull(parts);
        assertEquals(1, parts.length);
    }

    @Test
    public void testDelete() throws Exception {
        login();
        MailboxInfo mb = connection.select("INBOX");
        long exists = mb.getExists();
        AppendResult res = connection.append("INBOX", Flags.fromSpec("fs"),
            new Date(System.currentTimeMillis()), new Literal(Ascii.getBytes(MESSAGE)));
        assertNotNull(res);
        mb = connection.select("INBOX");
        assertEquals(exists, mb.getExists() - 1);
        connection.uidStore(String.valueOf(res.getUid()), "+FLAGS.SILENT", Flags.fromSpec("d"));
        mb = connection.select("INBOX");
        assertEquals(exists, mb.getExists() - 1);
        connection.expunge();
        mb = connection.select("INBOX");
        assertEquals(exists, mb.getExists());
    }

    @Test
    public void testFetch() throws Exception {
        connect();
        login();
        MailboxInfo mb = connection.select("INBOX");
        final AtomicLong count = new AtomicLong(mb.getExists());
        connection.uidFetch("1:*", "(FLAGS INTERNALDATE RFC822.SIZE ENVELOPE BODY BODY.PEEK[])", new ResponseHandler() {
            public void handleResponse(ImapResponse res) throws Exception {
                if (res.getCCode() != CAtom.FETCH) return;
                MessageData md = (MessageData) res.getData();
                assertNotNull(md);
                Envelope env = md.getEnvelope();
                assertNotNull(env);
                assertNotNull(env.getSubject());
                assertNotNull(md.getUid());
                assertTrue(md.getRfc822Size() != -1);
                assertNotNull(md.getInternalDate());
                BodyStructure bs = md.getBodyStructure();
                assertNotNull(bs);
                if (bs.isMultipart()) {
                    BodyStructure[] parts = bs.getParts();
                    for (BodyStructure part : parts) {
                        assertNotNull(part.getType());
                        assertNotNull(part.getSubtype());
                    }
                } else {
                    assertNotNull(bs.getType());
                    assertNotNull(bs.getSubtype());
                }
                Body[] body = md.getBodySections();
                assertNotNull(body);
                assertEquals(1, body.length);
                // assertNotNull(body[0].getBytes());
                count.decrementAndGet();
                System.out.printf("Fetched uid = %s\n", md.getUid());
            }
        });
        assertEquals(0, count.longValue());
    }

    @Test
    public void testID() throws Exception {
        IDInfo id = new IDInfo();
        id.setName("foo");
        assertEquals("foo", id.getName());
        assertEquals("foo", id.get("Name"));
        connect();
        IDInfo id1 = connection.id(id);
        assertNotNull(id1);
        assertEquals("Zimbra", id1.getName());
        IDInfo id2 = connection.id();
        assertEquals(id1, id2);
    }

    @Test
    public void testYahoo() throws Exception {
        ImapConfig config = new ImapConfig();
        config.setDebug(true);
        config.setTrace(true);
        config.setHost("imap.mail.yahoo.com");
        config.setAuthenticationId("dacztest");
        connection = new ImapConnection(config);
        connection.connect();
        IDInfo id = new IDInfo();
        id.put("guid", "unknown");
        connection.id(id);
        connection.login("test1234");
        char delim = connection.getDelimiter();
        assertEquals(0, delim);
        createTestMailbox("Large", 10000);
    }

    @Test
    public void testGMailAppend() throws Exception {
        ImapConfig config = new ImapConfig();
        config.setDebug(true);
        config.setTrace(true);
        config.setHost("imap.gmail.com");
        config.setSecurity(MailConfig.Security.SSL);
        config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        config.setAuthenticationId("dacztest");
        config.setMaxLiteralTraceSize(999999);
        connection = new ImapConnection(config);
        connection.connect();
        connection.login("test1234");
        MimeMessage mm = newTestMessage(new Random().nextInt());
        // Append and find unique message
        ImapAppender appender = new ImapAppender(connection, "INBOX");
        long uid1 = appender.appendMessage(getBytes(mm), null);
        System.out.println("XXX uid1 = " + uid1);
        assertTrue("uid1 not found", uid1 > 0);
        // Now append message again and make sure we can find the de-duped copy
        long uid2 = appender.appendMessage(getBytes(mm), null);
        assertTrue("uid2 not found", uid2 > 0);
        assertEquals(uid1, uid2);
        connection.close();
    }

    @Test
    public void testParseUidSet() {
        long[] seq = new long[] { 1, 2, 3, 4, 5 };
        assertArrayEquals(seq, ImapUtil.parseUidSet("1,2,3,4,5"));
        assertArrayEquals(seq, ImapUtil.parseUidSet("1,2:4,5"));
        assertArrayEquals(seq, ImapUtil.parseUidSet("4:1,5"));
        try {
            ImapUtil.parseUidSet("4::1,4");
            fail();
        } catch (IllegalArgumentException e) {}
        try {
            ImapUtil.parseUidSet("");
            fail();
        } catch (IllegalArgumentException e) {}
        try {
            ImapUtil.parseUidSet("1,,2");
            fail();
        } catch (IllegalArgumentException e) {}
    }
    
    private byte[] getBytes(MimeMessage mm) throws MessagingException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mm.writeTo(baos);
        return baos.toByteArray();
    }

    private void createTestMailbox(String name, int count)
        throws IOException, MessagingException {
        if (!connection.exists(name)) {
            connection.create(name);
        }
        MailboxInfo mb = connection.select(name);
        for (int i = (int) mb.getExists(); i < count; i++) {
            MimeMessage mm = newTestMessage(i);
            long uid = uidAppend(mm, null, null);
            LOG.debug("Appended test message " + i + ", uid = " + uid);
        }
    }

    private static MimeMessage newTestMessage(int num) throws MessagingException {
        MimeMessage mm = new MimeMessage(JMSession.getSession());
        mm.setHeader("Message-Id", "<test-" + num + "@foo.com>");
        mm.setHeader("To", "nobody@foo.com");
        mm.setHeader("From", "nobody@bar.com");
        mm.setSubject("Test message " + num);
        mm.setSentDate(new Date(System.currentTimeMillis()));
        mm.setContent("This is test message " + num, "text/plain");
        return mm;
    }
    
    private long uidAppend(MimeMessage msg, Flags flags, Date date)
        throws IOException {
        String name = connection.getMailboxInfo().getName();
        File tmp = null;
        OutputStream os = null;
        try {
            File dir = connection.getImapConfig().getLiteralDataDir();
            tmp = File.createTempFile("lit", null, dir);
            os = new FileOutputStream(tmp);
            msg.writeTo(os);
            os.close();
            AppendResult res = connection.append(name, flags, date, new Literal(tmp));
            return res != null ? res.getUid() : -1;
        } catch (MessagingException e) {
            throw new MailException("Error appending message", e);
        } finally {
            if (os != null) os.close();
            if (tmp != null) tmp.delete();
        }
    }

    /*
    public void testMailboxName() throws ParseException {
        testMailboxName("/go&h\\+o");
        testMailboxName("~peter/mail/&U,BTFw-/&ZeVnLIqe-");
        testMailboxName("Inbox/Foo");
        testMailboxName("Inbox\u0000Foo");
        testMailboxName("Foo\u0001\u0020\\Foo");
    }

    private static final Charset IMAP_UTF7 =
        new ImapUTF7("imap-utf-7", new String[] {});

    private void testMailboxName(String name) throws ParseException {
        String encoded1 = new MailboxName(name).encode();
        String encoded2 = Ascii.toString(IMAP_UTF7.encode(name));
        assertEquals(encoded2, encoded1);
        String decoded1 = MailboxName.decode(encoded1).toString();
        CharBuffer cb = IMAP_UTF7.decode(ByteBuffer.wrap(Ascii.getBytes(encoded1)));
        char[] cs = new char[cb.remaining()];
        for (int i = 0; cb.hasRemaining(); i++) {
            cs[i] = cb.get();
        }
        String decoded2 = new String(cs);
        assertEquals(decoded2, decoded1);
    }
    */
    
    /*
    public void testLiteral() throws Exception {
        connect(false);
        Object[] parts = new Object[] { USER, " ", PASS.getBytes() };
        mConnection.sendCommand("LOGIN", parts, false);
    }

    public void testBigLiteral() throws Exception {
        testBigLiteral(false);
    }

    public void testBigLiteralSync() throws Exception {
        testBigLiteral(true);
    }

    private void testBigLiteral(boolean sync) throws Exception {
        connect(false);
        byte[] lit1 = fill(new byte[13000000], 'x');
        byte[] lit2 = fill(new byte[100], 'y');
        Object[] parts = new Object[] { USER, " ", lit1, " ", lit2, "FOO"};
        try {
            mConnection.sendCommand("LOGIN", parts, sync);
        } catch (MailException e) {
            String msg = mConnection.getResponse();
            assertTrue("Expected [TOOBIG] response", msg.contains("[TOOBIG]"));
            return;
        }
        throw new AssertionError("Expected LOGIN command to fail");
    }

    private static byte[] fill(byte[] b, int c) {
        for (int i = 0; i < b.length; i++) b[i] = (byte) c;
        return b;
    }

    */

    private void login() throws IOException {
        connect();
        connection.login(PASS);
        connection.select("INBOX");
        assertTrue(connection.getMailboxInfo().getName().equals("INBOX"));
        ImapCapabilities cap = connection.getCapabilities();
        assertNotNull(cap);
        assertTrue(cap.hasCapability(ImapCapabilities.UIDPLUS));
        assertTrue(cap.hasCapability("UNSELECT"));
    }
    
    private void connect() throws IOException {
        connect(null);
    }
    
    private void connect(MailConfig.Security security) throws IOException {
        if (config == null) {
            config = getConfig(security);
        }
        System.out.println("---------");
        connection = new ImapConnection(config);
        connection.connect();
    }

    private static ImapConfig getConfig(MailConfig.Security security) throws IOException {
        ImapConfig config = new ImapConfig(HOST);
        config.setPort(PORT);
        if (security != null) {
            config.setSecurity(security);
            if (security == MailConfig.Security.SSL) {
                config.setPort(SSL_PORT);
                config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
            }
        }
        config.setDebug(DEBUG);
        config.setTrace(true);
        config.setMechanism("PLAIN");
        config.setAuthenticationId(USER);
        //config.setRawMode(true);
        return config;
    }

    public static void main(String... args) throws Exception {
        JUnitCore junit = new JUnitCore();
        if (args.length > 0) {
            for (String test : args) {
                String method = String.format("test%C%s", test.charAt(0), test.substring(1));
                junit.run(Request.method(TestImap.class, method));
            }
        } else {
            junit.run(TestImap.class);
        }
    }
}
