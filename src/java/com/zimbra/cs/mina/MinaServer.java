/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.service.ServiceException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoSession;

/**
 * Base class for MINA-baqsed server implementations.
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

    public static boolean isEnabled() {
        return LC.nio_enabled.booleanValue() ||
              Boolean.getBoolean(NIO_ENABLED_PROP);
    }
    
    public static synchronized SSLContext getSSLContext() {
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

    protected MinaServer(ServerConfig config)
            throws IOException, ServiceException {
        mChannel = config.getServerSocketChannel();
        mAcceptorConfig = new SocketAcceptorConfig();
        mAcceptorConfig.setReuseAddress(true);
        mExecutorService = Executors.newCachedThreadPool();
        mSocketAcceptor = new SocketAcceptor();
        mServerConfig = config;
    }

    public ServerConfig getConfig() {
        return mServerConfig;
    }
    
    public void start() throws IOException {
        ServerConfig sc = getConfig();
        DefaultIoFilterChainBuilder fc = mAcceptorConfig.getFilterChain();
        if (sc.isSSLEnabled()) {
            fc.addFirst("ssl", new SSLFilter(getSSLContext()));
        }
        fc.addLast("codec", new ProtocolCodecFilter(new MinaCodecFactory(this)));
        fc.addLast("executer", new ExecutorFilter(mExecutorService));
        fc.addLast("logger", new LoggingFilter());
        IoHandler handler = new MinaIoHandler(this);
        mSocketAcceptor.register(mChannel, handler, mAcceptorConfig);
        getLog().info("Starting MINA server (addr = %s, port = %d, ssl = %b)",
                      sc.getBindAddress(), sc.getBindPort(), sc.isSSLEnabled());
    }

    protected abstract MinaHandler createHandler(IoSession session);

    protected abstract MinaRequest createRequest(MinaHandler handler);

    public abstract Log getLog();

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
}
