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
package com.zimbra.cs.imap;

import com.zimbra.common.io.TcpServerInputStream;
import com.zimbra.common.util.ZimbraLog;

import java.io.IOException;

final class TcpImapRequest extends ImapRequest {
    final class ImapTerminatedException extends ImapParseException {
        private static final long serialVersionUID = 6105950126307803418L;
    }

    final class ImapContinuationException extends ImapParseException {
        private static final long serialVersionUID = 7925400980773927177L;
        boolean sendContinuation;
        ImapContinuationException(boolean send)  { super(); sendContinuation = send; }
    }

    private TcpServerInputStream input;
    private long literalCounter = -1;
    private boolean unlogged;
    private long requestSize = 0;
    private boolean maxRequestSizeExceeded = false;

    TcpImapRequest(String line, ImapHandler handler) {
        super(handler);
        addPart(line);
    }

    TcpImapRequest(TcpServerInputStream input, ImapHandler handler) {
        super(handler);
        this.input = input;
    }

    void continuation() throws IOException, ImapParseException {
        if (literalCounter >= 0) {
            continueLiteral();
        }

        String line = input.readLine();
        String logline = line;
        // TcpServerInputStream.readLine() returns null on end of stream!
        if (line == null)
            throw new ImapTerminatedException();
        incrementSize(line.length());
        addPart(line);

        if (parts.size() == 1 && !maxRequestSizeExceeded) {
            // check for "LOGIN" command and elide if necessary
            unlogged = isLogin();
            if (unlogged) {
                logline = line.substring(0, line.indexOf(' ') + 7) + "...";
            }
        }

        ZimbraLog.imap.trace("C: %s", logline);

        // if the line ends in a LITERAL+ non-blocking literal, keep reading
        if (line.endsWith("+}") && extensionEnabled("LITERAL+")) {
            int openBrace = line.lastIndexOf('{', line.length() - 3);
            if (openBrace >= 0) {
                long size;
                try {
                    size = Long.parseLong(line.substring(openBrace + 1, line.length() - 2));
                } catch (NumberFormatException nfe) {
                    size = -1;
                }
                if (size >= 0) {
                    if (!isAppend()) {
                        incrementSize(size);
                    }
                    literalCounter = size;
                    continuation();
                } else {
                    if (tag == null && index == 0 && offset == 0) {
                        tag = readTag();
                        rewind();
                    }
                    throw new ImapParseException(tag, "malformed nonblocking literal");
                }
            }
        }
    }

    private void continueLiteral() throws IOException, ImapParseException {
        if (maxRequestSizeExceeded) {
            long skipped = input.skip(literalCounter);
            if (literalCounter > 0 && skipped == 0) {
                throw new ImapTerminatedException();
            }
            literalCounter -= skipped;
        } else {
            Part part = parts.get(parts.size() - 1);
            Literal literal;
            if (part.isLiteral()) {
                literal = part.getLiteral();
            } else {
                literal = Literal.newInstance((int) literalCounter, isAppend());
                addPart(literal);
            }
            int read = literal.copy(input);
            if (read == -1)
                throw new ImapTerminatedException();
            // TODO How to log literal data now...
            if (!unlogged && ZimbraLog.imap.isTraceEnabled()) {
                ZimbraLog.imap.trace("C: {%s}", read);
            }
            literalCounter -= read;
        }
        if (literalCounter > 0) {
            throw new ImapContinuationException(false);
        }
        literalCounter = -1;
    }

    private Literal getCurrentBuffer() throws ImapParseException {
        return parts.get(index).getLiteral();
    }

    @Override
    protected Literal readLiteral() throws IOException, ImapParseException {
        boolean blocking = true;
        skipChar('{');
        long length = Long.parseLong(readNumber());
        if (peekChar() == '+' && extensionEnabled("LITERAL+")) {
            skipChar('+');  blocking = false;
        }
        skipChar('}');

        // make sure that the literal came at the very end of a line
        if (getCurrentLine().length() != offset) {
            throw new ImapParseException(tag, "extra characters after literal declaration");
        }
        boolean lastPart = (index == parts.size() - 1);
        if (lastPart || (index == parts.size() - 2 && literalCounter != -1)) {
            if (literalCounter == -1) {
                if (!isAppend()) {
                    incrementSize(length);
                }
                literalCounter = length;
            }
            if (!blocking && input.available() >= literalCounter) {
                continuation();
            } else {
                throw new ImapContinuationException(blocking && lastPart);
            }
        }
        index++;
        Literal result = getCurrentBuffer();
        index++;
        offset = 0;
        return result;
    }

    void incrementSize(long increment) {
        requestSize += increment;
        if (requestSize > mHandler.config.getMaxRequestSize()) {
            maxRequestSizeExceeded = true;
        }
    }

    boolean isMaxRequestSizeExceeded() {
        return maxRequestSizeExceeded;
    }

    /**
     * This implementation doesn't add any more parts if we have exceeded the maximum request size. The exception is if
     * this is the first part (request line) so we can recover the tag when sending an error response.
     */
    @Override
    void addPart(Part part) {
        if (!maxRequestSizeExceeded || parts.isEmpty()) {
            super.addPart(part);
        }
    }

}
