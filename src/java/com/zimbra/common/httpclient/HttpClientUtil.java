package com.zimbra.common.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;

import com.zimbra.common.util.ZimbraHttpConnectionManager;

public class HttpClientUtil {
    
    public static int executeMethod(HttpMethod method) throws HttpException, IOException {
        return executeMethod(ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient(), method, null);
    }    
    public static int executeMethod(HttpClient client, HttpMethod method) throws HttpException, IOException {
        return executeMethod(client, method, null);
    }

    public static int executeMethod(HttpClient client, HttpMethod method, HttpState state) throws HttpException, IOException {
        return client.executeMethod(HttpProxyConfig.getProxyConfig(client.getHostConfiguration(), method.getURI().toString()), method, state);
    }
    
    public static <T extends EntityEnclosingMethod> T addInputStreamToHttpMethod(T method, InputStream is, long size, String contentType) {
        if (size < 0) {
            size = InputStreamRequestEntity.CONTENT_LENGTH_AUTO;
        }
        method.setRequestEntity(new InputStreamRequestEntity(is, size, contentType));
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new InputStreamRequestHttpRetryHandler());
        return method;
    }
}
