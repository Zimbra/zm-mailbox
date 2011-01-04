/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @since 2004. 10. 26.
 * @author jhahm
 */
public class TcpServerInputStream extends BufferedInputStream {

    StringBuilder buffer;
    protected static final int CR = 13;
    protected static final int LF = 10;

    public TcpServerInputStream(InputStream in) {
        super(in);
        buffer = new StringBuilder(128);
    }

    public TcpServerInputStream(InputStream in, int size) {
        super(in, size);
        buffer = new StringBuilder(128);
    }

    /**
     * Reads a line from the stream.  A line is terminated with either
     * CRLF or bare LF.  (This is different from the behavior of
     * BufferedReader.readLine() which considers a bare CR as line
     * terminator.)
     * @return A String containing the contents of the line, not
     *         including any line-termination characters, or null if
     *         the end of the stream has been reached
     * @throws IOException
     */
    public String readLine() throws IOException {
        buffer.delete(0, buffer.length());
        while (true) {
            int ch = read();
            if (ch == -1) {
                return null;
            } else if (ch == CR) {
                continue;
            } else if (ch == LF) {
                return buffer.toString();
            }
            buffer.append((char)ch);
        }
    }
}
