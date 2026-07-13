/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public final class HttpUtilities {

    private static final CloseableHttpClient CLIENT =
            ZimbraHttpConnectionManager.getExternalHttpConnMgr().getDefaultHttpClient().build();

    private HttpUtilities() {
    }

    public static HttpResponseWrapper postForm(String url, Map<String, String> form, int connectTimeoutMs,
                                               int socketTimeoutMs, Map<String, String> headers)
            throws ServiceException {

        if (StringUtils.isEmpty(url)) {
            throw ServiceException.INVALID_REQUEST("Token endpoint for ROPC is not configured", null);
        }

        HttpPost post = new HttpPost(url);
        post.setConfig(RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .build());
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() != null && header.getValue() != null) {
                    post.setHeader(header.getKey(), header.getValue());
                }
            }
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (form != null) {
            for (Map.Entry<String, String> e : form.entrySet()) {
                if (e.getValue() != null) {
                    params.add(new BasicNameValuePair(e.getKey(), e.getValue()));
                }
            }
        }
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

        CloseableHttpResponse response = null;
        try {
            response = CLIENT.execute(post);
            int status = response.getStatusLine().getStatusCode();
            byte[] body = (response.getEntity() == null)
                    ? new byte[0] : EntityUtils.toByteArray(response.getEntity());
            return new HttpResponseWrapper(status, body);
        } catch (ConnectionPoolTimeoutException e) {
            throw ServiceException.FAILURE("ROPC HTTP connection pool exhausted (local)", e);
        } catch (ClientProtocolException e) {
            throw ServiceException.FAILURE("ROPC HTTP protocol error contacting IdP (remote)", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("ROPC HTTP transport error contacting IdP (remote)", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException ignore) {
                    // best-effort release
                }
            }
            post.releaseConnection();
        }
    }
}
