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

/*
 * Created on 2004. 10. 21.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.tcpserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

    public TcpServer(String name, int numThreads, ServerSocket serverSocket) {
    	    this(name, numThreads, Thread.NORM_PRIORITY, serverSocket);
    }

    public TcpServer(String name, int numThreads, int threadPriority, ServerSocket serverSocket) {
		mName = name;
        mServerSocket = serverSocket;
		mLog = LogFactory.getLog(TcpServer.class.getName() + "/" + serverSocket.getLocalPort()); 
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
