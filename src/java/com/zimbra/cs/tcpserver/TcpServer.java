/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.tcpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.Zimbra;

public abstract class TcpServer implements Runnable, Server {
    private final Log log;
    private final ThreadPoolExecutor pooledExecutor;
    private final String name;
    private final ServerSocket serverSocket;
    private final List<ProtocolHandler> activeHandlers;
    private boolean sslEnabled;
    private ServerConfig config;
    private volatile boolean shutdownRequested;

    public TcpServer(String name, ServerConfig config) throws ServiceException {
        this(name, config.getNumThreads(), config.getServerSocket());
        this.config = config;
        this.sslEnabled = config.isSslEnabled();
    }

    public TcpServer(String name, int numThreads, ServerSocket serverSocket) {
        this(name, numThreads, Thread.NORM_PRIORITY, serverSocket);
    }

    public TcpServer(String name, int numThreads, int threadPriority, ServerSocket serverSocket) {
        this.name = name;
        this.serverSocket = serverSocket;
        this.log = LogFactory.getLog(TcpServer.class.getName() + "/" + serverSocket.getLocalPort());

        if (numThreads <= 0) {
            log.warn("number of handler threads " + numThreads + " invalid; will use 10 threads instead");
            numThreads = 10;
        }

        // Core pool size is 1, to limit the number of idle threads in thread dumps.
        // Idle threads are aged out of the pool after 2 minutes.
        this.pooledExecutor = new ThreadPoolExecutor(1, numThreads, 2, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(), new TcpThreadFactory(this.name, false, threadPriority));

        // TODO a linked list is probably the wrong datastructure here
        // TODO write tests with multiple concurrent client
        // TODO write some tests for shutdown/startup
        this.activeHandlers = new LinkedList<ProtocolHandler>();
    }

    @Override
    public ServerConfig getConfig() {
        return config;
    }

    public int getConfigMaxIdleMilliSeconds() {
        if (config != null) {
            int secs = config.getMaxIdleSeconds();
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
        Thread t = new Thread(this);
        if (name != null) {
            t.setName(name);
        }
        t.start();
    }

    @Override
    public void stop() {
        stop(config.getShutdownGraceSeconds());
    }

    @Override
    public void stop(int forceShutdownAfterSeconds) {
        log.info(name + " initiating shutdown");
        shutdownRequested = true;

        try {
            serverSocket.close();
            Thread.yield();
        } catch (IOException ioe) {
            log.warn(name + " error closing server socket", ioe);
        }

        pooledExecutor.shutdown();

        shutdownActiveHandlers(true);

        if (numActiveHandlers() == 0) {
            log.info(name + " shutting down idle thread pool");
            pooledExecutor.shutdownNow();
            return;
        }

        log.info(name + " waiting " + forceShutdownAfterSeconds + " seconds for thread pool shutdown");
        try {
            pooledExecutor.awaitTermination(forceShutdownAfterSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            log.warn(name + " interrupted while waiting for graceful shutdown", ie);
        }

        if (numActiveHandlers() == 0) {
            log.info(name + " thread pool terminated");
            return;
        }

        shutdownActiveHandlers(false);

        log.info(name + " shutdown complete");
    }

    @Override
    public void run() {
        Thread.currentThread().setName(name);

        log.info("Starting accept loop: %d core threads, %d max threads.",
            pooledExecutor.getCorePoolSize(), pooledExecutor.getMaximumPoolSize());

        try {
            while (!shutdownRequested) {
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
            }
        } catch (IOException ioe) {
            if (!shutdownRequested) {
                Zimbra.halt("accept loop failed", ioe);
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
}
