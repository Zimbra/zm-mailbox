/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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

import junit.framework.TestCase;
import org.apache.log4j.BasicConfigurator;
import com.zimbra.cs.mailclient.pop3.Pop3Connection;
import com.zimbra.cs.mailclient.pop3.Pop3Config;
import com.zimbra.cs.mailclient.pop3.Pop3Capabilities;
import com.zimbra.cs.mailclient.util.SSLUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestPop3Client extends TestCase {
    private Pop3Config config;
    private Pop3Connection connection;

    private static final String HOST = "localhost";
    private static final int PORT = 7110;
    private static final int SSL_PORT = 7995;
    private static final String USER = "user1";
    private static final String PASS = "test123";

    private static final boolean DEBUG = true;
    
    static {
        BasicConfigurator.configure();
    }

    public void testDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
        config = null;
        connection = null;
    }
    
    public void testLogin() throws Exception {
        login();
    }

    public void testPlainAuth() throws Exception {
        connect();
        connection.authenticate(PASS);
    }

    public void testTls() throws Exception {
        config = getConfig(false);
        config.setTlsEnabled(true);
        connect();
        login();
    }
    
    public void testCapabilities() throws Exception {
        connect();
        Pop3Capabilities caps = connection.getCapabilities();
        assertNotNull(caps);
        assertTrue(caps.hasCapability(Pop3Capabilities.STLS));
        assertTrue(caps.hasCapability("StLs"));
        assertTrue(caps.hasCapability(Pop3Capabilities.IMPLEMENTATION, "ZimbraInc"));
        assertFalse(caps.hasCapability(Pop3Capabilities.EXPIRE, "NEVER"));
        //connection.getLogger().debug("Capabilities BEFORE = " + caps);
        login();
        caps = connection.getCapabilities();
        //connection.getLogger().debug("Capabilities AFTER = " + caps);
        assertTrue(caps.hasCapability(Pop3Capabilities.EXPIRE, "NEVER"));
    }

    public void testGetMessageSizes() throws Exception {
        login();
        List<Integer> sizes = connection.getMessageSizes();
        assertNotNull(sizes);
        assertEquals(connection.getMessageCount(), sizes.size());
        for (int i = 0; i < sizes.size(); i++) {
            assertEquals(sizes.get(i), (Integer) connection.getMessageSize(i + 1));
        }
    }

    public void testGetMessageUids() throws Exception {
        login();
        List<String> uids = connection.getMessageUids();
        assertNotNull(uids);
        assertEquals(connection.getMessageCount(), uids.size());
        for (int i = 0; i < uids.size(); i++) {
            assertEquals(uids.get(i), connection.getMessageUid(i + 1));
        }
    }

    public void testGetMessage() throws Exception {
        login();
        for (int msgno = 5; msgno < 20; msgno++) {
            int size = connection.getMessageSize(msgno);
            assertTrue(size > 0);
            InputStream is = connection.getMessage(msgno);
            assertNotNull(is);
            int count = countBytes(is);
            assertEquals(size, count);
            is.close();
        }
    }

    public void testDeleteMessage() throws Exception {
        login();
        boolean deleted = connection.deleteMessage(1);
        assertTrue(deleted);
        int count = connection.getMessageCount() - 1;
        connection.quit();
        login();
        int newCount = connection.getMessageCount();
        assertEquals(count, newCount);
    }

    private static int countBytes(InputStream is) throws IOException {
        int count = 0;
        while (is.read() != -1) {
            count++;
        }
        return count;
    }
    
    private void login() throws IOException {
        connect();
        connection.login(PASS);               
    }

    private void connect() throws IOException {
        connect(false);
    }

    private void connect(boolean ssl) throws IOException {
        if (config == null) {
            config = getConfig(ssl);
        }
        System.out.println("---------");
        connection = new Pop3Connection(config);
        connection.connect();
    }

    private static Pop3Config getConfig(boolean ssl) throws IOException {
        Pop3Config config = new Pop3Config(HOST, ssl);
        config.setPort(ssl ? SSL_PORT : PORT);
        config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        config.setDebug(DEBUG);
        config.setTrace(true);
        config.setMechanism("PLAIN");
        config.setAuthenticationId(USER);
        config.setRawMode(true);
        return config;
    }


}
