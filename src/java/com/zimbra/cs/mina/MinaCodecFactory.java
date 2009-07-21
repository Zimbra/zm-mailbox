/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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

package com.zimbra.cs.mina;

import com.zimbra.common.util.ZimbraLog;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.io.IOException;

import static com.zimbra.cs.mina.Constants.*;

/*
 * MINA protocol codec factory. Decodes request bytes and then passes complete
 * request to MINA handler.
 */
class MinaCodecFactory implements ProtocolCodecFactory {
    private MinaServer mServer;

    MinaCodecFactory(MinaServer server) {
        mServer = server;
    }

    public ProtocolEncoder getEncoder() {
        return new ProtocolEncoderAdapter() {
            public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) {
                out.write((ByteBuffer) msg);
            }
        };
    }

    public ProtocolDecoder getDecoder() {
        return new ProtocolDecoderAdapter() {
            public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out)
                throws IOException {
                decodeBytes(session, in, out);
            }
        };
    }

    private void decodeBytes(IoSession session, ByteBuffer in, ProtocolDecoderOutput out)
        throws IOException {
        java.nio.ByteBuffer bb = in.buf();
        while (bb.hasRemaining()) {
            MinaRequest req = (MinaRequest) session.getAttribute(MINA_REQUEST_ATTR);
            if (req == null) {
                MinaHandler handler = (MinaHandler) session.getAttribute(MINA_HANDLER_ATTR);
                assert handler != null;
                req = mServer.createRequest(handler);
                session.setAttribute(MINA_REQUEST_ATTR, req);
            }
            try {
                req.parse(bb);
            } catch (IllegalArgumentException e) {
                // Drop bad request
                ZimbraLog.imap.debug("Dropping bad request", e.getCause());
                session.removeAttribute(MINA_REQUEST_ATTR);
                break;
            }
            if (!req.isComplete()) break;
            out.write(req);
            session.removeAttribute(MINA_REQUEST_ATTR);
        }
    }

}
