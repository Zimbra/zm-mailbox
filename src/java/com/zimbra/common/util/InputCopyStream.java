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

public class InputCopyStream extends InputStream {
    private CopyStream cs;
    private InputStream is;
    private byte skip[] = null;
    private static final int SKIP_SIZE = 2 * 1024;

    public InputCopyStream(InputStream is) { this(is, 0); }

    public InputCopyStream(InputStream is, long sizeHint) {
        this(is, sizeHint, CopyStream.BUFFER_SIZE);
    }

    public InputCopyStream(InputStream is, long sizeHint, int maxBuffer) {
        this(is, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public InputCopyStream(InputStream is, long sizeHint, int maxBuffer, long
        maxSize) {
        cs = new CopyStream(sizeHint, maxBuffer, maxSize);
        this.is = is;
    }

    public InputCopyStream(InputStream is, CopyStream cs) {
        this.cs = cs;
        this.is = is;
    }

    public int available() throws IOException { return is.available(); }

    public void close() throws IOException { is.close(); }

    public long copy() throws IOException { return cs.copy(is); }
    
    public long copy(long len) throws IOException { return cs.copy(is, len); }
    
    public byte[] getBuffer() { return cs.getBuffer(); }

    public CopyStream getCopyStream() { return cs; }
    
    public File getFileBuffer() { return cs.getFileBuffer(); }

    public InputStream getInputStream() throws IOException {
        return cs.getInputStream();
    }
    
    public long getSize() { return cs.getSize(); }

    public int read() throws IOException {
        int in = is.read();
        
        if (in > 0)
            cs.write(in);
        return in;
    }

    public int read(byte data[], int off, int len) throws IOException {
        int in = is.read(data, off, len);
        
        if (in > 0)
            cs.write(data, 0, in);
        return in;
    }

    public void release() throws IOException { cs.release(); }

    public long skip(long len) throws IOException {
        int in;
        long left = len;
        
        if (skip == null)
            skip = new byte[SKIP_SIZE];
        while (left > 0) {
            in = read(skip, 0, left > skip.length ? skip.length : (int)left);
            if (in == -1)
                break;
            left -= in;
        }
        return len - left;
    }
}
