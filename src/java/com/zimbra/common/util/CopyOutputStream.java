/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CopyOutputStream extends OutputStream {
    private BufferStream bs;
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
        bs = new BufferStream(sizeHint, maxBuffer, maxSize);
        this.os = os;
    }

    public CopyOutputStream(OutputStream os, BufferStream bs) {
        this.bs = bs;
        this.os = os;
    }

    public void close() throws IOException { os.close(); }

    public void flush() throws IOException { os.flush(); }

    public BufferStream getBufferStream() { return bs; }

    public InputStream getInputStream() throws IOException {
        return bs.getInputStream();
    }
    
    public long getSize() { return bs.getSize(); }

    public long readFrom(InputStream is) throws IOException {
        return readFrom(is, Long.MAX_VALUE);
    }
    
    public long readFrom(InputStream is, long len) throws IOException {
        byte tmp[] = new byte[(int)Math.min(len, 32 * 1024)];
        int in;
        long out = 0;
        
        while (len > 0 && (in = is.read(tmp, 0, (int)Math.min(len,
            tmp.length))) > 0) {
            write(tmp, 0, in);
            len -= in;
            out += in;
        }
        return out;
    }
    
    public void release() { bs.close(); }
    
    public byte[] toByteArray() { return bs.toByteArray(); }

    public void write(int data) throws IOException {
        os.write(data);
        bs.write(data);
    }
    
    public void write(byte data[], int off, int len) throws IOException {
        os.write(data, off, len);
        bs.write(data, off, len);
    }
}
