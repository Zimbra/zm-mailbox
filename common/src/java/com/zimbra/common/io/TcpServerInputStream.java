/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.common.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @since 2004. 10. 26.
 * @author jhahm
 */
public class TcpServerInputStream extends BufferedInputStream {

    StringBuilder buffer;
    private BufferedReader br;
    protected static final int CR = 13;
    protected static final int LF = 10;

    public TcpServerInputStream(InputStream in) {
        super(in);

        br = new BufferedReader(new InputStreamReader(this, StandardCharsets.UTF_8));
        buffer = new StringBuilder(128);
    }

    public TcpServerInputStream(InputStream in, int size) {
        super(in, size);

        br = new BufferedReader(new InputStreamReader(this, StandardCharsets.UTF_8));
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
            int ch = br.read();
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
