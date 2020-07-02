/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.net.ProxyHostConfiguration;
import com.zimbra.common.util.ZimbraHttpConnectionManager;

public class HttpClientUtil {

    public static HttpResponse executeMethod(HttpRequestBase method) throws HttpException, IOException {
        return executeMethod(ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient().build(), method, null);
    }
    public static HttpResponse executeMethod(HttpClient client, HttpRequestBase method) throws HttpException, IOException {
        return executeMethod(client, method, null, null);
    }

    public static HttpResponse executeMethod(HttpClient client, HttpRequestBase method, BasicCookieStore state, HttpClientContext context) throws HttpException, IOException {
        ProxyHostConfiguration proxyConfig = HttpProxyConfig.getProxyConfig(method.getURI().toString());
        BasicCookieStore cookieStore = new  BasicCookieStore();
        if (state != null) {
            cookieStore = state;
        }
        if (proxyConfig != null && proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
            HttpHost proxy = new HttpHost(proxyConfig.getProxyHost(), proxyConfig.getProxyPort());
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyConfig.getUsername(), proxyConfig.getPassword());
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope (proxy),credentials);
            CloseableHttpClient httpClient = HttpClients.custom ()
                .setDefaultCredentialsProvider (credsProvider)
                .setDefaultRequestConfig(config)
                .setDefaultCookieStore(cookieStore)
                .build ();
            return httpClient.execute(method);
           
        }
        return client.execute(method);
    }
    
    public static HttpResponse executeMethod(HttpClient client, HttpRequestBase method, HttpClientContext context) throws HttpException, IOException {
        return client.execute(method, context);
    }


    public static BasicCookieStore newHttpState(ZAuthToken authToken, String host, boolean isAdmin) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        if (authToken != null) {
            Map<String, String> cookieMap = authToken.cookieMap(isAdmin);
            if (cookieMap != null) {
                for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                    BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                    cookie.setDomain(host);
                    cookie.setPath("/");
                    cookie.setSecure(false);
                    cookieStore.addCookie(cookie);
                }
            }
        }
        return cookieStore;
    }

    /**
     * Returns cookie store for Apache HTTP Components HttpClient 4.x
     * @param authToken
     * @param host
     * @param isAdmin
     * @return {@link CookieStore}
     */
    public static CookieStore newCookieStore(ZAuthToken authToken, String host, boolean isAdmin) {
        CookieStore cookieStore = new BasicCookieStore();
        if (authToken != null) {
            Map<String, String> cookieMap = authToken.cookieMap(isAdmin);
            if (cookieMap != null) {
                for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                    BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                    cookie.setDomain(host);
                    cookie.setPath("/");
                    cookie.setSecure(false);
                    cookieStore.addCookie(cookie);
                }
            }
        }
        return cookieStore;
    }
}

