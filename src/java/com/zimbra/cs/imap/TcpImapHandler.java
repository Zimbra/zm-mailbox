/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import com.zimbra.common.util.Constants;
import com.zimbra.common.util.NetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.server.ProtocolHandler;
import com.zimbra.cs.server.TcpServerInputStream;
import com.zimbra.cs.util.Config;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

class TcpImapHandler extends ProtocolHandler {
    private TcpServerInputStream input;
    private String remoteAddress;
    private TcpImapRequest request;
    private final ImapConfig config;
    private Socket socket;
    private final HandlerDelegate delegate;

    TcpImapHandler(TcpImapServer server) {
        super(server);
        config = server.getConfig();
        delegate = new HandlerDelegate(config);
    }

    @Override
    protected boolean setupConnection(Socket connection) throws IOException {
        socket = connection;
        socket.setSoTimeout(config.getMaxIdleTime() * 1000);
        remoteAddress = socket.getInetAddress().getHostAddress();
        INFO("connected");

        input = new TcpServerInputStream(connection.getInputStream());
        delegate.output = new BufferedOutputStream(connection.getOutputStream());

        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug("dropping connection because user services are disabled");
            dropConnection();
            return false;
        }

        delegate.sendGreeting();

        return true;
    }

    @Override
    protected boolean authenticate() {
        // we auth with the LOGIN command (and more to come)
        return true;
    }

    @Override
    protected void setIdle(boolean idle) {
        super.setIdle(idle);
        ImapSession i4selected = delegate.selectedFolder;
        if (i4selected != null) {
            i4selected.updateAccessTime();
        }
    }

    @Override
    protected boolean processCommand() throws IOException {
        // FIXME: throw an exception instead?
        if (input == null) {
            return false;
        }
        if (request == null) {
            request = new TcpImapRequest(input, delegate);
        }

        try {
            request.continuation();
            if (request.isMaxRequestSizeExceeded()) {
                setIdle(false);  // FIXME Why for only this error?
                throw new ImapParseException(request.getTag(), "maximum request size exceeded");
            }

            long start = ZimbraPerf.STOPWATCH_IMAP.start();
            // check account status before executing command
            if (!delegate.checkAccountStatus()) {
                return false;
            }
            boolean keepGoing;
            if (delegate.authenticator != null && !delegate.authenticator.isComplete()) {
                keepGoing = delegate.continueAuthentication(request);
            } else {
                keepGoing = delegate.executeRequest(request);
            }
            // FIXME Shouldn't we do these before executing the request??
            setIdle(false);
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (delegate.lastCommand != null) {
                ZimbraPerf.IMAP_TRACKER.addStat(delegate.lastCommand.toUpperCase(), start);
            }
            clearRequest();
            delegate.consecutiveBAD = 0;
            return keepGoing;
        } catch (TcpImapRequest.ImapContinuationException ice) {
            request.rewind();
            if (ice.sendContinuation) {
                delegate.sendContinuation("send literal data");
            }
            return true;
        } catch (TcpImapRequest.ImapTerminatedException ite) {
            return false;
        } catch (ImapParseException ipe) {
            clearRequest();
            delegate.handleParseException(ipe);
            return delegate.consecutiveBAD < ImapHandler.MAXIMUM_CONSECUTIVE_BAD;
        }
    }

    private void clearRequest() {
        if (request != null) {
            request.cleanup();
            request = null;
        }
    }

    @Override
    protected void notifyIdleConnection() {
        // we can, and do, drop idle connections after the timeout

        // TODO in the TcpServer case, is this duplicated effort with
        // session timeout code that also drops connections?
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    void INFO(String message, Throwable e) {
        if (ZimbraLog.imap.isInfoEnabled())
            ZimbraLog.imap.info(withClientInfo(message), e);
    }

    void INFO(String message) {
        if (ZimbraLog.imap.isInfoEnabled())
            ZimbraLog.imap.info(withClientInfo(message));
    }

    private StringBuilder withClientInfo(String message) {
        int length = 64;
        if (message != null)
            length += message.length();
        return new StringBuilder(length).append("[").append(remoteAddress).append("] ").append(message);
    }

    @Override
    protected void dropConnection() {
        delegate.dropConnection(true);
    }

    ImapHandler setCredentials(ImapCredentials creds) {
        delegate.setCredentials(creds);
        return delegate;
    }

    private final class HandlerDelegate extends ImapHandler {

        HandlerDelegate(ImapConfig config) {
            super(config);
        }

        @Override
        void sendLine(String line, boolean flush) throws IOException {
            ZimbraLog.imap.trace("S: %s", line);
            OutputStream os = output;
            if (os == null) {
                return;
            }
            os.write(line.getBytes());
            os.write(LINE_SEPARATOR_BYTES);
            if (flush) {
                os.flush();
            }
        }

        @Override
        protected void dropConnection(boolean sendBanner) {
            clearRequest();
            try {
                unsetSelectedFolder(false);
            } catch (Exception e) { }

            // wait at most 10 seconds for the untagged BYE to be sent, then force the stream closed
            new Thread() {
                @Override
                public void run() {
                    if (output == null) {
                        return;
                    }
                    try {
                        sleep(10 * Constants.MILLIS_PER_SECOND);
                    } catch (InterruptedException ie) { }

                    OutputStream os = output;
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException ioe) { }
                    }
                }
            }.start();

            if (credentials != null && !goodbyeSent) {
                ZimbraLog.imap.info("dropping connection for user %s (server-initiated)", credentials.getUsername());
            }

            ZimbraLog.addIpToContext(remoteAddress);
            try {
                OutputStream os = output;
                if (os != null) {
                    if (sendBanner && !goodbyeSent) {
                        sendBYE();
                    }
                    os.close();
                    output = null;
                }
                if (input != null) {
                    input.close();
                    input = null;
                }
                if (authenticator != null) {
                    authenticator.dispose();
                    authenticator = null;
                }
            } catch (IOException e) {
                if (ZimbraLog.imap.isDebugEnabled()) {
                    ZimbraLog.imap.debug("I/O error while closing connection", e);
                } else {
                    ZimbraLog.imap.debug("I/O error while closing connection: " + e);
                }
            } finally {
                ZimbraLog.clearContext();
            }
        }

        @Override
        void enableInactivityTimer() throws SocketException {
            mConnection.setSoTimeout(config.getAuthenticatedMaxIdleTime() * 1000);
        }

        @Override
        void completeAuthentication() throws IOException {
            delegate.setLoggingContext(remoteAddress);
            authenticator.sendSuccess();
            if (authenticator.isEncryptionEnabled()) {
                // switch to encrypted streams
                input = new TcpServerInputStream(authenticator.unwrap(mConnection.getInputStream()));
                output = authenticator.wrap(mConnection.getOutputStream());
            }
        }

        @Override
        boolean doSTARTTLS(String tag) throws IOException {
            if (!checkState(tag, State.NOT_AUTHENTICATED)) {
                return true;
            } else if (startedTLS) {
                sendNO(tag, "TLS already started");
                return true;
            }
            sendOK(tag, "Begin TLS negotiation now");

            SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket tlsconn = (SSLSocket) fac.createSocket(mConnection, mConnection.getInetAddress().getHostName(), mConnection.getPort(), true);
            NetUtil.setSSLEnabledCipherSuites(tlsconn, config.getSslExcludedCiphers());
            tlsconn.setUseClientMode(false);
            startHandshake(tlsconn);
            ZimbraLog.imap.debug("suite: " + tlsconn.getSession().getCipherSuite());
            input = new TcpServerInputStream(tlsconn.getInputStream());
            output = new BufferedOutputStream(tlsconn.getOutputStream());
            startedTLS = true;
            return true;
        }

        @Override
        InetSocketAddress getLocalAddress() {
            return (InetSocketAddress) socket.getLocalSocketAddress();
        }
    }
}
