package com.zimbra.cs.imap;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;

import java.io.IOException;

import com.zimbra.common.util.ZimbraLog;

/**
 * Handler for MINA IO events. Events are processed sequentially with respect
 * to each IoSession (i.e. connection) which simplifies the concurrency
 * model.
 */
public class MinaImapIoHandler implements IoHandler {
    private MinaImapServer server;

    private static final String IMAP_HANDLER = "ImapHandler";
    
    MinaImapIoHandler(MinaImapServer server) {
        this.server = server;
    }

    public void sessionCreated(IoSession session) {
    }

    public void sessionOpened(IoSession session) throws IOException {
        ZimbraLog.imap.debug("sessionOpened: %s", session);
        MinaImapHandler handler = new MinaImapHandler(server, session);
        if (!handler.handleConnect()) {
            // TODO Find a more graceful way to refuse connection
            throw new IOException("Connection refused");
        }
        session.setAttribute(IMAP_HANDLER, handler);

    }

    public void sessionClosed(IoSession session) {
        ZimbraLog.imap.debug("sessionClosed: session %s", session);
        getImapHandler(session).dropConnection();
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
        ZimbraLog.imap.debug("sessionIdle: session = %s, status = %s",
                             session, status);
        getImapHandler(session).notifyIdleConnection();
    }
    
    public void messageReceived(IoSession session, Object msg)
            throws IOException {
        assert msg instanceof MinaImapRequest;
        ZimbraLog.imap.debug("messageReceived: session = %s, message = %s",
                             session, msg);
        getImapHandler(session).handleRequest((MinaImapRequest) msg);
    }

    public void exceptionCaught(IoSession session, Throwable e) {
        ZimbraLog.imap.debug("exceptionCaught: session = %s", session, e);
        e.printStackTrace();
        getImapHandler(session).dropConnection();
    }

    public void messageSent(IoSession session, Object obj) {
        // TODO
    }
    
    public static MinaImapHandler getImapHandler(IoSession session) {
        MinaImapHandler handler = (MinaImapHandler)
            session.getAttribute(IMAP_HANDLER);
        assert handler != null;
        return handler;
    }
}
