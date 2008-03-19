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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.mailclient.MailOutputStream;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * IMAP literal data type.
 */
public final class Literal extends ImapData {
    private final byte[] bytes;
    private final File file;
    private final int size;
    private boolean tmp; // if true then file is temporary

    public Literal(byte[] bytes) {
        this.bytes = bytes;
        file = null;
        size = bytes.length;
    }

    public Literal(File file, boolean tmp) {
        bytes = null;
        this.file = file;
        size = (int) file.length();
        this.tmp = tmp;
    }

    public Literal(File file) {
        this(file, false);
    }

    public Type getType() {
        return Type.LITERAL;
    }
    
    public InputStream getInputStream() throws IOException {
        return bytes != null ?
            new ByteArrayInputStream(bytes) : new FileInputStream(file);
    }

    public int getSize() {
        return size;
    }

    public File getFile() {
        return file;
    }

    public byte[] getBytes() throws IOException {
        if (bytes != null) return bytes;
        DataInputStream is = new DataInputStream(getInputStream());
        try {
            byte[] b = new byte[size];
            is.readFully(b);
            return b;
        } finally {
            is.close();
        }
    }

    public void writePrefix(MailOutputStream os, boolean lp)
            throws IOException {
        os.write('{');
        os.write(String.valueOf(size));
        if (lp) os.write('+');
        os.writeLine("}");
    }

    public void writeData(OutputStream os) throws IOException {
        if (bytes != null) {
            os.write(bytes);
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
        try {
        return Ascii.toString(getBytes());
        } catch (IOException e) {
            throw new IllegalStateException(
                "I/O error while reading literal bytes", e);
        }
    }

    public void dispose() {
        if (file != null && tmp) {
            file.delete();
        }
    }

    public void finalize() throws Throwable {
        super.finalize();
        dispose();
    }
}
