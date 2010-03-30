/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.nio.NioUtil;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.net.SocketException;

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
        debug(session, "Session created");
        nextFilter.sessionCreated(session);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session) {
        debug(session, "Connection opened");
        nextFilter.sessionOpened(session);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session) {
        debug(session, "Connection closed");
        nextFilter.sessionClosed(session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session,
                            IdleStatus status) {
        if (mLog.isDebugEnabled()) {
            debug(session, "Connection idle: " + status);
        }
        nextFilter.sessionIdle(session, status);
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session,
                                Throwable cause) {
        ZimbraLog.addIpToContext(session.getRemoteAddress().toString());
        String msg = "Exception caught: " + cause;
        if (isSocketError(cause)) {
            // If connection error, then only log full stack trace if debug enabled
            if (mLog.isDebugEnabled()) {
                mLog.info(msg, cause);
            } else {
                mLog.info(msg);
            }
        } else {
            mLog.error(msg, cause);
        }
        nextFilter.exceptionCaught(session, cause);
    }

    private static boolean isSocketError(Throwable e) {
        return e instanceof SocketException || e instanceof SSLException;
    }

    public void messageReceived(NextFilter nextFilter, IoSession session,
                                Object message) {
        if (mLog.isDebugEnabled()) {
            debug(session, "Message received: " + pp(message));
        }
        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session,
                            Object message) {
        if (mLog.isDebugEnabled()) {
            debug(session, "Message sent: " + pp(message));
        }
        nextFilter.messageSent(session, message);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session,
                            WriteRequest writeRequest) {
        if (mLog.isDebugEnabled()) {
            debug(session, "Message written: " + pp(writeRequest));
        }
        nextFilter.filterWrite(session, writeRequest);
    }

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
        } else if (msg instanceof org.apache.mina.common.ByteBuffer) {
            bb = ((org.apache.mina.common.ByteBuffer) msg).buf();
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
        if (mLog.isDebugEnabled()) {
            ZimbraLog.addIpToContext(session.getRemoteAddress().toString());
            mLog.debug(msg);
        }
    }
}
