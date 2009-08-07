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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.zimbra.common.localconfig.LC;

public class BufferStream extends OutputStream {
    private byte buf[] = null;
    private FileOutputStream fs;
    private File file = null;
    private int maxMem;
    private long maxSize;
    private boolean sequenced = true;
    private long size = 0;

    public BufferStream() { this(0); }

    public BufferStream(long sizeHint) { this(sizeHint, Integer.MAX_VALUE); }

    public BufferStream(long sizeHint, int maxMem) {
        this(sizeHint, maxMem, Long.MAX_VALUE);
    }

    public BufferStream(long sizeHint, int maxMem, long maxSize) {
        if (maxMem > maxSize)
            maxMem = (int)maxSize;
        this.maxMem = maxMem;
        this.maxSize = maxSize;
        if (sizeHint > 0)
            buf = new byte[(int)Math.min(sizeHint, maxMem)];
    }

    private int buffer(int len) {
        if (buf == null) {
            if (maxMem == 0)
                return 0;
            buf = new byte[Math.min(maxMem, Math.max(len, 2 * 1024))];
            return buf.length;
        } else if (len <= buf.length - size) {
            return (int)(buf.length - size);
        } else if (buf.length < maxMem) {
            long tot = size + len;
            byte newBuf[];

            if (tot < buf.length << 1)
                tot = Math.min(buf.length << 1, maxMem);
            newBuf = new byte[(int)tot];
            System.arraycopy(buf, 0, newBuf, 0, (int)size);
            buf = newBuf;
            return (int)(buf.length - size);
        } else {
            return 0;
        }
    }
    
    public void close() {
        release();
        buf = null;
        maxSize = maxMem = 0;
    }

    public long copy(InputStream is) throws IOException {
        return copy(is, Long.MAX_VALUE);
    }
    
    public long copy(InputStream is, long len) throws IOException {
        int in;
        int left = buffer(len == Long.MAX_VALUE ? 0 : (int)Math.min(
            Integer.MAX_VALUE, len));
        long out;
        
        if (left > 0) {
            in = is.read(buf, (int)size, left);
            if (in <= 0)
                return 0;
            else if (in != left)
                return in;
            len -= in;
            out = in;
        } else {
            out = 0;
        }
        if (len == 0)
            return out;
        if (is.available() == 0) {
            if ((in = is.read()) == -1)
                return out;
            write(in);
            len--;
        }
        while (len > 0 && (left = buffer((int)Math.min(len, 8 * 1024))) == 0) {
            if ((in = is.read(buf, (int)size, (int)Math.min(len, left))) <= 0)
                return out;
            len -= in;
            size += in;
            if (in != left)
                return out;
        }

        byte tmp[] = new byte[(int)Math.min(len, 16 * 1024)];
        
        while (len > 0 && (in = is.read(tmp, 0, (int)Math.min(len,
            tmp.length))) > 0) {
            write(tmp, 0, in);
            len -= in;
            out += in;
            if (in != tmp.length)
                break;
        }
        return out;
    }
    
    protected void finalize() { release(); }
    
    public byte[] getBuffer() {
        if (buf != null && buf.length > size) {
            byte newBuf[] = new byte[(int)size];
            
            System.arraycopy(buf, 0, newBuf, 0, (int)size);
            buf = newBuf;
        }
        return buf;
    }

    public File getFile() { return file; }
    
    public static BufferStream getFixedBufferStream(int len) {
        return new BufferStream(len, len, len);
    }

    public InputStream getInputStream() throws IOException {
        if (size > maxSize)
            throw new EOFException("data exceeds copy capacity");
        if (file != null)
            fs.flush();
        if (buf == null)
            return file == null ? new ByteArrayInputStream(new byte[0]) :
                new FileInputStream(file);

        InputStream in = new ByteArrayInputStream(buf, 0, (int)Math.min(
            buf.length, size));
        
        return file == null ? in : new SequenceInputStream(in, new
            FileInputStream(file));
    }
    
    public int getMaxMem() { return maxMem; }
    
    public long getMaxSize() { return maxSize; }
    
    public byte[] getRawBuffer() { return buf; }

    public long getSize() { return size; }
    
    public boolean isSequenced() { return sequenced; }

    public void release() {
        if (file != null) {
            try {
                fs.close();
            } catch (Exception e) {
            }
            fs = null;
            if (!file.delete())
                file.deleteOnExit();
            file = null;
        }
    }
    
    public void setSequenced(boolean sequenced) { this.sequenced = sequenced; }

    protected boolean spool(int len) {
        if (size + len > maxSize) {
            release();
            return false;
        } else if (file == null) {
            try {
                file = File.createTempFile("cstrm", null, new File(
                    LC.zimbra_tmp_directory.value()));
                fs = new FileOutputStream(file);
                if (!sequenced) {
                    fs.write(buf);
                    buf = null;
                    maxMem = 0;
                }
            } catch (IOException e) {
                file = null;
                maxSize = maxMem;
                return false;
            }
        }
        return true;
    }
    
    public String toString() {
        try {
            return toString(Charset.defaultCharset().toString());
        } catch (Exception e) {
            return new String(buf);
        }
    }
    
    public String toString(String enc) throws IOException,
        UnsupportedEncodingException {
        if (buf == null)
            return new String();
        else if (file == null)
            return new String(buf, 0, (int)size, enc);
        else
            throw new IOException("data too large");
    }
    
    public void truncate(long len) throws IOException {
        if (len > size) {
            throw new IOException("cannot expand buffer");
        } else if (file != null) {
            if (len > maxMem) {
                fs.flush();
                fs.getChannel().truncate(len - maxMem);
            } else {
                release();
            }
        }
        size = len;
    }
    
    public void write(int data) {
        if (buffer(1) > 0) {
            buf[(int)size] = (byte)data;
        } else if (spool(1)) {
            try {
                fs.write(data);
            } catch (Exception e) {
                maxSize = size;
                release();
            }
        }
        size++;
    }
    
    public void write(byte data[], int off, int len) {
        int left = buffer(len);
        
        if (left > 0) {
            if (left > len)
                left = len;
            System.arraycopy(data, off, buf, (int)size, left);
            len -= left;
            size += left;
        }
        if (len > 0) {
            if (spool(len)) {
                try {
                    fs.write(data, off, len);
                } catch (Exception e) {
                    maxSize = size;
                    release();
                }
            }
            size += len;
        }
    }
}
