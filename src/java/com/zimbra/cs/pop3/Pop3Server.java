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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Zimbra;

public class Pop3Server extends TcpServer {

    private static final int D_POP3_THREADS = 10;
    private static final int D_POP3_BIND_PORT = 7110;
    private static final int D_POP3_BIND_SSL_PORT = 7995;    
    private static final String D_POP3_BIND_ADDRESS = null;
    private static final String D_POP3_ANNOUNCE_NAME = null;

    private static Pop3Server sPopServer;
    private static Pop3Server sPopSSLServer;
    
	private Log mLog;
    private boolean mAllowCleartextLogins;
    private boolean mConnectionSSL;

    boolean allowCleartextLogins()  { return mAllowCleartextLogins; }
    boolean isConnectionSSL()       { return mConnectionSSL; }
    
	public Pop3Server(int numThreads, int port, InetAddress bindAddress, boolean loginOK, boolean ssl) {
		super("Pop3Server", numThreads, port, bindAddress);
		mLog = LogFactory.getLog(Pop3Server.class.getName() + "/" + port);
        mAllowCleartextLogins = loginOK;
        mConnectionSSL = ssl;
	}

	protected ProtocolHandler newProtocolHandler() {
		return new Pop3Handler(this);
	}

	public void run() {
		/* Check is for initial sanity - you can always shoot yourself later by setting these to null. */
	    //if (getConfigName() == null) throw new IllegalStateException("Call LmtpServer.setConfigName() first");
	    //if (getConfigBackend() == null) throw new IllegalStateException("Call LmtpServer.setConfigBackend() first");
		super.run();
	}

	// TODO actually get it from configuration!

	/*
	 * Config idle. should be at least 10 minutes, per POP3 RFC 1939.
	 */
	public static final int DEFAULT_MAX_IDLE_SECONDS = 600;
	
	private int mConfigMaxIdleMilliSeconds = DEFAULT_MAX_IDLE_SECONDS * 1000;

	public void setConfigMaxIdleSeconds(int configMaxIdleSeconds) {
		mConfigMaxIdleMilliSeconds = configMaxIdleSeconds * 1000;
	}

	public int getConfigMaxIdleSeconds() {
		return mConfigMaxIdleMilliSeconds / 1000;
	}

	public int getConfigMaxIdleMilliSeconds() {
		return mConfigMaxIdleMilliSeconds;
	}

	/*
	 * Config name.
	 */
	private String mConfigName;

	public void setConfigNameFromHostname() {
		setConfigName(LC.zimbra_server_hostname.value());
	}
	
	public void setConfigName(String name) {
		mConfigName = name;
		mBanner = new String(name + " Zimbra POP3 server ready");
		mGoodbye = new String(name + " closing connection");
	}

	public String getConfigName() {
		return mConfigName;
	}
	
	/*
	 * This falls out of the configuration, so stick it here.
	 */
	private String mBanner;

	public String getBanner() {
		return mBanner;
	}
	
	private String mGoodbye;
	
	public String getGoodbye() {
		return mGoodbye;
	}

    private static InetAddress getBindAddress(String address) throws ServiceException {
        InetAddress bindAddress = null;
        if (address != null && address.length() > 0)
            try {
                bindAddress = InetAddress.getByName(address);
            } catch (UnknownHostException uhe) {
                throw ServiceException.FAILURE(uhe.getMessage(), uhe);
            }
        return bindAddress;
    }
    
	public synchronized static void startupPop3Server() throws ServiceException {
		if (sPopServer != null)
			return;
        
        Server server = Provisioning.getInstance().getLocalServer();
        boolean loginOK = server.getBooleanAttr(Provisioning.A_zimbraPop3CleartextLoginEnabled, false);
        InetAddress address = getBindAddress(server.getAttr(Provisioning.A_zimbraPop3BindAddress, D_POP3_BIND_ADDRESS));
        int port = server.getIntAttr(Provisioning.A_zimbraPop3BindPort, D_POP3_BIND_PORT);
        int numThreads = server.getIntAttr(Provisioning.A_zimbraPop3NumThreads, D_POP3_THREADS);

        sPopServer = new Pop3Server(numThreads, port, address, loginOK, false);

        String advName = server.getAttr(Provisioning.A_zimbraPop3AdvertisedName, D_POP3_ANNOUNCE_NAME);
        if (advName == null) {
            sPopServer.setConfigNameFromHostname();
        } else {
            sPopServer.setConfigName(advName);
        }
        
        Thread pop3Thread = new Thread(sPopServer);
        pop3Thread.setName("Pop3Server");
        pop3Thread.start();
	}
    
    public synchronized static void startupPop3SSLServer() throws ServiceException {
        if (sPopSSLServer != null)
            return;
        
        Server server = Provisioning.getInstance().getLocalServer();
        InetAddress address = getBindAddress(server.getAttr(Provisioning.A_zimbraPop3SSLBindAddress, D_POP3_BIND_ADDRESS));
        int port = server.getIntAttr(Provisioning.A_zimbraPop3SSLBindPort, D_POP3_BIND_SSL_PORT);
        int numThreads = server.getIntAttr(Provisioning.A_zimbraPop3NumThreads, D_POP3_THREADS);

        sPopSSLServer = new Pop3Server(numThreads, port, address, true, true);

        sPopSSLServer.setSSL(true);
        
        String advName = server.getAttr(Provisioning.A_zimbraPop3AdvertisedName, D_POP3_ANNOUNCE_NAME);
        if (advName == null) {
            sPopSSLServer.setConfigNameFromHostname();
        } else {
            sPopSSLServer.setConfigName(advName);
        }
        
        Thread pop3Thread = new Thread(sPopSSLServer);
        pop3Thread.setName("Pop3SSLServer");
        pop3Thread.start();
    }

	public synchronized static void shutdownPop3Servers() {
	    if (sPopServer != null)
            sPopServer.shutdown(10); // TODO shutdown grace period from config
        sPopServer = null;
        
        if (sPopSSLServer != null)
            sPopSSLServer.shutdown(10); // TODO shutdown grace period from config
        sPopSSLServer = null;
	}
    
    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup();
        startupPop3Server();
    }
}
