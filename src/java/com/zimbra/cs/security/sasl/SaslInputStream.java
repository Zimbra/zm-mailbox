/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.security.sasl;

import javax.security.sasl.SaslServer;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SaslInputStream extends InputStream {
    private final DataInputStream mInputStream;
    private final SaslServer mSaslServer;
    private ByteBuffer mBuffer;

    private static final boolean DEBUG = false;

    public SaslInputStream(InputStream is, SaslServer server) {
        mInputStream = new DataInputStream(is);
        mSaslServer = server;
    }

    public int read(byte[] b, int off, int len) throws IOException {
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

    public int read() throws IOException {
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
        mBuffer = ByteBuffer.wrap(mSaslServer.unwrap(b, 0, len));
        debug("fillBuffer: read finished");
        return true;
    }

    public void close() throws IOException {
        mInputStream.close();
        mBuffer = null;
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG] " + format + "\n", args);
        }
    }
}
