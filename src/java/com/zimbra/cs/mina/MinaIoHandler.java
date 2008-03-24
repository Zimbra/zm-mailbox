/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;

import java.io.IOException;

import com.zimbra.common.util.ZimbraLog;

/**
 * Handler for MINA I/O events. Responsible for notifying the connection's
 * MinaHandler when a connection has been opened, closed, become idle, or a
 * new request has been received.
 */
class MinaIoHandler implements IoHandler {
    private MinaServer mServer;

    private static final String PROTOCOL_HANDLER = "ProtocolHandler";

    MinaIoHandler(MinaServer server) {
        this.mServer = server;
    }

    public void sessionCreated(IoSession session) {
        if (MinaServer.isDebugEnabled()) {
            ZimbraLog.nio.debug("sessionCreated: session = %d", session.hashCode());
        }
        // Nothing to do here...
    }

    public void sessionOpened(IoSession session) throws IOException {
        if (MinaServer.isDebugEnabled()) {
            ZimbraLog.nio.debug("sessionOpened: session = %d", session.hashCode());
        }
        MinaHandler handler = mServer.createHandler(session);
        handler.connectionOpened();
        session.setAttribute(PROTOCOL_HANDLER, handler);
    }

    public void sessionClosed(IoSession session) throws IOException {
        if (MinaServer.isDebugEnabled()) {
            ZimbraLog.nio.debug("sessionClosed: session = %d", session.hashCode());
        }
        getHandler(session).connectionClosed();
    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws IOException{
        if (MinaServer.isDebugEnabled()) {
            ZimbraLog.nio.debug("sessionIdle: session = %d", session.hashCode());
        }
        getHandler(session).connectionIdle();
    }

    public void messageReceived(IoSession session, Object msg)
            throws IOException {
        assert msg instanceof MinaRequest;
        if (MinaServer.isDebugEnabled()) {
            ZimbraLog.nio.debug("messageReceived: session = %d", session.hashCode());
        }
        getHandler(session).requestReceived((MinaRequest) msg);
    }

    public void exceptionCaught(IoSession session, Throwable e)
            throws IOException {
        if (MinaServer.isDebugEnabled()) {
            ZimbraLog.nio.debug("exceptionCaught: session = %d", session.hashCode());
        }
        getHandler(session).connectionClosed();
    }

    public void messageSent(IoSession session, Object msg) {
        // Nothing to do here...
    }

    public static MinaHandler getHandler(IoSession session) {
        return (MinaHandler) session.getAttribute(PROTOCOL_HANDLER);
    }
}
