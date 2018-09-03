/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.io.TcpServerInputStream;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapParseException.ImapMaximumSizeExceededException;

final class TcpImapRequest extends ImapRequest {
    private final TcpServerInputStream input;
    private long literalCounter = -1;
    private boolean unlogged;
    private long requestSize = 0;
    private boolean maxRequestSizeExceeded = false;

    final class ImapTerminatedException extends ImapParseException {
        private static final long serialVersionUID = 6105950126307803418L;
    }

    final class ImapContinuationException extends ImapParseException {
        private static final long serialVersionUID = 7925400980773927177L;
        protected boolean sendContinuation;
        ImapContinuationException(boolean send)  { super(); sendContinuation = send; }
    }

    protected TcpImapRequest(TcpServerInputStream input, ImapHandler handler) {
        super(handler);
        this.input = input;
    }

    private void checkSize(long size) throws ImapParseException {
        int maxLiteralSize = Integer.MAX_VALUE;
        if (isAppend()) {
            try {
                long msgLimit = mHandler.getConfig().getMaxMessageSize();
                if ((msgLimit != 0 /* 0 means unlimited */) && (msgLimit < maxLiteralSize)
                        && (size > msgLimit)) {
                    throwSizeExceeded("message");
                }
            } catch (ServiceException se) {
                ZimbraLog.imap.warn("unable to check zimbraMtaMaxMessageSize", se);
            }
        }
        if (isMaxRequestSizeExceeded() || size > maxLiteralSize) {
            throwSizeExceeded("request");
        }
    }

    private void throwSizeExceeded(String exceededType) throws ImapParseException {
        if (tag == null && index == 0 && offset == 0) {
            tag = readTag(); rewind();
        }
        throw new ImapMaximumSizeExceededException(tag, exceededType);
    }

    protected void continuation() throws IOException, ImapParseException {
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
                    checkSize(size);
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
                checkSize(length);
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

    protected void incrementSize(long increment) {
        requestSize += increment;
        if (requestSize > mHandler.config.getMaxRequestSize()) {
            maxRequestSizeExceeded = true;
        }
    }

    protected boolean isMaxRequestSizeExceeded() {
        return maxRequestSizeExceeded;
    }

    /**
     * This implementation doesn't add any more parts if we have exceeded the maximum request size. The exception is if
     * this is the first part (request line) so we can recover the tag when sending an error response.
     */
    @Override
    protected void addPart(Part part) {
        if (!maxRequestSizeExceeded || parts.isEmpty()) {
            super.addPart(part);
        }
    }

}
