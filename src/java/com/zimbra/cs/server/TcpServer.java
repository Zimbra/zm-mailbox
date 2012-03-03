/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;

/**
 * Base class for TCP servers using thread per connection model.
 */
public abstract class TcpServer implements Runnable, Server {
    private Log log;
    private ThreadPoolExecutor pooledExecutor;
    private ServerSocket serverSocket;
    private List<ProtocolHandler> activeHandlers;
    private boolean sslEnabled;
    private final ServerConfig config;
    private volatile boolean shutdownRequested;

    public TcpServer(ServerConfig config) throws ServiceException {
        this.config = config;
        this.sslEnabled = config.isSslEnabled();
        init(config.getMaxThreads(), config.getServerSocket());
    }

    public TcpServer(int maxThreads, ServerSocket serverSocket) {
        config = null;
        init(maxThreads, serverSocket);
    }

    private void init(int maxThreads, ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        log = LogFactory.getLog(TcpServer.class.getName() + "/" + serverSocket.getLocalPort());

        if (maxThreads <= 0) {
            log.warn("max handler threads " + maxThreads + " invalid; will use 10 threads instead");
            maxThreads = 10;
        }

        // Core pool size is 1, to limit the number of idle threads in thread dumps.
        // Idle threads are aged out of the pool after X minutes.
        int keepAlive = config != null ? config.getThreadKeepAliveTime() : 2 * 60;
        pooledExecutor = new ThreadPoolExecutor(1, maxThreads, keepAlive, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new TcpThreadFactory(getName(), false, Thread.NORM_PRIORITY));

        // TODO a linked list is probably the wrong datastructure here
        // TODO write tests with multiple concurrent client
        // TODO write some tests for shutdown/startup
        activeHandlers = new LinkedList<ProtocolHandler>();
    }

    @Override
    public ServerConfig getConfig() {
        return config;
    }

    public int getConfigMaxIdleMilliSeconds() {
        if (config != null) {
            int secs = config.getMaxIdleTime();
            if (secs >= 0) {
                return secs * 1000;
            }
        }
        return -1;
    }

    protected void setSslEnabled(boolean ssl) {
        sslEnabled = ssl;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void addActiveHandler(ProtocolHandler handler) {
        synchronized (activeHandlers) {
            activeHandlers.add(handler);
        }
    }

    public void removeActiveHandler(ProtocolHandler handler) {
        synchronized (activeHandlers) {
            activeHandlers.remove(handler);
        }
    }

    protected int numActiveHandlers() {
        synchronized (activeHandlers) {
            return activeHandlers.size();
        }
    }

    public int numThreads() {
        return pooledExecutor.getPoolSize();
    }

    private void shutdownActiveHandlers(boolean graceful) {
        synchronized (activeHandlers) {
            for (ProtocolHandler handler : activeHandlers) {
                if (graceful) {
                    handler.gracefulShutdown("graceful shutdown requested");
                } else {
                    handler.hardShutdown("hard shutdown requested");
                }
            }
        }
    }

    @Override
    public void start() {
        Thread thread = new Thread(this);
        thread.setName(getName());
        thread.start();
    }

    @Override
    public void stop() {
        stop(config.getShutdownTimeout());
    }

    @Override
    public void stop(int forceShutdownAfterSeconds) {
        log.info(getName() + " initiating shutdown");
        shutdownRequested = true;

        try {
            serverSocket.close();
            Thread.yield();
        } catch (IOException ioe) {
            log.warn(getName() + " error closing server socket", ioe);
        }

        pooledExecutor.shutdown();

        shutdownActiveHandlers(true);

        if (numActiveHandlers() == 0) {
            log.info(getName() + " shutting down idle thread pool");
            pooledExecutor.shutdownNow();
            return;
        }

        log.info(getName() + " waiting " + forceShutdownAfterSeconds + " seconds for thread pool shutdown");
        try {
            pooledExecutor.awaitTermination(forceShutdownAfterSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            log.warn(getName() + " interrupted while waiting for graceful shutdown", ie);
        }

        if (numActiveHandlers() == 0) {
            log.info(getName() + " thread pool terminated");
            return;
        }

        shutdownActiveHandlers(false);

        log.info(getName() + " shutdown complete");
    }

    @Override
    public void run() {
        Thread.currentThread().setName(getName());

        log.info("Starting accept loop: %d core threads, %d max threads.",
            pooledExecutor.getCorePoolSize(), pooledExecutor.getMaximumPoolSize());

        while (!shutdownRequested) {
            try {
                Socket connection = serverSocket.accept();
                warnIfNecessary();
                ProtocolHandler handler = newProtocolHandler();
                handler.setConnection(connection);
                try {
                    pooledExecutor.execute(handler);
                } catch (RejectedExecutionException e) {
                    log.error("cannot handle connection; thread pool exhausted", e);
                    // send a "server busy" message to the client before dropping connection
                    //   (but skip if client expects an SSL handshake, which we can't do here)
                    if (config != null && !isSslEnabled()) {
                        String message = config.getConnectionRejected();
                        if (message != null) {
                            try {
                                OutputStream os = connection.getOutputStream();
                                if (os != null) {
                                    os.write((message + "\r\n").getBytes());
                                    os.flush();
                                }
                            } catch (Throwable t) {
                                // ignore any errors while notifying unhandled connection
                            }
                        }
                    }
                    try {
                        connection.close();
                    } catch (Throwable t) {
                        // ignore any errors while dropping unhandled connection
                    }
                }
            } catch (Throwable e) {
                if (e instanceof SocketException && shutdownRequested) {
                    break; // ignore SocketException: Socket closed
                }
                log.error("accept loop failed", e);
                try {
                    Thread.sleep(1000); // pause for 1 second
                } catch (InterruptedException ignore) {
                }
            }
        }

        log.info("finished accept loop");
    }

    private void warnIfNecessary() {
        if (log.isWarnEnabled()) {
            int warnPercent = LC.thread_pool_warn_percent.intValue();
            // Add 1 because the thread for this connection is not active yet.
            int active = pooledExecutor.getActiveCount() + 1;
            int max = pooledExecutor.getMaximumPoolSize();
            int utilization = active * 100 / max;
            if (utilization >= warnPercent) {
                log.warn("Thread pool is %d%% utilized.  %d out of %d threads in use.",
                    utilization, active, max);
            }
        }
    }

    protected abstract ProtocolHandler newProtocolHandler();
    
    protected Set<String> getThrottleSafeHosts() throws ServiceException {

        Set<String> safeHosts = new HashSet<String>();
        for (com.zimbra.cs.account.Server server : Provisioning.getInstance().getAllServers()) {
            safeHosts.add(server.getServiceHostname());
        }
        for (String ignoredHost : config.getIgnoredHosts()) {
            safeHosts.add(ignoredHost);
        }
        return safeHosts;
    }

}
