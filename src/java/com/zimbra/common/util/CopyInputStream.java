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

public class CopyInputStream extends InputStream {
    private BufferStream cs;
    private InputStream is;

    public CopyInputStream(InputStream is) { this(is, 0); }

    public CopyInputStream(InputStream is, long sizeHint) {
        this(is, sizeHint, Integer.MAX_VALUE);
    }

    public CopyInputStream(InputStream is, long sizeHint, int maxBuffer) {
        this(is, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public CopyInputStream(InputStream is, long sizeHint, int maxBuffer, long
        maxSize) {
        cs = new BufferStream(sizeHint, maxBuffer, maxSize);
        this.is = is;
    }

    public CopyInputStream(InputStream is, BufferStream cs) {
        this.cs = cs;
        this.is = is;
    }

    public int available() throws IOException { return is.available(); }

    public void close() throws IOException { is.close(); }

    public long copy() throws IOException { return copy(Long.MAX_VALUE); }
    
    public long copy(long len) throws IOException { return cs.copy(is, len); }
    
    public byte[] getBuffer() { return cs.getBuffer(); }

    public BufferStream getCopyStream() { return cs; }
    
    public File getFile() { return cs.getFile(); }

    public InputStream getInputStream() throws IOException {
        return cs.getInputStream();
    }
    
    public long getSize() { return cs.getSize(); }

    public void mark(int limit) { is.mark(limit); }

    public boolean markSupported() { return is.markSupported(); }

    public int read() throws IOException {
        int in = is.read();
        
        if (in != -1)
            cs.write(in);
        return in;
    }

    public int read(byte data[], int off, int len) throws IOException {
        int in = is.read(data, off, len);
        
        if (in > 0)
            cs.write(data, off, in);
        return in;
    }

    public void release() { cs.close(); }

    public void reset() throws IOException { is.reset(); }
}
