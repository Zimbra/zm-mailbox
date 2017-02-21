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
