/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.store.external.ExternalBlob;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;

abstract class Literal {
    public static Literal newInstance(int size) throws IOException {
        return newInstance(size, false);
    }

    public static Literal newInstance(int size, boolean useBlob) throws IOException {
        return (useBlob && size > 0) ? new BlobLiteral(size) : new ByteBufferLiteral(size);
    }

    public abstract int size();
    public abstract int remaining();
    public abstract int copy(InputStream is) throws IOException;
    public abstract int put(byte[] b, int off, int len) throws IOException;
    public abstract InputStream getInputStream() throws IOException;
    public abstract byte[] getBytes() throws IOException;
    public abstract Blob getBlob() throws IOException, ServiceException;
    public abstract void cleanup();

    public int put(ByteBuffer bb) throws IOException {
        assert bb.hasArray();
        int len = put(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
        bb.position(bb.position() + len);
        return len;
    }

    protected void checkComplete() {
        if (remaining() > 0) {
            throw new IllegalStateException("Incomplete literal");
        }
    }

    private static class ByteBufferLiteral extends Literal {
        private ByteBuffer buf;
        private Blob blob;

        ByteBufferLiteral(int size) {
            buf = ByteBuffer.allocate(size);
        }

        @Override public int size() {
            return buf.capacity();
        }

        @Override public int remaining() {
            return buf.capacity() - buf.position();
        }

        @Override public byte[] getBytes() {
            checkComplete();
            return buf.array();
        }

        @Override public InputStream getInputStream() {
            checkComplete();
            return new ByteArrayInputStream(buf.array());
        }

        @Override public int copy(InputStream is) throws IOException {
            int count = is.read(buf.array(), buf.position(), remaining());
            if (count > 0) {
                buf.position(buf.position() + count);
            }
            return count;
        }

        @Override public int put(byte[] b, int off, int len) {
            if (len > remaining()) {
                len = remaining();
            }
            buf.put(b, off, len);
            return len;
        }

        @Override public Blob getBlob() throws IOException, ServiceException {
            return StoreManager.getInstance().storeIncoming(getInputStream());
        }

        @Override public void cleanup() {
            buf = null;
            if (blob != null) {
                if (blob instanceof ExternalBlob) {
                    String locator = ((ExternalBlob)blob).getLocator();
                    StoreManager.getReaderSMInstance(locator).quietDelete(blob);
                } else {
                    StoreManager.getInstance().quietDelete(blob);
                }
                blob = null;
            }
        }
    }

    private static class BlobLiteral extends Literal {
        private BlobBuilder builder;
        int size;

        BlobLiteral(int size) throws IOException {
            try {
                this.size = size;
                builder = StoreManager.getInstance().getBlobBuilder();
                builder.init();
            } catch (ServiceException e) {
                throw error("Unable to initialize BlobBuilder", e);
            }
        }

        @Override public int size() {
            return size;
        }

        @Override public int remaining() {
            return size() - (int) builder.getTotalBytes();
        }

        @Override public byte[] getBytes() throws IOException {
            DataInputStream is = new DataInputStream(getInputStream());
            try {
                byte[] b = new byte[size()];
                is.readFully(b);
                assert is.read() == -1;
                return b;
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        @Override public InputStream getInputStream() throws IOException {
            return getBlob().getInputStream();
        }

        @Override public int put(byte[] b, int off, int len) throws IOException {
            if (len > remaining()) {
                len = remaining();
            }
            if (len > 0) {
                builder.append(b, off, len);
            }
            return len;
        }

        @Override public int copy(InputStream is) throws IOException {
            int count = 0;
            if (remaining() > 0) {
                byte[] b = new byte[8192];
                do {
                    int len = is.read(b, 0, Math.min(b.length, remaining()));
                    if (len < 0) {
                        throw new EOFException("Unexpected end of stream");
                    }
                    count += put(b, 0, len);
                } while (remaining() > 0 && is.available() > 0);
            }
            return count;
        }

        @Override public Blob getBlob() throws IOException {
            checkComplete();
            try {
                return builder.finish();
            } catch (ServiceException e) {
                throw error("Unable to finalize BlobBuilder", e);
            }
        }

        @Override public void cleanup() {
            if (builder != null) {
                builder.dispose();
                builder = null;
            }
        }
    }

    static IOException error(String msg, Throwable cause) {
        IOException e = new IOException(msg);
        e.initCause(cause);
        return e;
    }
}
