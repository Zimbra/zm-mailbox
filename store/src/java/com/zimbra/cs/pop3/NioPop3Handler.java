/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.server.NioOutputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

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
        ZimbraLog.addConnectionIdToContext(String.valueOf(connection.getId()));
        if (!startConnection(connection.getRemoteAddress().getAddress())) {
            connection.close();
        }
    }

    @Override
    public void connectionClosed() throws IOException {
        try {
            setLoggingContext();
            String summary = new String();
            for (Map.Entry<String, Integer> entry : commandCount.entrySet()) {
                String result = entry.getKey() + "=" + entry.getValue();
                summary += summary.isEmpty() ? result : ", " + result;
            }
            ZimbraLog.pop.info("[" + summary + "]" +
                " MailboxSize:" + mailboxSize + "->" + getMailboxSize() +
                " InboxNumMsgs:" + inboxNumMsgs + "->" + getInboxNumMessages() +
                " read[" + connection.getReadBytes() +
                "] write[" + connection.getWrittenBytes() +
                "] time[" + connection.getSessionDuration() + "]");
        } catch (Exception ignore) {
        }
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
        if (e instanceof javax.net.ssl.SSLException) {
            ZimbraLog.pop.error("Error detected by SSL subsystem, dropping connection:" + e);
            dropConnection();  // Bug 79904 prevent using SSL port in plain text
        } else if (e instanceof RecoverableProtocolDecoderException) {
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

    @Override
    public void setLoggingContext() {
        super.setLoggingContext();
        ZimbraLog.addConnectionIdToContext(String.valueOf(connection.getId()));
    }
}
