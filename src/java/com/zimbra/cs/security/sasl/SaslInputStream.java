/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.security.sasl;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslServer;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SaslInputStream extends InputStream {
    private final DataInputStream mInputStream;
    private final SaslSecurityLayer mSecurityLayer;
    private ByteBuffer mBuffer;

    private static final boolean DEBUG = false;

    public SaslInputStream(InputStream is, SaslServer server) {
        this(is, SaslSecurityLayer.getInstance(server));
    }

    public SaslInputStream(InputStream is, SaslClient client) {
        this(is, SaslSecurityLayer.getInstance(client));
    }

    public SaslInputStream(InputStream is, SaslSecurityLayer securityLayer) {
        mInputStream = new DataInputStream(is);
        mSecurityLayer = securityLayer;
    }
    
    @Override public int read(byte[] b, int off, int len) throws IOException {
        debug("read: enter len = %d", len);
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        if (!ensureBuffer()) return -1;
        if (len > mBuffer.remaining()) len = mBuffer.remaining();
        mBuffer.get(b, off, len);
        debug("read: exit len = %d", len);
        return len;
    }

    @Override public int read() throws IOException {
        return ensureBuffer() ? mBuffer.get() : -1;
    }
    
    // Ensure that at least some bytes are available in the buffer
    private boolean ensureBuffer() throws IOException {
        if (mBuffer != null && mBuffer.hasRemaining()) return true;
        do {
            if (!fillBuffer()) return false;
        } while (!mBuffer.hasRemaining());
        return true;
    }

    private boolean fillBuffer() throws IOException {
        int len;
        try {
            len = mInputStream.readInt();
        } catch (EOFException e) {
            return false;
        }
        debug("fillBuffer: len = %d", len);
        // A zero-length buffer is theoretically possible here
        if (len < 0) {
            throw new IOException("Invalid buffer length in stream");
        }
        byte[] b = new byte[len];
        mInputStream.readFully(b);
        mBuffer = ByteBuffer.wrap(mSecurityLayer.unwrap(b, 0, len));
        debug("fillBuffer: read finished");
        return true;
    }

    @Override public void close() throws IOException {
        mInputStream.close();
        mBuffer = null;
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG] " + format + "\n", args);
        }
    }
}
