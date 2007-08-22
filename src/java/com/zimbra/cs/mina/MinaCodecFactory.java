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

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.common.IoSession;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * MINA protocol codec factory.
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
            ByteBuffer bb;
            if (msg instanceof String) {
                bb = MinaUtil.toByteBuffer((String) msg);
            } else if (msg instanceof ByteBuffer) {
                bb = (ByteBuffer) msg;
            } else {
                throw new AssertionError();
            }
            out.write(org.apache.mina.common.ByteBuffer.wrap(bb));
        }
    }

    private class Decoder extends ProtocolDecoderAdapter {
        MinaRequest req;  // Request currently being decoded

        public void decode(IoSession session,
                           org.apache.mina.common.ByteBuffer in,
                           ProtocolDecoderOutput out) throws IOException {
            ByteBuffer bb = in.buf();
            while (bb.hasRemaining()) {
                if (req == null) {
                    req = mServer.createRequest(MinaIoHandler.getHandler(session));
                }
                req.parse(bb);
                if (!req.isComplete()) break;
                out.write(req);
                req = null;
            }
        }
    }


}
