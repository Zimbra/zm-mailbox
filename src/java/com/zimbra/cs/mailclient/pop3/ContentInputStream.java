/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.pop3;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

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

    @Override
    public int read() throws IOException {
        if (pos == -1) return -1;
        if (pos >= sbuf.length()) {
            if (!fillBufferWithNextLine()) return -1;
        }
        return sbuf.charAt(pos++);
    }

    public String readLine() throws IOException {
        if (pos == -1) return null;
        if (pos >= sbuf.length()) {
            if (!fillBufferWithNextLine()) return null;
        }
        // Return rest of line excluding trailing "\r\n"
        int len = sbuf.length() - pos;
        String line = len > 2 ? sbuf.substring(pos, len - 2) : "";
        pos = sbuf.length();
        return line;
    }

    @Override
    public void close() throws IOException {
        skipRemaining();
    }

    private void skipRemaining() throws IOException {
        while (read() != -1) {
            // Do nothing...
        }
    }

    /**
     * Fill input buffer with next line of content.
     *
     * <br />{@code http://tools.ietf.org/html/rfc1939 - section 3. Basic Operation} talks about multi-line response
     * as follows:<br />
     *     Responses to certain commands are multi-line.  In these cases, which are clearly indicated below, after
     *     sending the first line of the response and a CRLF, any additional lines are sent, each terminated by a CRLF
     *     pair.  When all lines of the response have been sent, a final line is sent, consisting of a termination
     *     octet (decimal code 046, ".") and a CRLF pair.  If any line of the multi-line response begins with the
     *     termination octet, the line is "byte-stuffed" by pre-pending the termination octet to that line of the
     *     response. Hence a multi-line response is terminated with the five octets "CRLF.CRLF".  When examining a
     *     multi-line response, the client checks to see if the line begins with the termination octet.  If so and if
     *     octets other than CRLF follow, the first octet of the line (the termination octet) is stripped away.  If so
     *     and if CRLF immediately follows the termination character, then the response from the POP server is ended
     *     and the line containing ".CRLF" is not considered part of the multi-line response.
     *
     * i.e.  Any real data line that begins with "." should have been prefixed with a single "." in the response.
     */
    private boolean fillBufferWithNextLine() throws IOException {
        sbuf.setLength(0);
        int currChar = 0;
        int prevChar;
        do {
            prevChar = currChar;
            currChar = in.read();
            if (currChar == -1) {
                throw new EOFException("Unexpected end of stream while reading content");
            }
            sbuf.append((char) currChar);
        } while (!(currChar == '\n' && prevChar == '\r'));
        int len = sbuf.length();
        // Check for end of content - ".\r\n"
        if (len == 3 && sbuf.charAt(0) == '.') {
            pos = -1;
            return false;
        }
        // Check for quoted "." at beginning of line
        if (len >= 4 && sbuf.charAt(0) == '.') {
            sbuf.deleteCharAt(0);
        }
        pos = 0;
        return true;
    }
}
