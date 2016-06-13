/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.InputStream;
import java.io.IOException;

public class CopyInputStream extends InputStream {
    private BufferStream bs;
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
        bs = new BufferStream(sizeHint, maxBuffer, maxSize);
        this.is = is;
    }

    public CopyInputStream(InputStream is, BufferStream bs) {
        this.bs = bs;
        this.is = is;
    }

    public int available() throws IOException { return is.available(); }

    public void close() throws IOException { is.close(); }

    public BufferStream getBufferStream() { return bs; }
    
    public InputStream getInputStream() throws IOException {
        return bs.getInputStream();
    }
    
    public long getSize() { return bs.getSize(); }

    public void mark(int limit) { is.mark(limit); }

    public boolean markSupported() { return is.markSupported(); }

    public int read() throws IOException {
        int in = is.read();
        
        if (in != -1)
            bs.write(in);
        return in;
    }

    public int read(byte data[], int off, int len) throws IOException {
        int in = is.read(data, off, len);
        
        if (in > 0)
            bs.write(data, off, in);
        return in;
    }

    public long readFrom() throws IOException { return bs.readFrom(is); }
    
    public long readFrom(long len) throws IOException {
        return bs.readFrom(is, len);
    }
    
    public void release() { bs.close(); }

    public void reset() throws IOException { is.reset(); }

    public byte[] toByteArray() { return bs.toByteArray(); }
}
