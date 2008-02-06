package com.zimbra.cs.mailclient;

import java.io.IOException;
import java.io.OutputStream;

public final class MailOutputStream extends OutputStream {
    private final OutputStream os;

    public MailOutputStream(OutputStream os) {
        this.os = os;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    public void write(int b) throws IOException {
        os.write(b);
    }
    
    public void write(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            write(s.charAt(i));
        }
    }

    public void writeLine(String s) throws IOException {
        write(s);
        newLine();
    }

    public void newLine() throws IOException {
        write('\r');
        write('\n');
    }

    public void flush() throws IOException {
        os.flush();
    }

    public void close() throws IOException {
        os.close();
    }
}
