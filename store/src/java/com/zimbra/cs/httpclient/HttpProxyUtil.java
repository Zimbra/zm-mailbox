/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.httpclient;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

public class HttpProxyUtil {

    private static String sProxyUrl = null;
    private static URI sProxyUri = null;
    private static AuthScope sProxyAuthScope = null;
    private static UsernamePasswordCredentials sProxyCreds = null;

    public static synchronized void configureProxy(HttpClientBuilder clientBuilder) {
        try {
            String url = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraHttpProxyURL, null);
            if (url == null) return;

            // need to initializae all the statics
            if (sProxyUrl == null || !sProxyUrl.equals(url)) {
                sProxyUrl = url;
                sProxyUri = new URI(url);
                sProxyAuthScope = null;
                sProxyCreds = null;
                String userInfo = sProxyUri.getUserInfo();
                if (userInfo != null) {
                    int i = userInfo.indexOf(':');
                    if (i != -1) {
                        sProxyAuthScope = new AuthScope(sProxyUri.getHost(), sProxyUri.getPort(), null);
                        sProxyCreds = new UsernamePasswordCredentials(userInfo.substring(0, i), userInfo.substring(i+1));
                    }
                }
            }
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("setting proxy: "+url);
            }

            HttpHost proxy = new HttpHost(sProxyUri.getHost(), sProxyUri.getPort());
            RequestConfig config = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
            clientBuilder.setDefaultRequestConfig(config);
            if (sProxyAuthScope != null && sProxyCreds != null)  {
                CredentialsProvider cred = new BasicCredentialsProvider();
                cred.setCredentials(sProxyAuthScope, sProxyCreds);
                clientBuilder.setDefaultCredentialsProvider(cred);
            }
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Unable to configureProxy: "+e.getMessage(), e);
        } catch (URISyntaxException e) {
            ZimbraLog.misc.warn("Unable to configureProxy: "+e.getMessage(), e);
        }
    }
}
