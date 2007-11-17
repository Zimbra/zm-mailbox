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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaUtil;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Config;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MinaImapHandler extends ImapHandler implements MinaHandler {
    private IoSession mSession;
    private WriteFuture mFuture;

    // Maximum number of milliseconds to wait for last write operation to
    // complete before we force close the session.
    private static final long WRITE_TIMEOUT = 5000;

    MinaImapHandler(MinaImapServer server, IoSession session) {
        super(server);
        this.mSession = session;
    }

    @Override
    boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) return true;
        if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return true;
        }
        MinaServer.startTLS(mSession);
        sendOK(tag, "Begin TLS negotiation now");
        mStartedTLS = true;
        return true;
    }

    public void connectionOpened() throws IOException {
        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug(
              "Dropping connection (user services are disabled)");
            // TODO Is there a better way of handling this?
            dropConnection();
            return;
        }
        mOutputStream = new MinaIoSessionOutputStream(mSession);
        sendUntagged(mConfig.getBanner(), true);
        mStartedTLS = mConfig.isSSLEnabled();
        mSession.setIdleTime(IdleStatus.BOTH_IDLE,
                             mConfig.getUnauthMaxIdleSeconds());
    }

    @Override
    protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }

    public void requestReceived(MinaRequest req) throws IOException {
        assert req instanceof MinaImapRequest;
        MinaImapRequest imapReq = (MinaImapRequest) req;
        
        if (imapReq.isMaxRequestSizeExceeded()) {
            imapReq.sendNO("[TOOBIG] request too big");
            return;
        }

        if (mCredentials != null) {
            ZimbraLog.addAccountNameToContext(mCredentials.getUsername());
        }
        if (mSelectedFolder != null) {
            ZimbraLog.addMboxToContext(mSelectedFolder.getMailbox().getId());
            mSelectedFolder.updateAccessTime();
        }
        ZimbraLog.addIpToContext(mSession.getRemoteAddress().toString());
        
        String origRemoteIp = getOrigRemoteIpAddr();
        if (origRemoteIp != null)
            ZimbraLog.addOrigIpToContext(origRemoteIp);
        
        long start = ZimbraPerf.STOPWATCH_IMAP.start();
        try {
            if (!processRequest(imapReq)) dropConnection();
        } finally {
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null) {
                sActivityTracker.addStat(mLastCommand.toUpperCase(), start);
            }
            ZimbraLog.clearContext();
        }
    }

    private boolean processRequest(MinaImapRequest req) throws IOException {
        if (!checkAccountStatus()) return false;
        if (mAuthenticator != null && !mAuthenticator.isComplete()) {
            return continueAuthentication(req);
        }
        try {
            return executeRequest(req);
        } catch (ImapParseException e) {
            handleParseException(e);
            return true;
        }
    }

    private void handleParseException(ImapParseException e) throws IOException {
        if (e.mTag == null) {
            sendUntagged("BAD " + e.getMessage(), true);
        } else if (e.mCode != null) {
            sendNO(e.mTag, '[' + e.mCode + "] " + e.getMessage());
        } else if (e.mNO) {
            sendNO(e.mTag, e.getMessage());
        } else {
            sendBAD(e.mTag, e.getMessage());
        }
    }

    // TODO Consider moving to ImapHandler base class
    private boolean checkAccountStatus() {
        if (mCredentials == null) return true;
        // Check authenticated user's account status before executing command
        try {
            Account account = mCredentials.getAccount();
            if (account == null || !isAccountStatusActive(account)) {
                ZimbraLog.imap.warn(
                    "account missing or not active; dropping connection");
                return false;
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(
                "error checking account status; dropping connection", e);
            return false;
        }
        // Check target folder owner's account status before executing command
        if (mSelectedFolder == null) return true;
        String id = mSelectedFolder.getTargetAccountId();
        if (mCredentials.getAccountId().equalsIgnoreCase(id)) return true;
        try {
            Account account =
                Provisioning.getInstance().get(Provisioning.AccountBy.id, id);
            if (account == null || !isAccountStatusActive(account)) {
                ZimbraLog.imap.warn(
                    "target account missing or not active; dropping connection");
                return false;
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(
                "error checking target account status; dropping connection", e);
            return false;
        }
        return true;
    }

    // TODO Consider adding method to Account base class
    private boolean isAccountStatusActive(Account account) {
        return account.getAccountStatus().equals(
            Provisioning.ACCOUNT_STATUS_ACTIVE);
    }
    
    /**
     * Called when connection is closed. No need to worry about concurrent
     * execution since requests are processed in sequence for any given
     * connection.
     */
    @Override
    protected void dropConnection(boolean sendBanner) {
        dropConnection(sendBanner, WRITE_TIMEOUT);
    }

    private void dropConnection(boolean sendBanner, long timeout) {
        if (!mSession.isConnected()) return; // No longer connected
        ZimbraLog.imap.debug("dropConnection: sendBanner = %s\n", sendBanner);
        if (mSelectedFolder != null) {
            mSelectedFolder.setHandler(null);
            SessionCache.clearSession(mSelectedFolder);
            mSelectedFolder = null;
        }
        try {
            if (sendBanner && !mGoodbyeSent) {
                sendUntagged(mConfig.getGoodbye(), true);
                mGoodbyeSent = true;
            }
            if (mFuture != null && timeout >= 0) {
                // Wait for last write to complete before closing session
                mFuture.join(timeout);
                if (!mFuture.isReady()) {
                    ZimbraLog.imap.warn("Force closing session because write timed out: " + mSession);
                }
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
        dropConnection();
    }

    public void connectionIdle() {
        notifyIdleConnection();
    }
    
    @Override
    protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected boolean authenticate() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void notifyIdleConnection() {
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override
    protected void enableInactivityTimer() {
        mSession.setIdleTime(IdleStatus.BOTH_IDLE,
                             ImapFolder.IMAP_IDLE_TIMEOUT_SEC);
    }

    @Override
    protected void completeAuthentication() throws IOException {
        sendCapability();
        if (mAuthenticator.isEncryptionEnabled()) {
            MinaServer.addSaslFilter(mSession, mAuthenticator.getSaslServer());
        }
        mAuthenticator.sendSuccess();
    }

    @Override
    protected void flushOutput() {
        // TODO Do we need to do anything here?
    }
    
    @Override
    void sendLine(String line, boolean flush) throws IOException {
        if (mSession.isClosing()) throw new IOException("Stream is closed");
        ByteBuffer bb = MinaUtil.toByteBuffer(line + "\r\n");
        mFuture = mSession.write(MinaUtil.toMinaByteBuffer(bb));
    }

    private void info(String msg, Throwable e) {
        if (!ZimbraLog.imap.isInfoEnabled()) return;
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(mSession.getRemoteAddress()).append("] ")
          .append(msg);
        if (e != null) {
            ZimbraLog.imap.info(sb.toString(), e);
        } else {
            ZimbraLog.imap.info(sb.toString());
        }
    }

    private void info(String msg) {
        info(msg, null);
    }
}
