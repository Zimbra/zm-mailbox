/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;

import com.google.common.base.Charsets;
import com.google.common.primitives.Ints;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * Protocol Decoder for IMAP. This decodes a text line terminated by LF or CRLF into a string, and an IMAP literal into
 * a byte array.
 *
 * @author ysasaki
 */
final class NioImapDecoder extends CumulativeProtocolDecoder {
    private static final CharsetDecoder CHARSET = Charsets.ISO_8859_1.newDecoder();

    private final int chunkSize;

    NioImapDecoder(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws IOException {
        int start = in.position(); // remember the initial position
        int literal = (Integer) session.getAttribute(getClass(), -1);
        byte prev = -1;

        while (in.hasRemaining()) {
            if (literal >= 0) {
                int len = Ints.min(in.remaining(), literal, chunkSize);
                byte[] chunk = new byte[len];
                in.get(chunk);
                try {
                    out.write(chunk);
                } finally {
                    literal -= len;
                    if (literal == 0) {
                        session.removeAttribute(getClass());
                    } else {
                        session.setAttribute(getClass(), literal);
                    }
                }
                return true;
            } else {
                byte b = in.get();
                if (b == '\n') {
                    int pos = in.position();
                    int limit = in.limit();
                    try {
                        in.position(start);
                        in.limit(prev == '\r' ? pos - 2 : pos - 1); // Swallow the previous CR
                        // The bytes between in.position() and in.limit() now contain a full CRLF terminated line.
                        String line = in.getString(CHARSET);
                        LiteralInfo li = LiteralInfo.parse(line);
                        if (li != null) {
                            session.setAttribute(getClass(), li.count);
                        }
                        out.write(line);
                    } finally {
                        // Set the position to point right after the detected line and set the limit to the old one.
                        in.limit(limit);
                        in.position(pos);
                    }
                    // Decoded one line. CumulativeProtocolDecoder will call me again until I return false.
                    // So just return true until there are no more lines in the buffer.
                    return true;
                } else {
                    prev = b;
                }
            }
        }
        // Could not find EOL in the buffer. Reset the initial position to the one we recorded above.
        in.position(start);
        return false;
    }

}
