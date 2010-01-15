/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;

import java.io.IOException;

/**
 * Handler for MINA I/O events. Responsible for notifying the connection's
 * MinaHandler when a connection has been opened, closed, become idle, or a
 * new request has been received.
 */
class MinaIoHandler implements IoHandler {
    private final MinaServer server;
    private final MinaStats stats;

    private static final String MINA_HANDLER_ATTR = "MinaHandler";

    MinaIoHandler(MinaServer server) {
        this.server = server;
        stats = server.getStats();
    }

    public void sessionCreated(IoSession session) throws IOException {
        session.setAttribute(MINA_HANDLER_ATTR, server.createHandler(session));
    }

    public void sessionOpened(IoSession session) throws IOException {
        getMinaHandler(session).connectionOpened();
        if (stats != null) {
            stats.activeSessions.incrementAndGet();
            stats.totalSessions.incrementAndGet();
        }
    }

    public void sessionClosed(IoSession session) throws IOException {
        getMinaHandler(session).connectionClosed();
        if (stats != null) {
            stats.activeSessions.decrementAndGet();
        }
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws IOException{
        getMinaHandler(session).connectionIdle();
    }

    public void messageReceived(IoSession session, Object msg) throws IOException {
        getMinaHandler(session).messageReceived(msg);
    }

    public void exceptionCaught(IoSession session, Throwable e) throws IOException {
        getMinaHandler(session).connectionClosed();
    }

    public void messageSent(IoSession session, Object msg) {
        if (stats != null && msg instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) msg;
            stats.sentBytes.addAndGet(bb.remaining());
        }
    }

    public static MinaHandler getMinaHandler(IoSession session) {
        return (MinaHandler) session.getAttribute(MINA_HANDLER_ATTR);
    }
}
