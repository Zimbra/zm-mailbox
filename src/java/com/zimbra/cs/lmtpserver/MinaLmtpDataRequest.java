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
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaUtil;

public class MinaLmtpDataRequest implements MinaRequest {
    private ByteBuffer mBuffer; // Data buffer
    private int mMatched;       // Number of bytes in EOM sequence matched so far
    private boolean complete;
    
    public MinaLmtpDataRequest(int size, String prefix) {
        if (size == 0) {
            size = 8192;
        } else if (prefix != null) {
            size += prefix.length();
        }
        mBuffer = ByteBuffer.allocate(size);
        if (prefix != null) MinaUtil.put(mBuffer, prefix);
        mMatched = 2; // Start as if CRLF already matched
    }

    public MinaLmtpDataRequest() {
        this(0, null);
    }

    public void parse(ByteBuffer bb) {
        if (isComplete()) return;
        mBuffer = expand(mBuffer, bb.remaining());
        while (bb.hasRemaining()) {
            byte c = bb.get();
            mBuffer.put(c);
            if (mMatched > 0) {
                if (matches(c)) {
                    mMatched++;
                    if (mMatched == 5) { // end of message
                        // Skip trailing "." + CRLF
                        mBuffer.position(mBuffer.position() - 3);
                        if (mBuffer.position() == 0) {
                            // If data was only "." + CRLF, then include CRLF
                            // for consistency with LmtpInputStream.
                            mBuffer.put(CR).put(LF);
                        }
                        mBuffer.flip();
                        complete = true;
                        break;
                    }
                } else {
                    if (mMatched == 3) {
                        // Special handling for data transparency. If any line
                        // begins with '.' and is not followed immediately by
                        // CRLF to indicate EOM, the leading '.' is omitted.
                        mBuffer.position(mBuffer.position() - 2);
                        mBuffer.put(c);
                    }
                    mMatched = (c == CR) ? 1 : 0;
                }
            } else if (c == CR) {
                mMatched = 1; // Start new sequence
            }
        }
    }

    public boolean isComplete() {
        return complete;
    }

    // Returns true if byte matches next expected character in EOM sequence
    private boolean matches(byte c) {
        switch (c) {
        case CR:    return mMatched == 3;
        case LF:    return mMatched == 1 || mMatched == 4;
        case DOT:   return mMatched == 2;
        default:    return false;
        }
    }

    public byte[] getBytes() {
        checkComplete();
        return MinaUtil.getBytes(mBuffer);
    }

    private void checkComplete() {
        if (!isComplete()) {
            throw new IllegalStateException("data is not complete");
        }
    }
}
