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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.zimbra.common.localconfig.LC;

public class BufferStream extends OutputStream {
    private byte buf[] = null;
    private File file = null;
    private FileOutputStream fos = null;
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

    public void copyTo(OutputStream os) throws IOException {
        if (buf != null)
            os.write(buf, 0, size <= buf.length ? (int)size : buf.length);
        if (file != null) {
            byte tmp[] = new byte[32 * 1024];
            FileInputStream fis = new FileInputStream(file);
            int in;

            try {
                while ((in = fis.read(tmp)) != -1)
                    os.write(tmp, 0, in);
            } finally {
                fis.close();
            }
        }
    }
    
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
    
    public byte[] getBuffer() {
        try {
            sync();
        } catch (IOException e) {
        }
        if (buf != null && buf.length > size) {
            byte newBuf[] = new byte[(int)size];
            
            System.arraycopy(buf, 0, newBuf, 0, (int)size);
            buf = newBuf;
        }
        return buf;
    }

    public File getFile() throws IOException { 
        sync();
        return file;
    }
    
    public InputStream getInputStream() throws IOException {
        sync();
        if (size > maxSize)
            throw new EOFException("data exceeds copy capacity");
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
    
    public byte[] getRawBuffer() {
        try {
            sync();
        } catch (IOException e) {
        }
        return buf;
    }

    public long getSize() {
        try {
            sync();
        } catch (IOException e) {
        }
        return size;
    }
    
    public boolean isPartial() { return size > maxMem && file == null; }

    public boolean isSequenced() { return sequenced; }

    public boolean isSpooled() { return file != null; }

    public static BufferStream newFixedBufferStream(int len) {
        return new BufferStream(len, len, len);
    }

    public long readFrom(InputStream is) throws IOException {
        return readFrom(is, Long.MAX_VALUE);
    }
    
    public long readFrom(InputStream is, long len) throws IOException {
        int in;
        int left = buffer(len == Long.MAX_VALUE ? 0 : (int)Math.min(
            Integer.MAX_VALUE, len));
        long out = 0;
        
        while (len > 0 && left > 0) {
            if ((in = is.read(buf, (int)size, left)) == -1)
                return out;
            left -= in;
            len -= in;
            out += in;
            size += in;
        }
        if (len == 0)
            return out;
        if (is.available() == 0) {
            if ((in = is.read()) == -1)
                return out;
            write(in);
            len--;
        }
        while (len > 0 && (left = buffer((int)Math.min(len, 8 * 1024))) > 0) {
            if ((in = is.read(buf, (int)size, (int)Math.min(len, left))) == -1)
                return out;
            out += in;
            size += in;
            len -= in;
        }
        if (len == 0)
            return out;

        byte tmp[] = new byte[(int)Math.min(len, 32 * 1024)];
        
        while (len > 0) {
            if ((in = is.read(tmp, 0, (int)Math.min(len, tmp.length))) == -1)
                return out;
            write(tmp, 0, in);
            out += in;
            len -= in;
        }
        return out;
    }
    
    public void release() {
        if (file != null) {
            try {
                fos.close();
            } catch (Exception e) {
            }
            fos = null;
            if (!file.delete())
                file.deleteOnExit();
            file = null;
            maxSize = maxMem;
        }
    }
    
    public void reset() {
        try {
            sync();
        } catch (IOException e) {
        }
        release();
        size = 0;
    }
    
    public void setSequenced(boolean sequenced) { this.sequenced = sequenced; }

    public long size() { return getSize(); }
    
    protected boolean spool(int len) {
        if (size + len > maxSize) {
            release();
            return false;
        } else if (file == null) {
            try {
                file = File.createTempFile("cstrm", null, new File(
                    LC.zimbra_tmp_directory.value()));
                fos = new FileOutputStream(file);
                if (!sequenced) {
                    fos.write(buf);
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
    
    public void sync() throws IOException {
        if (file != null) {
            try {
                fos.flush();
            } catch (IOException e) {
                release();
                throw e;
            }
        }
    }
    
    public byte[] toByteArray() {
        try {
            sync();
        } catch (IOException e) {
        }
        if (size <= maxMem) {
            byte tmp[] = getBuffer();
            
            return tmp == null ? new byte[0] : tmp;
        } else if (file == null || size > Integer.MAX_VALUE) {
            throw new RuntimeException("BufferStream overflow");
        } else {
            byte newBuf[] = new byte[(int)size];
            FileInputStream fis = null;
            
            System.arraycopy(buf, 0, newBuf, 0, buf.length);
            try {
                int in;
                int left = (int)(size - buf.length);
                
                fis = new FileInputStream(file);
                while (left > 0) {
                    if ((in = fis.read(newBuf, buf.length, left)) == -1)
                        throw new RuntimeException("BufferStream truncated");
                    left -= in;
                }
            } catch (IOException e) {
                throw new RuntimeException("BufferStream lost");
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
            return newBuf;
        }
    }
    
    public ByteBuffer toByteBuffer() {
        try {
            sync();
        } catch (IOException e) {
        }
        if (size == 0) {
            return ByteBuffer.allocate(0);
        } else if (size <= maxMem) {
            return ByteBuffer.wrap(buf, 0, (int)size);
        } else {
            return ByteBuffer.wrap(toByteArray());
        }
    }
    
    public String toString() {
        try {
            return toString(Charset.defaultCharset().toString());
        } catch (Exception e) {
            return new String(buf);
        }
    }
    
    public String toString(String cset) throws IOException,
        UnsupportedEncodingException {
        sync();
        if (buf == null)
            return new String();
        else if (file == null)
            return new String(buf, 0, (int)size, cset);
        else
            throw new IOException("BufferStream data too large");
    }
    
    public void truncate(long len) throws IOException {
        sync();
        if (len > size) {
            throw new IOException("BufferStream expansion");
        } else if (file != null) {
            if (len > maxMem)
                fos.getChannel().truncate(len - maxMem);
            else
                release();
        }
        size = len;
    }
    
    public void write(int data) {
        if (buffer(1) > 0) {
            buf[(int)size] = (byte)data;
        } else if (spool(1)) {
            try {
                fos.write(data);
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
                    fos.write(data, off, len);
                } catch (IOException e) {
                    maxSize = size;
                    release();
                }
            }
            size += len;
        }
    }

    public long writeTo(OutputStream os) throws IOException {
        return writeTo(os, Long.MAX_VALUE);
    }
    
    public long writeTo(OutputStream os, long len) throws IOException {
        int in;
        long out = 0;

        sync();
        if (len == Long.MAX_VALUE)
            len = size;
        else if (len > size)
            throw new IOException("BufferStream underflow");
        if (buf != null) {
            in = (int)Math.min(Math.min(size, len), buf.length);
            os.write(buf, 0, in);
            len -= in;
            out = in;
        }
        if (len == 0)
            return out;

        FileInputStream fis = null;
        byte tmp[] = new byte[(int)Math.min(len, 32 * 1024)];
        
        try {
            fis = new FileInputStream(file);
            while (len > 0 && (in = fis.read(tmp, 0, (int)Math.min(len,
                tmp.length))) != -1) {
                os.write(tmp, 0, in);
                len -= in;
                out += in;
            }
        } finally {
            if (fis != null)
                fis.close();
        }
        return out;
    }
}
