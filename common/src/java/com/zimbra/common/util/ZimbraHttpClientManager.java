/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

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
    final CloseableHttpClient internalClient;
    final CloseableHttpClient externalClient;
    final RequestConfig internalRequestConfig;
    final RequestConfig externalRequestConfig;
    final PoolingHttpClientConnectionManager internalConnectionMgr;
    final PoolingHttpClientConnectionManager externallConnectionMgr;
    
    public ZimbraHttpClientManager() {
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
    
    public CloseableHttpClient getExternalHttpClient() {
        return externalClient; 
    }
    
    public CloseableHttpClient getInternalHttpClient() {
        return internalClient;
    }
    
    @PostConstruct
    public void start() {
        //nothing to do here for now
    }
    
    /**
     * orderly shutdown the service
     */
    @PreDestroy
    public void shutDown() throws IOException {
        internalClient.close();
        externalClient.close();
        internalConnectionMgr.close();
        externallConnectionMgr.close();
    }
}
