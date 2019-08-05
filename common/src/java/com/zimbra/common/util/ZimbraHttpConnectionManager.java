/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;

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

    // connection manager for Zimbra internal connections
    private static final ZimbraHttpConnectionManager INTERNAL_CONN_MGR =
        new ZimbraHttpConnectionManager("Internal http client connection manager", new InternalConnMgrParams());

    // connection manager for all external connections
    private static final ZimbraHttpConnectionManager EXTERNAL_CONN_MGR =
        new ZimbraHttpConnectionManager("External http client connection manager", new ExternalConnMgrParams());

    // logger
    private static final Log sLog = LogFactory.getLog(ZimbraHttpConnectionManager.class);
    public abstract static class ZimbraConnMgrParams {

        protected  RequestConfig reqConfig = RequestConfig.DEFAULT;
        protected SocketConfig socketConfig = SocketConfig.DEFAULT;


        public RequestConfig getReqConfig() {
            return reqConfig;
        }

        public SocketConfig getSocketConfig() {
            return socketConfig;
        }
        abstract long getHttpClientConnectionTimeout();
        abstract long getReaperSleepInterval();
        abstract long getReaperConnectionTimeout();
        abstract int getDefaultMaxConnectionsPerHost();
        abstract int getMaxTotalConnection();

    }

    private static class InternalConnMgrParams extends ZimbraConnMgrParams {

        private InternalConnMgrParams() {
            this.socketConfig = SocketConfig.custom().setSoTimeout(LC.httpclient_external_connmgr_so_timeout.intValue())
                .setTcpNoDelay(LC.httpclient_external_connmgr_tcp_nodelay.booleanValue()).build();
            this.reqConfig = RequestConfig.custom().setStaleConnectionCheckEnabled(LC.httpclient_external_connmgr_stale_connection_check.booleanValue())
                .setConnectTimeout( LC.httpclient_internal_connmgr_connection_timeout.intValue())
                .build();
        }

        @Override
        public long getHttpClientConnectionTimeout() {
            return LC.httpclient_internal_client_connection_timeout.longValue();
        }

        @Override
        long getReaperSleepInterval() {
            //
            // Sets the interval between closing idle connections.
            // Idle connections will be closed every timeoutInterval milliseconds.
            //
            return LC.httpclient_internal_connmgr_idle_reaper_sleep_interval.longValue();
        }

        @Override
        long getReaperConnectionTimeout() {
            //
            // Sets the timeout value to use when testing for idle connections.
            //
            return LC.httpclient_internal_connmgr_idle_reaper_connection_timeout.longValue();
        }

        /* (non-Javadoc)
         * @see com.zimbra.common.util.ZimbraHttpConnectionManager.ZimbraConnMgrParams#getDefaultMaxConnectionsPerHost()
         */
        @Override
        int getDefaultMaxConnectionsPerHost() {
           return LC.httpclient_internal_connmgr_max_host_connections.intValue();
        }

        /* (non-Javadoc)
         * @see com.zimbra.common.util.ZimbraHttpConnectionManager.ZimbraConnMgrParams#getMaxTotalConnection()
         */
        @Override
        int getMaxTotalConnection() {
            return LC.httpclient_internal_connmgr_max_total_connections.intValue();
        }

    }

private static class ExternalConnMgrParams extends ZimbraConnMgrParams {

        private ExternalConnMgrParams() {
            this.socketConfig = SocketConfig.custom().setSoTimeout(LC.httpclient_external_connmgr_so_timeout.intValue())
                .setTcpNoDelay(LC.httpclient_external_connmgr_tcp_nodelay.booleanValue()).build();
            this.reqConfig = RequestConfig.custom().setStaleConnectionCheckEnabled(LC.httpclient_external_connmgr_stale_connection_check.booleanValue())
                .build();
        }

        @Override
        public long getHttpClientConnectionTimeout() {
            return LC.httpclient_external_client_connection_timeout.longValue();
        }

        @Override
        long getReaperSleepInterval() {
            return LC.httpclient_external_connmgr_idle_reaper_sleep_interval.longValue();
        }

        @Override
        long getReaperConnectionTimeout() {
            return LC.httpclient_external_connmgr_idle_reaper_connection_timeout.longValue();
        }

        /* (non-Javadoc)
         * @see com.zimbra.common.util.ZimbraHttpConnectionManager.ZimbraConnMgrParams#getDefaultMaxConnectionsPerHost()
         */
        @Override
        int getDefaultMaxConnectionsPerHost() {
           return LC.httpclient_internal_connmgr_max_host_connections.intValue();
        }

        /* (non-Javadoc)
         * @see com.zimbra.common.util.ZimbraHttpConnectionManager.ZimbraConnMgrParams#getMaxTotalConnection()
         */
        @Override
        int getMaxTotalConnection() {
            return LC.httpclient_internal_connmgr_max_total_connections.intValue();
        }
    }

    public static synchronized void startReaperThread() {
        INTERNAL_CONN_MGR.idleReaper.startReaperThread();
        EXTERNAL_CONN_MGR.idleReaper.startReaperThread();
    }

    public static synchronized void shutdownReaperThread() {
        INTERNAL_CONN_MGR.idleReaper.shutdownReaperThread();
        EXTERNAL_CONN_MGR.idleReaper.shutdownReaperThread();
    }

    public static ZimbraHttpConnectionManager getInternalHttpConnMgr() {
        return INTERNAL_CONN_MGR;
    }

    public static ZimbraHttpConnectionManager getExternalHttpConnMgr() {
        return EXTERNAL_CONN_MGR;
    }

    private String name;
    private ZimbraConnMgrParams zimbraConnMgrParams;
    private PoolingHttpClientConnectionManager httpConnMgr;
    private HttpClientBuilder defaultHttpClient;
    private IdleReaper idleReaper;


    private ZimbraHttpConnectionManager(String name, ZimbraConnMgrParams zimbraConnMgrParams) {
        this.name = name;
        this.zimbraConnMgrParams = zimbraConnMgrParams;
        if (SocketFactories.getRegistry() != null) {
            this.httpConnMgr = new PoolingHttpClientConnectionManager(SocketFactories.getRegistry());
        } else {
            try {
                final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true).build();
                this.httpConnMgr = new PoolingHttpClientConnectionManager(RegistryBuilder
                    .<ConnectionSocketFactory> create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https",
                        new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                    .build());
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                ZimbraLog.misc.info("Error creating http connection manager, with default socket factory");
            }
        }

        this.httpConnMgr.setDefaultMaxPerRoute( zimbraConnMgrParams.getDefaultMaxConnectionsPerHost());
        this.httpConnMgr.setMaxTotal(zimbraConnMgrParams.getMaxTotalConnection());

        this.httpConnMgr.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(LC.socket_so_timeout.intValue()).build());

        this.defaultHttpClient = createHttpClient();

        // Instantiate the reaper object.
        // Note: Reaper thread is not started until ZimbraHttpConnectionManager.startReaperThread
        // is called.
        this.idleReaper = new IdleReaper(this);

    }




    public ZimbraConnMgrParams getZimbraConnMgrParams() {
        return zimbraConnMgrParams;
    }


    /**
     * Returns the default HttpClient instance associated with this connection
     * manager.
     *
     * *** See "Important notes on using HttpClient returned by ..." above. ***
     *
     * http://hc.apache.org/httpclient-3.x/performance.html says: HttpClient is
     * fully thread-safe when used with a thread-safe connection manager such as
     * MultiThreadedHttpConnectionManager. Please note that each respective
     * thread of execution must have a local instance of HttpMethod and can have
     * a local instance of BasicCookieStore or/and HostConfiguration to represent a
     * specific host configuration and conversational state. At the same time
     * the HttpClient instance and connection manager should be shared among all
     * threads for maximum efficiency.
     *
     * For callsites obtaining the HttpClient instance from this API, if
     * HostConfiguration and/or BasicCookieStore need to be changed on an invocation of
     * HttpClient.executeMethod(), they should use the executeMethod API:
     * executeMethod(HostConfiguration hostconfig, HttpMethod method, HttpState
     * state) where all components for the executeMethod invocation is passed in
     * as parameters.
     *
     * Instead of calling HttpClient.setHostConfiguration(...),
     * HttpClient.setState(...).
     *
     * Also, callsites should *not* alter any state on the returned HttpClient
     * instance by calling any of the HttpClient.set***() methods because this
     * singleton instance is shared by all threads/callsites on the system.
     *
     * @return the default HttpClient instance associated with this connection
     *         manager
     */
    public HttpClientBuilder getDefaultHttpClient() {
        return defaultHttpClient;
    }

    public void closeIdleConnections () {
        this.httpConnMgr.closeIdleConnections(0, TimeUnit.MILLISECONDS);
    }

    private String getName() {
        return name;
    }

    private ZimbraConnMgrParams getParams() {
        return zimbraConnMgrParams;
    }

    /**
     *
     * @return
     */
    public HttpClientConnectionManager getHttpConnMgr() {
        return httpConnMgr;
    }

    /**
     * Create a new HttpClient instance associated with this connection manager.
     *
     * *** See "Important notes on using HttpClient returned by ..." above. ***
     *
     * Callsites of this API are free to alter states of the returned HttpClient
     * instance, because an new instance is created each time this API is
     * called.
     *
     * e.g. many of our callsites use the pattern: BasicCookieStore state = new
     * BasicCookieStore(); Cookie cookie = new Cookie(...); state.addCookie(cookie);
     * client.setState(state);
     * client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
     * executeMethod(method);
     *
     * The above is fine with the HttpClient object returned by this API.
     *
     * @return a new HttpClient instance associated with this connection
     *         manager.
     */
    public HttpClientBuilder newHttpClient() {
        return createHttpClient();
    }

    private HttpClientBuilder createHttpClient() {
        return HttpClients.custom()
            .setConnectionManager(this.httpConnMgr)
            .setDefaultRequestConfig(this.zimbraConnMgrParams.reqConfig)
            .setDefaultSocketConfig(this.zimbraConnMgrParams.socketConfig);
    }



    /*
     * =====================
     *  Idle Reaper
     * =====================
     *
     * HttpMethod.releaseConnection() doesn't actually close the socket unless
     * "Connection: close" was sent back from server or if commons client wasn't
     * explicitly asked to act like HTTP/1.0.   HttpMethod.releaseConnection()
     * just release the connection back to the connection manager that created
     * it, so it can be reused by others.  Sockets opened by httpclient will
     * have to be closed by finalizers, which is bad, as we don't have control
     * over it.   This will cause lots of CLOSE_WAIT on the server from which
     * http requests are sent via httpclient.
     *
     * To get around that, we run a reaper thread to close idle connections
     * owned by our connection manager.
     */
    private static class IdleReaper {
        // the ZimbraHttpConnectionManager instance this IdleReaper is for.
        private ZimbraHttpConnectionManager connMgr;

        private IdleConnectionMonitorThread reaperThread;

        private IdleReaper(ZimbraHttpConnectionManager connMgr) {
            this.connMgr = connMgr;
        }

        private void startReaperThread() {
            // sanity check
            if (isReaperThreadRunning()) {
                sLog.warn("Cannot start a second http client idle connection reaper thread while another one is running.");
                return;
            }

            if (!reaperEnabled()) {
                sLog.info("Not starting http client idle connection reaper thread for " + connMgr.getName() + " because it is disabled");
                return;
            }

            sLog.info("Starting http client idle connection reaper thread for " + connMgr.getName() +
                    " - reaper sleep interval=%d, reaper connection timeout=%d",
                    getReaperSleepInterval(), getReaperConnectionTimeout());


            // create and start thread
            reaperThread = new IdleConnectionMonitorThread(connMgr.getHttpConnMgr());
            reaperThread.setName("IdleConnectionTimeoutThread" + " for " + connMgr.getName());



            reaperThread.setConnectionTimeout(getReaperConnectionTimeout());
            reaperThread.setTimeoutInterval(getReaperSleepInterval());
            reaperThread.start();
        }

        private void shutdownReaperThread() {
            if (!isReaperThreadRunning()) {
                sLog.warn("shutting down http client idle connection reaper thread requested but the reaper thread is not running");
                return;
            }

            sLog.info("shutting down http client idle connection reaper thread");

            reaperThread.shutdown();
            reaperThread = null;
        }

        private boolean isReaperThreadRunning() {
            return (reaperThread != null);
        }

        private boolean reaperEnabled() {
            return getReaperSleepInterval() != 0;
        }

        private long getReaperSleepInterval() {
            return connMgr.getParams().getReaperSleepInterval();
        }

        private long getReaperConnectionTimeout() {
            return LC.httpclient_internal_connmgr_idle_reaper_connection_timeout.longValue();
        }
    }

    public static class IdleConnectionMonitorThread extends Thread {

        private long timeoutInterval = 1000;
        private long connectionTimeout = 3000;
        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        /**
         * @param reaperSleepInterval
         */
        public void setTimeoutInterval(long reaperSleepInterval) {
            this.timeoutInterval = reaperSleepInterval;

        }

        /**
         * @param reaperConnectionTimeout
         */
        public void setConnectionTimeout(long reaperConnectionTimeout) {
            this.connectionTimeout = reaperConnectionTimeout;

        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(this.timeoutInterval);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than set sec
                        connMgr.closeIdleConnections(this.connectionTimeout, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                sLog.debug("Reaper thread was interrupted");
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }

}
