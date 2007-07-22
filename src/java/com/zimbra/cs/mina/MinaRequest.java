package com.zimbra.cs.mina;

import org.apache.mina.common.IoSession;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * MINA protocol request.
 */
public interface MinaRequest {
    void parse(IoSession session, ByteBuffer bb) throws IOException;
    boolean isComplete();
}
