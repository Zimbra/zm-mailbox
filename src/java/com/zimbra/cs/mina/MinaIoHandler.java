package com.zimbra.cs.mina;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IdleStatus;

import java.io.IOException;

/**
 * Handler for MINA I/O events.
 */
class MinaIoHandler implements IoHandler {
    private MinaServer server;

    private static final String PROTOCOL_HANDLER = "ProtocolHandler";

    MinaIoHandler(MinaServer server) {
        this.server = server;
    }

    public void sessionCreated(IoSession session) {
    }

    public void sessionOpened(IoSession session) throws IOException {
        server.getLog().debug("sessionOpened: %s", session);
        MinaHandler handler = server.createHandler(session);
        handler.connectionOpened();
        session.setAttribute(PROTOCOL_HANDLER, handler);
    }

    public void sessionClosed(IoSession session) throws IOException {
        server.getLog().debug("sessionClosed: session %s", session);
        getHandler(session).connectionClosed();
    }

    public void sessionIdle(IoSession session, IdleStatus status)
            throws IOException{
        server.getLog().debug("sessionIdle: session = %s, status = %s",
                              session, status);
        getHandler(session).connectionIdle();
    }

    public void messageReceived(IoSession session, Object msg)
            throws IOException {
        assert msg instanceof MinaRequest;
        server.getLog().debug("messageReceived: session = %s, message = %s",
                               session, msg);
        getHandler(session).requestReceived((MinaRequest) msg);
    }

    public void exceptionCaught(IoSession session, Throwable e)
            throws IOException {
        server.getLog().debug("exceptionCaught: session = %s", session, e);
        getHandler(session).connectionClosed();
    }

    public void messageSent(IoSession session, Object obj) {
        // TODO
    }

    public static MinaHandler getHandler(IoSession session) {
        return (MinaHandler) session.getAttribute(PROTOCOL_HANDLER);
    }
}
