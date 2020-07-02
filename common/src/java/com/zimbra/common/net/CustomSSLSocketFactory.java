/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
        context.init(null, tm != null ? new TrustManager[] { tm } : null, null);
        sslFactory = context.getSocketFactory();
        sampleSSLSocket = (SSLSocket) sslFactory.createSocket();
        factory = sf;
        this.verifyHostname = verifyHostname && tm instanceof CustomTrustManager;
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