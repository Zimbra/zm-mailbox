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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.DummySSLSocketFactory;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.cs.util.Zimbra;

class TestIDLE {

    static Log mLog = LogFactory.getLog(TestIDLE.class);

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
            Thread notifier = new Thread() {
                public void run() {
                    while (true) {
                        try {
                            synchronized (mAppLock) {
                                mConnection.writeAsciiWithCRLF("notification");
                            }
                            Thread.sleep(100);
                        } catch (Exception e) {
                            mLog.error("exception occurred notifying", e);
                            mConnection.close();
                        }
                    }
                }
            };
            notifier.start();
        }

        public void handleInput(ByteBuffer content, boolean matched) throws IOException {
            //mLog.info(OzUtil.byteBufferDebugDump("got input", content, false));
            synchronized (mAppLock) {
                mConnection.writeAsciiWithCRLF("response");
            }
            mCommandMatcher.reset();
            mConnection.setMatcher(mCommandMatcher);
            mConnection.enableReadInterest();
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
        ServerSocket serverSocket = NetUtil.getBoundServerSocket(null, port, false);
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
        
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        int notifications = 0;
        int responses = 0;
        
        try { Thread.sleep(2000); } catch (InterruptedException ie) { }

        while (true) {
            byte[] request = "request\r\n".getBytes();
            do {
                out.write(request);
                out.flush();
                String response = in.readLine();
                if (response.equals("notification")) {
                    notifications++;
                    mLog.info("notification");
                } else if (response.equals("response")) {
                    responses++;
                    mLog.info("response (" + responses + "/" + notifications + ")");
                    break;
                } else {
                    RuntimeException re = new RuntimeException("bad response: " +response);
                    mLog.error(re);
                    throw re;
                }
            } while (true);
        }
    }

    private static OzServer mServer;

    public static void main(String[] args) throws IOException, ServiceException {
        Zimbra.toolSetup("INFO", null, true);
        int port = Integer.parseInt(args[0]);
        boolean secure = Boolean.parseBoolean(args[1]);
        boolean debug = Boolean.parseBoolean(args[2]);
        startServer(port, secure, debug);
        startClient(port, secure);
        System.exit(1); // if client ever returns...
    }
}
