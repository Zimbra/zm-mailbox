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

package com.zimbra.cs.server;

import java.nio.ByteBuffer;

import com.zimbra.cs.imap.NioImapRequest;

/**
 * Represents a MINA-request that is a single line of text. This is used for both POP3 and IMAP command line requests.
 * IMAP command line requests are handled specially in {@link NioImapRequest} with additional support for literal
 * command bytes.
 */
public class NioTextLineRequest implements NioRequest {
    private final NioLineBuffer mBuffer;

    /**
     * Creates a new request for parsing a text line.
     */
    public NioTextLineRequest() {
        mBuffer = new NioLineBuffer();
    }

    /**
     * Parses a line of text from the specified byte buffer.
     *
     * @param bb the ByteBuffer containing the line bytes
     */
    @Override
    public void parse(ByteBuffer bb) {
        mBuffer.parse(bb);
    }

    /**
     * Returns true if the line is complete, or false if more bytes are needed
     *
     * @return if request is complete, false otherwise
     */
    @Override
    public boolean isComplete() {
        return mBuffer.isComplete();
    }

    /**
     * Returns the request line as a string.
     *
     * @return the request line, or null if not yet complete
     */
    public String getLine() {
        return mBuffer.isComplete() ? mBuffer.toString() : null;
    }

    @Override
    public String toString() {
        return getLine();
    }
}
