/*
 * ***** BEGIN LICENSE BLOCK *****
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
        info(session, "CREATED");
        nextFilter.sessionCreated(session);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        info(session, "OPENED");
        nextFilter.sessionOpened(session);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        info(session, "CLOSED");
        nextFilter.sessionClosed(session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session,
                            IdleStatus status) {
        if (isInfo()) {
            info(session, "IDLE: " + status);
        }
        nextFilter.sessionIdle(session, status);
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session,
                                Throwable cause) {
        if (isInfo()) {
            info(session, "EXCEPTION: " + cause, cause);
        }
        nextFilter.exceptionCaught(session, cause);
    }

    public void messageReceived(NextFilter nextFilter, IoSession session,
                                Object message) {
        if (isDebug()) {
            debug(session, "RECEIVED: " + pp(message));
        }
        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session,
                            Object message) {
        if (isDebug()) {
            debug(session, "SENT: " + pp(message));
        }
        nextFilter.messageSent(session, message);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session,
                            WriteRequest writeRequest) {
        if (isDebug()) {
            debug(session, "WRITE: " + pp(writeRequest));
        }
        nextFilter.filterWrite(session, writeRequest);
    }

    public void filterClose(NextFilter nextFilter, IoSession session)
            throws Exception {
        info(session, "CLOSE");
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

    private boolean isDebug() {
        return mLog.isDebugEnabled();
    }

    private boolean isInfo() {
        return mLog.isInfoEnabled();
    }

    private void info(IoSession session, String msg) {
        info(session, msg, null);
    }
    
    private void info(IoSession session, String msg, Throwable e) {
        // bug 20632: only show stack trace if debug level
        if (isDebug()) {
            mLog.info(fmt(session, msg), e);
        } else if (isInfo()) {
            mLog.info(fmt(session, msg));
        }
    }

    private void debug(IoSession session, String msg) {
        debug(session, msg, null);
    }
    
    private void debug(IoSession session, String msg, Throwable e) {
        if (isInfo()) {
            mLog.debug(fmt(session, msg));
        }
    }
    
    private String fmt(IoSession session, String msg) {
        return String.format("[%s] %s", session.getRemoteAddress(), msg);
    }
}
