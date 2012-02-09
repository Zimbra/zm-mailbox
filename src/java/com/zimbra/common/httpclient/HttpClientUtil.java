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
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                state.addCookie(new Cookie(host, ck.getKey(), ck.getValue(), "/", null, false));
            }
        }
        return state;
    }
}

