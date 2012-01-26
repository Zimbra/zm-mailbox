/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.imap;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.common.zmime.ZSharedFileInputStream;

/**
 * IMAP literal data type.
 */
public final class Literal extends ImapData {
    private byte[] bytes;
    private final File file;
    private final InputStream stream;
    private final int size;
    private boolean tmp; // if true then file is temporary

    public Literal(byte[] bytes) {
        this.bytes = bytes;
        file = null;
        stream = null;
        size = bytes.length;
    }

    public Literal(File file, boolean tmp) {
        this.file = file;
        stream = null;
        size = (int) file.length();
        this.tmp = tmp;
    }

    public Literal(File file) {
        this(file, false);
    }

    public Literal(InputStream is, int size) {
        bytes = null;
        file = null;
        stream = is;
        this.size = size;
    }

    @Override
    public Type getType() {
        return Type.LITERAL;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (bytes != null) {
            return new SharedByteArrayInputStream(bytes);
        } else if (file != null) {
            return new ZSharedFileInputStream(file);
        } else {
            return stream;
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    public File getFile() {
        return file;
    }

    @Override
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

    public void writePrefix(ImapOutputStream os, boolean lp) throws IOException {
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

    @Override
    public String toString() {
        if (stream != null) {
            return String.format("[literal %d bytes]", size);
        }
        try {
            return new String(getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("UTF-8 charset not found");
        } catch (IOException e) {
            throw new IllegalStateException("I/O error while reading literal bytes", e);
        }
    }

    @Override
    public void dispose() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if (file != null && tmp) {
            file.delete();
        }
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        dispose();
    }
}
