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
import java.util.Iterator;
import java.util.List;

/**
 * Factory class for various ProxySelector types.
 */
public final class ProxySelectors {
    private static final ProxySelector systemProxySelector;
    private static final ProxySelector nativeProxySelector;
    private static final ProxySelector defaultProxySelector;

    static {
        systemProxySelector = ProxySelector.getDefault();
        if (useNativeProxySelector()) {
            nativeProxySelector = new NativeProxySelector();
            defaultProxySelector = nativeProxySelector;
        } else {
            nativeProxySelector = null;
            defaultProxySelector = new CustomProxySelector(systemProxySelector);
        }
    }

    private static boolean useNativeProxySelector() {
        return NetConfig.getInstance().isUseNativeProxySelector() &&
               Util.haveNativeCode() && ProxyInfo.isSupported();
    }

    /**
     * On supported systems returns the native ProxySelector otherwise
     * returns the system default.
     *
     * @return the default ProxySelector
     */
    public static ProxySelector defaultProxySelector() {
        return defaultProxySelector;
    }

    /**
     * Returns the original system default ProxySelector.
     * @return the system default ProxySelector
     */
    public static ProxySelector systemProxySelector() {
        return systemProxySelector;
    }

    /**
     * Returns the native ProxySelector if available, otherwise returns null.
     *
     * @return the native ProxySelector or null if not available
     */
    public static ProxySelector nativeProxySelector() {
        return nativeProxySelector;
    }

    /**
     * Returns a "dummy" ProxySelector whose select method always returns
     * a DIRECT connection. Used for testing.
     *
     * @return the dummy ProxySelector
     */
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

    /*
     * Native ProxySelector implementation that uses native code to workaround
     * issues with OS/X's default system ProxySelector. This implementation
     * correctly handles dynamic changes to system proxy settings.
     */
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

        public void connectFailed(URI uri, SocketAddress sa, IOException e) {
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
            case NONE:
                return Proxy.NO_PROXY;
            }
        }
        return null;
    }

    /*
     * Custom proxy selector that replaces system default if no native proxy
     * is available. This proxy selector will delegate to the system default
     * for HTTP as well as SOCKS if SOCKS support is enabled. Also excludes
     * invalid proxy results (i.e. invalid port) which works around issues
     * on Linux hosts with incorrect settings.
     */
    private static class CustomProxySelector extends ProxySelector {
        private final ProxySelector ps;

        CustomProxySelector(ProxySelector ps) {
            this.ps = ps;
        }

        public List<Proxy> select(URI uri) {
            List<Proxy> proxies = ps.select(uri);
            for (Iterator<Proxy> it = proxies.iterator(); it.hasNext(); ) {
                if (!isValidProxy(it.next())) {
                    it.remove();
                }
            }
            if (proxies.isEmpty()) {
                proxies.add(Proxy.NO_PROXY);
            }
            return proxies;
        }

        public void connectFailed(URI uri, SocketAddress sa, IOException e) {
            ps.connectFailed(uri, sa, e);
        }
    }

    private static boolean isValidProxy(Proxy proxy) {
        InetSocketAddress addr = (InetSocketAddress) proxy.address();
        switch (proxy.type()) {
        case SOCKS:
            return NetConfig.getInstance().isSocksEnabled() && addr.getPort() > 0;
        case HTTP:
            return addr.getPort() > 0;
        default:
            return true;
        }
    }

    private static SocketAddress saddr(String host, int port) {
        return new InetSocketAddress(host, port);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        String url = args.length > 0 ? args[0] : "http://www.news.com";
        System.out.printf("Proxy information for %s :\n", url);
        List<Proxy> proxies = defaultProxySelector().select(new URI(url));
        for (int i = 0; i < proxies.size(); i++) {
            System.out.printf("proxy[%d] = %s\n", i, proxies.get(i));
        }
    }
}
