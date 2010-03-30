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

package com.zimbra.cs.nio.mina;

import com.zimbra.cs.nio.NioHandler;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;

import java.io.IOException;

class MinaIoHandler implements IoHandler {
    private final MinaServer server;

    private static final String MINA_SESSION = "MinaSession";
    private static final String NIO_HANDLER = "NioHandler";

    MinaIoHandler(MinaServer server) {
        this.server = server;
    }

    public void sessionCreated(IoSession session) throws IOException {
        int idleTime = server.getConfig().getMaxIdleSeconds();
        if (idleTime > 0) {
            session.setIdleTime(IdleStatus.BOTH_IDLE, idleTime);
        }
        MinaSession minaSession = new MinaSession(server, session);
        session.setAttribute(MINA_SESSION, minaSession);
        session.setAttribute(NIO_HANDLER, server.newHandler(minaSession));
    }

    public void sessionOpened(IoSession session) throws IOException {
        nioHandler(session).connectionOpened();
        server.getStats().sessionOpened();
    }

    public void sessionClosed(IoSession session) throws IOException {
        nioHandler(session).connectionClosed();
        server.getStats().sessionClosed();
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws IOException{
        nioHandler(session).connectionIdle();
    }

    public void messageReceived(IoSession session, Object msg) throws IOException {
        nioHandler(session).messageReceived(msg);
    }

    public void exceptionCaught(IoSession session, Throwable e) throws IOException {
        nioHandler(session).connectionClosed();
    }

    public void messageSent(IoSession session, Object msg) {
        if (msg instanceof ByteBuffer) {
            int size = ((ByteBuffer) msg).remaining();
            minaSession(session).messageSent(size);
        }
    }

    private static MinaSession minaSession(IoSession session) {
        return (MinaSession) session.getAttribute(MINA_SESSION);
    }
    
    public static NioHandler nioHandler(IoSession session) {
        return (NioHandler) session.getAttribute(NIO_HANDLER);
    }
}
