package com.zimbra.common.httpclient;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

public class HttpClientUtil {
    
    public static int executeMethod(HttpClient client, HttpMethod method) throws HttpException, IOException {
        return executeMethod(client, method, null);
    }

    public static int executeMethod(HttpClient client, HttpMethod method, HttpState state) throws HttpException, IOException {
        return client.executeMethod(HttpProxyConfig.getProxyConfig(method.getURI().toString()), method, state);
    }
}
