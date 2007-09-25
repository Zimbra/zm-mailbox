/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import java.nio.ByteBuffer;

/**
 * Represents a MINA-request that is a single line of text. This is used for
 * both POP3 and IMAP command line requests. IMAP command line requests are
 * handled specially in MinaImapRequest with additional support for literal
 * command bytes.
 */
public class MinaTextLineRequest implements MinaRequest {
    private final LineBuffer mBuffer;

    /**
     * Creates a new request for parsing a text line.
     */
    public MinaTextLineRequest() {
        mBuffer = new LineBuffer();
    }

    /**
     * Parses a line of text from the specified byte buffer.
     * 
     * @param bb the ByteBuffer containing the line bytes
     */
    public void parse(ByteBuffer bb) {
        mBuffer.parse(bb);
    }

    /**
     * Returns true if the line is complete, or false if more bytes are needed
     *
     * @return if request is complete, false otherwise
     */
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

    public String toString() {
        return getLine();
    }
}
