package com.zimbra.cs.mailtest;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Represents IMAP literal data.
 */
public final class Literal {
    private final byte[] mBytes;
    private final File mFile;
    private final int mSize;
    private boolean mSync; // If true then synchronizing

    public Literal(byte[] bytes) {
        mBytes = bytes;
        mFile = null;
        mSize = bytes.length;
    }

    public Literal(File file) {
        mBytes = null;
        mFile = file;
        mSize = (int) file.length();
    }
    
    public void setSync(boolean sync) {
        mSync = sync;
    }
    
    public InputStream getInputStream() throws IOException {
        return mBytes != null ?
            new ByteArrayInputStream(mBytes) : new FileInputStream(mFile);
    }

    public int getSize() {
        return mSize;
    }

    public File getFile() {
        return mFile;
    }
    
    public byte[] getBytes() throws IOException {
        if (mBytes != null) return mBytes;
        DataInputStream is = new DataInputStream(getInputStream());
        try {
            byte[] b = new byte[mSize];
            is.readFully(b);
            return b;
        } finally {
            is.close();
        }
    }

    public void write(OutputStream os) throws IOException {
        os.write('{');
        Chars.write(os, String.valueOf(mSize));
        if (!mSync) os.write('+');
        Chars.write(os, "}\r\n");
        if (mBytes != null) {
            os.write(mBytes);
        } else {
            InputStream is = getInputStream();
            try {
                byte[] b = new byte[2048];
                int len;
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);
                }
            } finally {
                is.close();
            }
        }
    }

    public String toString() {
        return "LITERAL[size=" + mSize + "]";
    }
}
