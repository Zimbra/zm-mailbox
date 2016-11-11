package com.zimbra.common.httpclient;

import java.io.IOException;

import javax.annotation.PreDestroy;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import com.zimbra.common.localconfig.LC;

/**
 * 
 * @author Greg Solovyev
 *
 * Wrapper class for Apache HTTP Components HttpClient 4. 
 * Server code should use this wrapper to ensure that outgoing HTTP connections are being reused. 
 */

public class ZimbraHttpClientManager {
    protected static ZimbraHttpClientManager instance;
    final CloseableHttpAsyncClient internalAsyncClient;
    final RequestConfig internalRequestConfig;
    
    public ZimbraHttpClientManager() {
        internalRequestConfig = RequestConfig.custom()
                .setConnectTimeout(LC.httpclient_internal_connmgr_connection_timeout.intValue())
                .setSocketTimeout(LC.httpclient_internal_connmgr_so_timeout.intValue())
                    .build();
        
        internalAsyncClient = HttpAsyncClients.custom().setDefaultRequestConfig(internalRequestConfig).build();
    }
    
    public static synchronized ZimbraHttpClientManager getInstance() {
        if(instance == null) {
            instance = new ZimbraHttpClientManager();
        }
        return instance;
    }
    
    /**
     * @return instance of asynchronous HttpClient to be used for connections between Zimbra services
     */
    public CloseableHttpAsyncClient getInternalAsyncHttpClient() {
        return internalAsyncClient;
    }
    
    /**
     * orderly shutdown the client
     */
    @PreDestroy
    public void shutDown() throws IOException {
        internalAsyncClient.close();
    }
}
