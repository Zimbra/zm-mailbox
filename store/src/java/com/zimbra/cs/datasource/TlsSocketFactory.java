/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.datasource;

import com.zimbra.common.net.SocketFactories;

import javax.net.ssl.SSLSocketFactory;
import javax.net.SocketFactory;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/*
 * Special SSLSocketFactory implementation to support JavaMail TLS. Since
 * JavaMail only allows the configuration of one socket factory we need this
 * in order to implement basic socket factory operations using plain socket
 * (before TLS negotiation) but then delegate to an SSLSocketFactory instance
 * when wrapping an existing socket (after TLS negotiation).
 */
public class TlsSocketFactory extends SSLSocketFactory {
    private final SocketFactory factory;
    private final SSLSocketFactory sslFactory;

    private static final TlsSocketFactory THE_ONE = new TlsSocketFactory();

    public TlsSocketFactory() {
        factory = SocketFactories.defaultSocketFactory();
    	sslFactory = SocketFactories.defaultSSLSocketFactory();
    }
    
    public static TlsSocketFactory getInstance() {
        return THE_ONE;
    }

    public Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return factory.createSocket(address, port);
    }
    
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort) throws IOException {
        return factory.createSocket(address, port, localAddress, localPort);
    }

    public Socket createSocket(String host, int port) throws IOException {
        return factory.createSocket(host, port);
    }

    public Socket createSocket(String host, int port,
                               InetAddress localAddress, int localPort) throws IOException {
        return factory.createSocket(host, port, localAddress, localPort);
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    	return sslFactory.createSocket(s, host, port, autoClose);
    }

    public String[] getDefaultCipherSuites() {
        return sslFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return sslFactory.getSupportedCipherSuites();
    }
}
