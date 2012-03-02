/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MailboxName;

/**
 * IMAP server tests.
 */
public class TestImapThrottle {
    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final String USER = "imap-test-user";
    private static final String PASS = "test123";

    private int LOOP_LIMIT = LC.imap_throttle_command_limit.intValue();

    private ImapConnection connection;

    @Before
    public void createData() throws ServiceException, IOException {
        TestUtil.createAccount(USER);
        connection = connect();
    }

    @After
    public void clearData() throws ServiceException {
        @SuppressWarnings("unused")
        ZMailbox mbox = TestUtil.getZMailbox(USER); // funky, but somehow it
                                                    // gets us around SSL

        TestUtil.deleteAccount(USER);
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void append() throws Exception {
        assertTrue(connection.hasCapability("UIDPLUS"));
        Date date = new Date(System.currentTimeMillis());
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < LOOP_LIMIT; i++) {
            Literal msg = message(100000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }

        Literal msg = message(100000);
        try {
            connection.append("INBOX", flags, date, msg);
            Assert.fail("expected exception here...");
        } catch (Exception e) {
            Assert.assertTrue(connection.isClosed());
        } finally {
            msg.dispose();
        }
    }

    @Test
    public void list() throws IOException {
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.list("", "*");
        }

        try {
            connection.list("", "*");
            Assert.fail("Expected exception here...");
        } catch (Exception e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void lsub() throws IOException {
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.lsub("", "*");
        }

        try {
            connection.lsub("", "*");
            Assert.fail("Expected exception here...");
        } catch (Exception e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void xlist() throws IOException {
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.newRequest(CAtom.XLIST, new MailboxName(""), new MailboxName("*")).sendCheckStatus();
        }

        try {
            connection.newRequest(CAtom.XLIST, new MailboxName(""), new MailboxName("*")).sendCheckStatus();
            Assert.fail("Expected exception here...");
        } catch (Exception e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void create() throws IOException {
        // can't check exact repeats of create since it gets dropped by
        // imap_max_consecutive_error before imap_throttle_command_limit is reached
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.create("foo" + i);
            if (i % 10 == 0) {
                try {
                    Thread.sleep(250);
                    // sleep a bit so we don't provoke req/sec limits. this is
                    // fuzzy; increase sleep time if this test has sporadic failures
                } catch (InterruptedException e) {
                }
            }
        }

        try {
            connection.create("overthelimit");
            Assert.fail("should be over consecutive create limit");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void store() throws IOException {
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.store("1:3", "FLAGS", new String[] { "FOO", "BAR" });
        }

        try {
            connection.store("1:3", "FLAGS", new String[] { "FOO", "BAR" });
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void examine() throws IOException {
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.examine("INBOX");
        }

        try {
            connection.examine("INBOX");
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void select() throws IOException {
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.select("SENT");
        }

        try {
            connection.select("SENT");
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void fetch() throws IOException {
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.fetch(1, new String[] { "FLAGS", "UID" });
        }

        try {
            connection.fetch(1, new String[] { "FLAGS", "UID" });
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void uidfetch() throws IOException {
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.uidFetch("1:*", new String[] { "FLAGS", "UID" });
        }

        try {
            connection.uidFetch("1:*", new String[] { "FLAGS", "UID" });
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void copy() throws IOException {
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        connection.create("FOO");
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.copy("1:3", "FOO");
        }

        try {
            connection.copy("1:3", "FOO");
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void search() throws IOException {
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.search((Object[]) new String[] { "TEXT", "\"XXXXX\"" });
        }

        try {
            connection.search((Object[]) new String[] { "TEXT", "\"XXXXX\"" });
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    @Test
    public void sort() throws IOException {
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.newRequest("SORT (DATE REVERSE SUBJECT) UTF-8 ALL").sendCheckStatus();
        }

        try {
            connection.newRequest("SORT (DATE REVERSE SUBJECT) UTF-8 ALL").sendCheckStatus();
            Assert.fail("should have been rejected");
        } catch (CommandFailedException e) {
            Assert.assertTrue(connection.isClosed());
        }
    }

    private static Literal message(int size) throws IOException {
        File file = File.createTempFile("msg", null);
        file.deleteOnExit();
        FileWriter out = new FileWriter(file);
        try {
            out.write(simpleMessage("test message"));
            for (int i = 0; i < size; i++) {
                out.write('X');
                if (i % 72 == 0) {
                    out.write("\r\n");
                }
            }
        } finally {
            out.close();
        }
        return new Literal(file, true);
    }

    private static String simpleMessage(String text) {
        return "Return-Path: dac@zimbra.com\r\n" + "Date: Fri, 27 Feb 2004 15:24:43 -0800 (PST)\r\n"
                + "From: dac <dac@zimbra.com>\r\n" + "To: bozo <bozo@foo.com>\r\n" + "Subject: Foo foo\r\n\r\n" + text
                + "\r\n";
    }

    private ImapConnection connect() throws IOException {
        ImapConfig config = new ImapConfig(HOST);
        config.setPort(PORT);
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        connection.login(PASS);
        connection.select("INBOX");
        return connection;
    }
}
