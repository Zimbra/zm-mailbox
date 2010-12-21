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
    private final Log mLog;
    private final ThreadPoolExecutor mPooledExecutor;
    private final String mName;
    private final ServerSocket mServerSocket;
    private final List<ProtocolHandler> mActiveHandlers;
    private boolean mSSLEnabled;
    private ServerConfig mConfig;
    private volatile boolean mShutdownRequested;

    public TcpServer(String name, ServerConfig config) throws ServiceException {
        this(name, config.getNumThreads(), config.getServerSocket());
        mConfig = config;
    }

    public TcpServer(String name, int numThreads, ServerSocket serverSocket) {
        this(name, numThreads, Thread.NORM_PRIORITY, serverSocket);
    }

    public TcpServer(String name, int numThreads, int threadPriority, ServerSocket serverSocket) {
        mName = name;
        mServerSocket = serverSocket;
        mLog = LogFactory.getLog(TcpServer.class.getName() + "/" + serverSocket.getLocalPort());

        if (numThreads <= 0) {
            mLog.warn("number of handler threads " + numThreads + " invalid; will use 10 threads instead");
            numThreads = 10;
        }

        // Core pool size is 1, to limit the number of idle threads in thread dumps.
        // Idle threads are aged out of the pool after 2 minutes.
        mPooledExecutor = new ThreadPoolExecutor(1, numThreads, 2, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(), new TcpThreadFactory(mName, false, threadPriority));

        // TODO a linked list is probably the wrong datastructure here
        // TODO write tests with multiple concurrent client
        // TODO write some tests for shutdown/startup
        mActiveHandlers = new LinkedList<ProtocolHandler>();
    }

    @Override
    public ServerConfig getConfig() {
        return mConfig;
    }

    public int getConfigMaxIdleMilliSeconds() {
        if (mConfig != null) {
            int secs = mConfig.getMaxIdleSeconds();
            if (secs >= 0) {
                return secs * 1000;
            }
        }
        return -1;
    }

    public void setSSLEnabled(boolean ssl) {
        mSSLEnabled = ssl;
    }

    public boolean isSSLEnabled() {
        return mSSLEnabled;
    }

    public void addActiveHandler(ProtocolHandler handler) {
        synchronized (mActiveHandlers) {
            mActiveHandlers.add(handler);
        }
    }

    public void removeActiveHandler(ProtocolHandler handler) {
        synchronized (mActiveHandlers) {
            mActiveHandlers.remove(handler);
        }
    }

    protected int numActiveHandlers() {
        synchronized (mActiveHandlers) {
            return mActiveHandlers.size();
        }
    }
    
    public int numThreads() {
        return mPooledExecutor.getPoolSize();
    }

    private void shutdownActiveHandlers(boolean graceful) {
        synchronized (mActiveHandlers) {
            for (ProtocolHandler handler : mActiveHandlers) {
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
        if (mName != null) {
            t.setName(mName);
        }
        t.start();
    }

    @Override
    public void stop() {
        stop(mConfig.getShutdownGraceSeconds());
    }

    @Override
    public void stop(int forceShutdownAfterSeconds) {
        mLog.info(mName + " initiating shutdown");
        mShutdownRequested = true;

        try {
            mServerSocket.close();
            Thread.yield();
        } catch (IOException ioe) {
            mLog.warn(mName + " error closing server socket", ioe);
        }

        mPooledExecutor.shutdown();

        shutdownActiveHandlers(true);

        if (numActiveHandlers() == 0) {
            mLog.info(mName + " shutting down idle thread pool");
            mPooledExecutor.shutdownNow();
            return;
        }

        mLog.info(mName + " waiting " + forceShutdownAfterSeconds + " seconds for thread pool shutdown");
        try {
            mPooledExecutor.awaitTermination(forceShutdownAfterSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            mLog.warn(mName + " interrupted while waiting for graceful shutdown", ie);
        }

        if (numActiveHandlers() == 0) {
            mLog.info(mName + " thread pool terminated");
            return;
        }

        shutdownActiveHandlers(false);

        mLog.info(mName + " shutdown complete");
    }

    @Override
    public void run() {
        Thread.currentThread().setName(mName);

        mLog.info("Starting accept loop: %d core threads, %d max threads.",
            mPooledExecutor.getCorePoolSize(), mPooledExecutor.getMaximumPoolSize());

        try {
            while (!mShutdownRequested) {
                Socket connection = mServerSocket.accept();
                warnIfNecessary();
                ProtocolHandler handler = newProtocolHandler();
                handler.setConnection(connection);
                try {
                    mPooledExecutor.execute(handler);
                } catch (RejectedExecutionException e) {
                    mLog.error("cannot handle connection; thread pool exhausted", e);
                    try {
                        String message = mConfig.getConnectionRejected();
                        if (message != null) {
                            OutputStream os = connection.getOutputStream();
                            if (os != null) {
                                os.write((message + "\r\n").getBytes());
                                os.flush();
                            }
                        }
                    } catch (Throwable t) {
                        // ignore any errors while dropping unhandled connection
                    }
                    try {
                        connection.close();
                    } catch (Throwable t) {
                        // ignore any errors while dropping unhandled connection
                    }
                }
            }
        } catch (IOException ioe) {
            if (!mShutdownRequested) {
                Zimbra.halt("accept loop failed", ioe);
            }
        }

        mLog.info("finished accept loop");
    }
    
    private void warnIfNecessary() {
        if (mLog.isWarnEnabled()) {
            int warnPercent = LC.thread_pool_warn_percent.intValue();
            // Add 1 because the thread for this connection is not active yet.
            int active = mPooledExecutor.getActiveCount() + 1;
            int max = mPooledExecutor.getMaximumPoolSize();
            int utilization = active * 100 / max; 
            if (utilization >= warnPercent) {
                mLog.warn("Thread pool is %d%% utilized.  %d out of %d threads in use.",
                    utilization, active, max);
            }
        }
    }

    protected abstract ProtocolHandler newProtocolHandler();
}
