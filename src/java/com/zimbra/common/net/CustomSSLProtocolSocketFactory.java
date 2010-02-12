/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.net;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class CustomSSLProtocolSocketFactory implements SecureProtocolSocketFactory {
    private final SSLSocketFactory factory;

    public CustomSSLProtocolSocketFactory() throws GeneralSecurityException {
    	SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { CustomTrustManager.getInstance() }, null);
        factory = sslcontext.getSocketFactory();
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
    	return new CustomSSLSocket((SSLSocket) factory.createSocket(socket, host, port, autoClose), host, true);
    }
    
    public Socket createSocket(String host, int port) throws IOException {
    	return new CustomSSLSocket((SSLSocket) factory.createSocket(host, port), host, true);
    }
    
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
    	return new CustomSSLSocket((SSLSocket) factory.createSocket(host, port, localHost, localPort), host, true);
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        return createSocket(host, port, localAddress, localPort);
    }
}
