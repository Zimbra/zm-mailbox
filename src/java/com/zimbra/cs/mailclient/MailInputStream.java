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
package com.zimbra.cs.mailclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.EOFException;

public class MailInputStream extends InputStream {
    protected final InputStream in;
    protected final StringBuilder sbuf;
    private int nextByte = -1;

    public MailInputStream(InputStream is) {
        this.in = is;
        sbuf = new StringBuilder(132);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (nextByte != -1 && len > 0) {
            b[off++] = (byte) nextByte;
            nextByte = -1;
            len = in.read(b, off, len - 1);
            return len != -1 ? len + 1 : -1;
        }
        return in.read(b, off, len);
    }

    public String readLine() throws IOException {
        sbuf.setLength(0);
        int c = read();
        if (c == -1) return null;
        while (c != '\n' && c != -1) {
            sbuf.append((char) c);
            c = read();
        }
        int len = sbuf.length();
        if (len > 0 && sbuf.charAt(len - 1) == '\r') {
            sbuf.setLength(len - 1);
        }
        return sbuf.toString();
    }

    public char readChar() throws IOException {
        int c = read();
        if (c == -1) throw new EOFException("Unexpected end of stream");
        return (char) c;
    }

    public char peekChar() throws IOException {
        int c = peek();
        if (c == -1) throw new EOFException("Unexpected end of stream");
        return (char) c;
    }

    public int read() throws IOException {
        if (nextByte != -1) {
            int b = nextByte;
            nextByte = -1;
            return b;
        }
        return in.read();
    }

    public int peek() throws IOException {
        if (nextByte == -1) {
            nextByte = in.read();
        }
        return nextByte;
    }

    public boolean isEOF() throws IOException {
        return peek() == -1;
    }

    public void close() throws IOException {
        in.close();
    }
}
