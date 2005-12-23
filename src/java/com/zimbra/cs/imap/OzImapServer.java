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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.net.ServerSocket;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.ozserver.OzConnection;
import com.zimbra.cs.ozserver.OzConnectionHandler;
import com.zimbra.cs.ozserver.OzConnectionHandlerFactory;
import com.zimbra.cs.ozserver.OzServer;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

public class OzImapServer {
    private static OzServer mServer;
    
    public static void main(String[] args) throws ServiceException, IOException {
        Zimbra.toolSetup("DEBUG", null, true);
        startup();
    }

    private static final int IMAP_READ_BUFFER_SIZE = 1024;
    
    public synchronized static void startup() throws ServiceException, IOException {
        if (mServer != null)
            return;

        OzConnectionHandlerFactory imapHandlerFactory = new OzConnectionHandlerFactory() {
            public OzConnectionHandler newConnectionHandler(OzConnection conn) {
                return new OzImapConnectionHandler(conn);
            }
        };

        Server server = Provisioning.getInstance().getLocalServer();
        boolean allowCleartextLogins = server.getBooleanAttr(Provisioning.A_zimbraImapCleartextLoginEnabled, false);
        String address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null);
        int port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);

        ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, false);

        mServer = new OzServer("IMAP", IMAP_READ_BUFFER_SIZE, serverSocket, imapHandlerFactory, ZimbraLog.imap);
        mServer.start();
        
        String name = server.getAttr(Provisioning.A_zimbraImapAdvertisedName, null);
        if (name == null || name.trim().equals(""))
            name = server.getAttr(Provisioning.A_zimbraServiceHostname, null);
        if (name == null || name.trim().equals(""))
            name = "localhost";
    }
    
    public synchronized static void shutdown() {
        if (mServer != null) {
            mServer.shutdown();
            mServer = null;
        }
    }
}
