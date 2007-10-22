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

package com.zimbra.cs.imap;

import com.zimbra.cs.mina.LineBuffer;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class MinaImapRequest extends ImapRequest implements MinaRequest {
    private LineBuffer mLine;   // Command line
    private ByteBuffer mBuffer; // Buffer for literal data
    private State mState;       // Current request state
    private int mLiteralCount;  // Remaining byte count for current literal
    private boolean mBlocking;  // Current literal is blocking continuation

    private enum State {
        COMMAND,    // Parsing command line
        LITERAL,    // Parsing literal data
        COMPLETE    // Request is complete   
    }

    public MinaImapRequest(MinaHandler handler) {
        super((MinaImapHandler) handler);
        mState = State.COMMAND;
    }

    public void parse(ByteBuffer bb) throws IOException {
        try {
            while (mState != State.COMPLETE && bb.hasRemaining()) {
                switch (mState) {
                    case COMMAND:
                        parseLine(bb);
                        break;
                    case LITERAL:
                        parseLiteral(bb);
                        break;
                }
            }
        } catch (ImapParseException e) {
            mHandler.handleImapParseException(e);
            throw new IllegalArgumentException("Bad request line", e);
        }
    }

    public boolean isComplete() {
        return mState == State.COMPLETE;
    }

    private void parseLiteral(ByteBuffer bb) throws IOException, ImapParseException {
        int n = Math.min(mLiteralCount, bb.remaining());
        if (!isMaxRequestSizeExceeded()) {
            mBuffer.put((ByteBuffer) bb.slice().limit(n));
        }
        bb.position(bb.position() + n);
        mLiteralCount -= n;
        if (mLiteralCount == 0) {
            if (!isMaxRequestSizeExceeded()) {
                addPart(mBuffer.array());
            }
            mBuffer = null;
            mState = State.COMMAND;
        } else if (mBlocking) {
            sendContinuation();
        }
    }

    private void parseLine(ByteBuffer bb)
            throws IOException, ImapParseException {
        if (mLine == null) mLine = new LineBuffer();
        mLine.parse(bb);
        if (!mLine.isComplete()) return;
        String line = mLine.toString();
        mLine = null;
        incrementSize(line.length());
        addPart(line);
        parseLine(line);
    }

    private void parseLine(String line) throws IOException, ImapParseException {
        int i;
        if (!line.endsWith("}") || (i = line.lastIndexOf('{')) == -1) {
            mState = State.COMPLETE;
            return;
        }
        int j = line.length() - 2;
        if (line.charAt(j) == '+') {
            --j;
        } else {
            mBlocking = true;
        }
        mState = State.LITERAL;
        mLiteralCount = parseNumber(line, i + 1, j);
        if (mLiteralCount == -1) {
            throw new ImapParseException(getTag(), "bad literal format");
        }
        incrementSize(mLiteralCount);
        if (!isMaxRequestSizeExceeded()) {
            mBuffer = ByteBuffer.allocate(mLiteralCount);
        }
        if (mBlocking) {
            sendContinuation();
        }
    }

    private void sendContinuation() throws IOException, ImapParseException {
        if (isMaxRequestSizeExceeded()) {
            // If this request is too long then send an error response
            // rather than a continuation request
            throw new ImapParseException(getTag(), "TOOBIG", "request too big", false);
        }
        mHandler.sendContinuation("send literal data");
    }
    
    private static int parseNumber(String s, int i, int j) {
        if (i > j) return -1;
        int result = 0;
        while (i <= j) {
            int n = Character.digit(s.charAt(i++), 10);
            if (n == -1) return -1;
            result = result * 10 + n;
            if (result < 0) return -1; // Overflow
        }
        return result;
    }

    public byte[] readLiteral() throws ImapParseException {
        if (mIndex + 1 >= mParts.size()) {
            throw new ImapParseException(mTag, "no next literal");
        }
        Object part = mParts.get(mIndex + 1);
        if (!(part instanceof byte[]))
            throw new ImapParseException(mTag, "in string next not literal");
        mIndex += 2;
        mOffset = 0;
        return (byte[]) part;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Object part : mParts) {
            if (part instanceof String) {
                sb.append(part).append("\r\n");
            } else if (part instanceof byte[]) {
                byte[] b = (byte[]) part;
                try {
                    sb.append(new String(b, "US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                    throw new InternalError();
                }
            } else {
                throw new AssertionError();
            }
        }
        return sb.toString();
    }
}
