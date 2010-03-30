/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.nio.mina;

import com.zimbra.cs.nio.NioDecoder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

class MinaCodecFactory implements ProtocolCodecFactory {
    private final MinaServer server;

    MinaCodecFactory(MinaServer server) {
        this.server = server;
    }

    public ProtocolDecoder getDecoder() {
        return new Decoder();
    }

    public ProtocolEncoder getEncoder() {
        return new Encoder();
    }

    private class Decoder extends ProtocolDecoderAdapter {
        private final NioDecoder decoder = server.newDecoder();

        public void decode(IoSession session, ByteBuffer bb, final ProtocolDecoderOutput out) {
            int pos = bb.position();
            decoder.decode(bb.buf(), new NioDecoder.Output() {
                public void write(Object msg) {
                    out.write(msg);
                }
            });
            server.getStats().bytesReceived(bb.position() - pos);
        }
    }

    private class Encoder extends ProtocolEncoderAdapter {
        public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) {
            if (msg instanceof ByteBuffer) {
                ByteBuffer bb = (ByteBuffer) msg;
                out.write(bb);
            }
        }
    }
}
