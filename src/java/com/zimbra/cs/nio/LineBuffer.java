/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.nio;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;

public class LineBuffer {
    private ByteBuffer buf;
    private boolean complete;

    public LineBuffer() {}

    public LineBuffer(int size) {
        buf = ByteBuffer.allocate(size);
    }

    /**
     * Parses text line bytes from remaining bytes in the specified ByteBuffer
     * until a terminating LF is reached, in which case the ByteBuffer position
     * will be advanced to the character which immediately followed the LF.
     * Otherwise, if no LF was encountered than all remaining characters in
     * 'bb' are consumed.
     * 
     * @param bb the ByteBuffer from which bytes are to be parsed
     * @return true if line is complete, false otherwise
     */
    public boolean parse(ByteBuffer bb) {
        if (!complete) {
            int pos = indexOf(bb, '\n');
            if (pos >= 0) {
                int len = pos + 1 - bb.position();
                ByteBuffer tmp = bb.slice();
                tmp.limit(len);
                bb.position(pos + 1);
                buf = NioUtil.expand(buf, len, len).put(tmp);
                buf.flip();
                complete = true;
            } else {
                buf = NioUtil.expand(buf, bb.remaining()).put(bb);

            }
        }
        return complete;
    }

    public boolean matches(String s) {
        return s.length() == size() && startsWith(s);
    }
    
    public boolean startsWith(String s) {
        if (s.length() <= size()) {
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) != buf.get(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public ByteBuffer buf() {
        return buf;
    }

    public int size() {
        return complete ? buf.limit() : buf.position();
    }
    
    public String toString() {
        return toString(size());
    }

    public String getLine() {
        if (!isComplete()) {
            throw new IllegalStateException("Line not complete");
        }
        int len = buf.limit();
        if (len > 0 && buf.get(len - 1) == '\n') {
            while (--len > 0 && buf.get(len - 1) == '\r') ;
        }
        return toString(len);
    }

    public String toString(int len) {
        try {
            return new String(buf.array(), buf.arrayOffset(), len, "ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("ASCII charset missing");
        }
    }
    
    public boolean isComplete() {
        return complete;
    }

    public void reset() {
        buf.clear();
        complete = false;
    }

    public void rewind() {
        buf.rewind();
    }

    private static int indexOf(ByteBuffer bb, char c) {
        int limit = bb.limit();
        for (int pos = bb.position(); pos < limit; pos++) {
            if (bb.get(pos) == c) return pos;
        }
        return -1;
    }
}
