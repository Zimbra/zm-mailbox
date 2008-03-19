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
package com.zimbra.cs.mailclient.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Input stream filter for tracing mail client input from server.
 */
public class TraceInputStream extends InputStream {
    private final InputStream is;
    private final PrintStream ps;
    private boolean enabled = true;
    private boolean eol = true;

    public TraceInputStream(InputStream is, PrintStream ps) {
        this.is = is;
        this.ps = ps;
    }

    public TraceInputStream(InputStream is) {
        this(is, System.out);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int read() throws IOException {
        int b = is.read();
        if (b != -1 && enabled) {
            if (eol) ps.print("S: ");
            printByte((byte) b);
        }
        eol = (b == '\n');
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (!enabled) {
            return is.read(buf, off, len);
        }
        int start = off;
        int end = off + len;
        while (off < end) {
            int b = read();
            if (b == -1) {
                int total = off - start;
                return total > 0 ? total : -1;

            }
            buf[off++] = (byte) b;
        }
        return off - start;
    }

    private void printByte(byte b) {
        switch (b) {
        case '\r':
            break;
        case '\n':
            ps.println();
            break;
        default:
            ps.print(Ascii.pp(b));
        }
    }
}
