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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.EasySSLProtocolSocketFactory;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;


public class TestCiphers
extends TestCase {

    private static final String[] EXCLUDED_CIPHERS = new String[] {"SSL_RSA_WITH_DES_CBC_SHA"};
    
    // set to true when nio is enabled on the server, to toggle between IMAP_STARTTLS_RESPONSE and IMAP_STARTTLS_RESPONSE_NIO
    private static final boolean TESTING_NIO = false;
    
    /*
To enable nio, add the following in localconfig.xml

  <key name="nio_enabled">
    <value>true</value>
  </key>
  <key name="nio_imap_enabled">
    <value>true</value>
  </key>
  <key name="nio_pop3_enabled">
    <value>true</value>
  </key>
  <key name="nio_lmtp_enabled">
    <value>true</value>
  </key>
  
    */
    
    private static final boolean zimbraSSLExcludeCipherSuites_configured = true;
    
    /*

configure excluded ciphers, need to restart server
zmprov mcf +zimbraSSLExcludeCipherSuites SSL_RSA_WITH_DES_CBC_SHA +zimbraSSLExcludeCipherSuites SSL_DHE_RSA_WITH_DES_CBC_SHA +zimbraSSLExcludeCipherSuites SSL_DHE_DSS_WITH_DES_CBC_SHA +zimbraSSLExcludeCipherSuites SSL_RSA_EXPORT_WITH_RC4_40_MD5 +zimbraSSLExcludeCipherSuites SSL_RSA_EXPORT_WITH_DES40_CBC_SHA +zimbraSSLExcludeCipherSuites SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA +zimbraSSLExcludeCipherSuites SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA    
       
clear all excluded ciphers, need to restart server
zmprov mcf zimbraSSLExcludeCipherSuites ""

    */
    
    private static final String CRLF = "\r\n";
    private static final String HOSTNAME = "localhost";
    
    private static final String POP3_CONNECT_RESPONSE = "\\+OK .* POP3 server ready";
    private static final String POP3_USER = "USER user1" + CRLF;
    private static final String POP3_USER_RESPONSE = "\\+OK hello user1, please enter your password";
    private static final String POP3_PASS = "PASS test123" + CRLF;
    private static final String POP3_PASS_RESPONSE = "\\+OK server ready";
    private static final String POP3_STLS = "STLS" + CRLF;
    private static final String POP3_STLS_RESPONSE = "\\+OK Begin TLS negotiation";
    private static final String POP3_CLEARTEXT_FAILED_RESPONSE = "-ERR only valid after entering TLS mode";
    private static final String POP3_QUIT = "QUIT" + CRLF;
    private static final String POP3_QUIT_RESPONSE = "\\+OK .* closing connection";
    private static final String POP3_XOIP = "XOIP 100.99.98.97" + CRLF;
    private static final String POP3_XOIP_RESPONSE = "\\+OK";
    
    private static final String IMAP_CONNECT_RESPONSE = "\\* OK .* Zimbra IMAP4rev1 service ready";
    private static final String IMAP_LOGIN = "1 LOGIN user1 test123" + CRLF;
    private static final String IMAP_LOGIN_RESPONSE = "1 OK.*LOGIN completed";
    private static final String IMAP_CLEARTEXT_FAILED_RESPONSE = "1 NO cleartext logins disabled";
    private static final String IMAP_STARTTLS = "2 STARTTLS" + CRLF;
    private static final String IMAP_STARTTLS_RESPONSE = "2 OK Begin TLS negotiation now";      // our non-nio imap server returns Begin
    private static final String IMAP_STARTTLS_RESPONSE_NIO = "2 OK begin TLS negotiation now";  // our nio imap server returns begin 
    private static final String IMAP_LOGOUT = "3 LOGOUT" + CRLF;
    private static final String IMAP_LOGOUT_RESPONSE1 = "\\* BYE.*IMAP4rev1 server terminating connection";
    private static final String IMAP_LOGOUT_RESPONSE2 = "3 OK LOGOUT completed";
    private static final String IMAP_ID = "4 ID (\"X-ORIGINATING-IP\" \"100.99.98.97\")" + CRLF;
    private static final String IMAP_ID_RESPONSE1 = "\\* ID.*";
    private static final String IMAP_ID_RESPONSE2 = "4 OK ID completed";
    
    private static final String SOAP_ENV =
	    "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
	    "  <soap:Header>" +
	    "    <context xmlns=\"urn:zimbra\">" +
	    "      <session></session>" +
	    "    </context>" +
	    "  </soap:Header>" +
	    "  <soap:Body>" +
	    "    <PingRequest xmlns=\"urn:zimbraAdmin\"></PingRequest>" +
	    "  </soap:Body>" +
	    "</soap:Envelope>";
    
    private static final String HTTP_SOAP_PING = 
            "POST /service/admin/soap/ HTTP/1.1" + CRLF +
            "Content-Type: text/xml; charset=utf-8" + CRLF +
            "User-Agent: Jakarta Commons-HttpClient/3.0" + CRLF +
            "Host: localhost:7070" + CRLF +
            "Content-Length: " + SOAP_ENV.length() + CRLF +
            CRLF +
            SOAP_ENV;
    
    private static final String HTTP_SOAP_PING_RESPONSE = "HTTP/1.1 200 OK";
    private static final String HTTP_SOAP_PING_RESPONSE_CLEARTEXT = "HTTP/1.1 500 Internal Server Error";  // because PingRequest is an admin command and thus is only allowed on admin port
    
    private static int mPop3CleartextPort;
    private static int mPop3SslPort;
    private static int mImapCleartextPort;
    private static int mImapSslPort;
    private static int mHttpCleartextPort;
    private static int mHttpSslPort;
    
    private Map<Socket, BufferedReader> mReaders = new HashMap<Socket, BufferedReader>();
    
    static {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void init() throws Exception {
        // Initialize SSL for SOAP provisioning
        EasySSLProtocolSocketFactory.init();
        
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        mPop3CleartextPort = server.getIntAttr(Provisioning.A_zimbraPop3BindPort, 7110);
        mPop3SslPort = server.getIntAttr(Provisioning.A_zimbraPop3SSLBindPort, 7995);
        mImapCleartextPort = server.getIntAttr(Provisioning.A_zimbraImapBindPort, 7143);
        mImapSslPort = server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, 7995);
        mHttpCleartextPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 7070);
        mHttpSslPort = server.getIntAttr(Provisioning.A_zimbraAdminPort, 7071); // use zimbraAdminPort for testing because zimbraMailSSL Port may not be enabled 
    }
    
    private void setEnabledCipherSuites(Socket socket) {
        SSLSocket sslSocket = (SSLSocket)socket;
        sslSocket.setEnabledCipherSuites(EXCLUDED_CIPHERS);
    }
        
    public void pop3(boolean useExcludedCipher) throws Exception {
        // Test cleartext
        Socket socket = new Socket(HOSTNAME, mPop3CleartextPort);
        send(socket, "", POP3_CONNECT_RESPONSE);
        send(socket, POP3_USER, POP3_USER_RESPONSE);
        send(socket, POP3_PASS, POP3_PASS_RESPONSE);
        send(socket, POP3_QUIT, POP3_QUIT_RESPONSE);
        socket.close();

        // Test SSL
        boolean good = false;
        socket = DummySSLSocketFactory.getDefault().createSocket(HOSTNAME, mPop3SslPort);
        if (useExcludedCipher)
            setEnabledCipherSuites(socket);
        try {
            send(socket, "", POP3_CONNECT_RESPONSE);
            send(socket, POP3_USER, POP3_USER_RESPONSE);
            send(socket, POP3_PASS, POP3_PASS_RESPONSE);
            send(socket, POP3_QUIT, POP3_QUIT_RESPONSE);
            good = !zimbraSSLExcludeCipherSuites_configured || !useExcludedCipher;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            good = zimbraSSLExcludeCipherSuites_configured && useExcludedCipher;
        } finally {
            socket.close();
        }
        assertTrue(good);
        
        // Test TLS
        good = false;
        socket = new Socket(HOSTNAME, mPop3CleartextPort);
        send(socket, "", POP3_CONNECT_RESPONSE);
        send(socket, POP3_STLS, POP3_STLS_RESPONSE);
        SSLSocketFactory factory = (SSLSocketFactory) DummySSLSocketFactory.getDefault();
        socket = factory.createSocket(socket, HOSTNAME, mPop3CleartextPort, true);
        if (useExcludedCipher)
            setEnabledCipherSuites(socket);
        try {
            send(socket, POP3_USER, POP3_USER_RESPONSE);
            send(socket, POP3_PASS, POP3_PASS_RESPONSE);
            send(socket, POP3_QUIT, POP3_QUIT_RESPONSE);
            good = !zimbraSSLExcludeCipherSuites_configured || !useExcludedCipher;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            good = zimbraSSLExcludeCipherSuites_configured && useExcludedCipher;
        } finally {
            socket.close();
        }
        assertTrue(good);
    }
    
    public void imap(boolean useExcludedCipher) throws Exception {
        // Test cleartext
        Socket socket = new Socket(HOSTNAME, mImapCleartextPort);
        send(socket, null, IMAP_CONNECT_RESPONSE);
        send(socket, IMAP_LOGIN, IMAP_LOGIN_RESPONSE);
        send(socket, IMAP_LOGOUT, IMAP_LOGOUT_RESPONSE1);
        send(socket, null, IMAP_LOGOUT_RESPONSE2);
        
        // Test SSL
        boolean good = false;
        socket = DummySSLSocketFactory.getDefault().createSocket(HOSTNAME, mImapSslPort);
        if (useExcludedCipher)
            setEnabledCipherSuites(socket);
        try {
            send(socket, null, IMAP_CONNECT_RESPONSE);
            send(socket, IMAP_LOGIN, IMAP_LOGIN_RESPONSE);
            send(socket, IMAP_LOGOUT, IMAP_LOGOUT_RESPONSE1);
            send(socket, null, IMAP_LOGOUT_RESPONSE2);
            good = !zimbraSSLExcludeCipherSuites_configured || !useExcludedCipher;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            good = zimbraSSLExcludeCipherSuites_configured && useExcludedCipher;
        } finally {
            socket.close();
        }
        assertTrue(good);
        
        // Test TLS
        good = false;
        socket = new Socket(HOSTNAME, mImapCleartextPort);
        send(socket, null, IMAP_CONNECT_RESPONSE);
        String expectedStartTLSResp = TESTING_NIO?IMAP_STARTTLS_RESPONSE_NIO:IMAP_STARTTLS_RESPONSE;
        send(socket, IMAP_STARTTLS, expectedStartTLSResp);
        SSLSocketFactory factory = (SSLSocketFactory) DummySSLSocketFactory.getDefault();
        socket = factory.createSocket(socket, HOSTNAME, mImapCleartextPort, true);
        if (useExcludedCipher)
            setEnabledCipherSuites(socket);
        try {
            send(socket, IMAP_LOGIN, IMAP_LOGIN_RESPONSE);
            send(socket, IMAP_LOGOUT, IMAP_LOGOUT_RESPONSE1);
            send(socket, null, IMAP_LOGOUT_RESPONSE2);
            good = !zimbraSSLExcludeCipherSuites_configured || !useExcludedCipher;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            good = zimbraSSLExcludeCipherSuites_configured && useExcludedCipher;
        } finally {
            socket.close();
        }
        assertTrue(good);
    }
    
    public void http(boolean useExcludedCipher) throws Exception {
        // Test cleartext
        Socket socket = new Socket(HOSTNAME, mHttpCleartextPort);
        send(socket, HTTP_SOAP_PING, HTTP_SOAP_PING_RESPONSE_CLEARTEXT);
	
        // Test SSL
        // for http, don't take into account if zimbraSSLExcludeCipherSuites_configured 
        // unless using config rewrite for jetty.xml, which usually is not done in dev env.
        // 
        boolean good = false;
        socket = DummySSLSocketFactory.getDefault().createSocket(HOSTNAME, mHttpSslPort);
        if (useExcludedCipher)
            setEnabledCipherSuites(socket);
        try {
            send(socket, HTTP_SOAP_PING, HTTP_SOAP_PING_RESPONSE);
            good = !useExcludedCipher;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            good = useExcludedCipher;
        } finally {
            socket.close();
        }
        assertTrue(good);
    }
    
    public void testPop3() throws Exception {
        pop3(true);
        pop3(false);
    }
    
    public void testImap() throws Exception {
        imap(true);
        imap(false);
    }
    
    public void testHttp() throws Exception {
        http(true);
        http(false);
    }
    
    /**
     * Sends the given message to the socket's <code>OutputStream</code> and
     * validates the first line returned.
     * 
     * @param socket the socket
     * @param msg the message to send, or <code>null</code> to just read the next line
     * @param responsePattern the regexp pattern that the response should match 
     */
    private void send(Socket socket, String msg, String responsePattern)
    throws Exception {
        if (msg != null) {
            OutputStream out = socket.getOutputStream();
            out.write(msg.getBytes());
            out.flush();
        }

        // Get the reader associated with the socket.  We can't ever create
        // two readers for the same socket, since BufferedReader may read
        // past the current line.
        BufferedReader reader = mReaders.get(socket);
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mReaders.put(socket, reader);
        }
            
        String response = reader.readLine();
        String errorMsg = "Unexpected response: '" + response + "'";
        assertTrue(errorMsg, response.matches(responsePattern));
    }
    
    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestCiphers.class);        
    }
}

