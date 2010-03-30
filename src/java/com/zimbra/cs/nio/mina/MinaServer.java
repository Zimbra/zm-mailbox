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
package com.zimbra.cs.nio.mina;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.nio.NioDecoder;
import com.zimbra.cs.nio.NioHandler;
import com.zimbra.cs.nio.NioServer;
import com.zimbra.cs.nio.NioSession;
import com.zimbra.cs.nio.NioThreadFactory;
import com.zimbra.cs.nio.SSLInfo;
import com.zimbra.cs.server.ServerConfig;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
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
public abstract class MinaServer extends NioServer {
    private final ServerSocketChannel channel;
    private final SocketAcceptorConfig acceptorConfig;
    private final SocketAcceptor acceptor;
    private final MinaStats stats;
    private final int writeChunkSize;

    private static final int NUM_IO_PROCESSORS =
        Runtime.getRuntime().availableProcessors() + 1;
    
    // There is one I/O thread pool shared by all protocols
    private static final ExecutorService IO_THREAD_POOL =
        Executors.newCachedThreadPool(new NioThreadFactory("MinaIoProcessorThread"));


    protected MinaServer(ServerConfig config, ExecutorService pool) throws ServiceException {
        super(config, pool);
        channel = config.getServerSocketChannel();
        acceptorConfig = new SocketAcceptorConfig();
        acceptorConfig.setReuseAddress(true);
        acceptorConfig.setThreadModel(ThreadModel.MANUAL);
        acceptor = new SocketAcceptor(NUM_IO_PROCESSORS, IO_THREAD_POOL);
        stats = new MinaStats(this);
        // Save this value in order to avoid possible blocking LDAP request
        // since decoder is created within I/O processor thread.
        writeChunkSize = config.getNioWriteChunkSize();
    }

    public MinaStats getStats() {
        return stats;
    }
    
    /*
     * Starts the server. Binds the server port and starts the connection
     * handler. Optionally adds an SSLFilter if ssl is enabled.
     */
    public void start() throws IOException {
        ServerConfig sc = getConfig();
        DefaultIoFilterChainBuilder fc = acceptorConfig.getFilterChain();
        if (sc.isSslEnabled()) {
            fc.addFirst("ssl", newSSLFilter());
        }
        fc.addLast("codec", new ProtocolCodecFilter(new MinaCodecFactory(this)));
        fc.addLast("executer", new ExecutorFilter(getHandlerPool()));
        fc.addLast("logger", new MinaLoggingFilter(this, false));
        IoHandler handler = new MinaIoHandler(this);
        acceptor.register(channel, handler, acceptorConfig);
        getLog().info("Starting listener on %s%s",
                      channel.socket().getLocalSocketAddress(),
                      sc.isSslEnabled() ? " (SSL)" : "");
    }

    public SSLFilter newSSLFilter() {
        SSLInfo info = getSSLInfo();
        SSLFilter sslFilter = new SSLFilter(info.getSSLContext());
        String[] enabledCiphers = info.getEnabledCipherSuites();
        if (enabledCiphers != null) {
            sslFilter.setEnabledCipherSuites(enabledCiphers);
        }
        return sslFilter;
    }

    /*
     * Shuts down the server. Waits up to 'graceSecs' seconds for the server
     * to stop, otherwise the server is forced to shut down.
     */
    public void stop() {
        int graceSecs = getConfig().getShutdownGracePeriod();
        if (graceSecs <= 0) {
            graceSecs = 10;
        }
        getLog().info("Initiating shutdown");
        // Would prefer to unbind first then cleanly close active connections,
        // but mina unbind seems to automatically close the active sessions.
        // So we must close connections then unbind, which does expose us to
        // a potential race condition.
        long start = System.currentTimeMillis();
        long graceMSecs = graceSecs * 1000;
        // Close active sessions and handlers
        closeSessions(graceMSecs);
        // Unbind listener socket
        acceptor.unbind(getSocketAddress());
        // TODO Fix support for shared handler pool. Unbind all listener sockets
        // first, then wait for pool to terminate.
        ExecutorService pool = getHandlerPool();
        pool.shutdown();
        // Wait remaining grace period for handlers to cleanly terminate
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > graceMSecs) {
            try {
                pool.awaitTermination(graceMSecs - elapsed, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Fall through
            }
        }
        // Force shutdown after grace period has expired
        pool.shutdownNow();
    }

    private void closeSessions(long timeout) {
        // Close currently open sessions and get active handlers
        List<NioHandler> handlers = new ArrayList<NioHandler>();
        for (IoSession session : getSessions()) {
            getLog().info("Closing session = " + session);
            NioHandler handler = MinaIoHandler.nioHandler(session);
            if (handler != null) handlers.add(handler);
        }
        // Wait up to grace seconds to close active handlers
        long start = System.currentTimeMillis();
        for (NioHandler handler : handlers) {
            long elapsed = System.currentTimeMillis() - start;
            try {
                // Force close if timeout - elapsed < 0
                handler.dropConnection(timeout - elapsed);
            } catch (IOException e) {
                getLog().warn("Error closing handler: %s", handler, e);
            }
        }
    }

    public Log getLog() {
        return getConfig().getLog();
    }
    
    public Set<IoSession> getSessions() {
        return acceptor.getManagedSessions(getSocketAddress());
    }

    protected int getWriteChunkSize() {
        return writeChunkSize;
    }
    
    private SocketAddress getSocketAddress() {
        return channel.socket().getLocalSocketAddress();
    }

    protected abstract NioHandler newHandler(NioSession session);
    protected abstract NioDecoder newDecoder();
}