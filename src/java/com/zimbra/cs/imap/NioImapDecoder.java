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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

/**
 * Protocol Decoder for IMAP. This decodes a text line terminated by LF or CRLF into a string, and an IMAP literal into
 * a byte array.
 *
 * @author ysasaki
 */
final class NioImapDecoder extends CumulativeProtocolDecoder {

    private int maxChunkSize = 1024;
    private int maxLineLength = 1024;
    private long maxLiteralSize = -1L;

    void setMaxChunkSize(int bytes) {
        Preconditions.checkArgument(bytes > 0);
        maxChunkSize = bytes;
    }

    /**
     * Sets the allowed maximum size of a line to be decoded. If the size of the line to be decoded exceeds this
     * value, the decoder will throw a {@link TooLongLineException}. The default value is 1024 (1KB).
     *
     * @param value max line length in bytes
     */
    void setMaxLineLength(int bytes) {
        Preconditions.checkArgument(bytes > 0);
        maxLineLength = bytes;
    }

    /**
     * Sets the allowed maximum size of a literal to be decoded. If the size of the literal to be decoded exceeds this
     * value, the decoder will throw a {@link TooBigLiteralException}. The default is unlimited.
     *
     * @param bytes max literal size in bytes
     */
    void setMaxLiteralSize(long bytes) {
        Preconditions.checkArgument(bytes > 0);
        maxLiteralSize = bytes;
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
            throws ProtocolDecoderException, IOException {
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
                            throw new InvalidLiteralFormatException();
                        }
                        if (li != null && li.count > 0) { // ignore empty literal
                            if (maxLiteralSize >= 0 && li.count > maxLiteralSize) {
                                if (li.isBlocking()) { // return a negative continuation response
                                    throw new TooBigLiteralException(line);
                                } else { // non-blocking, swallow the entire literal
                                    ctx.literal = li.count;
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
        boolean overflow = false;
        int literal = -1;
        String request;
    }

    static final class TooLongLineException extends RecoverableProtocolDecoderException {
        private static final long serialVersionUID = -7284634822713651847L;

        @Override
        public String getMessage() {
            return "maximum line length exceeded";
        }
    }

    static final class TooBigLiteralException extends RecoverableProtocolDecoderException {
        private static final long serialVersionUID = 4272855594291614583L;

        private String request;

        private TooBigLiteralException(String req) {
            request = req;
        }

        public String getRequest() {
            return request;
        }

        @Override
        public String getMessage() {
            return "maximum literal size exceeded";
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
