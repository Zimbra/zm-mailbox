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
    private MinaServer server;

    MinaCodecFactory(MinaServer server) {
        this.server = server;
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
                bb = toByteBuffer((String) msg);
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
                    req = server.createRequest(MinaIoHandler.getHandler(session));
                }
                req.parse(session, bb);
                if (!req.isComplete()) break;
                out.write(req);
                req = null;
            }
        }
    }

    private static ByteBuffer toByteBuffer(String s) {
        ByteBuffer bb = ByteBuffer.allocate(s.length());
        for (int i = 0; i < s.length(); i++) {
            bb.put(i, (byte) s.charAt(i));
        }
        return bb;
    }
}
