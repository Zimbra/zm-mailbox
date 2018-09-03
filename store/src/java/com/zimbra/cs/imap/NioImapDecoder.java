/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

import com.google.common.base.Charsets;
import com.google.common.primitives.Ints;

/**
 * Protocol Decoder for IMAP. This decodes a text line terminated by LF or CRLF into a string, and an IMAP literal into
 * a byte array.
 *
 * @author ysasaki
 */
final class NioImapDecoder extends CumulativeProtocolDecoder {

    private final ImapConfig config;

    NioImapDecoder(ImapConfig config) {
        this.config = config;
    }

    /**
     * Decode a line or literal from the cumulative buffer.
     *
     * @throws TooLongLineException maximum line length exceeded
     * @throws TooBigLiteralException maximum literal size exceeded
     * @throws InvalidLiteralFormatException bad literal format
     * @throws IOException socket I/O error
     */
    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws ProtocolDecoderException, IOException, Exception {
        /** the allowed maximum size of a literal to be decoded. If the size of the literal to be
         * decoded exceeds this value, the decoder will throw a {@link TooBigLiteralException}.
         */
        long maxLiteralSize = config.getMaxMessageSize();
        int maxChunkSize = config.getWriteChunkSize();
        /** the allowed maximum size of a line to be decoded.
         * If the size of the line to be decoded exceeds this value, the decoder will throw a
         * {@link TooLongLineException}.
         */
        int maxLineLength = config.getMaxRequestSize();
        int start = in.position(); // remember the initial position
        Context ctx = (Context) session.getAttribute(Context.class);
        if (ctx == null) {
            ctx = new Context();
            session.setAttribute(Context.class, ctx);
        }
        byte prev = -1;

        while (in.hasRemaining()) {
            if (ctx.literal >= 0) {
                int len = Ints.min(in.remaining(), ctx.literal, maxChunkSize);
                if (ctx.overflow) { // swallow non-blocking literal
                    in.skip(len);
                } else {
                    byte[] chunk = new byte[len];
                    in.get(chunk);
                    out.write(chunk);
                }
                ctx.literal -= len;
                if (ctx.literal == 0) { // end of literal
                    ctx.literal = -1;
                    if (ctx.overflow) {
                        ctx.overflow = false;
                        dispose(session);
                        throw new TooBigLiteralException(ctx.request);
                    }
                }
                return true;
            } else {
                if (in.position() - start > maxLineLength) {
                    ctx.overflow = true;
                }
                if (ctx.overflow) {
                    if (in.get() == '\n') {
                        ctx.overflow = false;
                        throw new TooLongLineException();
                    } else {
                        return true; // swallow
                    }
                } else {
                    byte b = in.get();
                    if (b == '\n') {
                        int pos = in.position();
                        int limit = in.limit();
                        in.position(start);
                        in.limit(prev == '\r' ? pos - 2 : pos - 1); // Swallow the previous CR
                        // The bytes between in.position() and in.limit() now contain a full CRLF terminated line.
                        String line = in.getString(Charsets.ISO_8859_1.newDecoder());
                        // Set the position to point right after the detected line and set the limit to the old one.
                        in.limit(limit);
                        in.position(pos);
                        LiteralInfo li;
                        try {
                            li = LiteralInfo.parse(line);
                        } catch (IllegalArgumentException e) {
                            dispose(session);
                            throw new InvalidLiteralFormatException();
                        }
                        if (li != null && li.count > 0) { // ignore empty literal
                            if (maxLiteralSize >= 0 && li.count > maxLiteralSize) {
                                if (li.isBlocking()) { // return a negative continuation response
                                    dispose(session);
                                    throw new TooBigLiteralException(line);
                                } else { // non-blocking, swallow the entire literal
                                    ctx.literal = li.count;
                                    ctx.request = line;
                                    ctx.overflow = true;
                                    return true;
                                }
                            }
                            ctx.literal = li.count;
                            ctx.request = line;
                        }
                        out.write(line);
                        // Decoded one line. CumulativeProtocolDecoder will call me again until I return false.
                        // So just return true until there are no more lines in the buffer.
                        return true;
                    } else {
                        prev = b;
                    }
                }
            }
        }
        // Could not find EOL in the buffer. Reset the initial position to the one we recorded above.
        in.position(start);
        return false;
    }

    private static final class Context {
        private boolean overflow = false;
        private int literal = -1;
        private String request;
    }

    static final class TooLongLineException extends RecoverableProtocolDecoderException {
        private static final long serialVersionUID = -7284634822713651847L;

        @Override
        public String getMessage() {
            return "maximum line length exceeded";
        }
    }

    protected static final class TooBigLiteralException extends RecoverableProtocolDecoderException {
        private static final long serialVersionUID = 4272855594291614583L;
        private static final String maxLiteralSizeExceeded = "maximum literal size exceeded";
        private static final String maxMessageSizeExceeded = "maximum message size exceeded";

        /* The request string - e.g. "A02 LOGIN fred test123" */
        private final String request;
        private String requestTag = null;
        private String imapCmd = null;

        protected TooBigLiteralException(String req) {
            request = req;
        }

        /** @return the buffer containing the IMAP command processed so far*/
        public String getRequest() {
            return request;
        }

        /** @return tag for IMAP command */
        public String getRequestTag() {
            if (requestTag != null) {
                return requestTag;
            }
            try {
                requestTag = ImapRequest.parseTag(request);
            } catch (ImapParseException e1) {
                requestTag = "*";
            }
            return requestTag;
        }

        /** @return tag for IMAP command */
        public String getCommand() {
            if (imapCmd != null) {
                return imapCmd;
            }
            imapCmd = ImapRequest.getCommand(request);
            return imapCmd;
        }

        @Override
        public String getMessage() {
            if ("APPEND".equalsIgnoreCase(getCommand())) {
                /* Only one literal in APPEND cmd & this is a friendlier msg */
                return maxMessageSizeExceeded;
            }
            return maxLiteralSizeExceeded;
        }
    }

    static final class InvalidLiteralFormatException extends ProtocolDecoderException {
        private static final long serialVersionUID = 7224987995979809371L;

        @Override
        public String getMessage() {
            return "invalid literal format";
        }
    }

}
