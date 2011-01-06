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
import com.zimbra.cs.server.ProtocolHandler;
import com.zimbra.cs.server.TcpServerInputStream;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

final class TcpPop3Handler extends ProtocolHandler {
    private TcpServerInputStream input;
    private String remoteAddress;
    private final HandlerDelegate delegate;

    TcpPop3Handler(TcpPop3Server server) {
        super(server);
        delegate = new HandlerDelegate(server.getConfig());
    }

    @Override
    protected boolean setupConnection(Socket connection) throws IOException {
        remoteAddress = connection.getInetAddress().getHostAddress();
        connection.setSoTimeout(delegate.config.getMaxIdleTime() * 1000);
        input = new TcpServerInputStream(connection.getInputStream());
        delegate.output = new BufferedOutputStream(connection.getOutputStream());
        if (delegate.startConnection(connection.getInetAddress())) {
            return true;
        } else {
            dropConnection();
            return false;
        }
    }

    @Override
    protected boolean processCommand() throws IOException {
        setIdle(false);
        if (delegate.processCommand(input.readLine())) {
            return true;
        } else {
            dropConnection();
            return false;
        }
    }

    @Override
    protected void dropConnection() {
        ZimbraLog.addIpToContext(remoteAddress);
        try {
            if (input != null) {
                input.close();
                input = null;
            }
            if (delegate.output != null) {
                delegate.output.close();
                delegate.output = null;
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
    protected boolean authenticate() throws IOException {
        // we auth with the USER/PASS commands
        return true;
    }

    @Override
    protected void notifyIdleConnection() {
        // according to RFC 1939 we aren't supposed to snd a response on idle timeout
        ZimbraLog.pop.debug("idle connection");
    }

    private class HandlerDelegate extends Pop3Handler {

        HandlerDelegate(Pop3Config config) {
            super(config);
        }

        @Override
        protected void startTLS() throws IOException {
            sendOK("Begin TLS negotiation");
            SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sock = (SSLSocket) fac.createSocket(mConnection,
                    mConnection.getInetAddress().getHostName(), mConnection.getPort(), true);
            NetUtil.setSSLEnabledCipherSuites(sock, config.getSslExcludedCiphers());
            sock.setUseClientMode(false);
            startHandshake(sock);
            ZimbraLog.pop.debug("suite: %s", sock.getSession().getCipherSuite());
            input = new TcpServerInputStream(sock.getInputStream());
            output = new BufferedOutputStream(sock.getOutputStream());
        }

        @Override
        protected void completeAuthentication() throws IOException {
            setLoggingContext();
            authenticator.sendSuccess();
            if (authenticator.isEncryptionEnabled()) {
                // Switch to encrypted streams
                input = new TcpServerInputStream(authenticator.unwrap(mConnection.getInputStream()));
                output = authenticator.wrap(mConnection.getOutputStream());
            }
        }

        @Override
        InetSocketAddress getLocalAddress() {
            return (InetSocketAddress) mConnection.getLocalSocketAddress();
        }

        @Override
        void sendLine(String line, boolean flush) throws IOException {
            ZimbraLog.pop.trace("S: %s", line);
            output.write(line.getBytes());
            output.write(LINE_SEPARATOR);
            if (flush) {
                output.flush();
            }
        }

    }

}
