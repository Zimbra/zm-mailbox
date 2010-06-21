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

import com.zimbra.cs.mina.MinaHandler;

import java.io.IOException;

public class MinaImapRequest extends ImapRequest {
    private Literal literal;    // current literal data
    private int literalCount;   // remaining byte count for current literal
    private boolean complete;   // if true then request is complete

    public MinaImapRequest(MinaHandler handler) {
        super((MinaImapHandler) handler);
    }

    public boolean parse(Object obj) throws IOException {
        try {
            if (literal != null) {
                parseLiteral((byte[]) obj);
            } else {
                parseCommand((String) obj);
            }
            return complete;
        } catch (ImapParseException e) {
            cleanup();
            mHandler.handleParseException(e);
            throw new IllegalArgumentException("Bad request line", e);
        }
    }

    public boolean isComplete() {
        return complete;
    }

    private void parseLiteral(byte[] b) throws IOException, ImapParseException {
        //System.out.println("XXX parseLiteral: len = " + b.length);
        assert b.length <= literalCount;
        if (isMaxRequestSizeExceeded()) {
            literalCount -= Math.min(literalCount, b.length);
        } else {
            literalCount -= literal.put(b, 0, b.length);
        }
        if (literalCount <= 0) {
            assert literal.remaining() == 0;
            if (!isMaxRequestSizeExceeded()) {
                addPart(literal);
            }
            literal = null;
        }
    }

    private void parseCommand(String line) throws IOException, ImapParseException {
        //System.out.println("XXX parseCommand: line = |" + line + "|");
        incrementSize(line.length());
        addPart(line);
        LiteralInfo li;
        try {
            li = LiteralInfo.parse(line);
        } catch (IllegalArgumentException e) {
            throw new ImapParseException(getTag(), "bad literal format");
        }
        if (li != null) {
            literalCount = li.getCount();
            if (!isAppend()) {
                incrementSize(literalCount);
            }
            if (!isMaxRequestSizeExceeded()) {
                literal = Literal.newInstance(literalCount, isAppend());
            }
            if (li.isBlocking()) {
                sendContinuation();
            }
        } else {
            complete = true;
        }
    }

    private void sendContinuation() throws IOException, ImapParseException {
        if (isMaxRequestSizeExceeded()) {
            // If this request is too long then send an error response
            // rather than a continuation request
            throw new ImapParseException(getTag(), "maximum request size exceeded");
        }
        mHandler.sendContinuation("send literal data");
    }

    public Literal readLiteral() throws ImapParseException {
        skipChar('{');
        if (mIndex + 1 >= mParts.size()) {
            throw new ImapParseException(mTag, "no next literal");
        }
        Part part = mParts.get(mIndex + 1);
        mIndex += 2;
        mOffset = 0;
        return part.getLiteral();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Part part : mParts) {
            sb.append(part);
            if (part.isString()) sb.append("\r\n");
        }
        return sb.toString();
    }
}
