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

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

class ProxySelectorSocket extends SocketWrapper {
    private final ProxySelector proxySelector;

    private static final Log LOG = ZimbraLog.io;

    public ProxySelectorSocket(ProxySelector ps) {
        if (ps == null) {
            throw new NullPointerException("ps");
        }
        proxySelector = ps;
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        Proxy proxy = proxy((InetSocketAddress) endpoint);
        switch (proxy.type()) {
        case DIRECT:
            LOG.debug("Connecting directly to %s", endpoint);
        case SOCKS:
            LOG.debug("Connecting to %s via SOCKS proxy %s", endpoint, proxy.address());
        }
        setDelegate(new Socket(proxy));
        super.connect(endpoint, timeout);
    }

    private Proxy proxy(InetSocketAddress addr) {
        URI uri = uri(addr);
        for (Proxy proxy : proxySelector.select(uri)) {
            switch (proxy.type()) {
            case SOCKS: case DIRECT:
                return proxy;
            }
        }
        return new Proxy(Proxy.Type.DIRECT, addr);
    }

    private static URI uri(InetSocketAddress isa) {
        try {
            return new URI("socket", null, isa.getHostName(), isa.getPort(),
                           null, null, null);
        } catch (URISyntaxException e) {
            throw new AssertionError();
        }
    }
}
