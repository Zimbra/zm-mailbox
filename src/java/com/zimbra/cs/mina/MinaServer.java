/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.mina;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.security.sasl.SaslFilter;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.sasl.SaslServer;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.security.KeyStore;
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
    protected final ExecutorService mExecutorService;
    protected final SocketAcceptor mSocketAcceptor;
    protected final ServerConfig mServerConfig;

    private static SSLContext sslContext;

    // TODO Disable for production
    public static final String NIO_ENABLED_PROP = "ZimbraNioEnabled";
    public static final String NIO_DEBUG_ENABLED_PROP = "ZimbraNioDebugEnabled";

    public static boolean isEnabled() {
        return LC.nio_enabled.booleanValue() ||
              Boolean.getBoolean(NIO_ENABLED_PROP);
    }

    public static boolean isDebugEnabled() {
        return LC.nio_debug_enabled.booleanValue() ||
            Boolean.getBoolean(NIO_DEBUG_ENABLED_PROP);
    }

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
     * Creates a new server for the specified configuration.
     * 
     * @param config the ServerConfig for the server
     * @throws IOException if an I/O error occurred while creating the server
     * @throws ServiceException if a ServiceException occured
     */
    protected MinaServer(ServerConfig config) throws IOException, ServiceException {
        mChannel = config.getServerSocketChannel();
        mAcceptorConfig = new SocketAcceptorConfig();
        mAcceptorConfig.setReuseAddress(true);
        mExecutorService = Executors.newCachedThreadPool();
        mSocketAcceptor = new SocketAcceptor();
        mServerConfig = config;
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
    public void start() throws IOException {
        ServerConfig sc = getConfig();
        DefaultIoFilterChainBuilder fc = mAcceptorConfig.getFilterChain();
        if (sc.isSSLEnabled()) {
            fc.addFirst("ssl", new SSLFilter(getSSLContext()));
        }
        fc.addLast("codec", new ProtocolCodecFilter(new MinaCodecFactory(this)));
        fc.addLast("executer", new ExecutorFilter(mExecutorService));
        if (isDebugEnabled()) fc.addLast("logger", new MinaLoggingFilter(this, false));
        IoHandler handler = new MinaIoHandler(this);
        mSocketAcceptor.register(mChannel, handler, mAcceptorConfig);
        getLog().info("Starting listener on %s%s",
                      mChannel.socket().getLocalSocketAddress(),
                      sc.isSSLEnabled() ? " (SSL)" : "");
    }

    /**
     * Shuts down the server. Waits up to 'graceSecs' seconds for the server
     * to stop, otherwise the server is forced to shut down.
     *
     * @param graceSecs number of seconds to wait before forced shutdown
     */
    public void shutdown(int graceSecs) {
        mSocketAcceptor.unbindAll();
        mExecutorService.shutdown();
        try {
            mExecutorService.awaitTermination(graceSecs, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Fall through
        }
        mExecutorService.shutdownNow();
    }

    /**
     * Starts TLS handshake for the specified I/O session. Disables encryption
     * once so that notification can be sent to the client to start the
     * handshake.
     *
     * @param session the IoSession on which to start the TLS handshake
     */
    public static void startTLS(IoSession session) {
        SSLFilter filter = new SSLFilter(getSSLContext());
        session.getFilterChain().addFirst("ssl", filter);
        session.setAttribute(SSLFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public static void addSaslFilter(IoSession session, SaslServer server) {
        SaslFilter filter = new SaslFilter(server);
        session.getFilterChain().addFirst("sasl", filter);
        session.setAttribute(SaslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }
    
    /**
     * Creates a new handler for handling requests received by the
     * specified MINA session (connection).
     *
     * @param session the I/O session (connection) being opened
     * @return the MinaHandler for handling requests
     */
    protected abstract MinaHandler createHandler(IoSession session);

    /**
     * Creates a new MinaRequest for parsing a new request received by the
     * specified handler.
     *
     * @param handler the MinaHandler receiving the request
     * @return the new MinaRequest
     */
    protected abstract MinaRequest createRequest(MinaHandler handler);

    /**
     * Returns the logger for server log messages.
     *
     * @return the Zimbra Log for this server
     */
    public abstract Log getLog();
}
