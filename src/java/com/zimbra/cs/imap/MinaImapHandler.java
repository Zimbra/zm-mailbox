/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import com.zimbra.cs.mina.MinaOutputStream;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Config;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;

import java.io.IOException;
import java.net.Socket;

public class MinaImapHandler extends ImapHandler implements MinaHandler {
    private IoSession mSession;

    // Maximum number of milliseconds to wait for last write operation to
    // complete before we force close the session.
    private static final long WRITE_TIMEOUT = 5000;

    MinaImapHandler(MinaImapServer server, IoSession session) {
        super(server);
        this.mSession = session;
    }

    @Override boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) {
            return true;
        } else if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return true;
        }

        MinaServer.startTLS(mSession);
        sendOK(tag, "begin TLS negotiation now");
        mStartedTLS = true;
        return true;
    }

    public void connectionOpened() throws IOException {
        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug("Dropping connection (user services are disabled)");
            // TODO Is there a better way of handling this?
            dropConnection();
            return;
        }
        mOutputStream = new MinaIoSessionOutputStream(mSession);
        sendUntagged(mConfig.getBanner(), true);
        mStartedTLS = mConfig.isSSLEnabled();
        mSession.setIdleTime(IdleStatus.BOTH_IDLE, mConfig.getUnauthMaxIdleSeconds());
    }

    @Override protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }

    public void requestReceived(MinaRequest req) throws IOException {
        assert(req instanceof MinaImapRequest);
        MinaImapRequest imapReq = (MinaImapRequest) req;
        
        if (imapReq.isMaxRequestSizeExceeded()) {
            imapReq.sendNO("[TOOBIG] request too big");
            return;
        }

        ImapFolder i4selected = mSelectedFolder;
        Mailbox mbox = i4selected == null ? null : i4selected.getMailbox();
        String origRemoteIp = getOrigRemoteIpAddr();

        if (mCredentials != null)
            ZimbraLog.addAccountNameToContext(mCredentials.getUsername());
        if (mbox != null)
            ZimbraLog.addMboxToContext(mbox.getId());
        if (origRemoteIp != null)
            ZimbraLog.addOrigIpToContext(origRemoteIp);
        ZimbraLog.addIpToContext(mSession.getRemoteAddress().toString());

        if (i4selected != null)
            i4selected.updateAccessTime();

        long start = ZimbraPerf.STOPWATCH_IMAP.start();
        try {
            if (!processRequest(imapReq))
                dropConnection();
        } finally {
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null)
                ZimbraPerf.IMAP_TRACKER.addStat(mLastCommand.toUpperCase(), start);
            ZimbraLog.clearContext();
        }
    }

    private boolean processRequest(MinaImapRequest req) throws IOException {
        if (!checkAccountStatus())
            return STOP_PROCESSING;

        if (mAuthenticator != null && !mAuthenticator.isComplete())
            return continueAuthentication(req);

        try {
            return executeRequest(req);
        } catch (ImapParseException e) {
            handleParseException(e);
            return CONTINUE_PROCESSING;
        }
    }

    private void handleParseException(ImapParseException e) throws IOException {
        if (e.mTag == null)
            sendUntagged("BAD " + e.getMessage(), true);
        else if (e.mCode != null)
            sendNO(e.mTag, '[' + e.mCode + "] " + e.getMessage());
        else if (e.mNO)
            sendNO(e.mTag, e.getMessage());
        else
            sendBAD(e.mTag, e.getMessage());
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
        if (!mSession.isConnected())
            return; // No longer connected
        ZimbraLog.imap.debug("dropConnection: sendBanner = %s\n", sendBanner);
        cleanup();
        try {
            if (sendBanner && !mGoodbyeSent) {
                sendUntagged(mConfig.getGoodbye(), true);
                mGoodbyeSent = true;
            }
            MinaOutputStream out = (MinaOutputStream) mOutputStream;
            if (timeout >= 0 && out != null) {
                // Wait for all remaining bytes to be written
                if (!out.join(timeout))
                    ZimbraLog.imap.warn("Force closing session because write timed out: " + mSession);
            }
            mSession.close();
        } catch (IOException e) {
            info("exception while closing connection", e);
        }
    }

    public void dropConnection(long timeout) {
        dropConnection(true, timeout);
    }
    
    public void connectionClosed() {
        cleanup();
        mSession.close();
    }

    private void cleanup() {
        ImapFolder i4selected = mSelectedFolder;
        if (i4selected != null) {
            i4selected.setHandler(null);
            SessionCache.clearSession(i4selected);
            mSelectedFolder = null;
        }
    }
    
    public void connectionIdle() {
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
        mSession.setIdleTime(IdleStatus.BOTH_IDLE, ImapFolder.IMAP_IDLE_TIMEOUT_SEC);
    }

    @Override protected void completeAuthentication() throws IOException {
        if (mAuthenticator.isEncryptionEnabled())
            MinaServer.addSaslFilter(mSession, mAuthenticator.getSaslServer());
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

    private void info(String msg, Throwable e) {
        if (!ZimbraLog.imap.isInfoEnabled())
            return;
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(mSession.getRemoteAddress()).append("] ").append(msg);
        if (e != null)
            ZimbraLog.imap.info(sb.toString(), e);
        else
            ZimbraLog.imap.info(sb.toString());
    }

//    private void info(String msg) {
//        info(msg, null);
//    }
}
