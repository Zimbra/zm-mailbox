/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ResponseHandler;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.Mailbox;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.MailboxName;
import com.zimbra.cs.mailclient.util.SSLUtil;
import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.ParseException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mime.charset.ImapUTF7;
import com.zimbra.cs.util.JMSession;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Date;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.junit.Assert;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

public class TestImapClient extends TestCase {
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
    
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
        config = null;
        connection = null;
    }

    public void testLogin() throws Exception {
        connect(false);
        connection.login(PASS);
    }

    public void testSSLLogin() throws Exception {
        connect(true);
        connection.login(PASS);
    }

    public void testPlainAuth() throws Exception {
        try {
        connect(false);
        connection.authenticate(PASS);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testBadAuth() throws Exception {
        connect(false);
        try {
            connection.authenticate("foobaz");
        } catch (CommandFailedException e) {
            return;
        }
        throw new Exception("Expected auth failure");
    }

    public void testSelect() throws Exception {
        login();
        Mailbox mb = connection.getMailbox();
        assertNotNull(mb);
        assertTrue(mb.isReadWrite());
        assertTrue(mb.getUidValidity() > 0);
        assertTrue(mb.getUidNext() > 0);
    }

    public void testList() throws Exception {
        login();
        List<ListData> lds = connection.list("", "");
        assertTrue(lds.size() == 1);
        assertEquals('/', lds.get(0).getDelimiter());
    }

    public void testAppend() throws Exception {
        login();
        Mailbox mb = connection.select("INBOX");
        long exists = mb.getExists();
        Date date = new Date((System.currentTimeMillis() / 1000) * 1000);
        Flags flags = Flags.fromSpec("fs");
        long uid = connection.append("INBOX", Flags.fromSpec("fs"), date,
                                     new Literal(Ascii.getBytes(MESSAGE)));
        assertTrue(uid > 0);
        mb = connection.select("INBOX");
        assertEquals(1, mb.getExists() - exists);
        MessageData md = connection.uidFetch(uid, "(FLAGS BODY.PEEK[] INTERNALDATE)");
        assertNotNull(md);
        assertEquals(date, md.getInternalDate());
        assertEquals(uid, md.getUid());
        assertEquals(flags, md.getFlags());
        Body[] parts = md.getBodySections();
        assertNotNull(parts);
        assertEquals(1, parts.length);
    }

    public void testDelete() throws Exception {
        login();
        Mailbox mb = connection.select("INBOX");
        long exists = mb.getExists();
        long uid = connection.append("INBOX", Flags.fromSpec("fs"),
            new Date(System.currentTimeMillis()), new Literal(Ascii.getBytes(MESSAGE)));
        assertTrue(uid > 0);
        mb = connection.select("INBOX");
        assertEquals(exists, mb.getExists() - 1);
        connection.uidStore(String.valueOf(uid), "+FLAGS.SILENT", Flags.fromSpec("d"));
        mb = connection.select("INBOX");
        assertEquals(exists, mb.getExists() - 1);
        connection.expunge();
        mb = connection.select("INBOX");
        assertEquals(exists, mb.getExists());
    }

    public void testFetch() throws Exception {
        login();
        Mailbox mb = connection.select("INBOX");
        final AtomicLong count = new AtomicLong(mb.getExists());
        connection.uidFetch("1:*", "(ENVELOPE UID)", new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCCode() != CAtom.FETCH) return false;
                MessageData md = (MessageData) res.getData();
                assertNotNull(md);
                assertNotNull(md.getEnvelope());
                assertNotNull(md.getUid());
                count.decrementAndGet();
                System.out.printf("Fetched uid = %s\n", md.getUid());
                return true;
            }
        });
        assertEquals(0, count.longValue());
    }

    public void testID() throws Exception {
        IDInfo id = new IDInfo();
        id.setName("foo");
        assertEquals("foo", id.getName());
        assertEquals("foo", id.get("Name"));
        connect(false);
        IDInfo id1 = connection.id(id);
        assertNotNull(id1);
        assertEquals("Zimbra", id1.getName());
        IDInfo id2 = connection.id();
        assertEquals(id1, id2);
    }

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

    private void createTestMailbox(String name, int count)
        throws IOException, MessagingException {
        if (!connection.exists(name)) {
            connection.create(name);
        }
        Mailbox mb = connection.select(name);
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
        String name = connection.getMailbox().getName();
        File tmp = null;
        OutputStream os = null;
        try {
            File dir = connection.getImapConfig().getLiteralDataDir();
            tmp = File.createTempFile("lit", null, dir);
            os = new FileOutputStream(tmp);
            msg.writeTo(os);
            os.close();
            return connection.append(name, flags, date, new Literal(tmp));
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
    }
    
    private void connect() throws IOException {
        connect(false);
    }
    
    private void connect(boolean ssl) throws IOException {
        if (config == null) {
            config = getConfig(ssl);
        }
        System.out.println("---------");
        connection = new ImapConnection(config);
        connection.connect();
    }

    private static ImapConfig getConfig(boolean ssl) throws IOException {
        ImapConfig config = new ImapConfig(HOST);
        config.setSslEnabled(ssl);
        config.setPort(ssl ? SSL_PORT : PORT);
        if (ssl) {
            config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        }
        config.setDebug(DEBUG);
        config.setTrace(true);
        config.setMechanism("PLAIN");
        config.setAuthenticationId(USER);
        //config.setRawMode(true);
        return config;
    }

    public static void main(String[] args) throws Exception {
        new TestImapClient().testYahoo();
    }
}
