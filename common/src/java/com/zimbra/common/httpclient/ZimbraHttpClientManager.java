package com.zimbra.common.httpclient;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.ssl.SSLContexts;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.CustomTrustManager;
import com.zimbra.common.util.ZimbraLog;

/**
 * 
 * @author Greg Solovyev
 *
 * Wrapper class for Apache HTTP Components HttpClient 4. 
 * Server code should use this wrapper to ensure that outgoing HTTP connections are being reused. 
 */

public class ZimbraHttpClientManager {
    protected static ZimbraHttpClientManager instance;
    private final CloseableHttpAsyncClient internalAsyncClient;
    private final CloseableHttpClient internalClient;
    private final CloseableHttpClient externalClient;
    private ZimbraHttpClientManager() {
        PoolingHttpClientConnectionManager internalConnectionMgr;
        PoolingHttpClientConnectionManager externallConnectionMgr;
        RequestConfig internalRequestConfig;
        RequestConfig externalRequestConfig;
        SSLContext sslcontext = null;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(CustomTrustManager.loadKeyStore(), new TrustSelfSignedStrategy()).build();
        } catch (GeneralSecurityException e) {
            ZimbraLog.security.error("Failed to load local keystore");
        }
        if(sslcontext != null) {
            internalAsyncClient = HttpAsyncClients.custom().setSSLContext(sslcontext)
                    .setMaxConnTotal(LC.httpclient_internal_connmgr_max_total_connections.intValue()).build();
        } else {
            internalAsyncClient = HttpAsyncClients.custom()
                    .setMaxConnTotal(LC.httpclient_internal_connmgr_max_total_connections.intValue())
                    .build();
        }

        internalAsyncClient.start();

        internalConnectionMgr = new PoolingHttpClientConnectionManager();
        internalConnectionMgr.setDefaultMaxPerRoute(LC.httpclient_internal_connmgr_max_host_connections.intValue());
        internalConnectionMgr.setMaxTotal(LC.httpclient_internal_connmgr_max_total_connections.intValue());
        internalRequestConfig = RequestConfig.custom()
                .setConnectTimeout(LC.httpclient_internal_connmgr_connection_timeout.intValue())
                .setSocketTimeout(LC.httpclient_internal_connmgr_so_timeout.intValue())
                .setStaleConnectionCheckEnabled(LC.httpclient_internal_connmgr_stale_connection_check.booleanValue())
                    .build();
        internalClient = HttpClientBuilder.create()
                .setConnectionManager(internalConnectionMgr)
                .setDefaultRequestConfig(internalRequestConfig)
                .setDefaultSocketConfig(SocketConfig.custom()
                    .setTcpNoDelay(LC.httpclient_internal_connmgr_tcp_nodelay.booleanValue())
                        .build())
                            .build();
        externallConnectionMgr = new PoolingHttpClientConnectionManager();
        externallConnectionMgr.setDefaultMaxPerRoute(LC.httpclient_external_connmgr_max_host_connections.intValue());
        externallConnectionMgr.setMaxTotal(LC.httpclient_external_connmgr_max_total_connections.intValue());
        externalRequestConfig = RequestConfig.custom().
                setConnectTimeout(LC.httpclient_external_connmgr_connection_timeout.intValue())
                .setSocketTimeout(LC.httpclient_external_connmgr_so_timeout.intValue())
                .setStaleConnectionCheckEnabled(LC.httpclient_external_connmgr_stale_connection_check.booleanValue())
                    .build();
        externalClient = HttpClientBuilder.create()
                .setConnectionManager(externallConnectionMgr)
                .setDefaultRequestConfig(externalRequestConfig)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setTcpNoDelay(LC.httpclient_external_connmgr_tcp_nodelay.booleanValue())
                            .build())
                                .build();
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
     * @return instance of asynchronous HttpClient to be used for connections between Zimbra services
     */
    public CloseableHttpClient getInternalHttpClient() {
        return internalClient;
    }

    public CloseableHttpClient getExternalHttpClient() {
        return externalClient;
    }

    /**
     * orderly shutdown the client
     */
    @PreDestroy
    public void shutDown() throws IOException {
        internalAsyncClient.close();
        internalClient.close();
        externalClient.close();
    }
}
