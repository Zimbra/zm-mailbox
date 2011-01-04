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

import com.zimbra.common.util.NetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.TcpServerInputStream;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.net.Socket;

public class TcpPop3Handler extends Pop3Handler {
    private TcpServerInputStream inputStream;
    private String remoteAddress;

    TcpPop3Handler(Pop3Server server) {
        super(server);
    }

    @Override
    protected boolean setupConnection(Socket connection) throws IOException {
        remoteAddress = connection.getInetAddress().getHostAddress();
        connection.setSoTimeout(mConfig.getMaxIdleTime() * 1000);
        inputStream = new TcpServerInputStream(connection.getInputStream());
        mOutputStream = new BufferedOutputStream(connection.getOutputStream());
        return startConnection(connection.getInetAddress());
    }

    @Override
    protected boolean processCommand() throws IOException {
        try {
            return processCommand(inputStream.readLine());
        } finally {
            if (dropConnection) dropConnection();
        }
    }

    @Override
    protected void dropConnection() {
        ZimbraLog.addIpToContext(remoteAddress);
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
        } catch (IOException e) {
            if (ZimbraLog.pop.isDebugEnabled()) {
                ZimbraLog.pop.debug("I/O error while closing connection", e);
            } else {
                ZimbraLog.pop.debug("I/O error while closing connection: " + e);
            }
        } finally {
            ZimbraLog.clearContext();
        }
    }

    @Override
    protected void startTLS() throws IOException {
        sendOK("Begin TLS negotiation");
        SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sock = (SSLSocket)
            fac.createSocket(mConnection, mConnection.getInetAddress().getHostName(), mConnection.getPort(), true);
        NetUtil.setSSLEnabledCipherSuites(sock, mConfig.getSslExcludedCiphers());
        sock.setUseClientMode(false);
        startHandshake(sock);
        ZimbraLog.pop.debug("suite: %s", sock.getSession().getCipherSuite());
        inputStream = new TcpServerInputStream(sock.getInputStream());
        mOutputStream = new BufferedOutputStream(sock.getOutputStream());
    }

    @Override
    protected void completeAuthentication() throws IOException {
        mAuthenticator.sendSuccess();
        if (mAuthenticator.isEncryptionEnabled()) {
            // Switch to encrypted streams
            inputStream = new TcpServerInputStream(mAuthenticator.unwrap(mConnection.getInputStream()));
            mOutputStream = mAuthenticator.wrap(mConnection.getOutputStream());
        }
    }
}
