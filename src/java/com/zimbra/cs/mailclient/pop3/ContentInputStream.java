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
 * Input stream for reading POP3 response content.
 */
public final class ContentInputStream extends InputStream {
    private final InputStream in;
    private final StringBuilder sbuf;
    private int pos;

    public ContentInputStream(InputStream is) {
        in = is;
        sbuf = new StringBuilder(132);
        sbuf.setLength(0);
    }

    public int read() throws IOException {
        if (pos == -1) return -1;
        if (pos >= sbuf.length()) {
            if (!fillBuffer()) return -1;
        }
        return sbuf.charAt(pos++);
    }

    public String readLine() throws IOException {
        if (pos == -1) return null;
        if (pos >= sbuf.length()) {
            if (!fillBuffer()) return null;
        }
        // Return rest of line excluding trailing "\r\n"
        int len = sbuf.length() - pos;
        String line = len > 2 ? sbuf.substring(pos, len - 2) : "";
        pos = sbuf.length();
        return line;
    }

    public void close() throws IOException {
        skipRemaining();
    }

    private void skipRemaining() throws IOException {
        while (read() != -1) {
            // Do nothing...
        }
    }

    // Fill input buffer with next line of content
    private boolean fillBuffer() throws IOException {
        sbuf.setLength(0);
        int b;
        do {
            b = in.read();
            if (b == -1) {
                throw new EOFException(
                    "Unexpected end of stream while reading content");
            }
            sbuf.append((char) b);
        } while (b != '\n');
        int len = sbuf.length();
        // Make sure line ends with "\r\n"
        if (len < 2 || sbuf.charAt(len - 2) != '\r') {
            throw new ParseException("Invalid end of line character");
        }
        // Check for end of content
        if (len == 3 && sbuf.charAt(0) == '.') {
            pos = -1;
            return false;
        }
        // Check for quoted "." at beginning of line
        if (len == 4 && sbuf.charAt(0) == '.' && sbuf.charAt(1) == '.') {
            sbuf.deleteCharAt(0);
        }
        pos = 0;
        return true;
    }
}
