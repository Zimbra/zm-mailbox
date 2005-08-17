package com.liquidsys.coco.ozserver;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface OzProtocolHandler {
    void handleConnect(OzConnectionHandler connection) throws IOException;

    void handleInput(OzConnectionHandler connection, ByteBuffer buffer, boolean matched) throws IOException;

    void handleEOF(OzConnectionHandler connection);
}
