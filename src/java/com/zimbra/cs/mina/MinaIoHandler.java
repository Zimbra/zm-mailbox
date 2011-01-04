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

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.google.common.base.Charsets;
import com.zimbra.cs.util.Config;

/**
 * Handler for MINA I/O events. Responsible for notifying the connection's {@link MinaHandler} when a connection has
 * been opened, closed, become idle, or a new request has been received.
 */
class MinaIoHandler implements IoHandler {
    private final MinaServer server;
    private final MinaStats stats;

    private static final String MINA_SESSION = "MinaSession";
    private static final String MINA_HANDLER = "MinaHandler";

    MinaIoHandler(MinaServer server) {
        this.server = server;
        stats = server.getStats();
    }

    /**
     * Invoked from an I/O processor thread when a new connection has been created. Because this method is supposed to
     * be called from the same thread that handles I/O of multiple sessions, please implement this method to perform
     * tasks that consumes minimal amount of time such as socket parameter and user-defined session attribute
     * initialization.
     */
    @Override
    public void sessionCreated(IoSession ioSession) throws IOException {
        MinaSession session = new MinaSession(server, ioSession);
        ioSession.setAttribute(MINA_SESSION, session);
        ioSession.setAttribute(MINA_HANDLER, server.createHandler(session));
    }

    /**
     * Invoked when a connection has been opened. This method is invoked after {@link #sessionCreated(IoSession)}. The
     * biggest difference from {@link #sessionCreated(IoSession)} is that it's invoked from a handler thread instead of
     * an I/O processor thread.
     */
    @Override
    public void sessionOpened(IoSession session) throws IOException {
        MinaHandler handler = getMinaHandler(session);
        long numSessions = stats.activeSessions.incrementAndGet();
        stats.totalSessions.incrementAndGet();

        if (!Config.userServicesEnabled()) {
            server.getLog().warn("Dropping connection (user services are disabled)");
            session.close(true);
        } else if (numSessions > server.getConfig().getMaxConnections()) {
            server.getLog().warn("Dropping connection (max connections exceeded)");
            String message = server.getConfig().getConnectionRejected();
            if (message != null) {
                session.write(IoBuffer.wrap((message + "\r\n").getBytes(Charsets.ISO_8859_1)));
            }
            session.close(false);
        } else {
            handler.connectionOpened();
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws IOException {
        getMinaHandler(session).connectionClosed();
        getMinaSession(session).close();
        stats.activeSessions.decrementAndGet();
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws IOException{
        getMinaHandler(session).connectionIdle();
    }

    @Override
    public void messageReceived(IoSession session, Object msg) throws IOException {
        getMinaHandler(session).messageReceived(msg);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable e) throws IOException {
        getMinaHandler(session).connectionClosed();
    }

    @Override
    public void messageSent(IoSession session, Object msg) {
        if (msg instanceof IoBuffer) {
            IoBuffer buf = (IoBuffer) msg;
            stats.sentBytes.addAndGet(buf.remaining());
            getMinaSession(session).messageSent();
        }
    }

    public static MinaHandler getMinaHandler(IoSession session) {
        return (MinaHandler) session.getAttribute(MINA_HANDLER);
    }

    private static MinaSession getMinaSession(IoSession session) {
        return (MinaSession) session.getAttribute(MINA_SESSION);
    }
}
