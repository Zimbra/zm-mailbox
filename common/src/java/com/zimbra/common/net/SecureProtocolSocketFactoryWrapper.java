/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016, 2018 Synacor, Inc.
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

class SecureProtocolSocketFactoryWrapper extends ProtocolSocketFactoryWrapper 
implements LayeredConnectionSocketFactory {
    
    private SSLSocketFactory factory;

    SecureProtocolSocketFactoryWrapper(SSLSocketFactory factory) {
        super(factory);
        this.factory = factory;
    }
    
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return factory.createSocket(socket, host, port, autoClose);
    }

    /* (non-Javadoc)
     * @see org.apache.http.conn.socket.ConnectionSocketFactory#connectSocket(int, java.net.Socket, org.apache.http.HttpHost, java.net.InetSocketAddress, java.net.InetSocketAddress, org.apache.http.protocol.HttpContext)
     */
    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host,
        InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
        throws IOException {

        sock.bind(localAddress);
        sock.connect(remoteAddress, connectTimeout);
        return sock;

    }

    /* (non-Javadoc)
     * @see org.apache.http.conn.socket.ConnectionSocketFactory#createSocket(org.apache.http.protocol.HttpContext)
     */
    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        if (context != null) {
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpHost  host = clientContext.getTargetHost();
            return factory.createSocket(host.getHostName(), host.getPort());
        } 
        return factory.createSocket();
    }

    /* (non-Javadoc)
     * @see org.apache.http.conn.socket.LayeredConnectionSocketFactory#createLayeredSocket(java.net.Socket, java.lang.String, int, org.apache.http.protocol.HttpContext)
     */
    @Override
    public Socket createLayeredSocket(Socket socket,  String target, int port, HttpContext context)
        throws IOException, UnknownHostException {
        if (socket == null) {
            if (context != null) {
                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpHost  host = clientContext.getTargetHost();
                return factory.createSocket(host.getHostName(), host.getPort());
            } else {
                return  factory.createSocket(target, port);
            }
        } else {
            return  factory.createSocket(socket, target, port, true);
        }
    }
}
