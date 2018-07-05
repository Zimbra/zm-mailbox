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

import javax.net.SocketFactory;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

class ProtocolSocketFactoryWrapper implements ConnectionSocketFactory {

    private final SocketFactory factory;

    ProtocolSocketFactoryWrapper(SocketFactory factory) {
        this.factory = factory;
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.conn.socket.ConnectionSocketFactory#createSocket(org.
     * apache.http.protocol.HttpContext)
     */
    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        return factory.createSocket();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.http.conn.socket.ConnectionSocketFactory#connectSocket(int,
     * java.net.Socket, org.apache.http.HttpHost, java.net.InetSocketAddress,
     * java.net.InetSocketAddress, org.apache.http.protocol.HttpContext)
     */
    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host,
        InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
        throws IOException {

        if (connectTimeout > 0) {
            sock.bind(localAddress);
            sock.connect(remoteAddress, connectTimeout);
            return sock;
        } else {
            sock.connect(remoteAddress, connectTimeout);
            return sock;
        }

    }

}
