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
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.util.List;
import java.util.Iterator;

/**
 * I/O utility methods.
 */
public final class Io {
    public static void readFully(InputStream is, byte[] b, int off, int len)
            throws IOException {
        new DataInputStream(is).readFully(b, off, len);
    }

    public static void readFully(InputStream is, byte[] b) throws IOException {
        readFully(is, b, 0, b.length);
    }
    
    public static void copyBytes(InputStream is, OutputStream os, int len)
        throws IOException {
        byte[] b = new byte[4096];
        while (len > 0) {
            int n = is.read(b, 0, Math.min(b.length, len));
            if (n == -1) {
                throw new EOFException("Unexpected end of stream");
            }
            os.write(b, 0, n);
            len -= n;
        }
    }

    public static void copyBytes(InputStream is, OutputStream os)
        throws IOException {
        byte[] b = new byte[4096];
        int len;
        while ((len = is.read(b, 0, b.length)) != -1) {
            os.write(b, 0, len);
        }
    }

    public static String toString(List<Object> items, String separator) {
        StringBuilder sb = new StringBuilder();
        Iterator<Object> it = items.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(separator).append(it.next());
            }
        }
        return sb.toString();
    }
}
