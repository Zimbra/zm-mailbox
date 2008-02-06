package com.zimbra.cs.mailclient;

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
