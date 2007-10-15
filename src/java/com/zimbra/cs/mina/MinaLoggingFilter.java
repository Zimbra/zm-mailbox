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

import com.zimbra.common.util.Log;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import java.nio.ByteBuffer;

/**
 * Optionally logs all MINA protocol events to the server log.
 */
class MinaLoggingFilter extends IoFilterAdapter {
    private final Log mLog;
    private final boolean mHexDump;

    MinaLoggingFilter(MinaServer server, boolean hexDump) {
        mLog = server.getLog();
        mHexDump = hexDump;
    }
    
    public void sessionCreated(NextFilter nextFilter, IoSession session) {
        log(session, "CREATED");
        nextFilter.sessionCreated(session);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        log(session, "OPENED");
        nextFilter.sessionOpened(session);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        log(session, "CLOSED");
        nextFilter.sessionClosed(session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session,
                            IdleStatus status) {
        if (isLoggable()) log(session, "IDLE: " + status);
        nextFilter.sessionIdle(session, status);
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session,
                                Throwable cause) {
        if (isLoggable()) log(session, "EXCEPTION:", cause);
        nextFilter.exceptionCaught(session, cause);
    }

    public void messageReceived(NextFilter nextFilter, IoSession session,
                                Object message) {
        if (isLoggable()) log(session, "RECEIVED: " + pp(message));
        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session,
                            Object message) {
        if (isLoggable()) log(session, "SENT: " + pp(message));
        nextFilter.messageSent(session, message);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session,
                            WriteRequest writeRequest) {
        if (isLoggable()) log(session, "WRITE: " + pp(writeRequest));
        nextFilter.filterWrite(session, writeRequest);
    }

    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        log(session, "CLOSE");
        nextFilter.filterClose(session);
    }

    private Object pp(Object msg) {
        if (msg instanceof WriteRequest) {
            return pp(((WriteRequest) msg).getMessage());
        }
        ByteBuffer bb;
        if (msg instanceof ByteBuffer) {
            bb = (ByteBuffer) msg;
        } else if (msg instanceof org.apache.mina.common.ByteBuffer) {
            bb = ((org.apache.mina.common.ByteBuffer) msg).buf();
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
            sb = MinaUtil.appendHex(sb.append('('), bb).append(") ");
        }
        int limit = bb.limit();
        for (int i = bb.position(); i < limit; i++) {
            sb.append((char) (bb.get(i) & 0xff));
        }
        return sb.toString();
    }

    // TODO Should this log at debug level instead?
    private boolean isLoggable() {
        return mLog.isInfoEnabled();
    }

    private void log(IoSession session, String msg, Throwable e) {
        if (isLoggable()) mLog.info("%s %s", session.getRemoteAddress(), msg, e);
    }

    private void log(IoSession session, String msg) {
        log(session, msg, null);
    }
}
