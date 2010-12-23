/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.security.sasl;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslServer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MINA SASL filter implementation.
 */
public class SaslFilter extends IoFilterAdapter {
    private final SaslSecurityLayer mSecurityLayer;
    private final SaslInputBuffer mInputBuffer;
    private final SaslOutputBuffer mOutputBuffer;

    private static final boolean DEBUG = false;

    /** When set, encryption is disabled for the first write */
    public static final String DISABLE_ENCRYPTION_ONCE =
        SaslFilter.class.getName() + ".DisableEncryptionOnce";

    public SaslFilter(SaslServer server) {
        this(SaslSecurityLayer.getInstance(server));
    }

    public SaslFilter(SaslClient client) {
        this(SaslSecurityLayer.getInstance(client));
    }

    public SaslFilter(SaslSecurityLayer securityLayer) {
        mSecurityLayer = securityLayer;
        mInputBuffer = new SaslInputBuffer(securityLayer.getMaxRecvSize());
        mOutputBuffer = new SaslOutputBuffer(securityLayer.getMaxSendSize());
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws IOException {
        IoBuffer buf = (IoBuffer) message;
        debug("messageReceived: size = %d", buf.remaining());
        synchronized (mInputBuffer) {
            // Read and decrypt cipher blocks from input buffer
            while (buf.hasRemaining()) {
                debug("messageReceived: remaining = %d", buf.remaining());
                mInputBuffer.put(buf);
                debug("messageReceived: remaining = %d", buf.remaining());
                debug("messageReceived: length = %d", mInputBuffer.getLength());
                if (mInputBuffer.isComplete()) {
                    debug("messageReceived: input complete");
                    byte[] b = mInputBuffer.unwrap(mSecurityLayer);
                    nextFilter.messageReceived(session, IoBuffer.wrap(b));
                    mInputBuffer.clear();
                }
            }
        }
        buf.clear();
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws IOException {
        IoBuffer buf = (IoBuffer) writeRequest.getMessage();
        // Allows us to disable encryption until authentication OK response
        // has been sent to client.
        if (session.containsAttribute(DISABLE_ENCRYPTION_ONCE)) {
            debug("filterWrite: before encryption size = %d", buf.remaining());
            session.removeAttribute(DISABLE_ENCRYPTION_ONCE);
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        // Encrypt input buffer
        debug("filterWrite: message size = %d", buf.remaining());
        if (buf.remaining() == 0) {
            // Some clients (i.e. imtest) choke upon receiving an empty block
            debug("filterWrite: skipping encryption of empty buffer");
            nextFilter.filterWrite(session, writeRequest);
            return;
        }

        List<IoBuffer> buffers = encrypt(buf);
        buf.clear();

        // Create and send new WriteRequest for each output buffer. The last
        // request includes the WriteFuture from the original request, and this
        // ensures correctness of the WriteFuture since the earlier requests
        // will have been written before the last.
        int size = buffers.size();
        for (int i = 0; i < size - 1; i++) {
            nextFilter.filterWrite(session, new DefaultWriteRequest(buffers.get(i)));
        }
        nextFilter.filterWrite(session, new DefaultWriteRequest(buffers.get(size - 1), writeRequest.getFuture()));
    }

    /*
     * Encrypt input buffer into possibly multiple buffers. Each cipher block
     * is split into two byte buffers: the first contains the block length and
     * the second the actual data. This is more efficient than having to copy
     * the bytes around in order to merge into one big buffer.
     */
    private List<IoBuffer> encrypt(IoBuffer buf) throws IOException {
        debug("encrypt enter: input buffer size = %d", buf.remaining());
        List<IoBuffer> buffers = new ArrayList<IoBuffer>(2);
        synchronized (mOutputBuffer) {
            // May loop more than once if RAW_SEND_SIZE is exceeded
            do {
                mOutputBuffer.put(buf);
                byte[] b = mOutputBuffer.wrap(mSecurityLayer);
                debug("encrypt wrap: encrypted buffer size = %d", b.length);
                buffers.add(IoBuffer.allocate(4).putInt(b.length).flip());
                buffers.add(IoBuffer.wrap(b));
                mOutputBuffer.clear();
            } while (buf.hasRemaining());
        }
        debug("encrypt exit: buffer count = %d", buffers.size());
        return buffers;
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws IOException {
        debug("onPostRemove: enter");
        mSecurityLayer.dispose();
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG SaslFilter] " + format + "\n", args);
        }
    }
}
