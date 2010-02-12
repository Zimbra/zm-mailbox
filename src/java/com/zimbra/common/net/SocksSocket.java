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
import java.util.List;

class SocksSocket extends SocketWrapper {
    private final ProxySelector proxySelector;

    private static final Log LOG = ZimbraLog.io;

    SocksSocket(ProxySelector ps) {
        proxySelector = ps;
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        InetSocketAddress isa = (InetSocketAddress) endpoint;
        URI uri;
        try {
            uri = new URI("socket", null, isa.getHostName(), isa.getPort(),
                          null, null, null);
        } catch (URISyntaxException e) {
            throw new AssertionError();
        }
        ProxySelector ps = proxySelector;
        if (ps == null) {
            ps = ProxySelector.getDefault();
        }
        List<Proxy> proxies = ps.select(uri);
        if (proxies.isEmpty()) {
            LOG.debug("Connecting to %s", isa);
            setDelegate(new Socket());
        } else {
            Proxy proxy = proxies.get(0);
            LOG.debug("Connecting to %s via SOCKS proxy %s",
                      isa, proxy.address());
            setDelegate(new Socket(proxy));
        }
        super.connect(endpoint, timeout);
    }
}
