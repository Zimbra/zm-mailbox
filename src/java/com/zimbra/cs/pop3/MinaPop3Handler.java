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

package com.zimbra.cs.pop3;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MinaPop3Handler extends Pop3Handler implements MinaHandler {
    private MinaSession mSession;

    private static final long WRITE_TIMEOUT = 5000; // 5 seconds

    public MinaPop3Handler(MinaPop3Server server, MinaSession session) {
        super(server);
        this.mSession = session;
        mOutputStream = session.getOutputStream();
        mSession.setMaxIdleSeconds(mConfig.getMaxIdleSeconds());
    }

    public void connectionOpened() throws IOException {
        startConnection(((InetSocketAddress) mSession.getRemoteAddress()).getAddress());
    }

    public void connectionClosed() throws IOException {
        mSession.close();
    }

    public void connectionIdle() {
        ZimbraLog.pop.debug("idle connection");
        dropConnection();
    }
    
    public void messageReceived(Object msg) throws IOException {
        try {
            processCommand((String) msg);
        } finally {
            if (dropConnection) dropConnection();
        }
    }

    @Override
    protected void startTLS() throws IOException {
        mSession.startTls();
        sendOK("Begin TLS negotiation");
    }
    
    @Override
    protected void dropConnection() {
        dropConnection(WRITE_TIMEOUT);
    }
    
    public void dropConnection(long timeout) {
        if (mSession.isClosed())
            return;
        try {
            mOutputStream.close();
        } catch (IOException e) {
            // Should never happen...
        }
        if (timeout >= 0) {
            // Wait for all remaining bytes to be written
            if (!mSession.drainWriteQueue(timeout)) {
                ZimbraLog.pop.warn("Force closing session because write timed out: " + mSession);
            }
        }
        mSession.close();
    }

    @Override
    protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void completeAuthentication() throws IOException {
        if (mAuthenticator.isEncryptionEnabled()) {
            mSession.startSasl(mAuthenticator.getSaslServer());
        }
        mAuthenticator.sendSuccess();
    }
}
