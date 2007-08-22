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

package com.zimbra.cs.mina;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.OutputStream;

public abstract class MinaOutputStream extends OutputStream {
    private final int mMinSize;
    private ByteBuffer mBuffer;
    private boolean closed;

    public MinaOutputStream(int minSize) {
        mMinSize = minSize;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len)
            throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
	    throw new IndexOutOfBoundsException();
        }
        if (closed) throw new IOException("Stream has been closed");
        while (len > 0) {
            if (mBuffer == null) {
                mBuffer = ByteBuffer.allocate(Math.max(len, mMinSize));
            }
            int count = Math.min(len, mBuffer.remaining());
            mBuffer.put(b, off, count);
            if (!mBuffer.hasRemaining()) flushBytes();
            len -= count;
            off += count;
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    @Override
    public synchronized void flush() throws IOException {
        if (!closed) flushBytes();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            flushBytes();
            closed = true;
        }
    }

    private void flushBytes() throws IOException {
        if (mBuffer == null || mBuffer.position() <= 0) return;
        mBuffer.flip();
        writeBytes(mBuffer);
        mBuffer = null;
    }

    /**
     * Writes specified bytes. Implementation can assume that byte buffer will
     * not be subsequently reused, so this is safe for asynchronous writes.
     *
     * @param bb the bytes to be written
     * @throws IOException if an I/O error occurs
     */
    protected abstract void writeBytes(ByteBuffer bb) throws IOException;
}
