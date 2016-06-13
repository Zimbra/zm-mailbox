/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.io.IOException;
import java.nio.charset.Charset;

public class StringBufferStream extends BufferStream implements Appendable {
    String cset;
    StringBuilder sbuf;
    static final int DEFAULT_SIZE_HINT = 512;

    public StringBufferStream() { this(null); }

    public StringBufferStream(long sizeHint) {
        this(null, sizeHint);
    }

    public StringBufferStream(long sizeHint, int maxBuffer) {
        this(null, sizeHint, maxBuffer);
    }

    public StringBufferStream(long sizeHint, int maxBuffer, long maxSize) {
        this(null, sizeHint, maxBuffer, maxSize);
    }

    public StringBufferStream(String cset) { this(cset, 0); }

    public StringBufferStream(String cset, long sizeHint) {
        this(cset, sizeHint, Integer.MAX_VALUE);
    }

    public StringBufferStream(String cset, long sizeHint, int maxBuffer) {
        this(cset, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public StringBufferStream(String cset, long sizeHint, int maxBuffer,
        long maxSize) {
        super(sizeHint, maxBuffer, maxSize);
        this.cset = cset == null ? Charset.defaultCharset().toString() : cset;
        sbuf = new StringBuilder((int)Math.min(sizeHint == 0 ?
            DEFAULT_SIZE_HINT : sizeHint, 2 * 1024));
    }

    public Appendable append(char c) throws IOException {
        flush(1);
        sbuf.append(c);
        return this;
    }

    public Appendable append(CharSequence cs) throws IOException {
        flush(cs.length());
        if (cs.length() > sbuf.capacity() - sbuf.length())
            write(cs.toString().getBytes(cset));
        else
            sbuf.append(cs);
        return this;
    }

    public Appendable append(CharSequence cs, int start, int end) throws
        IOException {
        int len = end - start;
        
        flush(len);
        if (len > sbuf.capacity() - sbuf.length())
            write(cs.subSequence(start, end).toString().getBytes(cset));
        else
            sbuf.append(cs, start, end);
        return this;
    }

    private void flush(int len) throws IOException {
        if (sbuf.capacity() - sbuf.length() < len && sbuf.length() > 0) {
            write(sbuf.toString().getBytes(cset));
            sbuf.setLength(0);
        }
    }
    
    public void sync() throws IOException {
        flush(Integer.MAX_VALUE);
        super.sync();
    }
    
    public String toString() {
        try {
            return super.toString(cset);
        } catch (Exception e) {
            return new String(getBuffer());
        }
    }
    
    public static void main(String[] args) throws IOException {
        StringBufferStream sbs = new StringBufferStream(12);
        
        sbs.append("start ");
        sbs.append(new StringBuilder("-middle-"), 1, 7);
        sbs.append(" end");
        System.out.println(sbs.toString());
    }

}
