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

public class OutputCopyStream extends OutputStream {
    private CopyStream cs;
    private OutputStream os;

    public OutputCopyStream(OutputStream os) { this(os, 0); }

    public OutputCopyStream(OutputStream os, long sizeHint) {
        this(os, sizeHint, CopyStream.BUFFER_SIZE);
    }

    public OutputCopyStream(OutputStream os, long sizeHint, int maxBuffer) {
        this(os, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public OutputCopyStream(OutputStream os, long sizeHint, int maxBuffer, long
        maxSize) {
        cs = new CopyStream(sizeHint, maxBuffer, maxSize);
        this.os = os;
    }

    public OutputCopyStream(OutputStream os, CopyStream cs) {
        this.cs = cs;
        this.os = os;
    }

    public void close() throws IOException { os.close(); }

    public void flush() throws IOException { os.flush(); }

    public byte[] getBuffer() { return cs.getBuffer(); }

    public CopyStream getCopyStream() { return cs; }

    public File getFileBuffer() { return cs.getFileBuffer(); }

    public InputStream getInputStream() throws IOException {
        return cs.getInputStream();
    }
    
    public long getSize() { return cs.getSize(); }

    public void release() throws IOException { cs.release(); }

    public void write(int data) throws IOException {
        os.write(data);
        cs.write(data);
    }
    
    public void write(byte data[], int off, int len) throws IOException {
        os.write(data, off, len);
        cs.write(data, off, len);
    }
}
