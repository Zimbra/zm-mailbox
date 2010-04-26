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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.Zimbra;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public abstract class TcpServer implements Runnable, Server {
    private final Log mLog;
    private final PooledExecutor mPooledExecutor;
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

        mPooledExecutor = new PooledExecutor(new BoundedLinkedQueue(), numThreads);
        mPooledExecutor.setMinimumPoolSize(numThreads);
        mPooledExecutor.setThreadFactory(new TcpThreadFactory(mName, false, threadPriority));
        mPooledExecutor.waitWhenBlocked();

        // TODO a linked list is probably the wrong datastructure here
        // TODO write tests with multiple concurrent client
        // TODO write some tests for shutdown/startup
        mActiveHandlers = new LinkedList<ProtocolHandler>();
    }

    public ServerConfig getConfig() {
        return mConfig;
    }

    public int getConfigMaxIdleMilliSeconds() {
        if (mConfig != null) {
            int secs = mConfig.getMaxIdleSeconds();
            if (secs >= 0) return secs * 1000;
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

    public void start() {
        Thread t = new Thread(this);
        if (mName != null) t.setName(mName);
        t.start();
    }

    public void shutdown() {
        shutdown(mConfig.getShutdownGraceSeconds());
    }
    
    public void shutdown(int forceShutdownAfterSeconds) {
        mLog.info(mName + " initiating shutdown");
        mShutdownRequested = true;

        try {
            mServerSocket.close();
            Thread.yield();
        } catch (IOException ioe) {
            mLog.warn(mName + " error closing server socket", ioe);
        }

        mPooledExecutor.shutdownAfterProcessingCurrentlyQueuedTasks();

        shutdownActiveHandlers(true);

        if (numActiveHandlers() == 0) {
            mLog.info(mName + " shutting down idle thread pool");
            mPooledExecutor.shutdownNow();
            return;
        }

        mLog.info(mName + " waiting " + forceShutdownAfterSeconds + " seconds for thread pool shutdown");
        try {
            mPooledExecutor.awaitTerminationAfterShutdown(forceShutdownAfterSeconds * 1000);
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

    public void run() {
        Thread.currentThread().setName(mName);

        mLog.info("starting accept loop");
        try {
            while (!mShutdownRequested) {
                Socket connection = mServerSocket.accept();
                ProtocolHandler handler = newProtocolHandler();
                handler.setConnection(connection);
                try {
                    mPooledExecutor.execute(handler);
                } catch (InterruptedException ie) {
                    mLog.warn("handler thread pool execution request interrupted", ie);
                }
            }
        } catch (IOException ioe) {
            if (!mShutdownRequested) {
                Zimbra.halt("accept loop failed", ioe);
            }
        }
        mLog.info("finished accept loop");
    }

    protected abstract ProtocolHandler newProtocolHandler();
}
