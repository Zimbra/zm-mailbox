/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CopyOutputStream extends OutputStream {
    private BufferStream cs;
    private OutputStream os;

    public CopyOutputStream(OutputStream os) { this(os, 0); }

    public CopyOutputStream(OutputStream os, long sizeHint) {
        this(os, sizeHint, Integer.MAX_VALUE);
    }

    public CopyOutputStream(OutputStream os, long sizeHint, int maxBuffer) {
        this(os, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public CopyOutputStream(OutputStream os, long sizeHint, int maxBuffer, long
        maxSize) {
        cs = new BufferStream(sizeHint, maxBuffer, maxSize);
        this.os = os;
    }

    public CopyOutputStream(OutputStream os, BufferStream cs) {
        this.cs = cs;
        this.os = os;
    }

    public void close() throws IOException { os.close(); }

    public long copy(InputStream is) throws IOException {
        return copy(is, Long.MAX_VALUE);
    }
    
    public long copy(InputStream is, long len) throws IOException {
        byte buf[] = new byte[(int)Math.min(len, 16 * 1024)];
        int in;
        long out = 0;
        
        while ((in = is.read(buf)) > 0) {
            write(buf, 0, in);
            out += in;
        }
        return out;
    }
    
    public void flush() throws IOException { os.flush(); }

    public byte[] getBuffer() { return cs.getBuffer(); }

    public BufferStream getCopyStream() { return cs; }

    public File getFile() { return cs.getFile(); }

    public InputStream getInputStream() throws IOException {
        return cs.getInputStream();
    }
    
    public long getSize() { return cs.getSize(); }

    public void release() { cs.close(); }

    public void write(int data) throws IOException {
        os.write(data);
        cs.write(data);
    }
    
    public void write(byte data[], int off, int len) throws IOException {
        os.write(data, off, len);
        cs.write(data, off, len);
    }
}
