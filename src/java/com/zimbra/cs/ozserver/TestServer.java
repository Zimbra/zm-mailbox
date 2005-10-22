/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.NetUtil;

class TestServer {
    
    static Log mLog = LogFactory.getLog(TestServer.class);

    private static OzServer mServer;

    public TestServer(int port) throws IOException, ServiceException {
        OzConnectionHandlerFactory testHandlerFactory = new OzConnectionHandlerFactory() {
            public OzConnectionHandler newConnectionHandler(OzConnection conn) {
                return new TestConnectionHandler(conn);
            }
        };
        ServerSocket serverSocket = NetUtil.getBoundServerSocket(null, port, false);
    	mServer = new OzServer("Test", 64, serverSocket, testHandlerFactory, mLog);
        mServer.setSnooper(new OzSnooper(mLog, OzSnooper.ALL));
        mServer.start();
    }
    
    public static void main(String[] args) throws IOException, ServiceException {
    	new TestServer(Integer.parseInt(args[0]));
    }
    
    void shutdown() {
        mServer.shutdown();
    }
}
