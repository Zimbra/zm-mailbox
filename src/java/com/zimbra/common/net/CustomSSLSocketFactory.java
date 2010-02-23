/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Override SSLSocketFactory to provide a createSocket() interface
 */
class CustomSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory sslFactory;
    private final SSLSocket sampleSSLSocket;    // Sample socket for obtaining default SSL params
    private final SocketFactory factory;        // Optional SocketFactory
    private final boolean verifyHostname;

    CustomSSLSocketFactory(TrustManager tm, SocketFactory sf, boolean verifyHostname)
        throws GeneralSecurityException, IOException {
        
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { tm }, null);
        sslFactory = context.getSocketFactory();
        sampleSSLSocket = (SSLSocket) sslFactory.createSocket();
        factory = sf;
        this.verifyHostname = verifyHostname;
    }

    boolean isVerifyHostname() {
        return verifyHostname;
    }

    SSLSocket getSampleSSLSocket() {
        return sampleSSLSocket;
    }

    SSLSocket wrap(Socket socket) throws IOException {
         return (SSLSocket) sslFactory.createSocket(
            socket, socket.getInetAddress().getHostName(), socket.getPort(), true);
    }

    @Override
    public Socket createSocket() throws IOException {
        if (factory != null) {
            return new CustomSSLSocket(this, factory.createSocket());
        } else {
            return new CustomSSLSocket(this, (SSLSocket) sslFactory.createSocket(), null);
        }
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(new InetSocketAddress(address, port), null);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return createSocket(new InetSocketAddress(host, port), null);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(new InetSocketAddress(host, port), new InetSocketAddress(localHost, localPort));
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean flag) throws IOException {
        return new CustomSSLSocket(this, (SSLSocket) sslFactory.createSocket(socket, host, port, flag), host);
    }

    private Socket createSocket(InetSocketAddress endpoint, InetSocketAddress bindpoint) throws IOException {
        Socket sock = createSocket();
        if (bindpoint != null) {
            sock.bind(bindpoint);
        }
        if (endpoint != null) {
            sock.connect(endpoint);
        }
        return sock;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslFactory.getSupportedCipherSuites();
    }
}