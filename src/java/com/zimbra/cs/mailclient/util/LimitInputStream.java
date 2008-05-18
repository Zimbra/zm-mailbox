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
import java.io.FilterInputStream;

public class LimitInputStream extends FilterInputStream {
    private int remaining;

    public LimitInputStream(InputStream is, int count) {
        super(is);
        remaining = count;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        if (len > remaining) len = remaining;
        len = in.read(b, off, len);
        off += len;
        remaining -= len;
        return len;
    }

    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int c = in.read();
        --remaining;
        return c;
    }

    public int available() throws IOException {
        return Math.min(remaining, in.available());
    }

    public long skip(long n) throws IOException {
        if (n > remaining) n = remaining;
        return in.skip(n);
    }

    public void close() {
        // Do not close underlying stream
    }
}
