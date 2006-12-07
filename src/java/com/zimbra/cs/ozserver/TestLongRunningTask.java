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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * If there is no read interest enabled on a channel, and the channel is
 * registered with the selector, and the client closes the channel, the select()
 * loop just spins. Test for that.
 */
class TestLongRunningTask {

    static Log mLog = LogFactory.getLog(TestLongRunningTask.class);

    private static class ConnHandler implements OzConnectionHandler {
        private final OzConnection mConnection;

        ConnHandler(OzConnection connection) {
            mConnection = connection;
        }

        Object mAppLock = new Object();

        private OzByteArrayMatcher mCommandMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLF, null);

        public void handleConnect() throws IOException {
            mLog.info("inside connected");
            mConnection.setMatcher(mCommandMatcher);
            mConnection.enableReadInterest();
        }

        public void handleInput(ByteBuffer content, boolean matched) throws IOException {
            //mLog.info(OzUtil.byteBufferDebugDump("got input", content, false));
            if (!matched) {
                return;
            }
            try { Thread.sleep(Integer.MAX_VALUE); } catch (InterruptedException ie) { }
        }     

        public void handleAlarm() throws IOException {
            mLog.info("connection was idle, terminating");
            mConnection.close();
        }

        public void handleDisconnect() {
            mLog.info("connection disconnect");
        }
    }

    public static void startServer(int port, boolean secure, boolean debugLogging) throws IOException, ServiceException {
        final boolean isSecure = secure;
        final boolean isDebugLogging = debugLogging;
        OzConnectionHandlerFactory testHandlerFactory = new OzConnectionHandlerFactory() {
            public OzConnectionHandler newConnectionHandler(OzConnection connection) {
                if (isSecure) {
                    connection.addFilter(new OzTLSFilter(connection, isDebugLogging, mLog));
                }
                return new ConnHandler(connection);
            }
        };
        ServerSocket serverSocket = NetUtil.getOzServerSocket(null, port);
        mServer = new OzServer("IDLE", 64, serverSocket, testHandlerFactory, debugLogging, mLog);
        mServer.start();
    }

    public static void startClient(int port, boolean ssl) throws IOException {
        DummySSLSocketFactory socketFactory = new DummySSLSocketFactory();
        Socket socket;
        if (ssl) {
            socket = socketFactory.createSocket("localhost", port); 
        } else {
            socket = new Socket("localhost", port);
        }
        if (!socket.isConnected()) {
            throw new IOException("not connected");
        }
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        try { Thread.sleep(2000); } catch (InterruptedException ie) { }
        byte[] request = "request\r\n".getBytes();
        mLog.info("CLIENT: writing request");
        out.write(request);
        out.flush();
        try { Thread.sleep(2000); } catch (InterruptedException ie) { }
        out.write(request);
        out.flush();
        out.close();
        mLog.info("CLIENT: closing connection");
    }

    private static OzServer mServer;

    public static void main(String[] args) throws IOException, ServiceException {
        Zimbra.toolSetup("TRACE", null, true);
        int port = Integer.parseInt(args[0]);
        boolean secure = Boolean.parseBoolean(args[1]);
        boolean debug = Boolean.parseBoolean(args[2]);
        startServer(port, secure, debug);
        startClient(port, secure);
    }
}
