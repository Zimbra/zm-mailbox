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

import java.io.PrintStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream filter for tracing mail client output to server.
 */
public class TraceOutputStream extends OutputStream {
    private final OutputStream out;
    private final PrintStream traceOut;
    private boolean enabled = true;
    private boolean eol = true;

    public TraceOutputStream(OutputStream out, PrintStream traceOut) {
        this.out = out;
        this.traceOut = traceOut;
    }

    public TraceOutputStream(OutputStream out) {
        this(out, System.out);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        if (enabled) {
            if (eol) traceOut.print("C: ");
            printByte((byte) b);
            eol = (b == '\n');
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        if (enabled) {
            while (--len >= 0) write(buf[off++]);
        } else {
            out.write(buf, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        out.flush();
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
