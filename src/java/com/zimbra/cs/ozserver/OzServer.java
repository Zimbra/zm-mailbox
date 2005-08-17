package com.zimbra.cs.ozserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.util.LiquidLog;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

public class OzServer {
    
    private Selector mSelector;
    
    private ServerSocket mServerSocket;
    
    private ServerSocketChannel mServerSocketChannel;
    
    private String mServerName;
    
    private IoThread mIoThread;
    
    private OzProtocolHandlerFactory mProtocolHandlerFactory;
    
    private OzSnooper mSnooper = new OzSnooper(null);
    
    public OzServer(String name, InetAddress bindAddress, int port,
                    OzProtocolHandlerFactory protocolHandlerFactory) 
        throws IOException
    {
        mServerSocketChannel = ServerSocketChannel.open();
        mServerSocketChannel.configureBlocking(false);
        
        mServerSocket = mServerSocketChannel.socket();
        InetSocketAddress address = new InetSocketAddress(bindAddress, port);
        mServerSocket.bind(address);
        
        mServerName = name + "-" + port;
        
        mProtocolHandlerFactory = protocolHandlerFactory;
        
        mSelector = Selector.open();
        mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        
        mPooledExecutor = new PooledExecutor();
        mPooledExecutor.setThreadFactory(new OzThreadFactory());
        mPooledExecutor.waitWhenBlocked(); // TODO - revisit this - can we wait?
        setPoolThreadsMax(Runtime.getRuntime().availableProcessors() * 2);
    }

    public void setSnooper(OzSnooper snooper) {
        mSnooper = snooper;
    }
    
    public OzSnooper getSnooper() {
        return mSnooper;
    }
    
    OzProtocolHandler newProtocolHandler() {
        return mProtocolHandlerFactory.newProtocolHandler();
    }
    
    private class IoThread extends Thread {

        public void run() {

            while (true) {

                if (mShutdownRequested) {
                    break;
                }

                int readyCount = 0;

                try {
                    if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("entering select");
                    readyCount = mSelector.select();
                } catch (IOException ioe) {
                    LiquidLog.ozserver.warn("OzServer IOException in select", ioe);
                }

                if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("selected readyCount=" + readyCount);

                if (readyCount == 0) {
                    continue;
                }
                
                Iterator iter = mSelector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey readyKey = (SelectionKey) iter.next();
                    iter.remove();

                    int readyKeyConnectionId = -1;
                    Object readyKeyAttachment = readyKey.attachment();
                    if (readyKeyAttachment != null && readyKeyAttachment instanceof OzConnectionHandler) {
                        readyKeyConnectionId = ((OzConnectionHandler)readyKeyAttachment).getId();
                    }

                    if (!readyKey.isValid()) {
                        LiquidLog.ozserver.info("ready key cid=" + readyKeyConnectionId + " invalid hence ignored");
                    }
                    
                    if (LiquidLog.ozserver.isDebugEnabled()) {
                    	OzUtil.logSelectionKey(readyKey, readyKeyConnectionId, "ready key");
                    }
                    
                    if (readyKey.isAcceptable()) {
                        if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("acceptable key");
                        OzConnectionHandler connection = null;
                        try {
                            Socket newSocket = mServerSocket.accept();
                            SocketChannel newChannel = newSocket.getChannel(); 
                            newChannel.configureBlocking(false);
                            connection = new OzConnectionHandler(OzServer.this, newChannel);
                            LiquidLog.ozserver.info("connected cid=" + connection.getId() + " " + newSocket);
                        } catch (Exception e) {
                            LiquidLog.ozserver.warn("ignoring exception that occurred while handling acceptable key", e);
                            if (connection != null) {
                                connection.close();
                            }
                        }
                    }
                    
                    if (readyKey.isReadable()) {
                        OzConnectionHandler connection = null;
                        try {
                            connection = (OzConnectionHandler)readyKey.attachment();
                            connection.handleRead();
                        } catch (Exception e) {
                            LiquidLog.ozserver.warn("ignoring exception that occurred while handling readable key", e);
                            connection.close();
                        }
                    }
                    
                    if (readyKey.isWritable()) {
                        OzConnectionHandler connection = null;
                        try {
                            connection = (OzConnectionHandler)readyKey.attachment();
                            connection.handleWrite();
                        } catch (Exception e) {
                            LiquidLog.ozserver.warn("ignoring exception that occurred while handling writable key", e);
                            connection.close();
                        }
                    }
                    
                } /* end of ready keys iteration loop */

                if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("processed " + readyCount + " ready keys");

                synchronized (mIoThreadTasks) {
                    if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("running " + mIoThreadTasks.size() + " IO thread tasks");
                    for (Iterator taskIter = mIoThreadTasks.iterator(); taskIter.hasNext(); taskIter.remove()) {
                        Runnable task = (Runnable) taskIter.next();
                        try {
                            task.run();
                        } catch (Exception e) {
                            LiquidLog.ozserver.warn("ignoring exception that occurred while running IO thread tasks", e);
                        }
                    }
                }

            }

            assert(mShutdownRequested);

            LiquidLog.ozserver.info("shutting down thread pool");
            mPooledExecutor.shutdownNow();

            try {
                LiquidLog.ozserver.info("waiting for thread pool to shutdown");
                mPooledExecutor.awaitTerminationAfterShutdown(10*1000);
            } catch (InterruptedException ie) {
                LiquidLog.ozserver.warn("unexpected exception when waiting for shutdown");
            }
            LiquidLog.ozserver.info("done waiting for thread pool to shutdown");
            
            try {
                mSelector.close();
            } catch (IOException ioe) {
                LiquidLog.ozserver.warn("unexpected exception when closing selector");
            }
            LiquidLog.ozserver.info("closed selector");
        }
    }

    private boolean mShutdownRequested;
    
    public void shutdown() throws IOException {
        mShutdownRequested = true;
        mSelector.wakeup();
    }
    
    public void start() {
        mIoThread = new IoThread();
        mIoThread.setName(mServerName + "-IO");
        mIoThread.start();
    }

    private List mIoThreadTasks = new ArrayList(128); 
    
    void runTaskInIoThread(Runnable task) {
        if (Thread.currentThread() == mIoThread) {
            if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("already in IO thread, just running");
            task.run();
        } else {
            if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("scheduling for IO Thread later");
            synchronized (mIoThreadTasks) {
                mIoThreadTasks.add(task);
            }
        }
    }
    
    private PooledExecutor mPooledExecutor;

    void execute(Runnable task) {
        try {
            mPooledExecutor.execute(task);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
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
}
