/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.net.ProxyHostConfiguration;
import com.zimbra.common.util.ZimbraHttpConnectionManager;

public class HttpClientUtil {

    public static int executeMethod(HttpMethod method) throws HttpException, IOException {
        return executeMethod(ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient(), method, null);
    }
    public static int executeMethod(HttpClient client, HttpMethod method) throws HttpException, IOException {
        return executeMethod(client, method, null);
    }

    public static int executeMethod(HttpClient client, HttpMethod method, HttpState state) throws HttpException, IOException {
        ProxyHostConfiguration proxyConfig = HttpProxyConfig.getProxyConfig(client.getHostConfiguration(), method.getURI().toString());
        if (proxyConfig != null && proxyConfig.getUsername() != null && proxyConfig.getPassword() != null) {
            if (state == null) {
                state = client.getState();
                if (state == null) {
                    state = new HttpState();
                }
            }
            state.setProxyCredentials(new AuthScope(proxyConfig.getHost(), proxyConfig.getPort()), new UsernamePasswordCredentials(proxyConfig.getUsername(), proxyConfig.getPassword()));
        }
        return client.executeMethod(proxyConfig, method, state);
    }

    public static <T extends EntityEnclosingMethod> T addInputStreamToHttpMethod(T method, InputStream is, long size, String contentType) {
        if (size < 0) {
            size = InputStreamRequestEntity.CONTENT_LENGTH_AUTO;
        }
        method.setRequestEntity(new InputStreamRequestEntity(is, size, contentType));
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new InputStreamRequestHttpRetryHandler());
        return method;
    }

    public static HttpState newHttpState(ZAuthToken authToken, String host, boolean isAdmin) {
        HttpState state = new HttpState();
        if (authToken != null) {
            Map<String, String> cookieMap = authToken.cookieMap(isAdmin);
            if (cookieMap != null) {
                for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                    state.addCookie(new Cookie(host, ck.getKey(), ck.getValue(), "/", null, false));
                }
            }
        }
        return state;
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

