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

package com.zimbra.cs.mina;

import java.nio.ByteBuffer;

import static com.zimbra.cs.mina.MinaUtil.*;

/**
 * Utility class for incrementally parsing a line of text from a ByteBuffer.
 * Used when parsing IMAP, POP3, and LMTP command line requests. 
 */
public class LineBuffer {
    private ByteBuffer mBuffer;
    private boolean mComplete;

    /**
     * Creates a new empty line buffer.
     */
    public LineBuffer() {}

    /**
     * Creates a new line buffer with initial capacity of 'size' bytes.
     * 
     * @param size the initial size of the buffer in bytes
     */
    public LineBuffer(int size) {
        mBuffer = ByteBuffer.allocate(size);
    }

    /**
     * Parses text line bytes from remaining bytes in the specified ByteBuffer
     * until a terminating LF is reached, in which case the ByteBuffer position
     * will be advanced to the character which immediately followed the LF.
     * Otherwise, if no LF was encountered than all remaining characters in
     * 'bb' are consumed.
     * 
     * @param bb the ByteBuffer from which bytes are to be parsed
     */
    public void parse(ByteBuffer bb) {
        if (isComplete()) return;
        int pos = findLF(bb);
        if (pos == -1) {
            // No end of line found, so just add remaining bytes to buffer
            mBuffer = MinaUtil.expand(mBuffer, bb.remaining()).put(bb);
            return;
        }
        // End of line found,
        int len = pos - bb.position();
        ByteBuffer lbb = bb.slice();
        lbb.limit(len);
        bb.position(pos + 1);
        mBuffer = MinaUtil.expand(mBuffer, len, len).put(lbb);
        // Remove trailing CR's
        pos = mBuffer.position();
        while (pos > 0 && mBuffer.get(pos - 1) == CR) --pos;
        mBuffer.position(pos).flip();
        mComplete = true;
    }

    /**
     * Returns the line which has been parsed, excluding the terminating
     * LF and any immediately preceding CRs which are stripped from the line.
     * 
     * @return the ByteBuffer containing the line, or null if the line has
     *         not been completely parsed
     */
    public ByteBuffer getByteBuffer() {
        return isComplete() ? mBuffer : null;
    }

    /**
     * Returns the line which has been parsed as a string, exluding the
     * terminating LF and any immediately preceding CRs.
     * 
     * @return the line as a String, or null if the line has not been
     *         completely parsed.
     */
    public String toString() {
        return isComplete() ? MinaUtil.toString(mBuffer) : null;
    }

    /**
     * Returns true if the line has been completely parsed.
     *
     * @return true if the line has been parsed, false otherwise
     */
    public boolean isComplete() {
        return mComplete;
    }

    private static int findLF(ByteBuffer bb) {
        int limit = bb.limit();
        for (int pos = bb.position(); pos < limit; pos++) {
            if (bb.get(pos) == LF) return pos;
        }
        return -1;
    }
}
