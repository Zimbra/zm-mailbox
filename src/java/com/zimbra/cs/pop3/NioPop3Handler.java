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

package com.zimbra.cs.pop3;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.tcpserver.NioHandler;
import com.zimbra.cs.tcpserver.NioConnection;

import java.io.IOException;
import java.net.Socket;

final class NioPop3Handler extends Pop3Handler implements NioHandler {
    private final NioConnection connection;

    NioPop3Handler(NioPop3Server server, NioConnection conn) {
        super(server);
        connection = conn;
        mOutputStream = conn.getOutputStream();
    }

    @Override
    public void connectionOpened() throws IOException {
        startConnection(connection.getRemoteAddress().getAddress());
    }

    @Override
    public void connectionClosed() throws IOException {
        connection.close();
    }

    @Override
    public void connectionIdle() {
        ZimbraLog.pop.debug("idle connection");
        dropConnection();
    }

    @Override
    public void messageReceived(Object msg) throws IOException {
        try {
            processCommand((String) msg);
        } finally {
            if (dropConnection) dropConnection();
        }
    }

    @Override
    protected void startTLS() throws IOException {
        connection.startTls();
        sendOK("Begin TLS negotiation");
    }

    @Override
    public void dropConnection() {
        if (!connection.isOpen()) {
            return;
        }
        try {
            mOutputStream.close();
        } catch (IOException never) {
        }
        connection.close();
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
            connection.startSasl(mAuthenticator.getSaslServer());
        }
        mAuthenticator.sendSuccess();
    }
}
