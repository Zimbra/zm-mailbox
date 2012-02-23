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
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.server.NioOutputStream;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

final class NioPop3Handler extends Pop3Handler implements NioHandler {
    private final NioConnection connection;

    NioPop3Handler(NioPop3Server server, NioConnection conn) {
        super(server.getConfig());
        connection = conn;
        output = conn.getOutputStream();
    }

    @Override
    public void connectionOpened() throws IOException {
        if (!startConnection(connection.getRemoteAddress().getAddress())) {
            connection.close();
        }
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
        if (!processCommand((String) msg)) {
            dropConnection();
        }
    }

    @Override
    public void exceptionCaught(Throwable e) throws IOException {
        if (e instanceof RecoverableProtocolDecoderException) {
            RecoverableProtocolDecoderException re = (RecoverableProtocolDecoderException) e;
            int hexdumpIdx = re.getMessage() != null ? re.getMessage().indexOf("(Hexdump:") : -1;
            if (hexdumpIdx >= 0) {
                sendERR(e.getMessage().substring(0, hexdumpIdx));
            } else {
                sendERR(e.getMessage());
            }
        }
    }

    @Override
    public void dropConnection() {
        if (!connection.isOpen()) {
            return;
        }
        try {
            output.close();
        } catch (IOException never) {
        }
        connection.close();
    }

    @Override
    protected void startTLS() throws IOException {
        connection.startTls();
        sendOK("Begin TLS negotiation");
    }

    @Override
    protected void completeAuthentication() throws IOException {
        if (authenticator.isEncryptionEnabled()) {
            connection.startSasl(authenticator.getSaslServer());
        }
        authenticator.sendSuccess();
    }

    @Override
    InetSocketAddress getLocalAddress() {
        return connection.getLocalAddress();
    }

    @Override
    void sendLine(String line, boolean flush) throws IOException {
        NioOutputStream nioutput = (NioOutputStream) output;
        nioutput.write(line);
        nioutput.write(LINE_SEPARATOR);
        if (flush) {
            nioutput.flush();
        }
    }
}
