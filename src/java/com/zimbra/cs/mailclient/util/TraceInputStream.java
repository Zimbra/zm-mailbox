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
    private final InputStream in;
    private final PrintStream traceOut;
    private boolean enabled = true;
    private boolean eol = true;

    public TraceInputStream(InputStream in, PrintStream traceOut) {
        this.in = in;
        this.traceOut = traceOut;
    }

    public TraceInputStream(InputStream in) {
        this(in, System.out);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1 && enabled) {
            if (eol) traceOut.print("S: ");
            printByte((byte) b);
        }
        eol = (b == '\n');
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (!enabled) {
            return in.read(buf, off, len);
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
            traceOut.println();
            break;
        default:
            traceOut.print(Ascii.pp(b));
        }
    }
}
