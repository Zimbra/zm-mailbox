/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import com.zimbra.common.util.DummySSLSocketFactory;
import junit.framework.TestCase;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class TestImapAuth extends TestCase {
    private Socket sock;
    private BufferedWriter out;
    private BufferedReader in;

    private static final String CRLF = "\r\n";
    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final int SSL_PORT = 7993;
    private static final String USER = "user1";
    private static final String PASS = "test123";

    private static final boolean DEBUG = true;
    
    public void tearDown() throws IOException {
        logout();
        sock.close();
    }

    public void testLogin() throws IOException {
        connect(HOST, PORT, false);
        login(USER, PASS);
    }

    public void testSSLLogin() throws IOException {
        connect(HOST, SSL_PORT, true);
        login(USER, PASS);
    }

    private void login(String user, String pass) throws IOException {
        send(". LOGIN " + user + " " + pass);
        expect("* CAPABILITY");
        expect(". OK LOGIN");
    }

    public void testPlainAuth() throws IOException {
        connect(HOST, SSL_PORT, true);
        send(". AUTHENTICATE PLAIN");
        expect("+ ");
        send(encodePlain(USER, PASS));
        expect("* CAPABILITY");
        expect(". OK");
    }

    public void testPlainAuthInitialResponse() throws IOException {
        connect(HOST, SSL_PORT, true);
        send(". AUTHENTICATE PLAIN " + encodePlain(USER, PASS));
        expect("* CAPABILITY");
        expect(". OK");
    }

    public void testBadAuth() throws IOException {
        connect(HOST, SSL_PORT, true);
        send(". AUTHENTICATE PLAIN");
        expect("+ ");
        send(encodePlain(USER, "bad_pass"));
        expect(". NO ");
    }

    private String encodePlain(String user, String pass) {
        return encode64("\0" + user + "\0" + pass);
    }
    
    private void connect(String host, int port, boolean ssl) throws IOException {
        pd("--------");
        sock = new Socket(host, port);
        if (ssl) {
            sock = DummySSLSocketFactory.getDefault().createSocket(HOST, port);
        }
        out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        expect("* OK");
    }

    private void close() throws IOException {
        sock.close();
    }

    private void logout() throws IOException {
        send(". LOGOUT");
        expect("* BYE");
        expect(". OK");
        close();
    }
    
    private void expect(String prefix) throws IOException {
        String s = recv();
        if (s == null) {
            throw new IOException("Unexpected end of stream");
        }
        if (!s.startsWith(prefix)) {
            throw new IOException("Expected line starting with '" + prefix + "'");
        }
    }

    private void send(String cmd) throws IOException {
        pd("C: " + cmd);
        out.write(cmd);
        out.write(CRLF);
        out.flush();
    }

    private String recv() throws IOException {
        String s = in.readLine();
        pd("S: " + s);
        return s;
    }

    private void pd(String s) {
        if (DEBUG) System.out.println(s);
    }
    
    private final String CHARSET = "us-ascii";

    private String encode64(String s) {
        try {
        return new String(Base64.encodeBase64(s.getBytes(CHARSET)), CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }
}
