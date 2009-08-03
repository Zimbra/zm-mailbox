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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.SequenceInputStream;

import com.zimbra.common.localconfig.LC;

public class CopyStream extends OutputStream {
    private byte array[] = null;
    private OutputStream bufferStream;
    private File file = null;
    private int maxBuffer;
    private long maxSize;
    private long size = 0;
    public static final int BUFFER_SIZE = 512 * 1024;

    public CopyStream() { this(0); }

    public CopyStream(long sizeHint) { this(sizeHint, BUFFER_SIZE); }

    public CopyStream(long sizeHint, int maxBuffer) {
        this(sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public CopyStream(long sizeHint, int maxBuffer, long maxSize) {
        if (maxBuffer > maxSize)
            maxBuffer = (int)maxSize;
        this.maxBuffer = maxBuffer;
        this.maxSize = maxSize;
        if (sizeHint > Integer.MAX_VALUE)
            sizeHint = Integer.MAX_VALUE;
        else if (sizeHint > maxBuffer)
            sizeHint = maxBuffer;
        if (sizeHint > 0)
            bufferStream = new ByteArrayOutputStream((int)sizeHint);
        else if (maxBuffer > 0)
            bufferStream = new ByteArrayOutputStream();
        else
            bufferStream = null;
    }

    public void close() throws IOException { release(); }

    public long copy(InputStream is) throws IOException {
        return copy(is, Long.MAX_VALUE);
    }
    
    public long copy(InputStream is, long len) throws IOException {
        byte buf[] = new byte[32 * 1024];
        int in;
        long out = 0;
        
        while (len > 0 && (in = is.read(buf, 0, len < buf.length ? (int)len :
            buf.length)) > 0) {
            write(buf, 0, in);
            len -= in;
            out += in;
        }
        return out;
    }
    
    protected void finalize() { release(); }
    
    public byte[] getBuffer() {
        if (array == null && maxBuffer > 0 && bufferStream != null) {
            array = ((ByteArrayOutputStream)bufferStream).toByteArray();
            bufferStream = null;
            maxBuffer = array.length;
        }
        return array;
    }

    public File getFileBuffer() { return file; }

    public InputStream getInputStream() throws IOException {
        if (getBuffer() == null) {
            if (size > maxSize)
                throw new EOFException("data exceeed copy capacity");
            return file == null ? new ByteArrayInputStream(new byte[0]) :
                new FileInputStream(file);
        }

        InputStream in = new ByteArrayInputStream(array);
        
        return file == null ? in : new SequenceInputStream(in, new
            FileInputStream(file));
    }
    
    public long getSize() { return size; }

    public void release() {
        array = null;
        if (file != null) {
            try {
                bufferStream.close();
            } catch (Exception e) {
            }
            file.delete();
            file = null;
        }
        bufferStream = null;
    }

    protected void spool() throws IOException {
        getBuffer();
        file = File.createTempFile("cstrm", null, new File(
            LC.zimbra_tmp_directory.value()));
        bufferStream = new FileOutputStream(file);
    }
    
    public void write(int data) throws IOException {
        if (maxSize - size > 0 ) {
            if (maxBuffer == size)
                spool();
            bufferStream.write(data);
        } else if (maxSize == size) {
            release();
        }
        size++;
    }
    
    public void write(byte data[], int off, int len) throws IOException {
        long left = maxBuffer - size;
        
        // fill memory buffer
        if (left > 0 && bufferStream != null) {
            if (left > len)
                left = len;
            bufferStream.write(data, off, (int)left);
            len -= left;
            size += left;
        }
        // overflow to disk buffer
        if (len > 0) {
            left = maxSize - size;
            size += len;
            if (len > left) {
                release();
                return;
            }
            if (file == null)
                spool();
            bufferStream.write(data, off, len);
        }
    }
}
