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

import com.zimbra.cs.mailtest.ImapClient;
import com.zimbra.cs.mailtest.SSLUtil;
import junit.framework.TestCase;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class TestImap extends TestCase {
    private ImapClient mClient;

    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final int SSL_PORT = 7993;
    private static final String USER = "user1";
    private static final String PASS = "test123";

    private static final boolean DEBUG = true;

    public void tearDown() throws Exception {
        if (mClient != null) mClient.logout();
    }

    public void testLogin() throws Exception {
        connect(false);
        mClient.login();
    }

    public void testSSLLogin() throws Exception {
        connect(true);
        mClient.login();
    }

    public void testPlainAuth() throws Exception {
        connect(false);
        mClient.authenticate();
    }

    public void testPlainAuthInitialResponse() throws Exception {
        connect(false);
        mClient.authenticate(true);
    }

    public void testBadAuth() throws Exception {
        connect(false);
        mClient.setPassword("bad_pass");
        try {
            mClient.authenticate();
        } catch (LoginException e) {
            return;
        }
        throw new Exception("Expected auth failure");
    }

    public void testLiteral() throws Exception {
        connect(false);
        Object[] parts = new Object[] { " ", USER, " ", PASS.getBytes() };
        assertTrue(mClient.sendCommand("LOGIN", parts, false));
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
        Object[] parts = new Object[] { " ", USER, " ", lit1, " ", lit2, "FOO"};
        boolean res = mClient.sendCommand("LOGIN", parts, sync);
        assertFalse("Expected command to fail", res);
        assertTrue("Expected [TOOBIG] response", mClient.getMessage().contains("[TOOBIG]"));
    }

    private static byte[] fill(byte[] b, int c) {
        for (int i = 0; i < b.length; i++) b[i] = (byte) c;
        return b;
    }

    private void connect(boolean ssl) throws IOException {
        System.out.println("---------");
        mClient = new ImapClient(HOST, ssl ? SSL_PORT : PORT);
        if (ssl) {
            mClient.setSslEnabled(true);
            mClient.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        }
        mClient.setDebug(DEBUG);
        mClient.setLogStream(System.out);
        mClient.connect();
        mClient.setMechanism("PLAIN");
        mClient.setAuthenticationId(USER);
        mClient.setPassword(PASS);
    }
}
