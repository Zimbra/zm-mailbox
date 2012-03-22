/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.cs.util.MockTcpServer;

/**
 * Unit test for {@link ImapProxy}.
 *
 * @author ysasaki
 */
public final class ImapProxyTest {
    private static final int PORT = 9143;
    private MockTcpServer server;

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.destroy();
        }
    }

    @Test(timeout = 30000)
    public void bye() throws Exception {
        server = MockTcpServer.scenario()
            .sendLine("* OK server ready")
            .recvLine() // CAPABILITY
            .sendLine("* CAPABILITY IMAP4rev1 AUTH=X-ZIMBRA")
            .reply(Pattern.compile("(.*) CAPABILITY"), "{0} OK CAPABILITY\r\n")
            .recvLine() // ID
            .sendLine("* ID (\"NAME\" \"Zimbra\")")
            .reply(Pattern.compile("(.*) ID"), "{0} OK ID completed\r\n")
            .recvLine() // AUTHENTICATE
            .sendLine("+ ready for literal")
            .reply(Pattern.compile("(.*) AUTHENTICATE"), "{0} OK AUTHENTICATE\r\n")
            .recvLine() // credential
            .recvLine() // NOOP
            .reply(Pattern.compile("(.*) NOOP"), "{0} OK NOOP\r\n")
            .sendLine("* BYE server closing connection")
            .build().start(PORT);

        MockImapHandler handler = new MockImapHandler();
        ImapProxy proxy = new ImapProxy(new InetSocketAddress(PORT), "test@zimbra.com", "secret", handler);
        proxy.proxy("001", "NOOP");
        try {
            proxy.proxy("002", "NOOP");
            Assert.fail();
        } catch (ImapProxyException expected) {
        }

        // verify BYE was not proxied
        Assert.assertEquals("001 OK NOOP\r\n", handler.output.toString());

        server.shutdown(3000);
        Assert.assertEquals("C01 CAPABILITY\r\n", server.replay());
        String id = server.replay();
        Assert.assertTrue(id, id.matches(
                "C02 ID \\(\"name\" \"ZCS\" \"version\" \".*\" \"X-VIA\" \"127\\.0\\.0\\.1\"\\)\r\n"));
        Assert.assertEquals("C03 AUTHENTICATE X-ZIMBRA\r\n", server.replay());
        server.replay(); // auth token
        Assert.assertEquals("001 NOOP\r\n", server.replay());
        Assert.assertEquals(null, server.replay());
    }

    private static final class MockImapHandler extends ImapHandler {

        MockImapHandler() {
            super(new ImapConfig(false));
            output = new ByteArrayOutputStream();
        }

        @Override
        String getRemoteIp() {
            return "127.0.0.1";
        }

        @Override
        void sendLine(String line, boolean flush) throws IOException {
            output.write(line.getBytes(Charsets.UTF_8));
        }

        @Override
        void dropConnection(boolean sendBanner) {
        }

        @Override
        void close() {
        }

        @Override
        void enableInactivityTimer() throws IOException {
        }

        @Override
        void completeAuthentication() throws IOException {
        }

        @Override
        boolean doSTARTTLS(String tag) throws IOException {
            return false;
        }

        @Override
        InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("localhost", 0);
        }

    }

}
