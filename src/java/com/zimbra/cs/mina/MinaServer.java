/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.NetUtil;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.Zimbra;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for MINA-based IMAP/POP3/LMTP servers. Handles creation of new
 * MINA request and connection handler instances.
 */
public abstract class MinaServer implements Server {
    protected final ServerSocketChannel mChannel;
    protected final SocketAcceptorConfig mAcceptorConfig;
    protected final ExecutorService mHandlerThreadPool;
    protected final SocketAcceptor mSocketAcceptor;
    protected final ServerConfig mServerConfig;
    protected final MinaStats mStats;
    
    private static SSLContext sslContext;
    private static String[] mSslEnabledCipherSuites;

    private static final String HANDLER_THREAD_NAME = "MinaProtocolHandler";
    
    private static final int NUM_IO_PROCESSORS =
        Runtime.getRuntime().availableProcessors() + 1;
    
    // There is one I/O thread pool shared by all protocol handlers
    private static final ExecutorService IO_THREAD_POOL =
        Executors.newCachedThreadPool(new MinaThreadFactory("MinaIoProcessorThread"));
    
    private static synchronized SSLContext getSSLContext() {
        if (sslContext == null) {
            try {
                sslContext = initSSLContext();
            } catch (Exception e) {
                Zimbra.halt("exception initializing SSL context", e);
            }
        }
        return sslContext;
    }
    
    private static SSLContext initSSLContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] pass = LC.mailboxd_keystore_password.value().toCharArray();
        ks.load(new FileInputStream(LC.mailboxd_keystore.value()), pass);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, pass);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return context;
    }

    /**
     * Our cipher config attribute zimbraSSLExcludeCipherSuites specifies a list of ciphers that should be 
     * disabled instead of enabled.  This is because we want the same attribute to control all SSL protocols 
     * running on mailbox servers.  For https, Jetty configuration only supports an excluded list.  
     * Therefore we adapted the same scheme for zimbraSSLExcludeCipherSuites, which is written to jetty.xml 
     * by config rewrite, and will be used for protocols (imaps/pop3s) handled by Zimbra code.
     * 
     * For nio based servers/handlers, MinaServer uses SSLFilter for SSL communication.  SSLFilter wraps 
     * an SSLEngine that actually does all the work.  SSLFilter.setEnabledCipherSuites() sets the list of 
     * cipher suites to be enabled when the underlying SSLEngine is initialized.  Since we only have an 
     * excluded list, we need to exclude those from the ciphers suites which are currently enabled for use 
     * on a engine. 
     *
     * Since we do not directly interact with a SSLEngine while sessions are handled,  and there is 
     * no SSLFilter API to alter the SSLEngine it wraps, we workaround this by doing the following:
     *   - create a dummy SSLEngine from the same SSLContext instance that will be used for all SSL communication.
     *   - get the enabled ciphers from SSLEngine.getEnabledCipherSuites()
     *   - exclude the ciphers we need to exclude from the enabled ciphers, so we now have a net enabled ciphers
     *     list.
     * The above is only done once and we keep a singleton of this cipher list.  We then can pass it to 
     * SSLFilter.setEnabledCipherSuites() for SSL and StartTLS session.
     * 
     * @param sslCtxt ssl context
     * @param serverConfig mina server config
     * @return array of enabled ciphers, or empty array (note, not null) if all default ciphers should be enabled.
     *         (i.e. we do not need to alter the default ciphers)
     */
    private static synchronized String[] getSSLEnabledCiphers(SSLContext sslCtxt, ServerConfig serverConfig) {
        if (mSslEnabledCipherSuites == null) {
            try {
                String[] excludeCiphers = serverConfig.getSslExcludedCiphers();

                if (excludeCiphers != null && excludeCiphers.length > 0) {
                    // create a SSLEngine to get the ciphers enabled for the engine
                    SSLEngine sslEng = sslCtxt.createSSLEngine();
                    String[] enabledCiphers = sslEng.getEnabledCipherSuites();
                    mSslEnabledCipherSuites = NetUtil.computeEnabledCipherSuites(enabledCiphers, excludeCiphers);
                } 

                /*
                 * if null, it means we do not need to alter the ciphers - either excluded cipher is not configures,
                 * or the original SSLEngine default is null/empty (can this happen?).
                 * 
                 * set it to empty array to indicate that mSslEnabledCipherSuites has been initialized.
                 */
                if (mSslEnabledCipherSuites == null)
                    mSslEnabledCipherSuites = new String[0];
                
            } catch (Exception e) {
                Zimbra.halt("exception initializing SSL enabled ciphers", e);
            }
        }
        return mSslEnabledCipherSuites;
    }

    public SSLFilter newSSLFilter() {
        SSLContext sslCtxt = getSSLContext();
        SSLFilter sslFilter = new SSLFilter(sslCtxt);
        String [] enabledCiphers = getSSLEnabledCiphers(sslCtxt, mServerConfig);
        if (enabledCiphers.length > 0)
            sslFilter.setEnabledCipherSuites(enabledCiphers);
        return sslFilter;
    }
    
    /**
     * Creates a new server for the specified configuration.
     * 
     * @param config the ServerConfig for the server
     * @param pool the optional handler thread pool to use for this server
     * @throws ServiceException if a ServiceException occured
     */
    protected MinaServer(ServerConfig config, ExecutorService pool) throws ServiceException {
        mServerConfig = config;
        mHandlerThreadPool = pool;
        mChannel = config.getServerSocketChannel();
        mAcceptorConfig = new SocketAcceptorConfig();
        mAcceptorConfig.setReuseAddress(true);
        mAcceptorConfig.setThreadModel(ThreadModel.MANUAL);
        mSocketAcceptor = new SocketAcceptor(NUM_IO_PROCESSORS, IO_THREAD_POOL);
        mStats = new MinaStats(this);
    }

    protected MinaServer(ServerConfig config) throws IOException, ServiceException {
        this(config, Executors.newFixedThreadPool(
            config.getNumThreads(), new MinaThreadFactory(HANDLER_THREAD_NAME)));
    }

    public MinaStats getStats() {
        return mStats;
    }
    
    /**
     * Returns the configuration for this server.
     * 
     * @return the ServerConfig for this server
     */
    public ServerConfig getConfig() {
        return mServerConfig;
    }

    /**
     * Starts the server. Binds the server port and starts the connection
     * handler. Optionally adds an SSLFilter if ssl is enabled.
     * 
     * @throws IOException if an I/O error occured while starting the server
     */
    public void start() throws ServiceException {
        ServerConfig sc = getConfig();
        DefaultIoFilterChainBuilder fc = mAcceptorConfig.getFilterChain();
        if (sc.isSslEnabled()) {
            fc.addFirst("ssl", newSSLFilter());
        }
        fc.addLast("codec", new ProtocolCodecFilter(getProtocolCodecFactory()));
        fc.addLast("executer", new ExecutorFilter(mHandlerThreadPool));
        fc.addLast("logger", new MinaLoggingFilter(this, false));
        IoHandler handler = new MinaIoHandler(this);
        try {
            mSocketAcceptor.register(mChannel, handler, mAcceptorConfig);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to register socket acceptor", e);
        }
        getLog().info("Starting listener on %s%s",
                      mChannel.socket().getLocalSocketAddress(),
                      sc.isSslEnabled() ? " (SSL)" : "");
    }

    /**
     * Shuts down the server. Waits up to 'graceSecs' seconds for the server
     * to stop, otherwise the server is forced to shut down.
     *
     * @param graceSecs number of seconds to wait before forced shutdown
     */
    public void stop(int graceSecs) {
        getLog().info("Initiating shutdown");
        // Would prefer to unbind first then cleanly close active connections,
        // but mina unbind seems to automatically close the active sessions
        // so we must close connections then unbind, which does expose us to
        // a potential race condition.
        long start = System.currentTimeMillis();
        long graceMSecs = graceSecs * 1000;
        // Close active sessions and handlers
        closeSessions(graceMSecs);
        // Unbind listener socket
        mSocketAcceptor.unbind(getSocketAddress());
        // Shutdown protocol handler threads
        mHandlerThreadPool.shutdown();
        // Wait remaining grace period for handlers to cleanly terminate
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > graceMSecs) {
            try {
                mHandlerThreadPool.awaitTermination(
                    graceMSecs - elapsed, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Fall through
            }
        }
        // Force shutdown after grace period has expired
        mHandlerThreadPool.shutdownNow();
    }

    public void stop() {
        stop(getConfig().getShutdownGraceSeconds());
    }

    private void closeSessions(long timeout) {
        // Close currently open sessions and get active handlers
        List<MinaHandler> handlers = new ArrayList<MinaHandler>();
        for (IoSession session : getSessions()) {
            getLog().info("Closing session = " + session);
            MinaHandler handler = MinaIoHandler.getMinaHandler(session);
            if (handler != null) handlers.add(handler);
        }
        // Wait up to grace seconds to close active handlers
        long start = System.currentTimeMillis();
        for (MinaHandler handler : handlers) {
            long elapsed = System.currentTimeMillis() - start;
            try {
                // Force close if timeout - elapsed < 0
                handler.dropConnection(timeout - elapsed);
            } catch (IOException e) {
                getLog().warn("Error closing handler: %s", handler, e);
            }
        }
    }

    public Set<IoSession> getSessions() {
        return mSocketAcceptor.getManagedSessions(getSocketAddress());
    }

    private SocketAddress getSocketAddress() {
        return mChannel.socket().getLocalSocketAddress();
    }

    /**
     * Creates a new handler for handling requests received by the
     * specified MINA session (connection).
     *
     * @param session the I/O session (connection) being opened
     * @return the MinaHandler for handling requests
     */
    protected abstract MinaHandler createHandler(MinaSession session);

    protected abstract ProtocolCodecFactory getProtocolCodecFactory();
    
    protected void registerMinaStatsMBean(String type) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(getStats(), new ObjectName("ZimbraCollaborationSuite:type=" + type));
        } catch (Exception e) {
            getLog().warn("Unable to register MinaStats mbean", e);
        }
    }

    public Log getLog() {
        return getConfig().getLog();
    }
}
