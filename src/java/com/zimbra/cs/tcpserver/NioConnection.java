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
package com.zimbra.cs.tcpserver;

import com.zimbra.cs.security.sasl.SaslFilter;

import javax.security.sasl.SaslServer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class NioConnection {
    private final NioServer server;
    private final IoSession session;
    private final OutputStream out;
    private final InetSocketAddress remoteAddress;

    NioConnection(NioServer server, IoSession session) {
        this.server = server;
        this.session = session;
        remoteAddress = (InetSocketAddress) session.getRemoteAddress();
        out = new NioOutputStream(this);
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public NioServer getServer() {
        return server;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setMaxIdleSeconds(int secs) {
        session.getConfig().setBothIdleTime(secs);
    }

    public synchronized void startTls() throws IOException {
        ensureOpened();
        SslFilter filter = server.newSSLFilter();
        session.getFilterChain().addFirst("ssl", filter);
        session.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public synchronized void startSasl(SaslServer sasl) throws IOException {
        ensureOpened();
        SaslFilter filter = new SaslFilter(sasl);
        session.getFilterChain().addFirst("sasl", filter);
        session.setAttribute(SaslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public synchronized void messageSent() {
        notify();
    }

    public synchronized void send(ByteBuffer bb) throws IOException {
        ensureOpened();
        session.write(IoBuffer.wrap(bb));
    }

    public synchronized void send(Object obj) throws IOException {
        ensureOpened();
        session.write(obj);
    }

    public synchronized long getScheduledWriteBytes() {
        return session.getScheduledWriteBytes();
    }

    public synchronized void close() {
        if (isOpen()) {
            session.close(false);
        }
    }

    public synchronized boolean isOpen() {
        return session.isConnected();
    }

    private void ensureOpened() throws SocketException {
        if (!isOpen()) {
            throw new SocketException("Session is closed");
        }
    }
}
