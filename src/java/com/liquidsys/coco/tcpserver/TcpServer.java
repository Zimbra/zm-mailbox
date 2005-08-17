/*
 * Created on 2004. 10. 21.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.tcpserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

//import javax.net.ServerSocketFactory;
//import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class TcpServer implements Runnable {

	protected abstract ProtocolHandler newProtocolHandler();
	public abstract int getConfigMaxIdleMilliSeconds();


	private Log mLog;

	private PooledExecutor mPooledExecutor;

	private String mName;
	private InetAddress mBindAddress;
	private int mListenPort;
	private ServerSocket mServerSocket;

	private boolean mShutdownRequested;
    private boolean mSSL; 

    public TcpServer(String name, int numThreads, int port, InetAddress bindAddress) {
    	    this(name, numThreads, Thread.NORM_PRIORITY, port, bindAddress);
    }

    public TcpServer(String name, int numThreads, int threadPriority, int port, InetAddress bindAddress) {
		mName = name;
		mListenPort = port;
		mBindAddress = bindAddress;
		mLog = LogFactory.getLog(TcpServer.class.getName() + "/" + port); 
		mShutdownRequested = false;

		if (numThreads <= 0) {
			mLog.warn("number of handler threads " + numThreads + " invalid; will use 10 threads instead");
			numThreads = 10;
		}

		mPooledExecutor = new PooledExecutor(new BoundedLinkedQueue(), numThreads);
		mPooledExecutor.setMinimumPoolSize(numThreads);
		mPooledExecutor.setThreadFactory(new TcpThreadFactory(mName, false, threadPriority));
		mPooledExecutor.waitWhenBlocked();

		mActiveHandlers = new LinkedList();
        
	}

	// TODO a linked list is probably the wrong datastructure here
	// TODO write tests with multiple concurrent client
	// TODO write some tests for shutdown/startup
	private List mActiveHandlers;

   
    public void setSSL(boolean ssl) {
        mSSL = ssl;
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

	private int numActiveHandlers() {
		synchronized (mActiveHandlers) {
			return mActiveHandlers.size();
		}
	}
	
	private void shutdownActiveHandlers(boolean graceful) {
		synchronized (mActiveHandlers) {
			for (Iterator iter = mActiveHandlers.iterator(); iter.hasNext();) {
				ProtocolHandler handler = (ProtocolHandler)iter.next();
                if (graceful)
                    handler.gracefulShutdown("graceful shutdown requested");
                else
    				handler.hardShutdown("hard shutdown requested");
			}
		}
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

		try {
            if (!mSSL) {
                mServerSocket = new ServerSocket();
            } else {
                SSLServerSocketFactory fact = (SSLServerSocketFactory)
                    SSLServerSocketFactory.getDefault();
                mServerSocket = fact.createServerSocket();//mListenPort);
            }
			mServerSocket.setReuseAddress(true);
			InetSocketAddress isa = new InetSocketAddress(mBindAddress, mListenPort);
			mServerSocket.bind(isa);
		} catch (IOException ioe) {
			mLog.fatal("initialization failed: port=" + mListenPort, ioe);
			return;
		}

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
				// TODO !mShutdownRequested check should be wrapped in a synchronized
				// to guarantee we see the variable change right away.  This may cause
				// bogus "accept loop failed" messages to be logged.
				mLog.fatal("accept loop failed", ioe);
			}
		}
		mLog.info("finished accept loop");
	}
}
