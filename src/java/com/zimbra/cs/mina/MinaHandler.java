package com.zimbra.cs.mina;

import java.io.IOException;

/**
 * MINA-based protocol handler.
 */
public interface MinaHandler {
    void connectionOpened() throws IOException;
    void connectionClosed() throws IOException;
    void connectionIdle() throws IOException;
    void requestReceived(MinaRequest req) throws IOException;
}
