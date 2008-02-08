package com.zimbra.cs.mailtest;

import java.io.IOException;
import java.io.InputStream;

public class MailInputStream extends InputStream {
    private final InputStream mInputStream;
    private final StringBuilder mStringBuilder;

    public MailInputStream(InputStream is) {
        this.mInputStream = is;
        mStringBuilder = new StringBuilder(132);
    }

    public int read() throws IOException {
        return mInputStream.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return mInputStream.read(b, off, len);
    }

    public String readLine() throws IOException {
        mStringBuilder.setLength(0);
        int c = read();
        if (c == -1) return null;
        while (c != '\n' && c != -1) {
            mStringBuilder.append((char) c);
            c = read();
        }
        int len = mStringBuilder.length();
        if (len > 0 && mStringBuilder.charAt(len - 1) == '\r') {
            mStringBuilder.setLength(len - 1);
        }
        return mStringBuilder.toString();
    }

    public void close() throws IOException {
        mInputStream.close();
    }
}
