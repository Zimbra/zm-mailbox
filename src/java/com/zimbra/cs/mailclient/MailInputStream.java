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

/**
 * Input stream for reading line-oriented mail protocol data. Also supports a
 * single character look ahead.
 */
public class MailInputStream extends InputStream {
    /** The underlying input stream */
    protected final InputStream in;

    /** Character buffer for reading line data */
    protected final StringBuilder sbuf;
    
    private int nextByte = -1;

    /**
     * Creates a new <tt>MailInputStream</tt> for the specified underlying
     * input stream.
     * 
     * @param is the underlying input stream
     */
    public MailInputStream(InputStream is) {
        this.in = is;
        sbuf = new StringBuilder(132);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (nextByte != -1 && len > 0) {
            b[off++] = (byte) nextByte;
            nextByte = -1;
            len = in.read(b, off, len - 1);
            return len != -1 ? len + 1 : 1;
        }
        return in.read(b, off, len);
    }

    /**
     * Reads the next line of ASCII input data. A input line can be terminated
     * with either CRLF or LF, which is excluded from the returned string.
     * @return the next input line, excluding line terminator
     * @throws IOException if an I/O error occurs
     */
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

    @Override
    public int read() throws IOException {
        if (nextByte != -1) {
            int b = nextByte;
            nextByte = -1;
            return b;
        }
        return in.read();
    }

    /**
     * Returns the next byte of data from the input stream without actually
     * reading it. This provides a one byte lookahead.
     *
     * @return the next byte of input data, or <tt>-1</tt> if end of stream
     *         would be reached
     * @throws IOException if an I/O error occurs
     */
    public int peek() throws IOException {
        if (nextByte == -1) {
            nextByte = in.read();
        }
        return nextByte;
    }
    /**
     * Reads an ASCII character from the input stream. An <tt>EOFException</tt>
     * is thrown if the end of the stream is reached.
     *
     * @return the character that has been read
     * @throws EOFException if the end of stream was reached
     * @throws IOException if an I/O error occurs
     */
    public char readChar() throws IOException {
        int c = read();
        if (c == -1) throw new EOFException("Unexpected end of stream");
        return (char) c;
    }

    /**
     * Returns the next ASCII character from the input stream without actually
     * reading it. An <tt>EOFException</tt> is thrown if the end of the stream
     * would be reached.
     *
     * @return the character that would be read
     * @throws EOFException if the end of the stream would be reached
     * @throws IOException if an I/O error occurs
     */
    public char peekChar() throws IOException {
        int c = peek();
        if (c == -1) throw new EOFException("Unexpected end of stream");
        return (char) c;
    }

    /**
     * Returns <tt>true</tt> if the end of the input stream has been reached.
     *
     * @return <tt>true</tt> if at end of stream, <tt>false</tt> if not
     * @throws IOException if an I?O error occurs
     */
    public boolean isEOF() throws IOException {
        return peek() == -1;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        if (nextByte != -1) {
            nextByte = -1;
            --n;
        }
        return in.skip(n);
    }
    
    @Override
    public void close() throws IOException {
        in.close();
    }
}
