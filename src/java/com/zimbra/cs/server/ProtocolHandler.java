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

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.AsynchronousCloseException;

/**
 * Protocol-agnostic abstract base class for handling each client connection
 * to TcpServer.  Each derived class of this class implements a specific
 * protocol, for example LMTP.
 */
public abstract class ProtocolHandler implements Runnable {
    /**
     * Performs any necessary setup steps upon connection from client.
     * @param connection the Socket for the connection
     * @return true if connection was accepted; false if refused
     * @throws IOException if an I/O error occurred
     */
    protected abstract boolean setupConnection(Socket connection) throws IOException;

    /**
     * Authenticates the client.
     * @return true if authenticated, false if not authenticated
     * @throws IOException if an I/O error occurred
     */
    protected abstract boolean authenticate() throws IOException;

    /**
     * Reads and processes one command sent by client.
     * @return true if expecting more commands, false if QUIT command was
     *         received and server disconnected the connection
     * @throws Exception if an error occurred
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

    private final Log log;

    protected Socket mConnection;
    private volatile boolean idle = true;
    private volatile boolean shuttingDown = false;
    private final TcpServer mServer;
    private Thread mHandlerThread;

    public ProtocolHandler(TcpServer server) {
        mServer = server;
        log = server.getConfig().getLog();
    }

    protected void setIdle(boolean value) {
        idle = value;
    }

    private boolean getShuttingDown() {
        return shuttingDown;
    }

    private void setShuttingDown(boolean value) {
        shuttingDown = value;
    }

    void setConnection(Socket connection) {
        mConnection = connection;
    }

    @Override
    public void run() {
        // This asserts any reuse of a ProtocolHandler object.  Once we close a connection
        // we also set it to null, and since there is no setConnection, you can't meaningfully
        // reuse an ProtocolHandler object and if you do we give you this ISE.
        if (mConnection == null)
            throw new IllegalStateException("Connection can not be null when running ProtocolHandler");
        String remoteAddress = mConnection.getInetAddress().getHostAddress();

        mHandlerThread = Thread.currentThread();
        mServer.addActiveHandler(this);

        ZimbraLog.clearContext();

        try {
            if (mConnection instanceof SSLSocket) {
                startHandshake((SSLSocket) mConnection);
            }

            if (setupConnection(mConnection)) {
                if (authenticate()) {
                    processConnection();
                } else {
                    log.info("Authentication failed for client " + remoteAddress);
                }
            } else {
                log.info("Connection refused for client " + remoteAddress);
            }
        } catch (SocketTimeoutException e) {
            ZimbraLog.addIpToContext(remoteAddress);
            log.debug("Idle timeout: " + e);
            notifyIdleConnection();
        } catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
        } catch (Error e) {
            Zimbra.halt("Fatal error occurred while handling connection", e);
        } catch (Throwable e) {
            ZimbraLog.addIpToContext(remoteAddress);
            log.info("Exception occurred while handling connection", e);
        } finally {
            dropConnection();
            ZimbraLog.addIpToContext(remoteAddress);
            try {
                mConnection.close();
            } catch (IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.info("I/O error while closing connection", ioe);
                } else {
                    log.info("I/O error while closing connection: " + ioe);
                }
            } finally {
                ZimbraLog.clearContext();
            }
        }

        ZimbraLog.clearContext();
        mServer.removeActiveHandler(this);
        mHandlerThread = null;
        log.info("Handler exiting normally");
    }

    /*
     * Starts handshake on specified SSL socket. Also adds handshake listener
     * so that connection will be automatically closed upon SSL renegotiation
     * attempt (see bug 42857).
     */
    protected void startHandshake(final SSLSocket sock) throws IOException {
        sock.startHandshake();
        sock.addHandshakeCompletedListener(new HandshakeCompletedListener() {
            @Override
            public void handshakeCompleted(HandshakeCompletedEvent event) {
                hardShutdown("SSL renegotiation denied: " + sock);
            }
        });
    }

    private void processConnection() throws Exception {
        boolean cont = true;
        while (cont && !getShuttingDown()) {
            try {
                cont = processCommand();
                setIdle(true);
            } catch (IOException e) {
                ZimbraLog.addIpToContext(mConnection.getInetAddress().getHostAddress());
                if (isSocketError(e)) {
                    cont = false;
                    if (getShuttingDown()) {
                        // Unless debug level enabled, don't log connection
                        // error if error occurs while socket is being closed
                        log.debug("Shutdown in progress", e);
                    } else if (log.isDebugEnabled()) {
                        // For other connection errors, only include full stack
                        // trace if debug level enabled.
                        log.info("I/O error while processing connection", e);
                    } else {
                        log.info("I/O error while processing connection: " + e);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private static boolean isSocketError(IOException e) {
        return e instanceof SocketException || e instanceof SSLException ||
               e instanceof AsynchronousCloseException;
    }

    void gracefulShutdown(String reason) {
        setShuttingDown(true);
        if (reason != null) {
            log.info(reason);
        }
        // If handler is idle, it must be blocked on socket read of
        // next command and can be safely interrupted.
        // If handler is not idle, it is in the middle of processing
        // a command.  At the end of the command the handler thread
        // will exit normally because of setShuttingDown(true) call
        // above.
        if (idle) {
            hardShutdown(null);
        }
    }

    void hardShutdown(String reason) {
        setShuttingDown(true);
        if (reason != null)
            log.info(reason);
        if (!mConnection.isClosed()) {
            dropConnection();
            try {
                mConnection.close();
            } catch (IOException ioe) {
                log.info("Exception while closing connection", ioe);
            }
        }
        if (mHandlerThread != null) {
            mHandlerThread.interrupt();
        }
    }
}
