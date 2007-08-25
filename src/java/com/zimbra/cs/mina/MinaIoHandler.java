/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

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
    private MinaServer mServer;

    private static final String PROTOCOL_HANDLER = "ProtocolHandler";

    MinaIoHandler(MinaServer server) {
        this.mServer = server;
    }

    public void sessionCreated(IoSession session) {
        // Nothing to do here...
    }

    public void sessionOpened(IoSession session) throws IOException {
        MinaHandler handler = mServer.createHandler(session);
        handler.connectionOpened();
        session.setAttribute(PROTOCOL_HANDLER, handler);
    }

    public void sessionClosed(IoSession session) throws IOException {
        getHandler(session).connectionClosed();
    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws IOException{
        getHandler(session).connectionIdle();
    }

    public void messageReceived(IoSession session, Object msg)
            throws IOException {
        assert msg instanceof MinaRequest;
        getHandler(session).requestReceived((MinaRequest) msg);
    }

    public void exceptionCaught(IoSession session, Throwable e)
            throws IOException {
        getHandler(session).connectionClosed();
    }

    public void messageSent(IoSession session, Object msg) {
        // Nothing to do here...
    }

    public static MinaHandler getHandler(IoSession session) {
        return (MinaHandler) session.getAttribute(PROTOCOL_HANDLER);
    }
}
