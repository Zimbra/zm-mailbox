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
package com.zimbra.cs.session;

import java.io.IOException;
import java.net.ServerSocket;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.ozserver.OzConnection;
import com.zimbra.cs.ozserver.OzConnectionHandler;
import com.zimbra.cs.ozserver.OzConnectionHandlerFactory;
import com.zimbra.cs.ozserver.OzServer;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.common.util.ZimbraLog;

public class OzNotificationServer {
    
    private static OzServer sServer;
    
    private static final int NOTIFY_READ_BUFFER_SIZE = 256;
    
    public synchronized static void startup() throws ServiceException {
        if (sServer != null)
            return;
        
        OzConnectionHandlerFactory notificationHandlerFactory = new OzConnectionHandlerFactory() {
            public OzConnectionHandler newConnectionHandler(OzConnection conn) {
                return new OzNotificationConnectionHandler(conn);
            }
        };

        Server server = Provisioning.getInstance().getLocalServer();
        
        String address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null); // fixme
//        int port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, 7035); // fixme 
        int port = 7035;

        ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, false);
        
        try {
            
            sServer = new OzServer("NOTIFY", NOTIFY_READ_BUFFER_SIZE, serverSocket, notificationHandlerFactory, true, ZimbraLog.misc);
            sServer.start();
            
        } catch (IOException e) {
            throw ServiceException.FAILURE("Initializing NOTIFY server at "+address+":"+port, e);
        }
    }
    
    public synchronized static void shutdown() {
        if (sServer != null) {
            sServer.shutdown();
            sServer = null;
        }
    }
    
    
}
