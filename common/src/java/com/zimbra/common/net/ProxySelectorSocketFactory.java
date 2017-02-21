/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;

class ProxySelectorSocketFactory extends SocketFactory {
    private final ProxySelector proxySelector;

    ProxySelectorSocketFactory(ProxySelector ps) {
        proxySelector = ps;
    }

    ProxySelectorSocketFactory() {
        this(null);
    }

    @Override
    public Socket createSocket() throws IOException {
        return new ProxySelectorSocket(proxySelector != null ?
            proxySelector : ProxySelectors.defaultProxySelector());
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return createSocket(new InetSocketAddress(host, port), null);
    }

    @Override
    public Socket createSocket(String host, int port,
                               InetAddress localAddress, int localPort)
        throws IOException {
        return createSocket(new InetSocketAddress(host, port),
                            new InetSocketAddress(localAddress, localPort));
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(new InetSocketAddress(address, port), null);
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort)
        throws IOException {
        return createSocket(new InetSocketAddress(address, port),
                            new InetSocketAddress(localAddress, localPort));
    }

    private Socket createSocket(SocketAddress endpoint,
                                SocketAddress bindpoint) throws IOException {
        Socket sock = createSocket();
        if (bindpoint != null) {
            sock.bind(bindpoint);
        }
        if (endpoint != null) {
            sock.connect(endpoint);
        }
        return sock;
    }
}
