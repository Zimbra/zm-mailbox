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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioOutputStream;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.stats.ZimbraPerf;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

final class NioImapHandler extends ImapHandler implements NioHandler {
    private final ImapConfig config;
    private final NioConnection connection;
    private NioImapRequest request;

    NioImapHandler(NioImapServer server, NioConnection conn) {
        super(server.getConfig());
        connection = conn;
        config = server.getConfig();
        output = conn.getOutputStream();
    }

    @Override
    String getRemoteIp() {
        return connection.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public void connectionOpened() throws IOException {
        sendGreeting();
    }

    @Override
    public void messageReceived(Object msg) throws IOException {
        if (request == null) {
            request = new NioImapRequest(this);
        }

        if (request.parse(msg)) {
            // Request is complete
            try {
                if (!processRequest(request)) {
                    dropConnection();
                }
            } finally {
                ZimbraLog.clearContext();
                if (request != null) {
                    request.cleanup();
                    request = null;
                }
            }
            if (consecutiveError >= LC.imap_max_consecutive_error.intValue()) {
                dropConnection();
            }
        }
    }

    @Override
    public void exceptionCaught(Throwable e) throws IOException {
        try {
            if (e instanceof NioImapDecoder.TooBigLiteralException) {
                String tag;
                if (request != null) {
                    tag = request.getTag();
                } else {
                    try {
                        tag = ImapRequest.parseTag(((NioImapDecoder.TooBigLiteralException) e).getRequest());
                    } catch (ImapParseException e1) {
                        tag = "*";
                    }
                }
                sendBAD(tag, e.getMessage());
            } else if (e instanceof RecoverableProtocolDecoderException) {
                sendBAD("*", e.getMessage());
            } else if (e instanceof ProtocolDecoderException) {
                sendBAD("*", e.getMessage());
                dropConnection(true);
            }
        } finally {
            if (request != null) {
                request.cleanup();
                request = null;
            }
        }
    }

    private boolean processRequest(NioImapRequest req) throws IOException {
        ImapSession i4selected = selectedFolder;
        if (i4selected != null)
            i4selected.updateAccessTime();

        long start = ZimbraPerf.STOPWATCH_IMAP.start();

        try {
            if (!checkAccountStatus()) {
                return false;
            }
            if (authenticator != null && !authenticator.isComplete()) {
                return continueAuthentication(req);
            }
            try {
                return executeRequest(req);
            } catch (ImapProxyException e) {
                ZimbraLog.imap.debug("proxy failed", e);
                sendNO(req.getTag(), "Shared folder temporally unavailable");
                return false; // disconnect
            } catch (ImapParseException e) {
                handleParseException(e);
                return true;
            } catch (ImapException e) { // session closed
                ZimbraLog.imap.debug("stop processing", e);
                return false;
            }
        } finally {
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (lastCommand != null) {
                ZimbraPerf.IMAP_TRACKER.addStat(lastCommand.toUpperCase(), start);
            }
        }
    }

    @Override
    public void dropConnection() {
        dropConnection(true);
    }

    /**
     * Called when connection is closed. No need to worry about concurrent execution since requests are processed in
     * sequence for any given connection.
     */
    @Override
    public void connectionClosed() {
        if (request != null) {
            request.cleanup();
            request = null;
        }
        try {
            unsetSelectedFolder(false);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void connectionIdle() {
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override
    public void setLoggingContext() {
        super.setLoggingContext();
    }

    @Override
    void sendLine(String line, boolean flush) throws IOException {
        NioOutputStream out = (NioOutputStream) output;
        if (out != null) {
            out.write(line);
            out.write(LINE_SEPARATOR_BYTES);
            if (flush) {
                out.flush();
            }
        }
    }

    /**
     * Close the connection.
     *
     * Do not clean up the session here, but let the framework call {@link #connectionClosed()}, so that concurrency
     * issues are taken care of.
     */
    @Override
    void dropConnection(boolean sendBanner) {
        if (credentials != null && !goodbyeSent) {
            ZimbraLog.imap.info("dropping connection for user %s (server-initiated)", credentials.getUsername());
        }
        if (!connection.isOpen()) {
            return; // No longer connected
        }
        if (sendBanner && !goodbyeSent) {
            sendBYE();
        }
        connection.close();
    }

    @Override
    void close() {
        dropConnection(true);
    }

    @Override
    void enableInactivityTimer() {
        connection.setMaxIdleSeconds(config.getAuthenticatedMaxIdleTime());
    }

    @Override
    void completeAuthentication() throws IOException {
        if (authenticator.isEncryptionEnabled()) {
            connection.startSasl(authenticator.getSaslServer());
        }
        authenticator.sendSuccess();
    }

    @Override
    boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) {
            return true;
        } else if (startedTLS) {
            sendNO(tag, "TLS already started");
            return true;
        }

        connection.startTls();
        sendOK(tag, "begin TLS negotiation now");
        startedTLS = true;
        return true;
    }

    @Override
    InetSocketAddress getLocalAddress() {
        return connection.getLocalAddress();
    }
}
