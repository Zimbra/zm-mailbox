package com.zimbra.cs.mailclient.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;

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
}
