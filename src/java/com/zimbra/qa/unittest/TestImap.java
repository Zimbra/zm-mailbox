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
import com.zimbra.cs.mailclient.util.SSLUtil;
import com.zimbra.cs.mailclient.CommandFailedException;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import org.apache.log4j.BasicConfigurator;

public class TestImap extends TestCase {
    private ImapConnection connection;

    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final int SSL_PORT = 7993;
    private static final String USER = "user1";
    private static final String PASS = "test123";

    private static final boolean DEBUG = true;

    static {
        BasicConfigurator.configure();
    }
    
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
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
        assertEquals('/', (char) lds.get(0).getDelimiter());
    }
    
    public void testFetch() throws Exception {
        connect(false);
        connection.login(PASS);
        connection.select("INBOX");
        final AtomicInteger count = new AtomicInteger();
        connection.fetch("1:*", "(ENVELOPE UID)", new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCCode() != CAtom.FETCH) return false;
                MessageData md = (MessageData) res.getData();
                System.out.printf("Fetched uid = %s\n", md.getUid());
                count.incrementAndGet();
                return true;
            }
        });
    }

    public void testID() throws Exception {
        connect(false);
        IDInfo id = connection.id();
        System.out.println("ID = " + id);
        assertNotNull(id);
    }
    
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
        if (connection == null) {
            connect();
        }
        connection.login(PASS);
        connection.select("INBOX");
    }
    
    private void connect() throws IOException {
        connect(false);
    }
    
    private void connect(boolean ssl) throws IOException {
        System.out.println("---------");
        ImapConfig config = new ImapConfig(HOST, ssl);
        config.setPort(ssl ? SSL_PORT : PORT);
        if (ssl) {
            config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        }
        config.setDebug(DEBUG);
        config.setTrace(true);
        config.setMechanism("PLAIN");
        config.setAuthenticationId(USER);
        config.setRawMode(true);
        connection = new ImapConnection(config);
        connection.connect();
    }

    public static void main(String[] args) throws Exception {
        new TestImap().testFetch();
    }
}
