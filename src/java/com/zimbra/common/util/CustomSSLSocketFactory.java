/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2010 Zimbra, Inc.
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
    
    boolean verifyHostname = true;
    
    public CustomSSLSocketFactory(boolean verifyHostname)  throws GeneralSecurityException {
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { CustomTrustManager.getInstance() }, null);
        factory = sslcontext.getSocketFactory();
        this.verifyHostname = verifyHostname;
    }
    
    public CustomSSLSocketFactory() throws GeneralSecurityException {
    	this(true);
    }
    
    @Override
    public Socket createSocket() throws IOException {
    	//javamail smtp uses unconnected socket
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(), null, verifyHostname);
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(address, port), address.getHostName(), verifyHostname);
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(address, port, localAddress, localPort), address.getHostName(), verifyHostname);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(host, port), host, verifyHostname);
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(host, port, localHost, localPort), host, verifyHostname);
    }
    
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean flag) throws IOException {
    	return new CustomSSLSocket((SSLSocket)factory.createSocket(socket, host, port, flag), host, verifyHostname);
    }

    public static SocketFactory getDefault() {
    	try {
    		return new CustomSSLSocketFactory();
    	} catch (GeneralSecurityException x) {
    		ZimbraLog.security.error(x);
    		return null;
    	}
    }
    
    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}