/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolDecoderException;

import com.google.common.base.CharMatcher;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

/**
 * Optionally logs all MINA protocol events to the server log.
 */
final class NioLoggingFilter extends IoFilterAdapter {
    private final Log log;
    private final boolean hexDump;

    NioLoggingFilter(NioServer server, boolean hexDump) {
        log = server.getLog();
        this.hexDump = hexDump;
    }

    @Override
    public void exceptionCaught(NextFilter next, IoSession session, Throwable cause) {
        NioHandlerDispatcher.getHandler(session).setLoggingContext();
        try {
            if (cause instanceof IOException || cause instanceof ProtocolDecoderException) {
                // intend to ignore "Connection reset by peer" and "Broken pipe"
                log.debug(cause, cause);
            } else {
                log.error(cause, cause);
            }
            next.exceptionCaught(session, cause);
        } finally {
            ZimbraLog.clearContext();
        }
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) {
        NioHandlerDispatcher.getHandler(session).setLoggingContext();
        try {
            if (log.isTraceEnabled()) {
                log.trace("C: %s", pp(message));
            }
            nextFilter.messageReceived(session, message);
        } finally {
            ZimbraLog.clearContext();
        }
    }

    @Override
    public void filterWrite(NextFilter next, IoSession session, WriteRequest req) {
        if (log.isTraceEnabled()) {
            log.trace("S: %s", pp(req));
        }
        next.filterWrite(session, req);
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
        if (hexDump) {
            sb = NioUtil.appendHex(sb.append('('), bb).append(") ");
        }
        int limit = bb.limit();
        for (int i = bb.position(); i < limit; i++) {
            sb.append((char) (bb.get(i) & 0xff));
        }
        return CharMatcher.anyOf("\r\n").trimTrailingFrom(sb);
    }

}
