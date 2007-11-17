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

package com.zimbra.cs.pop3;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaTextLineRequest;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MinaPop3Handler extends Pop3Handler implements MinaHandler {
    private IoSession mSession;

    public MinaPop3Handler(MinaPop3Server server, IoSession session) {
        super(server);
        this.mSession = session;
    }

    public void connectionOpened() throws IOException {
        mOutputStream = new MinaIoSessionOutputStream(mSession);
        mSession.setIdleTime(IdleStatus.BOTH_IDLE, mConfig.getMaxIdleSeconds());
        setupConnection(((InetSocketAddress) mSession.getRemoteAddress()).getAddress());
    }

    public void connectionClosed() throws IOException {
        dropConnection();
    }

    public void connectionIdle() {
        ZimbraLog.pop.debug("idle connection");
        dropConnection();
    }
    
    public void requestReceived(MinaRequest req) throws IOException {
        try {
            processCommand(((MinaTextLineRequest) req).getLine());
        } finally {
            if (dropConnection) dropConnection();
        }
    }

    @Override
    protected void startTLS() throws IOException {
        MinaServer.startTLS(mSession);
        sendOK("Begin TLS negotiation");
    }
    
    @Override
    protected void dropConnection() {
        if (!mSession.isClosing()) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                // Should never happen...
            }
            mSession.close();
        }
    }

    public void dropConnection(long timeout) {
        // TODO Handle timeout here
        dropConnection();
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
            MinaServer.addSaslFilter(mSession, mAuthenticator.getSaslServer());
        }
        mAuthenticator.sendSuccess();
    }
}
