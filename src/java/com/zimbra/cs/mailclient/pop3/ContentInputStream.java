/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.ParseException;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;

/**
 * Input stream for reading POP3 message data.
 */
public final class ContentInputStream extends InputStream {
    private final InputStream mInputStream;
    private final StringBuilder mBuffer;
    private int mPos;

    public ContentInputStream(InputStream is) {
        mInputStream = is;
        mBuffer = new StringBuilder(132);
        mBuffer.setLength(0);
    }

    public int read() throws IOException {
        if (mPos == -1) return -1;
        if (mPos > mBuffer.length()) {
            if (!fillBuffer()) return -1;
        }
        return mBuffer.charAt(mPos++);
    }

    public String readLine() throws IOException {
        if (mPos == -1) return null;
        int len = mBuffer.length();
        if (mPos >= len) {
            if (!fillBuffer()) return null;
        }
        // Remove trailing '\n' or '\r\n'
        len -= Math.min(len - mPos, 2);
        String line = mBuffer.substring(mPos, len);
        mPos = 0;
        return line;
    }

    private boolean fillBuffer() throws IOException {
        mBuffer.setLength(0);
        int b;
        do {
            b = mInputStream.read();
            if (b == -1) {
                throw new EOFException(
                    "Unexpected end of stream while reading content");
            }
            mBuffer.append((char) b);
        } while (b != '\n');
        int len = mBuffer.length();
        if (len < 2 || mBuffer.charAt(len - 2) != '\r') {
            throw new ParseException("Invalid end of line character");
        }
        if (len == 3 && mBuffer.charAt(0) == '.') {
            // Buffer always ends with "\r\n"
            mPos = -1;
            return false;
        }
        if (len == 4 && mBuffer.charAt(0) == '.' && mBuffer.charAt(1) == '.') {
            mBuffer.deleteCharAt(0);
        }
        mPos = 0;
        return true;
    }

}
