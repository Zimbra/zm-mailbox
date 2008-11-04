/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.util.ByteUtil;

/**
 * Returns data from the encapsulated stream until <tt>CRLFCRLF</tt> is reached.
 * The trailing <tt>CRLFCRLF</tt> is read from the wrapped stream, but is not
 * returned by the <tt>read()</tt> methods in <tt>HeadersOnlyInputStream</tt>.
 */
public class HeadersOnlyInputStream extends InputStream {

    private InputStream mIn;
    private boolean mEOF = false;
    
    public HeadersOnlyInputStream(InputStream in) {
        if (!in.markSupported())
            in = new BufferedInputStream(in);
        mIn = in;
    }
    
    @Override public int read() throws IOException {
        if (mEOF)
            return -1;

        int c = mIn.read();
        if (c == '\r') {
            mIn.mark(4);
            if (mIn.read() != '\n' ||
                mIn.read() != '\r' ||
                mIn.read() != '\n') {
                mIn.reset();
            } else {
                mEOF = true;
                return -1;
            }
        }
        return c;
    }

    @Override public void close() throws IOException {
        mIn.close();
    }

    @Override public synchronized void mark(int readlimit) {
        mIn.mark(readlimit);
    }

    @Override public boolean markSupported() {
        return mIn.markSupported();
    }

    @Override public synchronized void reset() throws IOException {
        mIn.reset();
    }

    public static byte[] getHeaders(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        ByteUtil.copy(new HeadersOnlyInputStream(is), true, buf, false);
        return buf.toByteArray();
    }
}
