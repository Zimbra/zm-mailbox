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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MINA SASL filter implementation.
 */
public class SaslFilter extends IoFilterAdapter {
    private final SaslServer mSaslServer;
    private final SaslInputBuffer mInputBuffer;
    private final SaslOutputBuffer mOutputBuffer;

    private static final int MAX_SEND_SIZE = 4096;
    private static final int MAX_RECV_SIZE = 4096;
    
    private static final boolean DEBUG = true;

    /** When set, encryption is disabled for the first write */
    public static final String DISABLE_ENCRYPTION_ONCE =
        SaslFilter.class.getName() + ".DisableEncryptionOnce"; 

    public SaslFilter(SaslServer server) {
        mSaslServer = server;
        mInputBuffer = new SaslInputBuffer(getMaxRecvSize(mSaslServer));
        mOutputBuffer = new SaslOutputBuffer(getMaxSendSize(mSaslServer));
    }

    private static int getMaxSendSize(SaslServer server) {
        String s = (String) server.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return MAX_SEND_SIZE;
        }
    }

    private static int getMaxRecvSize(SaslServer server) {
        String s = (String) server.getNegotiatedProperty(Sasl.MAX_BUFFER);
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return MAX_RECV_SIZE;
        }
    }
    
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
                                Object message) throws IOException {
        ByteBuffer buf = (ByteBuffer) message;
        debug("messageReceived: size = %d", buf.remaining());
        synchronized (mInputBuffer) {
            // Read and decrypt cipher blocks from input buffer
            while (buf.hasRemaining()) {
                debug("messageReceived: remaining = %d", buf.remaining());
                mInputBuffer.read(buf);
                debug("messageReceived: remaining = %d", buf.remaining());
                debug("messageReceived: length = %d", mInputBuffer.getLength());
                if (mInputBuffer.isComplete()) {
                    debug("messageReceived: input complete");
                    byte[] b = mInputBuffer.unwrap(mSaslServer);
                    nextFilter.messageReceived(session, ByteBuffer.wrap(b));
                    mInputBuffer.clear();
                }
            }
        }
        buf.release();
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
                            WriteRequest writeRequest) throws IOException {
        ByteBuffer buf = (ByteBuffer) writeRequest.getMessage();
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
        
        List<ByteBuffer> buffers = encrypt(buf);
        buf.release();

        // Create and send new WriteRequest for each output buffer. The last
        // request includes the WriteFuture from the original request, and this
        // ensures correctness of the WriteFuture since the earlier requests
        // will have been written before the last.
        int size = buffers.size();
        for (int i = 0; i < size - 1; i++) {
            nextFilter.filterWrite(session, new WriteRequest(buffers.get(i)));
        }
        nextFilter.filterWrite(session, new WriteRequest(
            buffers.get(size - 1), writeRequest.getFuture()));
    }

    /*
     * Encrypt input buffer into possibly multiple buffers. Each cipher block
     * is split into two byte buffers: the first contains the block length and
     * the second the actual data. This is more efficient than having to copy
     * the bytes around in order to merge into one big buffer.
     */
    private List<ByteBuffer> encrypt(ByteBuffer bb) throws IOException {
        debug("encrypt enter: input buffer size = %d", bb.remaining());
        List<ByteBuffer> buffers = new ArrayList<ByteBuffer>(2);
        synchronized (mOutputBuffer) {
            // May loop more than once if RAW_SEND_SIZE is exceeded 
            do {
                mOutputBuffer.write(bb);
                byte[] b = mOutputBuffer.wrap(mSaslServer);
                debug("encrypt wrap: encrypted buffer size = %d", b.length);
                buffers.add(ByteBuffer.allocate(4).putInt(b.length).flip());
                buffers.add(ByteBuffer.wrap(b));
                mOutputBuffer.clear();
            } while (bb.hasRemaining());
        }
        debug("encrypt exit: buffer count = %d", buffers.size());
        return buffers;
    }

    @Override
    public void filterClose(NextFilter nextFilter, IoSession session)
            throws IOException {
        mSaslServer.dispose();
    }
    
    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG SaslFilter] " + format + "\n", args);
        }
    }
}
