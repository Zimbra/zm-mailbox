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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SaslOutputStream extends OutputStream {
    private final DataOutputStream mOutputStream;
    private final SaslServer mSaslServer;
    private final SaslOutputBuffer mBuffer;

    private static final boolean DEBUG = false;

    public SaslOutputStream(OutputStream os, SaslServer server) {
        mOutputStream = new DataOutputStream(os);
        mSaslServer = server;
        mBuffer = new SaslOutputBuffer(SaslUtil.getMaxSendSize(mSaslServer));
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
        ByteBuffer bb = ByteBuffer.wrap(b, off, len);
        mBuffer.put(bb);
        return bb.position();
    }

    private void ensureBuffer() throws IOException {
        if (mBuffer.isFull()) flushBuffer();
    }

    private void flushBuffer() throws IOException {
        byte[] b = mBuffer.wrap(mSaslServer);
        mOutputStream.writeInt(b.length);
        mOutputStream.write(b);
        mBuffer.clear();
    }

    public void flush() throws IOException {
        if (DEBUG) debug("flushBuffer: size = %d", mBuffer.size());
        if (mBuffer.size() > 0) flushBuffer();
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
