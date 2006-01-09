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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ozserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

// TODO idle connection support and drop unauthenticated connections
//      in sooner

// TODO runWhenBlocked is bad for tasks scheduled from
//      server thread because this will block server
//      and stop it from reaping connections etc - because
//      of tasks that execute in server thread.  Revisit this.

// TODO add a clearConnection so that protocol handlers will
//      let go of the connection objects

public class OzServer {
    
    private Log mLog;

    private Selector mSelector;
    
    private ServerSocket mServerSocket;
    
    private ServerSocketChannel mServerSocketChannel;
    
    private String mServerName;
    
    private Thread mServerThread;
    
    private OzConnectionHandlerFactory mConnectionHandlerFactory;
    
    private int mReadBufferSize;
    
    private SSLContext mSSLContext;

    public OzServer(String name, int readBufferSize, ServerSocket serverSocket,
                    OzConnectionHandlerFactory connectionHandlerFactory, Log log)
        throws IOException
    {
        mLog = log;

        mServerSocket = serverSocket;
        mServerSocketChannel = serverSocket.getChannel();
        mServerSocketChannel.configureBlocking(false);

        mServerName = name + "-" + mServerSocket.getLocalPort();
        mReadBufferSize = readBufferSize;
        
        mConnectionHandlerFactory = connectionHandlerFactory;
        
        mSelector = Selector.open();
        mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        
        /* TODO revisit these thread pool defaults; also make them
         * configurable. */
        mPooledExecutor = new PooledExecutor(new BoundedLinkedQueue(1024));
        mPooledExecutor.setMaximumPoolSize(50);
        mPooledExecutor.setMinimumPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        mPooledExecutor.runWhenBlocked();
        mPooledExecutor.setThreadFactory(new OzThreadFactory());
    }

    OzConnectionHandler newConnectionHandler(OzConnection connection) {
        return mConnectionHandlerFactory.newConnectionHandler(connection);
    }
    

    private Object mSelectorGuard = new Object();

    void wakeupSelector() {
        synchronized (mSelectorGuard) {
            mSelector.wakeup();
        }
    }


    private void serverLoop() {
        while (true) {
            synchronized (this) {
                if (mShutdownRequested) {
                    break;
                }
            }

            int readyCount = 0;

            try {
                if (mLog.isDebugEnabled()) mLog.debug("entering select");
                readyCount = mSelector.select();
                synchronized (mSelectorGuard) { }
            } catch (IOException ioe) {
                mLog.warn("OzServer IOException in select", ioe);
            }

            if (mLog.isDebugEnabled()) mLog.debug("selected " + readyCount);

            if (readyCount == 0) {
                continue;
            }

            Iterator<SelectionKey> iter = mSelector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey readyKey = iter.next();
                iter.remove();

                OzConnection readyConnection = null; 
                try {
                    if (readyKey.attachment() != null && readyKey.attachment() instanceof OzConnection) {
                        readyConnection = (OzConnection)readyKey.attachment();
                        readyConnection.addToNDC();
                    }
                    
                    if (!readyKey.isValid()) {
                        continue;
                    }

                    synchronized (readyKey) {
                        OzUtil.logKey(mLog, readyKey, "ready key");
                    }
                    
                    if (readyKey.isAcceptable()) {
                        Socket newSocket = mServerSocket.accept();
                        SocketChannel newChannel = newSocket.getChannel(); 
                        newChannel.configureBlocking(false);
                        readyConnection= new OzConnection(OzServer.this, newChannel);
                    }
                    
                    if (readyKey.isReadable()) {
                        readyConnection.doReadReady();
                    }
                    
                    if (readyKey.isWritable()) {
                        readyConnection.doWriteReady();
                    }
                } catch (Throwable t) {
                    mLog.warn("ignoring exception that occurred while handling selected key", t);
                    if (readyConnection != null) {
                        readyConnection.channelClose();
                    }
                } finally {
                    if (readyConnection != null) {
                        readyConnection.clearFromNDC();
                    }
                }
                
            } /* end of ready keys loop */

            if (mLog.isDebugEnabled()) mLog.debug("processed " + readyCount + " ready keys");
        }
        
        assert(mShutdownRequested);

        mLog.info("shutting down thread pool");
        mPooledExecutor.shutdownNow();
        try {
            mLog.info("waiting for thread pool to shutdown");
            mPooledExecutor.awaitTerminationAfterShutdown(10*1000);
        } catch (InterruptedException ie) {
            mLog.warn("unexpected exception when waiting for shutdown");
        }
        mLog.info("done waiting for thread pool to shutdown");

        mLog.info("closing all selection keys");
        Set keys = mSelector.keys();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            SelectionKey key = (SelectionKey) iter.next();
            try {
                key.channel().close();
            } catch (IOException ioe) {
                mLog.info("exception closing selection key", ioe);
            }
        }

        try {
            mSelector.close();
        } catch (IOException ioe) {
            mLog.warn("unexpected exception when closing selector");
        }
        mLog.info("closed selector");

        mLog.info("initiating buffer pool destroy");

        synchronized (mShutdownCompleteCondition) {
            mShutdownComplete = true;
            mShutdownCompleteCondition.notify();
        }
    }
    
    private boolean mShutdownRequested;
    
    private boolean mShutdownComplete;
    
    private Object mShutdownCompleteCondition = new Object();
    
    public void shutdown() {
        if (mLog.isDebugEnabled()) mLog.debug("server shutdown requested");
        synchronized (this) {
            mShutdownRequested = true;
        }

        wakeupSelector();
        
        synchronized (mShutdownCompleteCondition) {
            while (!mShutdownComplete) {
                try {
                    mShutdownCompleteCondition.wait();
                } catch (InterruptedException ie) {
                    mLog.warn("exception occurred while waiting for shutdown", ie);
                }
            }
        }
    }
    
    public void start() {
        mServerThread = new Thread() {
            public void run() {
                try {
                    serverLoop();
                } catch (Throwable t) {
                    shutdown();
                }
            }
        };        
        mServerThread.setName(mServerName + "-Server");
        mServerThread.start();
    }

    private PooledExecutor mPooledExecutor;

    void execute(Runnable task) {
        try {
            mPooledExecutor.execute(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void setPoolThreadsMax(int size) {
        mPooledExecutor.setMaximumPoolSize(size);
    }

    public int getPoolThreadsMax() {
        return mPooledExecutor.getMaximumPoolSize();
    }

    private int mPoolThreadsPriority = Thread.NORM_PRIORITY;
    
    public void setPoolThreadsPriority(int priority) {
        mPoolThreadsPriority = priority;
    }
    
    public int getPoolThreadsPriority() {
        return mPoolThreadsPriority;
    }
    
    private boolean mPoolThreadsAreDaemon = true;
    
    public void setPoolThreadsAreDaemon(boolean areDaemon) {
        mPoolThreadsAreDaemon = areDaemon;
    }
    
    public boolean getPoolThreadsAreDaemon() {
        return mPoolThreadsAreDaemon;
    }
    
    private class OzThreadFactory implements ThreadFactory {
        private int mCount = 0;

        public Thread newThread(Runnable runnable) {
            int n;
            synchronized (this) {
                n = ++mCount;
            }
            StringBuffer sb = new StringBuffer(mServerName);
            sb.append("-W").append(n);
            Thread thread = new Thread(runnable, sb.toString());
            thread.setDaemon(mPoolThreadsAreDaemon);
            thread.setPriority(mPoolThreadsPriority);
            return thread;
        }
    }
    
    Selector getSelector() {
        return mSelector;
    }

    int getReadBufferSize() {
        return mReadBufferSize;
    }
   
    Log getLog() {
        return mLog;
    }

    private Map mProperties = new HashMap();
    
    public String getProperty(String key, String defaultValue) {
        synchronized (mProperties) {
            String result = (String)mProperties.get(key);
            if (result == null) {
                result = defaultValue;
            }
            return result;
        }
    }
    
    public void setProperty(String key, String value) {
        synchronized (mProperties) {
            mProperties.put(key, value);
        }
    }
}


