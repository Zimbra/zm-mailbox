/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;

public class SocksSocketFactory extends SocketFactory {
    private final ProxySelector proxySelector;

    public SocksSocketFactory(ProxySelector ps) {
        proxySelector = ps;
    }

    public SocksSocketFactory() {
        this(null);
    }

    @Override
    public Socket createSocket() throws IOException {
        return new SocksSocket(proxySelector);
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
