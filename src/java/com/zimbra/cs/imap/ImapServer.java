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

/*
 * Created on Apr 9, 2005
 */
package com.zimbra.cs.imap;

import java.net.ServerSocket;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * @author dkarp
 */
public class ImapServer extends TcpServer {

    // Config idle. should be at least 30 minutes, per IMAP4 RFC 3501.
    public static final int DEFAULT_MAX_IDLE_SECONDS = 1800;
    private int mConfigMaxIdleMilliSeconds = DEFAULT_MAX_IDLE_SECONDS * 1000;

    private static ImapServer sImapServer;
    private static ImapServer sImapSSLServer;

    private String  mBanner;
    private String  mGoodbye;
    private boolean mAllowCleartextLogins;
    private boolean mConnectionSSL;


	public ImapServer(int numThreads, ServerSocket serverSocket, boolean loginOK, boolean ssl) {
        super(ssl ? "ImapSSLServer" : "ImapServer", numThreads, serverSocket);
        mAllowCleartextLogins = loginOK;
        mConnectionSSL = ssl;
    }

	protected ProtocolHandler newProtocolHandler() {
        return new ImapHandler(this);
	}

    boolean allowCleartextLogins()  { return mAllowCleartextLogins; }
    boolean isConnectionSSL()       { return mConnectionSSL; }

	public int getConfigMaxIdleMilliSeconds() {
		return mConfigMaxIdleMilliSeconds;
	}

    private void setHostname(Server server) {
        String name = server.getAttr(Provisioning.A_zimbraImapAdvertisedName, null);
        if (name == null || name.trim().equals(""))
            name = server.getAttr(Provisioning.A_zimbraServiceHostname, null);
        if (name == null || name.trim().equals(""))
            name = "localhost";

        mBanner  = "OK " + name + " Zimbra IMAP4rev1 service ready";
        mGoodbye = "BYE Zimbra IMAP4rev1 server terminating connection";
    }

    public String getBanner()  { return mBanner; }
    public String getGoodbye() { return mGoodbye; }

    public synchronized static void startupImapServer() throws ServiceException {
        if (sImapServer != null)
            return;
        
        Server server = Provisioning.getInstance().getLocalServer();
        int threads = server.getIntAttr(Provisioning.A_zimbraImapNumThreads, 10);
        boolean loginOK = server.getBooleanAttr(Provisioning.A_zimbraImapCleartextLoginEnabled, false);
        String address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null);
        int port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);

        ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, false);

        sImapServer = new ImapServer(threads, serverSocket, loginOK, false);
        sImapServer.setSSL(false);
        sImapServer.setHostname(server);
        
        Thread imap = new Thread(sImapServer);
        imap.setName("ImapServer");
        imap.start();            
    }

    public synchronized static void startupImapSSLServer() throws ServiceException {
        if (sImapSSLServer != null)
            return;

        Server server = Provisioning.getInstance().getLocalServer();
        int threads = server.getIntAttr(Provisioning.A_zimbraImapNumThreads, 10);
        String address = server.getAttr(Provisioning.A_zimbraImapSSLBindAddress, null);
        int port = server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, Config.D_IMAP_SSL_BIND_PORT);
        
        ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, true);
        
        sImapSSLServer = new ImapServer(threads, serverSocket, true, true);
        sImapSSLServer.setSSL(true);
        sImapSSLServer.setHostname(server);

        Thread imaps = new Thread(sImapSSLServer);
        imaps.setName("ImapSSLServer");
        imaps.start();            
    }

    public synchronized static void shutdownImapServers() {
        if (sImapServer != null)
            sImapServer.shutdown(10); // TODO shutdown grace period from config
        sImapServer = null;

        if (sImapSSLServer != null)
            sImapSSLServer.shutdown(10); // TODO shutdown grace period from config
        sImapSSLServer = null;
    }
    
    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup("INFO");
        startupImapServer();
    }
}
