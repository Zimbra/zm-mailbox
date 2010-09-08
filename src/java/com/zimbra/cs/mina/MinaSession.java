/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;

import javax.security.sasl.SaslServer;
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
        ioSession.setIdleTime(IdleStatus.BOTH_IDLE, secs);
    }

    public synchronized void startTls() throws IOException {
        checkNotClosed();
        SSLFilter filter = server.newSSLFilter();
        ioSession.getFilterChain().addFirst("ssl", filter);
        ioSession.setAttribute(SSLFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public synchronized void startSasl(SaslServer sasl) throws IOException {
        checkNotClosed();
        SaslFilter filter = new SaslFilter(sasl);
        ioSession.getFilterChain().addFirst("sasl", filter);
        ioSession.setAttribute(SaslFilter.DISABLE_ENCRYPTION_ONCE, true);
    }

    public synchronized void messageSent() {
        notify();
    }

    public synchronized void send(ByteBuffer bb) throws IOException {
        checkNotClosed();
        ioSession.write(org.apache.mina.common.ByteBuffer.wrap(bb));
    }

    public synchronized void send(Object obj) throws IOException {
        checkNotClosed();
        ioSession.write(obj);
    }
    
    public boolean drainWriteQueue(long timeout) {
        return drainWriteQueue(0, timeout);
    }
    
    public synchronized boolean drainWriteQueue(int threshold, long timeout) {
        if (timeout <= 0) timeout = Long.MAX_VALUE;
        long start = System.currentTimeMillis();
        while (!isClosed() && threshold < getScheduledWriteBytes()) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > timeout) return false;
            try {
                wait(timeout - elapsed);
            } catch (InterruptedException e) {
                return true;
            }
        }
        return true;
    }

    public synchronized int getScheduledWriteBytes() {
        return ioSession.getScheduledWriteBytes();
    }

    public synchronized void close() {
        if (!isClosed()) {
            ioSession.close();
        }
        notify();
    }

    public synchronized boolean isClosed() {
        return ioSession.isClosing();
    }

    private void checkNotClosed() throws IOException {
        if (isClosed()) {
            throw new IOException("Session is closed");
        }
    }
}
