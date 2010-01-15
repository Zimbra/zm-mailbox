/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2010 Zimbra, Inc.
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
package com.zimbra.common.util;

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

    private SSLSocketFactory factory;
    
    public CustomSSLProtocolSocketFactory() throws GeneralSecurityException {
    	SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { CustomTrustManager.getInstance() }, null);
        factory = sslcontext.getSocketFactory();
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(socket, host, port, autoClose), host, true);
    }
    
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(host, port), host, true);
    }
    
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(host, port, clientHost, clientPort), host, true);
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        return createSocket(host, port, localAddress, localPort);
    }

    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(CustomSSLProtocolSocketFactory.class));
    }

    public int hashCode() {
        return CustomSSLProtocolSocketFactory.class.hashCode();
    }
}
