/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Override SSLSocketFactory to provide a createSocket() interface
 *  
 * @author jjzhuang
 */
public class CustomSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;
    
    public CustomSSLSocketFactory() throws GeneralSecurityException {
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { CustomTrustManager.getInstance() }, null);
        factory = sslcontext.getSocketFactory();
    }
    
    public static SocketFactory getDefault() {
    	try {
    		return new CustomSSLSocketFactory();
    	} catch (GeneralSecurityException x) {
    		ZimbraLog.security.error(x);
    		return null;
    	}
    }
    
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket(socket, s, i, flag);
    	CustomSSLSocketUtil.verifyHostname(sslSocket);
    	return sslSocket;
    }
    
    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket(inaddr, i, inaddr1, j);
    	CustomSSLSocketUtil.verifyHostname(sslSocket);
    	return sslSocket;
    }
    
    public Socket createSocket(InetAddress inaddr, int i) throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket(inaddr, i);
    	CustomSSLSocketUtil.verifyHostname(sslSocket);
    	return sslSocket;
    }

    public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket(s, i, inaddr, j);
    	CustomSSLSocketUtil.verifyHostname(sslSocket);
    	return sslSocket;
    }

    public Socket createSocket(String s, int i) throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket(s, i);
    	CustomSSLSocketUtil.verifyHostname(sslSocket);
    	return sslSocket;
    }
    
    public Socket createSocket() throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket();
    	CustomSSLSocketUtil.verifyHostname(sslSocket);
    	return sslSocket;
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}