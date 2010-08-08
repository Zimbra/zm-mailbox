/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.tcpserver.TcpServerInputStream;

import java.io.IOException;

public class TcpImapRequest extends ImapRequest {
    final class ImapTerminatedException extends ImapParseException {
        private static final long serialVersionUID = 6105950126307803418L;
    }

    final class ImapContinuationException extends ImapParseException {
        private static final long serialVersionUID = 7925400980773927177L;
        boolean sendContinuation;
        ImapContinuationException(boolean send)  { super(); sendContinuation = send; }
    }

    private TcpServerInputStream mStream;
    private long mLiteral = -1;
    private boolean mUnlogged;

    TcpImapRequest(String line, ImapHandler handler) {
        super(handler);
        addPart(line);
    }

    TcpImapRequest(TcpServerInputStream tsis, ImapHandler handler) {
        super(handler);
        mStream = tsis;
    }

    void continuation() throws IOException, ImapParseException {
        if (mLiteral >= 0) continueLiteral();

        String line = mStream.readLine(), logline = line;
        // TcpServerInputStream.readLine() returns null on end of stream!
        if (line == null)
            throw new ImapTerminatedException();
        incrementSize(line.length());
        addPart(line);

        if (mParts.size() == 1 && !isMaxRequestSizeExceeded()) {
            // check for "LOGIN" command and elide if necessary
            mUnlogged = isLogin();
            if (mUnlogged) {
                logline = line.substring(0, line.indexOf(' ') + 7) + "...";
            }
        }
        
        if (ZimbraLog.imap.isDebugEnabled())
            ZimbraLog.imap.debug("C: " + logline);

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
                    mLiteral = size;
                    continuation();
                } else {
                    if (mTag == null && mIndex == 0 && mOffset == 0) {
                        mTag = readTag(); rewind();
                    }
                    throw new ImapParseException(mTag, "malformed nonblocking literal");
                }
            }
        }
    }

    private void continueLiteral() throws IOException, ImapParseException {
        if (isMaxRequestSizeExceeded()) {
            long skipped = mStream.skip(mLiteral);
            if (mLiteral > 0 && skipped == 0)
                throw new ImapTerminatedException();
            mLiteral -= skipped;
        } else {
            Part part = mParts.get(mParts.size() - 1);
            Literal literal;
            if (part.isLiteral()) {
                literal = part.getLiteral();
            } else {
                literal = Literal.newInstance((int) mLiteral, isAppend());
                addPart(literal);
            }
            int read = literal.copy(mStream);
            if (read == -1)
                throw new ImapTerminatedException();
            // TODO How to log literal data now...
            if (!mUnlogged && ZimbraLog.imap.isDebugEnabled())
                ZimbraLog.imap.debug("C: {" + read + "}");
            mLiteral -= read;
        }
        if (mLiteral > 0)
            throw new ImapContinuationException(false);
        mLiteral = -1;
    }

    private Literal getCurrentBuffer() throws ImapParseException {
        return mParts.get(mIndex).getLiteral();
    }

    @Override protected Literal readLiteral() throws IOException, ImapParseException {
        boolean blocking = true;
        skipChar('{');
        long length = Long.parseLong(readNumber());
        if (peekChar() == '+' && extensionEnabled("LITERAL+")) {
            skipChar('+');  blocking = false;
        }
        skipChar('}');

        // make sure that the literal came at the very end of a line
        if (getCurrentLine().length() != mOffset)
            throw new ImapParseException(mTag, "extra characters after literal declaration");

        boolean lastPart = (mIndex == mParts.size() - 1);
        if (lastPart || (mIndex == mParts.size() - 2 && mLiteral != -1)) {
            if (mLiteral == -1) {
                if (!isAppend()) {
                    incrementSize(length);
                }
                mLiteral = length;
            }
            if (!blocking && mStream.available() >= mLiteral)
                continuation();
            else
                throw new ImapContinuationException(blocking && lastPart);
        }
        mIndex++;
        Literal result = getCurrentBuffer();
        mIndex++;
        mOffset = 0;
        return result;
    }
}
