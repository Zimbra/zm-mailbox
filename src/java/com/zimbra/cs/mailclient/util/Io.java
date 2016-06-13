/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
