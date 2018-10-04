/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.zimbra.common.io.TcpServerInputStream;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.NetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.ProtocolHandler;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.IOUtil;

final class TcpImapHandler extends ProtocolHandler {
    private TcpServerInputStream input;
    private String remoteIp;
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
        remoteIp = socket.getInetAddress().getHostAddress();
        INFO("connected");

        input = new TcpServerInputStream(connection.getInputStream());
        delegate.output = new BufferedOutputStream(connection.getOutputStream());

        if (!config.isServiceEnabled()) {
            ZimbraLog.imap.debug("dropping TCP connection because user services are disabled");
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
        ImapListener i4selected = delegate.selectedFolderListener;
        if (i4selected != null) {
            try {
                i4selected.updateAccessTime();
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("unable to update access time of %s", i4selected);
            }
        }
    }

    @Override
    protected boolean processCommand() throws IOException {
        delegate.setLoggingContext();
        // FIXME: throw an exception instead?
        if (input == null) {
            clearRequest();
            return false;
        }
        if (request == null) {
            request = new TcpImapRequest(input, delegate);
        }
        boolean complete = true;
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
            long elapsed = ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (delegate.lastCommand != null) {
                ZimbraLog.imap.info("%s elapsed=%d (TCP)", delegate.lastCommand.toUpperCase(), elapsed);
                ZimbraPerf.IMAP_TRACKER.addStat(delegate.lastCommand.toUpperCase(), start);
                ZimbraPerf.IMAPD_TRACKER.addStat(delegate.lastCommand.toUpperCase(), start);
            } else {
                ZimbraLog.imap.info("(unknown) elapsed=%d (TCP)", elapsed);
            }
            return keepGoing && (LC.imap_max_consecutive_error.intValue() <= 0 || delegate.consecutiveError < LC.imap_max_consecutive_error.intValue());
        } catch (TcpImapRequest.ImapContinuationException e) {
            request.rewind();
            complete = false; // skip clearRequest()
            if (e.sendContinuation) {
                delegate.sendContinuation("send literal data");
            }
            return true;
        } catch (TcpImapRequest.ImapTerminatedException e) {
            return false;
        } catch (ImapParseException e) {
            delegate.handleParseException(e);
            return LC.imap_max_consecutive_error.intValue() <= 0 || delegate.consecutiveError < LC.imap_max_consecutive_error.intValue();
        } catch (ImapException e) { // session closed
            ZimbraLog.imap.debug("stop processing", e);
            return false;
        } catch (IOException e) {
            if (socket.isClosed()) {
                ZimbraLog.imap.debug("stop processing", e);
                return false;
            }
            throw e;
        } catch (Exception e) {
            ZimbraLog.imap.error("unexpected exception", e);
            delegate.sendBAD("Unknown Error");
            return false;
        } finally {
            if (complete) {
                clearRequest();
            }
        }
    }

    /**
     * Only an IMAP handler thread may call. Don't call by other threads including IMAP session sweeper thread,
     * otherwise concurrency issues will arise.
     */
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
        ZimbraLog.imap.debug("dropping TCP connection for inactivity");
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
        if (message != null) {
            length += message.length();
        }
        return new StringBuilder(length).append("[").append(remoteIp).append("] ").append(message);
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
        protected String getRemoteIp() {
            return remoteIp;
        }

        @Override
        protected void sendLine(String line, boolean flush) throws IOException {
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
            try {
                unsetSelectedFolder(false);
            } catch (Exception e) {
            } finally {
                logout();
            }

            //TODO use thread pool
            // wait at most 10 seconds for the untagged BYE to be sent, then force the stream closed
            new Thread() {
                @Override
                public void run() {
                    if (output == null) {
                        return;
                    }
                    try {
                        sleep(10 * Constants.MILLIS_PER_SECOND);
                    } catch (InterruptedException e) {
                    }
                    IOUtil.closeQuietly(output);
                }
            }.start();

            if (credentials != null && !goodbyeSent) {
                ZimbraLog.imap.info("TCP:dropping connection for user %s (server-initiated)", credentials.getUsername());
            }

            ZimbraLog.addIpToContext(remoteIp);
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
                    ZimbraLog.imap.debug("TCP:I/O error while closing connection", e);
                } else {
                    ZimbraLog.imap.debug("TCP:I/O error while closing connection: %s", e);
                }
            } finally {
                ZimbraLog.clearContext();
            }
        }

        /**
         * Close the IMAP connection immediately without sending an untagged BYE.
         *
         * This is necessarily violating RFC 3501 3.4:
         * <pre>
         *   A server MUST NOT unilaterally close the connection without
         *   sending an untagged BYE response that contains the reason for
         *   having done so.
         * </pre>
         * because there is no easy way to interrupt a blocking read from the socket ({@link Socket#shutdownInput()} is
         * not supported by {@link SSLSocket}) and trying to send an untagged BYE not from this IMAP handler has
         * concurrency issues.
         */
        @Override
        protected void close() {
            try {
                socket.close(); // blocking read from this socket will throw SocketException
            } catch (Throwable e) {
                ZimbraLog.imap.debug("Failed to close socket", e);
            }
        }

        @Override
        protected void enableInactivityTimer() throws SocketException {
            connection.setSoTimeout(config.getAuthenticatedMaxIdleTime() * 1000);
        }

        @Override
        protected void completeAuthentication() throws IOException {
            delegate.setLoggingContext();
            authenticator.sendSuccess();
            if (authenticator.isEncryptionEnabled()) {
                // switch to encrypted streams
                input = new TcpServerInputStream(authenticator.unwrap(connection.getInputStream()));
                output = authenticator.wrap(connection.getOutputStream());
            }
        }

        @Override
        protected boolean doSTARTTLS(String tag) throws IOException {
            if (!checkState(tag, State.NOT_AUTHENTICATED)) {
                return true;
            } else if (startedTLS) {
                sendNO(tag, "TLS already started");
                return true;
            }
            sendOK(tag, "Begin TLS negotiation now");

            SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket tlsconn = (SSLSocket) fac.createSocket(connection, connection.getInetAddress().getHostName(),
                    connection.getPort(), true);
            NetUtil.setSSLProtocols(tlsconn, config.getMailboxdSslProtocols());
            NetUtil.setSSLEnabledCipherSuites(tlsconn, config.getSslExcludedCiphers(), config.getSslIncludedCiphers());
            tlsconn.setUseClientMode(false);
            startHandshake(tlsconn);
            ZimbraLog.imap.debug("suite: %s", tlsconn.getSession().getCipherSuite());
            input = new TcpServerInputStream(tlsconn.getInputStream());
            output = new BufferedOutputStream(tlsconn.getOutputStream());
            startedTLS = true;
            return true;
        }

        @Override
        protected InetSocketAddress getLocalAddress() {
            return (InetSocketAddress) socket.getLocalSocketAddress();
        }
    }
}
