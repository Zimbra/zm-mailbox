/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaOutputStream;
import com.zimbra.cs.mina.MinaSession;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Config;

import java.io.IOException;
import java.net.Socket;

class MinaImapHandler extends ImapHandler implements MinaHandler {
    private MinaImapServer mServer;
    private MinaSession mSession;
    private MinaImapRequest mRequest;

    private static final long WRITE_TIMEOUT = LC.nio_imap_write_timeout.longValue() * 1000;
    private static final int MAX_SESSIONS = LC.nio_imap_max_sessions.intValue();
    
    MinaImapHandler(MinaImapServer server, MinaSession session) {
        super(server);
        this.mServer = server;
        this.mSession = session;
        mOutputStream = mSession.getOutputStream();
    }

    @Override boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) {
            return true;
        } else if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return true;
        }

        mSession.startTls();
        sendOK(tag, "begin TLS negotiation now");
        mStartedTLS = true;
        return true;
    }

    @Override public void connectionOpened() throws IOException {
        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug("Dropping connection (user services are disabled)");
            dropConnection();
        } else if (mServer.getStats().getActiveSessions() >= MAX_SESSIONS) {
            ZimbraLog.imap.debug("Dropping connection (max sessions exceeded)");
            sendBYE("Server too busy");
            dropConnection();
        } else {
            sendGreeting();
        }
    }

    @Override protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }

    @Override public void messageReceived(Object msg) throws IOException {
        if (mRequest == null)
            mRequest = new MinaImapRequest(this);
        
        if (mRequest.parse(msg)) {
            // Request is complete
            setUpLogContext(mSession.getRemoteAddress().toString());
            try {
                if (!processRequest(mRequest))
                    dropConnection();
            } catch (ImapParseException ipe) {
                handleParseException(ipe);
            } finally {
                ZimbraLog.clearContext();
                if (mRequest != null) {
                    mRequest.cleanup();
                    mRequest = null;
                }
            }
            if (mConsecutiveBAD >= MAXIMUM_CONSECUTIVE_BAD)
                dropConnection();
        }
    }

    private boolean processRequest(MinaImapRequest req) throws IOException, ImapParseException {
        if (req.isMaxRequestSizeExceeded())
            throw new ImapParseException(req.getTag(), "maximum request size exceeded");

        ImapSession i4selected = mSelectedFolder;
        if (i4selected != null)
            i4selected.updateAccessTime();

        long start = ZimbraPerf.STOPWATCH_IMAP.start();

        try {
            if (!checkAccountStatus())
                return STOP_PROCESSING;

            if (mAuthenticator != null && !mAuthenticator.isComplete())
                return continueAuthentication(req);

            try {
                boolean keepGoing = executeRequest(req);
                mConsecutiveBAD = 0;
                return keepGoing;
            } catch (ImapParseException ipe) {
                handleParseException(ipe);
                return CONTINUE_PROCESSING;
            }
        } finally {
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null)
                ZimbraPerf.IMAP_TRACKER.addStat(mLastCommand.toUpperCase(), start);
        }
    }

    /**
     * Called when connection is closed. No need to worry about concurrent
     * execution since requests are processed in sequence for any given
     * connection.
     */
    @Override protected void dropConnection(boolean sendBanner) {
        dropConnection(sendBanner, WRITE_TIMEOUT);
    }

    private void dropConnection(boolean sendBanner, long timeout) {
        try {
            unsetSelectedFolder(false);
        } catch (Exception e) { }

        if (mSession.isClosed())
            return; // No longer connected
        ZimbraLog.imap.debug("dropConnection: sendBanner = %s\n", sendBanner);
        cleanup();
        if (sendBanner && !mGoodbyeSent) {
            sendBYE();
        }
        if (!mSession.drainWriteQueue(timeout)) {
            ZimbraLog.imap.warn("Force closing connection with unsent data");
        }
        mSession.close();
    }

    @Override public void dropConnection(long timeout) {
        dropConnection(true, timeout);
    }
    
    @Override public void connectionClosed() {
        cleanup();
        mSession.close();
    }

    private void cleanup() {
        if (mRequest != null) {
            mRequest.cleanup();
            mRequest = null;
        }
        try {
            unsetSelectedFolder(false);
        } catch (Exception e) {}
    }
    
    @Override public void connectionIdle() {
        notifyIdleConnection();
    }
    
    @Override protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }
    
    @Override protected boolean authenticate() {
        throw new UnsupportedOperationException();
    }

    @Override protected void notifyIdleConnection() {
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override protected void enableInactivityTimer() {
        mSession.setMaxIdleSeconds(mConfig.getAuthenticatedMaxIdleSeconds());
    }

    @Override protected void completeAuthentication() throws IOException {
        if (mAuthenticator.isEncryptionEnabled()) {
            mSession.startSasl(mAuthenticator.getSaslServer());
        }
        mAuthenticator.sendSuccess();
    }

    @Override protected void flushOutput() throws IOException {
        mOutputStream.flush();
    }

    @Override void sendLine(String line, boolean flush) throws IOException {
        MinaOutputStream out = (MinaOutputStream) mOutputStream;
        if (out != null) {
            out.write(line);
            out.write("\r\n");
            if (flush)
                out.flush();
        }
    }
}
