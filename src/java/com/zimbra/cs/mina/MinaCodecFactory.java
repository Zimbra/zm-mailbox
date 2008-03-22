/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import com.zimbra.common.util.ZimbraLog;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.io.IOException;

/*
 * MINA protocol codec factory. Decodes new MinaRequests' as they are received
 * by a connection then passed them to the appropriate MinaHandler.
 */
class MinaCodecFactory implements ProtocolCodecFactory {
    private MinaServer mServer;

    MinaCodecFactory(MinaServer server) {
        mServer = server;
    }

    public ProtocolEncoder getEncoder() {
        return new Encoder();
    }

    public ProtocolDecoder getDecoder() {
        return new Decoder();
    }

    private class Encoder extends ProtocolEncoderAdapter {
        public void encode(IoSession session, Object msg,
                           ProtocolEncoderOutput out) {
            out.write((ByteBuffer) msg);
        }
    }

    private class Decoder extends ProtocolDecoderAdapter {
        MinaRequest req;  // Request currently being decoded

        public void decode(IoSession session,
                           org.apache.mina.common.ByteBuffer in,
                           ProtocolDecoderOutput out) throws IOException {
            if (MinaServer.isDebugEnabled()) {
                ZimbraLog.nio.debug("Decode request: session = %d, bytes = %d",
                                    session.hashCode(), in.remaining());
            }
            java.nio.ByteBuffer bb = in.buf();
            while (bb.hasRemaining()) {
                if (req == null) {
                    req = mServer.createRequest(MinaIoHandler.getHandler(session));
                }
                try {
                    req.parse(bb);
                } catch (IllegalArgumentException e) {
                    // Drop bad request
                    ZimbraLog.imap.debug("Dropping bad request", e.getCause());
                    req = null;
                    break;
                }
                if (!req.isComplete()) break;
                out.write(req);
                req = null;
            }
            if (MinaServer.isDebugEnabled()) {
                ZimbraLog.nio.debug("Decode finished: session = %d", session.hashCode());
            }
        }
    }


}
