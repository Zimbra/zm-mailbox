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
package com.zimbra.cs.server;

import com.zimbra.cs.security.sasl.SaslFilter;

import javax.security.sasl.SaslServer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class NioConnection {
    private final NioServer server;
    private final IoSession session;
    private final OutputStream out;
    private final InetSocketAddress remoteAddress;

    NioConnection(NioServer server, IoSession session) {
        this.server = server;
        this.session = session;
        remoteAddress = (InetSocketAddress) session.getRemoteAddress();
        out = new NioOutputStream(session, server.getConfig().getWriteChunkSize());
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
