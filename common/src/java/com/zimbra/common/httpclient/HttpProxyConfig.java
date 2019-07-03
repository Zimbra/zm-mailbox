/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.common.httpclient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.AuthProxy;
import com.zimbra.common.net.ProxyHostConfiguration;
import com.zimbra.common.net.ProxySelectors;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;

public class HttpProxyConfig {
    
    public static ProxyHostConfiguration getProxyConfig(String uriStr) {
        if (!LC.client_use_system_proxy.booleanValue())
            return null;
        
        URI uri = null;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException x) {
            ZimbraLog.net.info(uriStr, x);
            return null;
        }

        //no need to filter out localhost as the DefaultProxySelector will do that.
        
        List<Proxy> proxies = ProxySelectors.defaultProxySelector().select(uri);
        for (Proxy proxy : proxies) {
            switch (proxy.type()) {
            case DIRECT:
                return null;
            case HTTP:
                InetSocketAddress addr = (InetSocketAddress)proxy.address();
                if (ZimbraLog.net.isDebugEnabled()) {
                    ZimbraLog.net.debug("URI %s to use HTTP proxy %s", safePrint(uri), addr.toString());
                }
                ProxyHostConfiguration nhc = new ProxyHostConfiguration();
                nhc.setProxyHost(addr.getHostName());
                nhc.setProxyPort(addr.getPort());
                if (proxy instanceof AuthProxy) {
                    nhc.setUsername(((AuthProxy) proxy).getUsername());
                    nhc.setPassword(((AuthProxy) proxy).getPassword());
                }
                return nhc;
            case SOCKS: //socks proxy can be handled at socket factory level
            default:
                continue;
            }
        }
        return null;
    }

    private static String safePrint(URI uri) {
        String urlStr = null;
        if (uri.getRawQuery() != null) {
            urlStr = uri.toString().replace("?"+uri.getRawQuery(), "");
        } else {
            urlStr = uri.toString();
        }
        return HttpUtil.sanitizeURL(urlStr);
    }
}
