/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.IOException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.util.IdleConnectionTimeoutThread;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * A wrapper class for HttpConnectionManagerParams.
 * 
 * Purposes:
 * 
 * 1. There should be a single common MultiThreadedHttpconnectionManager shared by
 *    nearly everyone so that sockets can be reused and a configurable limit put on
 *    client connections (per remote host and overall).
 *    
 * 2. HttpClient debug logging can be turned on/off on account level.
 *
 */
public class ZimbraHttpConnectionManager {
    
    private static HttpConnectionManagerParams sConnParams;
    // private static MultiThreadedHttpConnectionManager sHttpConnMgr;
    private static HttpConnectionManager sHttpConnMgr;
    private static IdleConnectionTimeoutThread sReaperThread;
    private static HttpClient sHttpClient;
    
    private static final Log sLog = LogFactory.getLog(ZimbraHttpConnectionManager.class);
    
    static {
        sConnParams = new HttpConnectionManagerParams();

        /* ------------------------------------------------------------------------
         * HttpConnectionManagerParams(subclass of HttpConnectionParams) params
         * ------------------------------------------------------------------------
         */ 
        
        /*
         * Defines the maximum number of connections allowed per host configuration.
         * 
         * HttpConnectionManagerParams.MAX_HOST_CONNECTIONS 
         */
        sConnParams.setDefaultMaxConnectionsPerHost(LC.httpclient_connmgr_max_host_connections.intValue());
        
        /*
         * Defines the maximum number of connections allowed overall.
         *
         * HttpConnectionManagerParams.MAX_TOTAL_CONNECTIONS 
         */
        sConnParams.setMaxTotalConnections(LC.httpclient_connmgr_max_total_connections.intValue());
        
        
        /* -------------------------------
         * HttpConnectionParams params 
         * -------------------------------
         */
        
        /*
         * Determines the timeout until a connection is established. A value of zero means the timeout is not used.
         *
         * HttpConnectionParams.CONNECTION_TIMEOUT
         */
         sConnParams.setConnectionTimeout(LC.httpclient_connmgr_connection_timeout.intValue());

        
        //
        // Determines the specified linger time in seconds
        //
        // HttpConnectionParams.SO_LINGER
        //
        // use default and do not expose in LC
        //
        
        //
        // Determines a hint the size of the underlying buffers used by the platform for outgoing network I/O.
        //
        // HttpConnectionParams.SO_RCVBUF
        //
        // use default and do not expose in LC
        //

        //
        // Determines a hint the size of the underlying buffers used by the platform for outgoing network I/O.
        //
        // HttpConnectionParams.SO_SNDBUF
        //
        // use default and do not expose in LC
        //

        /*
         * Defines the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
         * A timeout value of zero is interpreted as an infinite timeout. This value is used when no socket timeout 
         * is set in the HTTP method parameters. 
         * 
         * HttpConnectionParams.SO_TIMEOUT
         */
         sConnParams.setSoTimeout(LC.httpclient_connmgr_so_timeout.intValue());
         
        //
        // Determines whether stale connection check is to be used.
        //
        // HttpConnectionParams.STALE_CONNECTION_CHECK
        //
         sConnParams.setStaleCheckingEnabled(LC.httpclient_connmgr_keepalive_connections.booleanValue());
        
        //
        // Determines whether Nagle's algorithm is to be used.
        //
        // HttpConnectionParams.TCP_NODELAY
        //
         sConnParams.setTcpNoDelay(LC.httpclient_connmgr_tcp_nodelay.booleanValue());

         
        sHttpConnMgr = new MultiThreadedHttpConnectionManager();
        sHttpConnMgr.setParams(sConnParams);
        sHttpClient = new HttpClient(sHttpConnMgr);
        
//        sLog.info("initailized with parameters:\n" + 
//                  dumpParams(sConnParams));
    }
    
    public static HttpConnectionManager getDefaultHttpConnectinMangager() {
        return sHttpConnMgr;
    }
    
    /**
     * Create a HttpClient using our connection manager
     * 
     * 
     * http://hc.apache.org/httpclient-3.x/performance.html says:
     *     HttpClient is fully thread-safe when used with a thread-safe connection manager such as 
     *     MultiThreadedHttpConnectionManager.  Please note that each respective thread of execution 
     *     must have a local instance of HttpMethod and can have a local instance of HttpState or/and 
     *     HostConfiguration to represent a specific host configuration and conversational state. 
     *     At the same time the HttpClient instance and connection manager should be shared among 
     *     all threads for maximum efficiency. 
     *
     * That's best if used with the executeMethod API:
     *     executeMethod(HostConfiguration hostconfig,
     *                   HttpMethod method,
     *                   HttpState state)
     * where all components for the executeMethod invocation is passed in as parameters.
     * 
     * But for the following use pattern, which many of our callsites use, e.g. :
     *     HttpState state = new HttpState();
     *     Cookie cookie = new Cookie(...);
     *     state.addCookie(cookie);
     *     client.setState(state);
     *     client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
     *     executeMethod(method);
     *     
     * Although it is still "thread-safe", but the behavior is not desired.
     * Because if one thread set any property on the client, e.g. a HttpState, 
     * a HostConfiguration, or any HttpClient parameters, while all that are 
     * thread-safe, the properties of the HttpClient instance will be changes, 
     * and will be used by all callsites of ZimbraHttpConnectionManager.  
     * 
     * Sharing a HttpClient instance might be desired (for exactly the reasons
     * described above) by some callsites.  And if so, managing a shared instance 
     * of ZimbraHttpClient (returned by ZimbraHttpConnectionManager.getHttpClient()) 
     * should be done at the callsites - at least for now.  If it makes more sense 
     * to manage that by ZimbraHttpConnectionManager, we can do that later.
     * 
     * @return
     */
    public static ZimbraHttpClient getHttpClient() {
        return new ZimbraHttpClient();
    }
    
    public static ZimbraHttpClient getHttpClient(HttpClientParams httpClientParams) {
        return new ZimbraHttpClient(httpClientParams);
    }

    public static HttpClient getDefaultHttpClient() {
        return sHttpClient;
    }
    
    /*
     * HttpMethod.releaseConnection() doesn't actually close the socket unless 
     * "Connection: close" was sent back from server or if commons client wasn't 
     * explicitly asked to act like HTTP/1.0.   HttpMethod.releaseConnection() 
     * just release the connection back to the connection manager that fabricated 
     * it, so it can be reused by others.  Sockets opened by htpclient will 
     * have to be closed by finalizers, which is bad, as we don't have control 
     * over it.   This will cause lots of CLOSE_WAIT on the server from which 
     * http requests are sent via httpclient.
     * 
     * To get around that, we run a reaper thread to close idle connections 
     * owned by our connection manager..
     */
    
    /*
     * reaper thread methods
     */
    public static synchronized void startReaperThread() {
        Reaper.start();
    }
    
    public static synchronized void shutdownReaperThread() {
        Reaper.shutdown();
    }
    
    private static class Reaper {
        private static synchronized void start() {
            if (isReaperThreadRunning()) {
                sLog.warn("Cannot start a second http client idle connection reaper thread while another one is running.");
                return;
            }
            
            if (!reaperEnabled()) {
                sLog.info("Not starting http client idle connection reaper thread because it is disabled");
                return;
            }
    
             sLog.info("Starting http client idle connection reaper thread with sleep interval %s.", getReaperSleepInterval());
    
            // Start thread
            sReaperThread = new IdleConnectionTimeoutThread();
            sReaperThread.addConnectionManager(sHttpConnMgr);
            sReaperThread.setConnectionTimeout(getReaperConnectionTimeout());
            sReaperThread.setTimeoutInterval(getReaperSleepInterval());
            sReaperThread.start();
        }
        
        private static synchronized void shutdown() {
            if (!isReaperThreadRunning()) {
                sLog.warn("shutting down http client idle connection reaper thread requested but the reaper thread is not running");
                return;
            }
            
            sLog.warn("shutting down http client idle connection reaper thread");
            
            sReaperThread.shutdown();
            sReaperThread = null;
        }
        
        private static synchronized boolean isReaperThreadRunning() {
            return (sReaperThread != null);
        }
        
        private static long getReaperSleepInterval() {
            return LC.httpclient_connmgr_idle_reaper_sleep_interval.longValue();
        }
        
        private static boolean reaperEnabled() {
            return getReaperSleepInterval() != 0;
        }
        
        private static long getReaperConnectionTimeout() {
            return LC.httpclient_connmgr_idle_reaper_connection_timeout.longValue();
        }
    }
    
    /**
     * 
     * A wrapper of HttpClient to disable methods that could change
     * connection manager parameters
     * 
     * All instances of HttpClient wrapped in ZimbraHttpClient are created/managed 
     * by the same instance of HttpConnectionManager, which get it's parameters 
     * from LC keys.  We do not want any of the HttpConnectionManager parameters 
     * changed via any ZimbraHttpClient instances, because it will affect all 
     * subsequent connections (e.g. connection timeout) fabricated by the connection 
     * manager.
     * 
     * e.g. if we didn't have this wrapper and just return the HttpClient instance 
     *      directly to callers, a callsite can do:
     *      httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
     *      and the connection timeout change will affect all connections managed by 
     *      the connection manager when new sockets are being opened.
     *      
     *      or it can change connection manager parameter like http.connection-manager.max-per-host,
     *      http.connection-manager.max-total, which defeats the whole purpose of having 
     *      a common connection manager.
     *       
     *       
     * To get around the limitation, there are 2 options when you need to change connection 
     * parameters:
     * 
     * 1. If a HttpConnectionManager or HttpConnection parameter is also configurable on 
     *    HttpClient or HttpMethod or HostConfiguration, it should be set on the HttpClient 
     *    or HttpMethod/HostConfiguration instance that are passed to
     *    HttpClient.executeMethod(HttpMethod method) or
     *    HttpClient.executeMethod(HostConfiguration hostConfiguration, HttpMethod method) 
     * 
     *    e.g. The connection parameter HttpConnectionParams.SO_TIMEOUT ("http.socket.timeout") 
     *         can also be set on a HttpClient or HttpMethod instance. 
     *      
     *         See http://hc.apache.org/httpclient-3.x/preference-api.html for the 
     *         Global -> HttpClient -> HostConfiguration -> HttpMethod preference hierarchy.
     *      
     *         To do that using a ZimbraHttpClient, instead of doing:
     *      
     *             zimbraHttpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
     *      
     *         which is disabled anyway in ZimbraHttpClient, do this:
     *      
     *             GetMethod httpGet = new GetMethod(uri);
     *             httpGet.getParams().setLongParameter(HttpConnectionParams.SO_TIMEOUT, soTimeout);
     *             try {
     *                 zimbraHttpClient.executeMethod(httpGet);
     *                 ...
     *             } finally {
     *                 httpGet.releaseConnection();
     *             }
     *      
     * 2. If 1 is still restricting you from doing things you need, do not use 
     *    ZimbraHttpConnectionManager.getHttpClient().  Use the native "new HttpClient()" 
     *    or "new HttpClient(HttpConnectionManager)" instead.
     *    This of course defeats the purpose of having ZimBraHttpConnectionManager, but is 
     *    a way out if really necessary.  This route should be avoided if possible.
     *
     */

    public static class ZimbraHttpClient {

        private HttpClient mHttpClient;
        
        private ZimbraHttpClient() {
            mHttpClient = new HttpClient(sHttpConnMgr);
        }
        
        private ZimbraHttpClient(HttpClientParams httpClientParams) {
            mHttpClient = new HttpClient(httpClientParams, sHttpConnMgr);
        }
        
        /*
         * For disabled HttpClient methods, throw a ServiceException.
         * For other methods, delegate to the wrapped HttpClient instance.
         * 
         * This is kind of ugly because if HttpClient API changes we will have to 
         * change accordingly.  
         * 
         * Another way to do this is subclassing HttpClient instead of wrapping it.
         * 
         * It is not done that way because:
         * 1. HttpClient.getHttpConnectionManager() is also invoked from within http client 
         *    package internallly, we can't mess up with it in the subclass.
         *    
         * and   
         * 2. We won't be able to throw ServiceException for the methods that are disabled, 
         *    because it does not conform to the HttpClient API.   
         */
        
        /* ===========================
         * 
         *     disabled methods
         *     
         * ===========================    
         */
        
        // after getting the HttpConnectionManager, one can change it's parameters, disallow it
        public HttpConnectionManager getHttpConnectionManager() throws ServiceException {
            throw ServiceException.FAILURE("method disabled", null);
        }

        // HttpClient.setConnectionTimeout actually sets the connection timeout parameter on the 
        // HttpConnectionManager instance of the HttpClient, disallow it.
        public void setConnectionTimeout(int newTimeoutInMilliseconds) throws ServiceException {
            throw ServiceException.FAILURE("method disabled", null);
        }

        // no no, can't change the connection manager
        public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) throws ServiceException {
            throw ServiceException.FAILURE("method disabled", null);
        }
        
        
        /* ===========================
         * 
         *     not disabled methods
         *     
         * ===========================    
         */
        
        public int executeMethod(HostConfiguration hostConfiguration, HttpMethod method) throws IOException, HttpException {
            return mHttpClient.executeMethod(hostConfiguration, method);
        }
        
        public int executeMethod(HostConfiguration hostconfig, HttpMethod method, HttpState state) throws IOException, HttpException {
            return mHttpClient.executeMethod(hostconfig, method, state);
        }
        
        public int executeMethod(HttpMethod method) throws IOException, HttpException {
            return mHttpClient.executeMethod(method);
        }
        
        public String getHost() {
            return mHttpClient.getHost();
        }
        
        public HostConfiguration getHostConfiguration() {
            return mHttpClient.getHostConfiguration();
        }
        
        public HttpClientParams getParams() {
            return mHttpClient.getParams();
        }
        
        public int getPort() {
            return mHttpClient.getPort();
        }
        
        public HttpState getState() {
            return mHttpClient.getState();
        }
        
        public boolean isStrictMode() {
            return mHttpClient.isStrictMode();
        }
        
        public void setHostConfiguration(HostConfiguration hostConfiguration) {
            mHttpClient.setHostConfiguration(hostConfiguration);
        }
        
        public void setHttpConnectionFactoryTimeout(long timeout) {
            mHttpClient.setHttpConnectionFactoryTimeout(timeout);
        }
        
        public void setParams(HttpClientParams params) {
            mHttpClient.setParams(params);
        }
        
        public void setState(HttpState state) {
            mHttpClient.setState(state);
        }
        
        public void setStrictMode(boolean strictMode) {
            mHttpClient.setStrictMode(strictMode);
        }
        
        public void setTimeout(int newTimeoutInMilliseconds) {
            mHttpClient.setTimeout(newTimeoutInMilliseconds);
        }
    }
    
    
    
    
    /*
     * methods for unittest only, do not call it in the server!
     */
    public static int getConnectionsInPool() {
        // return sHttpConnMgr.getConnectionsInPool();
        return 0;
    }
    
    private static String dumpParams(HttpConnectionManagerParams params) {
        // dump httpclient package defaults if params is null
        if (params == null)
            params = new HttpConnectionManagerParams();
     
        StringBuilder sb = new StringBuilder();
        sb.append("HttpConnectionManagerParams DefaultMaxConnectionsPerHost  : " + params.getDefaultMaxConnectionsPerHost() + "\n");
        sb.append("HttpConnectionManagerParams MaxTotalConnections           : " + params.getMaxTotalConnections() + "\n");
        
        sb.append("HttpConnectionParams ConnectionTimeout                    : " + params.getConnectionTimeout() + "\n");
        sb.append("HttpConnectionParams Linger                               : " + params.getLinger() + "\n");
        sb.append("HttpConnectionParams ReceiveBufferSize                    : " + params.getReceiveBufferSize() + "\n");
        sb.append("HttpConnectionParams SendBufferSize                       : " + params.getSendBufferSize() + "\n");
        sb.append("HttpConnectionParams SoTimeout                            : " + params.getSoTimeout() + "\n");
        sb.append("HttpConnectionParams TcpNoDelay                           : " + params.getTcpNoDelay() + "\n");
        sb.append("HttpConnectionParams isStaleCheckingEnabled               : " + params.isStaleCheckingEnabled() + "\n");
        
        return sb.toString();
    }
    
    public static void main(String[] args) {
        System.out.println(dumpParams(null));
    }
    
}


