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
package com.zimbra.cs.mina;

import com.zimbra.cs.security.sasl.SaslFilter;

import javax.security.sasl.SaslServer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class MinaSession {
    private final MinaServer server;
    private final IoSession ioSession;
    private final OutputStream out;

    MinaSession(MinaServer server, IoSession ioSession) {
        this.server = server;
        this.ioSession = ioSession;
        out = new MinaOutputStream(this);
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public MinaServer getServer() {
        return server;
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) ioSession.getRemoteAddress();
    }

    public void setMaxIdleSeconds(int secs) {
        ioSession.getConfig().setBothIdleTime(secs);
    }

    public synchronized void startTls() throws IOException {
        ensureOpened();
        SslFilter filter = server.newSSLFilter();
        ioSession.getFilterChain().addFirst("ssl", filter);
        ioSession.setAttribute(SslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public synchronized void startSasl(SaslServer sasl) throws IOException {
        ensureOpened();
        SaslFilter filter = new SaslFilter(sasl);
        ioSession.getFilterChain().addFirst("sasl", filter);
        ioSession.setAttribute(SaslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public synchronized void messageSent() {
        notify();
    }

    public synchronized void send(ByteBuffer bb) throws IOException {
        ensureOpened();
        ioSession.write(IoBuffer.wrap(bb));
    }

    public synchronized void send(Object obj) throws IOException {
        ensureOpened();
        ioSession.write(obj);
    }

    public synchronized long getScheduledWriteBytes() {
        return ioSession.getScheduledWriteBytes();
    }

    public synchronized void close() {
        if (!isClosed()) {
            ioSession.close(false);
        }
    }

    public synchronized boolean isClosed() {
        return ioSession.isClosing();
    }

    private void ensureOpened() throws IOException {
        if (isClosed()) {
            throw new IOException("Session is closed");
        }
    }
}
