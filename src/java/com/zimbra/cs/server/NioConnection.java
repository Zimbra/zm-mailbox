/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.server;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.security.sasl.SaslServer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

import com.zimbra.cs.security.sasl.SaslFilter;

public final class NioConnection {
    private final NioServer server;
    private final IoSession session;
    private final OutputStream out;
    private final InetSocketAddress remoteAddress;

    NioConnection(NioServer server, IoSession session) {
        this.server = server;
        this.session = session;
        remoteAddress = (InetSocketAddress) session.getRemoteAddress();
        out = new NioOutputStream(session, server.getConfig().getWriteChunkSize(),
                        server.getConfig().getNioMaxWriteQueueSize(), server.getConfig().getNioMaxWriteQueueDelay());
    }

    /**
     * Returns the connection ID.
     */
    public long getId() {
        return session.getId();
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public NioServer getServer() {
        return server;
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) session.getServiceAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setMaxIdleSeconds(int secs) {
        session.getConfig().setBothIdleTime(secs);
    }

    public void startTls() {
        SslFilter filter = server.newSSLFilter();
        session.getFilterChain().addFirst("ssl", filter);
        session.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public void startSasl(SaslServer sasl) {
        SaslFilter filter = new SaslFilter(sasl);
        session.getFilterChain().addFirst("sasl", filter);
        session.setAttribute(SaslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public void send(Object obj) {
        session.write(obj);
    }

    public long getScheduledWriteBytes() {
        return session.getScheduledWriteBytes();
    }

    public boolean isOpen() {
        return session.isConnected();
    }

    public void close() {
        session.close(false);
    }

}
