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

import org.junit.*;
import org.junit.Test;
import org.junit.runner.*;
import static org.junit.Assert.*;

import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.AppendMessage;
import com.zimbra.cs.mailclient.imap.Flags;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.FileWriter;
import java.sql.Date;

/**
 * IMAP server tests.
 */
public class TestImap {
    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final String USER = "user1";
    private static final String PASS = "test123";

    private static ImapConnection connection;
    
    @BeforeClass
    public static void setUp() throws Exception {
        connection = connect();
    }

    @AfterClass
    public static void tearDown() {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void testAppend() throws Exception {
        assertTrue(connection.hasCapability("UIDPLUS"));
        Flags flags = Flags.fromSpec("afs");
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
        try {
            AppendResult res = connection.append("INBOX", flags, date, msg);
            assertNotNull(res);
            byte[] b = fetchBody(res.getUid());
            assertArrayEquals("content mismatch", msg.getBytes(), b);
        } finally {
            msg.dispose();
        }
    }

    @Test
    public void testAppendNoLiteralPlus() throws Exception {
        withLiteralPlus(false, new RunnableTest() {
            public void run() throws Exception {
                testAppend();
            }
        });
    }

    @Test
    public void testCatenateSimple() throws Exception {
        assertTrue(connection.hasCapability("CATENATE"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        String part1 = simpleMessage("test message");
        String part2 = "more text\r\n";
        AppendMessage am = new AppendMessage(
            null, null, literal(part1), literal(part2));
        AppendResult res = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] body = fetchBody(res.getUid());
        assertArrayEquals("content mismatch", bytes(part1 + part2), body);
    }


    @Test
    public void testCatenateSimpleNoLiteralPlus() throws Exception {
        withLiteralPlus(false, new RunnableTest() {
            public void run() throws Exception {
                testCatenateSimple();
            }
        });
    }

    @Test
    public void testCatenateUrl() throws Exception {
        assertTrue(connection.hasCapability("CATENATE"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        String msg1 = simpleMessage("test message");
        AppendResult res1 = connection.append("INBOX", null, null, literal(msg1));
        String s1 = "first part\r\n";
        String s2 = "second part\r\n";
        String msg2 = msg1 + s1 + s2;
        AppendMessage am = new AppendMessage(
            null, null, url("INBOX", res1), literal(s1), literal(s2));
        AppendResult res2 = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] b2 = fetchBody(res2.getUid());
        assertArrayEquals("content mismatch", bytes(msg2), b2);
    }

    @Test
    public void testMultiappend() throws Exception {
        assertTrue(connection.hasCapability("MULTIAPPEND"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        AppendMessage msg1 = new AppendMessage(null, null, literal("test 1"));
        AppendMessage msg2 = new AppendMessage(null, null, literal("test 2"));
        AppendResult res = connection.append("INBOX", msg1, msg2);
        assertNotNull(res);
        assertEquals("expecting 2 uids", 2, res.getUids().length);
    }

    @Test
    public void testMultiappendNoLiteralPlus() throws Exception {
        withLiteralPlus(false, new RunnableTest() {
            public void run() throws Exception {
                testMultiappend();
            }
        });
    }

    private String url(String mbox, AppendResult res) {
        return String.format("/%s;UIDVALIDITY=%d/;UID=%d",
                             mbox, res.getUidValidity(), res.getUid());
    }
    
    private static Literal literal(String s) {
        return new Literal(bytes(s));
    }
    
    private static byte[] bytes(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            fail("UTF8 encoding not supported");
        }
        return null;
    }

    private byte[] fetchBody(long uid) throws IOException {
        MessageData md = connection.uidFetch(uid, "(BODY.PEEK[])");
        assertNotNull("message not found", md);
        assertEquals(uid, md.getUid());
        Body[] bs = md.getBodySections();
        assertNotNull(bs);
        assertEquals(1, bs.length);
        return bs[0].getImapData().getBytes();
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
        return "Return-Path: dac@zimbra.com\r\n" +
            "Date: Fri, 27 Feb 2004 15:24:43 -0800 (PST)\r\n" +
            "From: dac <dac@zimbra.com>\r\n" +
            "To: bozo <bozo@foo.com>\r\n" +
            "Subject: Foo foo\r\n\r\n" + text + "\r\n";
    }

    private void withLiteralPlus(boolean lp, RunnableTest test) throws Exception {
        ImapConfig config = connection.getImapConfig();
        boolean oldLp = config.isUseLiteralPlus();
        config.setUseLiteralPlus(lp);
        try {
            test.run();
        } finally {
            config.setUseLiteralPlus(oldLp);
        }
    }

    private static interface RunnableTest {
        void run() throws Exception;
    }
    
    private static ImapConnection connect() throws IOException {
        ImapConfig config = new ImapConfig(HOST);
        config.setPort(PORT);
        config.setAuthenticationId(USER);
        config.setTrace(true);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        connection.login(PASS);
        connection.select("INBOX");
        return connection;
    }

    public static void main(String... args) throws Exception {
        JUnitCore junit = new JUnitCore();
        if (args.length > 0) {
            for (String test : args) {
                String method = String.format("test%c%s",
                    Character.toUpperCase(test.charAt(0)), test.substring(1));
                System.out.printf("** Running test '%s'\n", method);
                junit.run(Request.method(TestImap.class, method));
            }
        } else {
            System.out.println("** Running all tests");
            junit.run(TestImap.class);
        }
    }
}
