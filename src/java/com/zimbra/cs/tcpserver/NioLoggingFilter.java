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

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import javax.net.ssl.SSLException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteException;
import org.apache.mina.core.write.WriteRequest;

import java.nio.ByteBuffer;
import java.net.SocketException;

/**
 * Optionally logs all MINA protocol events to the server log.
 */
class NioLoggingFilter extends IoFilterAdapter {
    private final Log log;
    private final boolean mHexDump;

    NioLoggingFilter(NioServer server, boolean hexDump) {
        log = server.getLog();
        mHexDump = hexDump;
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session) {
        debug(session, "Session created");
        nextFilter.sessionCreated(session);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        debug(session, "Connection opened");
        nextFilter.sessionOpened(session);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        debug(session, "Connection closed");
        nextFilter.sessionClosed(session);
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) {
        if (log.isDebugEnabled()) {
            debug(session, "Connection idle: " + status);
        }
        nextFilter.sessionIdle(session, status);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) {
        // Use NioConnection.getRemoteAddress() because IoSession.getRemoteAddress() can be null.
        ZimbraLog.addIpToContext(IoHandlerImpl.getConnection(session).getRemoteAddress().toString());
        String msg = "Exception caught: " + cause;
        if (isSocketError(cause)) {
            // If connection error, then only log full stack trace if debug enabled
            if (log.isDebugEnabled()) {
                log.info(msg, cause);
            } else {
                log.info(msg);
            }
        } else {
            log.error(msg, cause);
        }
        nextFilter.exceptionCaught(session, cause);
    }

    private static boolean isSocketError(Throwable e) {
        return e instanceof SocketException || e instanceof SSLException || e instanceof WriteException;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) {
        if (log.isTraceEnabled()) {
            trace(session, "C: %s", pp(message));
        }
        nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest message) {
        if (log.isTraceEnabled()) {
            trace(session, "S: %s", pp(message));
        }
        nextFilter.messageSent(session, message);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,  WriteRequest writeRequest) {
        nextFilter.filterWrite(session, writeRequest);
    }

    @Override
    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        debug(session, "Connection closed by client");
        nextFilter.filterClose(session);
    }

    private Object pp(Object msg) {
        if (msg instanceof WriteRequest) {
            return pp(((WriteRequest) msg).getMessage());
        }
        if (msg instanceof String) {
            return msg;
        }
        ByteBuffer bb;
        if (msg instanceof ByteBuffer) {
            bb = (ByteBuffer) msg;
        } else if (msg instanceof IoBuffer) {
            bb = ((IoBuffer) msg).buf();
        } else if (msg instanceof byte[]) {
            bb = ByteBuffer.wrap((byte[]) msg);
        } else {
            return msg;
        }
        return isPrintable(bb) ? pp(bb) : msg;
    }

    private boolean isPrintable(ByteBuffer bb) {
        int limit = bb.limit();
        for (int i = bb.position(); i < limit; i++) {
            if (!isPrintable(bb.get(i))) return false;
        }
        return true;
    }

    private static boolean isPrintable(byte b) {
        switch (b) {
        case '\r': case '\n': case '\t':
            return true;
        default:
            return b > 31 && b < 127;
        }
    }

    private String pp(ByteBuffer bb) {
        StringBuilder sb = new StringBuilder(bb.remaining());
        if (mHexDump) {
            sb = NioUtil.appendHex(sb.append('('), bb).append(") ");
        }
        int limit = bb.limit();
        for (int i = bb.position(); i < limit; i++) {
            sb.append((char) (bb.get(i) & 0xff));
        }
        return sb.toString();
    }

    private void debug(IoSession session, String msg) {
        if (log.isDebugEnabled()) {
            ZimbraLog.addIpToContext(session.getRemoteAddress().toString());
            log.debug(msg);
        }
    }

    private void trace(IoSession session, String format, Object arg) {
        ZimbraLog.addIpToContext(session.getRemoteAddress().toString());
        log.trace(format, arg);
    }

}
