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

package com.zimbra.cs.imap;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SaslOutputStream extends OutputStream {
    private final DataOutputStream mOutputStream;
    private final SaslServer mSaslServer;
    private final ByteBuffer mBuffer;

    private static final boolean DEBUG = false;

    public SaslOutputStream(OutputStream os, SaslServer server) {
        mOutputStream = new DataOutputStream(os);
        mSaslServer = server;
        int size = Integer.parseInt((String)
            server.getNegotiatedProperty(Sasl.RAW_SEND_SIZE));
        mBuffer = ByteBuffer.allocate(size);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        debug("write: enter len = %d", len);
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        for (int count = 0; count < len; ) {
            int n = writeBytes(b, off, len - count);
            count += n;
            off += n;
            debug("write: loop n = %d, count = %d", n, count);
        }
        debug("write: exit");
    }

    public void write(int b) throws IOException {
        debug("write: enter b = %d", b);
        ensureBuffer();
        mBuffer.put((byte) b);
    }
    
    private int writeBytes(byte[] b, int off, int len) throws IOException {
        ensureBuffer();
        if (len > mBuffer.remaining()) len = mBuffer.remaining();
        mBuffer.put(b, off, len);
        return len;
    }

    private void ensureBuffer() throws IOException {
        if (!mBuffer.hasRemaining()) flushBuffer();
    }

    private void flushBuffer() throws IOException {
        debug("flushBuffer: enter len = %d", mBuffer.position());
        byte[] b = mSaslServer.wrap(mBuffer.array(), 0, mBuffer.position());
        mOutputStream.writeInt(b.length);
        mOutputStream.write(b);
        mBuffer.clear();
    }

    public void flush() throws IOException {
        debug("flush: enter");
        if (mBuffer.position() > 0) flushBuffer();
        mOutputStream.flush();
    }

    public void close() throws IOException {
        flush();
        mOutputStream.close();
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG] " + format + "\n", args);
        }
    }
}
