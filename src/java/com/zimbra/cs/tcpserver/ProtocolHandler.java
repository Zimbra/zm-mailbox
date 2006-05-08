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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.util.Zimbra;

import EDU.oswego.cs.dl.util.concurrent.ClockDaemon;

/**
 * @author jhahm
 *
 * Protocol-agnostic abstract base class for handling each client connection
 * to TcpServer.  Each derived class of this class implements a specific
 * protocol, for example LMTP.
 */
public abstract class ProtocolHandler implements Runnable {

	/**
	 * Performs any necessary setup steps upon connection from client.
     * @return true if connection was accepted; false if refused
	 * @throws IOException
	 */
	protected abstract boolean setupConnection(Socket connection) throws IOException;

	/**
	 * Authenticates the client.
	 * @return true if authenticated, false if not authenticated
	 * @throws IOException
	 */
	protected abstract boolean authenticate() throws IOException;

	/**
	 * Reads and processes one command sent by client.
	 * @return true if expecting more commands, false if QUIT command was
	 *         received and server disconnected the connection
	 * @throws Exception
	 */
	protected abstract boolean processCommand() throws Exception;

	/**
	 * Closes any input/output streams with the client.  May get called
	 * multiple times, even after connection was refused.
	 */
	protected abstract void dropConnection();

	/**
	 * Called when a connection has been idle for too long.  Sends
	 * protocol-specific message to client notifying it that the
	 * connection is being dropped due to idle timeout.
	 */
	protected abstract void notifyIdleConnection();

    /**
     * Gets the watchdog task. The default watchdog task periodically checks if any
     * progress has been made processing the connection. If the connection is found
     * to be idle, the server closes that connection.
     * 
     * Subclass can override this method to provide a watchdog task that functions differently.
     * @return
     */
	protected Runnable getWatchdogTask() {
	    return new WatchdogTask();
    }
    
	/*
	 * Watchdog to kill idle connections.
	 */
	private static ClockDaemon mWatchdogDaemon;
	private Object mWatchdogTask;
	static {
		mWatchdogDaemon = new ClockDaemon();
		mWatchdogDaemon.setThreadFactory(new TcpThreadFactory("TcpWatchdog", true));
	}
    private boolean mMadeProgress = true;
    private final Object mMadeProgressGuard = new Object();
	
	protected class WatchdogTask implements Runnable {
		public void run() {
            boolean idle;
			if (getMadeProgress() == false && getIdle() == true) {
				notifyIdleConnection();
				mIdleClosed = true;
				hardShutdown("Closing idle connection");
			} else {
				setMadeProgress(false); // and we'll check again later if you did anything
			}
		}
	}

    protected boolean getMadeProgress() {
    	synchronized (mMadeProgressGuard) {
    		return mMadeProgress;
        }
    }

    private void setMadeProgress(boolean b) {
    	synchronized (mMadeProgressGuard) {
    		mMadeProgress = b;
        }
    }


	private static Log mLog = LogFactory.getLog(ProtocolHandler.class);

    protected Socket mConnection;
    private boolean mIdle;
    private final Object mIdleGuard = new Object();
    private boolean mIdleClosed;
    private boolean mShuttingDown;
    private final Object mShuttingDownGuard = new Object();
	private TcpServer mServer;
	private Thread mHandlerThread;

	public ProtocolHandler(TcpServer server) {
		mServer = server;
        mIdle = true;
        mShuttingDown = false;
    }

    protected boolean getIdle() {
    	synchronized (mIdleGuard) {
    		return mIdle;
        }
    }

	protected void setIdle(boolean idle) {
        synchronized (mIdleGuard) {
        	mIdle = idle;
        }
	}
    
    private boolean getShuttingDown() {
    	synchronized (mShuttingDownGuard) {
    		return mShuttingDown;
        }
    }

    private void setShuttingDown(boolean b) {
    	synchronized (mShuttingDownGuard) {
    		mShuttingDown = b;
        }
    }

	void setConnection(Socket connection) {
		mConnection = connection;
		mIdleClosed = false;
	}

	public void run() {
		// This asserts any reuse of a ProtocolHandler object.  Once we close a connection
		// we also set it to null, and since there is no setConnection, you can't meaningfully
		// reuse an ProtocolHandler object and if you do we give you this ISE.
		if (mConnection == null) throw new IllegalStateException("Connection can not be null when running ProtocolHandler");
		String remoteAddress = mConnection.getInetAddress().getHostAddress();

		mHandlerThread = Thread.currentThread();
		mServer.addActiveHandler(this);
		try {
			if (setupConnection(mConnection)) {
    			if (authenticate())
    				processConnection();
    			else
    				mLog.info("Authentication failed for client " + remoteAddress);
            } else {
                mLog.info("Connection refused for client " + remoteAddress);
            }
        } catch (Error e) {
            Zimbra.halt("Fatal error occurred while handling connection", e);
		} catch (Throwable e) {
			mLog.info("Exception occurred while handling connection", e);
		} finally {
			cancelWatchdog();
			dropConnection();
			try {
				mConnection.close();
			} catch (IOException ioe) {
				mLog.info("Exception while closing connection", ioe);
			}
		}
		mServer.removeActiveHandler(this);
		mHandlerThread = null;
        mLog.info("Handler exiting normally");
	}

	private void processConnection() throws Exception {
		int idleTimeoutMS = mServer.getConfigMaxIdleMilliSeconds();
		if (idleTimeoutMS > 0)
			mWatchdogTask = mWatchdogDaemon.executePeriodically(idleTimeoutMS, getWatchdogTask(), true);
		boolean cont = true;
		while (cont) {
            if (getShuttingDown())
                break;
			try {
				cont = processCommand();
				setIdle(true);
				setMadeProgress(true);
			} catch (IOException e) {
                if (e instanceof SocketException || e instanceof AsynchronousCloseException ) {
                    // If we get a network exception on a connection that was closed
                    // by watchdog due to idleness, treat it as if we got a QUIT
                    // command and closed the connection normally.
                    cont = false;
                    if (mIdleClosed) {
                        mLog.debug("Idle timeout: " + e);
                    } else if (getShuttingDown()) {
                        // Don't print stack trace with info/error level if socket
                        // was closed due to shutdown.
                        mLog.debug("Shutdown in progress", e);
                    } else {
                        throw e;
                    }
                } else {
                    // propagate other types of I/O exceptions
                    throw e;
                }
			}
		}
	}

	private void cancelWatchdog() {
		if (mWatchdogTask != null) {
			ClockDaemon.cancel(mWatchdogTask);
			mWatchdogTask = null;
		}
	}

    void gracefulShutdown(String reason) {
        setShuttingDown(true);
        if (reason != null)
            mLog.info(reason);
        synchronized (mIdleGuard) {
            // If handler is idle, it must be blocked on socket read of
            // next command and can be safely interrupted.
            // If handler is not idle, it is in the middle of processing
            // a command.  At the end of the command the handler thread
            // will exit normally because of setShuttingDown(true) call
            // above.
        	if (mIdle)
                hardShutdown(null);
        }
    }

	void hardShutdown(String reason) {
        setShuttingDown(true);
        if (reason != null)
    		mLog.info(reason);
		if (!mConnection.isClosed()) {
			dropConnection();
			try {
				mConnection.close();
			} catch (IOException ioe) {
				mLog.info("Exception while closing connection", ioe);
			}
		}
		if (mHandlerThread != null) {
			mHandlerThread.interrupt();
		}
	}
}
