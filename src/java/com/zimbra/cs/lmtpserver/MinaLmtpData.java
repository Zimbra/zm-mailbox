/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import java.nio.ByteBuffer;

import static com.zimbra.cs.mina.MinaUtil.*;
import com.zimbra.cs.mina.MinaUtil;

public class MinaLmtpData {
    private ByteBuffer buffer; // Data buffer
    private int matched;       // Number of bytes in EOM sequence matched so far
    private boolean complete;

    public MinaLmtpData(int size, String prefix) {
        if (size == 0) {
            size = 8192;
        } else if (prefix != null) {
            size += prefix.length();
        }
        buffer = ByteBuffer.allocate(size);
        if (prefix != null) MinaUtil.put(buffer, prefix);
        matched = 2; // Start as if CRLF already matched
    }

    public MinaLmtpData() {
        this(0, null);
    }

    public boolean parse(ByteBuffer bb) {
        if (isComplete()) return true;
        buffer = expand(buffer, bb.remaining());
        while (bb.hasRemaining()) {
            byte c = bb.get();
            buffer.put(c);
            if (matched > 0) {
                if (matches(c)) {
                    matched++;
                    if (matched == 5) { // end of message
                        // Skip trailing "." + CRLF
                        buffer.position(buffer.position() - 3);
                        if (buffer.position() == 0) {
                            // If data was only "." + CRLF, then include CRLF
                            // for consistency with LmtpInputStream.
                            buffer.put(CR).put(LF);
                        }
                        buffer.flip();
                        complete = true;
                        break;
                    }
                } else {
                    if (matched == 3) {
                        // Special handling for data transparency. If any line
                        // begins with '.' and is not followed immediately by
                        // CRLF to indicate EOM, the leading '.' is omitted.
                        buffer.position(buffer.position() - 2);
                        buffer.put(c);
                    }
                    matched = (c == CR) ? 1 : 0;
                }
            } else if (c == CR) {
                matched = 1; // Start new sequence
            }
        }
        return complete;
    }

    public boolean isComplete() {
        return complete;
    }

    // Returns true if byte matches next expected character in EOM sequence
    private boolean matches(byte c) {
        switch (c) {
        case CR:    return matched == 3;
        case LF:    return matched == 1 || matched == 4;
        case DOT:   return matched == 2;
        default:    return false;
        }
    }

    public byte[] getBytes() {
        checkComplete();
        return MinaUtil.getBytes(buffer);
    }

    private void checkComplete() {
        if (!isComplete()) {
            throw new IllegalStateException("data is not complete");
        }
    }
}
