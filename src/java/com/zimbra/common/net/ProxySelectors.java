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

import com.zimbra.znative.ProxyInfo;
import com.zimbra.znative.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ProxySelectors {
    private static final ProxySelector nativeProxySelector;

    static {
        nativeProxySelector =
            Util.haveNativeCode() && ProxyInfo.isSupported() ?
                new NativeProxySelector() : null;
    }
    
    public static ProxySelector defaultProxySelector() {
        return nativeProxySelector != null ?
            nativeProxySelector : ProxySelector.getDefault();
    }

    public static ProxySelector nativeProxySelector() {
        return nativeProxySelector;
    }

    public static ProxySelector dummyProxySelector() {
        return new ProxySelector() {
            public List<Proxy> select(URI uri) {
                return Arrays.asList(Proxy.NO_PROXY);
            }

            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                // Do nothing...
            }
        };
    }

    private static class NativeProxySelector extends ProxySelector {
        public List<Proxy> select(URI uri) {
            List<Proxy> proxies = new ArrayList<Proxy>();
            for (ProxyInfo pi : ProxyInfo.getProxyInfo(uri.toString())) {
                Proxy proxy = getProxy(pi);
                if (proxy != null) {
                    proxies.add(proxy);
                }
            }
            if (proxies.isEmpty()) {
                proxies.add(Proxy.NO_PROXY);
            }
            return proxies;
        }

        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            // Do nothing...
        }
    }

    private static Proxy getProxy(ProxyInfo pi) {
        String host = pi.getHost();
        int port = pi.getPort();
        if (host != null && port > 0) {
            switch (pi.getType()) {
            case HTTP: case HTTPS:
                return new Proxy(Proxy.Type.HTTP, saddr(host, port));
            case SOCKS:
                return new Proxy(Proxy.Type.SOCKS, saddr(host, port));
            }
        }
        return null;
    }

    private static SocketAddress saddr(String host, int port) {
        return new InetSocketAddress(host, port);
    }

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://www.news.com";
        System.out.printf("Proxy information for %s :\n", url);
        List<Proxy> proxies = defaultProxySelector().select(new URI(url));
        for (int i = 0; i < proxies.size(); i++) {
            System.out.printf("proxy[%d] = %s\n", i, proxies.get(i));
        }
    }
}
