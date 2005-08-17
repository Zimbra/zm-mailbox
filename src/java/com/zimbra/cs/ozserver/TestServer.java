package com.zimbra.cs.ozserver;

import java.io.IOException;

class TestServer {
    
    static final int PORT = 10043;
    
    private static OzServer mServer;
   
    public static void main(String[] args) throws IOException {
        OzProtocolHandlerFactory testHandlerFactory = new OzProtocolHandlerFactory() {
            public OzProtocolHandler newProtocolHandler() {
                return new TestProtocolHandler();
            }
        };
        mServer = new OzServer("Test", null, PORT, testHandlerFactory);
        mServer.setSnooper(new OzSnooper(System.out, OzSnooper.ALL));
        mServer.start();
    }
    
    static void shutdown() throws IOException {
        mServer.shutdown();
    }
}
