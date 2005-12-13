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

package com.zimbra.cs.lmtpserver;

import java.net.ServerSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.Counter;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.cs.util.Zimbra;

public class LmtpServer extends TcpServer {

    Counter mLmtpRcvdMsgs = Counter.getInstance("lmtp_rcvd_msgs");
    Counter mLmtpRcvdBytes = Counter.getInstance("lmtp_rcvd_bytes");
    Counter mLmtpRcvdRcpt = Counter.getInstance("lmtp_rcvd_rcpt");
    Counter mLmtpDlvdMsgs = Counter.getInstance("lmtp_dlvd_msgs");
    Counter mLmtpDlvdBytes = Counter.getInstance("lmtp_dlvd_bytes");

	private Log mLog;

	public LmtpServer(int numThreads, ServerSocket serverSocket) {
		super("LmtpServer", numThreads, serverSocket);
		mLog = LogFactory.getLog(LmtpServer.class.getName() + "/" + serverSocket.getLocalPort());
	}

	protected ProtocolHandler newProtocolHandler() {
		return new LmtpHandler(this);
	}

	public void run() {
		/* Check is for initial sanity - you can always shoot yourself later by setting these to null. */
		if (getConfigName() == null) throw new IllegalStateException("Call LmtpServer.setConfigName() first");
		if (getConfigBackend() == null) throw new IllegalStateException("Call LmtpServer.setConfigBackend() first");
		super.run();
	}


	// TODO actually get it from configuration!

	/*
	 * Config idle.
	 */
	public static final int DEFAULT_MAX_IDLE_SECONDS = 300;
	
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
		m220Greeting = new String("220 " + name + " Zimbra LMTP ready");
		m221Goodbye = new String("221 " + name + " closing connection");
        m421Error = new String("421 4.3.2 " + name + " Service not available, closing transmission channel");
	}

    /*
     * Config to allow address extensions.
     */
    private String mRecipientDelimiter;
    
    public void setConfigRecipientDelimiter(String delimiter) {
        mRecipientDelimiter = delimiter; 
    }

    public String getConfigRecipientDelimiter() {
        return mRecipientDelimiter;
    }
    
	public String getConfigName() {
		return mConfigName;
	}
	
	/*
	 * Config size.
	 */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = Integer.MAX_VALUE;
	
	private int mConfigMaxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

	public int getConfigMaxMessageSize() {
		return mConfigMaxMessageSize;
	}
	
	public void setConfigMaxMessageSize(int configMaxMessageSize) {
		mConfigMaxMessageSize = configMaxMessageSize;
	}

	/*
	 * Config backend.
	 */
	private LmtpBackend mConfigBackend;
	
	public void setConfigBackend(LmtpBackend backend) {
		mConfigBackend = backend;
	}
	
	public LmtpBackend getConfigBackend() {
		return mConfigBackend;
	}
	
	private void setDefaultConfigBackend() {
		mConfigBackend = new ZimbraLmtpBackend();
	}
	
	/*
	 * This falls out of the configuration, so stick it here.
	 */
	private String m220Greeting;

	public String get220Greeting() {
		return m220Greeting;
	}
	
	private String m221Goodbye;
	
	public String get221Goodbye() {
		return m221Goodbye;
	}

    private String m421Error;

    public String get421Error() {
    	return m421Error;
    }

	private static LmtpServer theInstance;

	public static void startupLmtpServer() throws ServiceException {
		if (theInstance != null)
			return;

        Server sconfig = Provisioning.getInstance().getLocalServer();
        
        int numThreads = sconfig.getIntAttr(Provisioning.A_zimbraLmtpNumThreads, -1);
        if (numThreads < 0) {
            Zimbra.halt("invalid value " + numThreads + " for " + Provisioning.A_zimbraLmtpNumThreads);
        }

        int port = sconfig.getIntAttr(Provisioning.A_zimbraLmtpBindPort, -1);
        if (port < 0) {
            Zimbra.halt("invalid value " + port + " for " + Provisioning.A_zimbraLmtpBindPort);
        }

        String address = sconfig.getAttr(Provisioning.A_zimbraLmtpBindAddress, null);
        String advertisedName = sconfig.getAttr(Provisioning.A_zimbraLmtpAdvertisedName, null);
        
        Config gconfig = Provisioning.getInstance().getConfig();
        String delimiter = gconfig.getAttr(Provisioning.A_zimbraMtaRecipientDelimiter, null);

        ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, false);

        theInstance = new LmtpServer(numThreads, serverSocket);
        theInstance.setConfigRecipientDelimiter(delimiter);
        
        if (advertisedName == null || advertisedName.length() == 0) {
            theInstance.setConfigNameFromHostname();
        } else {
            theInstance.setConfigName(advertisedName);
        }
        theInstance.setConfigBackend(new ZimbraLmtpBackend());
        Thread lmtpThread = new Thread(theInstance);
        lmtpThread.setName("LmtpServer");
        lmtpThread.start();
	}

	public static void shutdownLmtpServer() {
		if (theInstance != null) {
			theInstance.shutdown(10); // TODO shutdown grace period from config
		}
		theInstance = null;
	}
}
