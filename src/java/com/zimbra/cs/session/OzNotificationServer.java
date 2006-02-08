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
import com.zimbra.cs.util.ZimbraLog;

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
