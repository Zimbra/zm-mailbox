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

/*
 * Created on Apr 9, 2005
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.ozserver.OzConnection;
import com.zimbra.cs.ozserver.OzConnectionHandler;
import com.zimbra.cs.ozserver.OzConnectionHandlerFactory;
import com.zimbra.cs.ozserver.OzServer;
import com.zimbra.cs.ozserver.OzTLSFilter;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.NetUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp
 */
public class ImapServer extends TcpServer
implements RealtimeStatsCallback {

    private static Object sImapServer;
    private static Object sImapSSLServer;

    private boolean mAllowCleartextLogins;
    private boolean mConnectionSSL;


	public ImapServer(int numThreads, ServerSocket serverSocket, boolean loginOK, boolean ssl) {
        super(ssl ? "ImapSSLServer" : "ImapServer", numThreads, serverSocket);
        mAllowCleartextLogins = loginOK;
        mConnectionSSL = ssl;
        ZimbraPerf.addStatsCallback(this);
    }

	protected ProtocolHandler newProtocolHandler() {
        return new ImapHandler(this);
	}

    boolean allowCleartextLogins()  { return mAllowCleartextLogins; }
    boolean isConnectionSSL()       { return mConnectionSSL; }

	public int getConfigMaxIdleMilliSeconds() {
		return (int)ImapSession.IMAP_IDLE_TIMEOUT_MSEC;
	}

    static String getBanner() {
        return "OK " + LC.zimbra_server_hostname.value() + " Zimbra IMAP4rev1 service ready";
    }
    
    static String getGoodbye() {
        return "BYE Zimbra IMAP4rev1 server terminating connection";
    }
    
    public static final int IMAP_READ_SIZE_HINT = 4096;
    
    /* Throw out connections that do not authenticate in a minute */
    public static final long IMAP_UNAUTHED_CONNECTION_MAX_IDLE_MILLISECONDS = 1 * Constants.MILLIS_PER_MINUTE;
    
    public static final int IMAP_WRITE_QUEUE_MAX_SIZE_UNAUTH = LC.nio_imap_write_queue_max_size_unauth.intValue();
    
    public synchronized static void startupImapServer() throws ServiceException {
        if (sImapServer != null)
            return;
        
        Server server = Provisioning.getInstance().getLocalServer();
        int threads = server.getIntAttr(Provisioning.A_zimbraImapNumThreads, 10);
        boolean loginOK = server.getBooleanAttr(Provisioning.A_zimbraImapCleartextLoginEnabled, false);
        String address = server.getAttr(Provisioning.A_zimbraImapBindAddress, null);
        int port = server.getIntAttr(Provisioning.A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);

        ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, false);

        if (LC.nio_imap_enable.booleanValue()) {
            OzConnectionHandlerFactory imapHandlerFactory = new OzConnectionHandlerFactory() {
                public OzConnectionHandler newConnectionHandler(OzConnection conn) {
                    conn.setAlarm(IMAP_UNAUTHED_CONNECTION_MAX_IDLE_MILLISECONDS);
                    conn.setWriteQueueMaxCapacity(IMAP_WRITE_QUEUE_MAX_SIZE_UNAUTH);
                    return new OzImapConnectionHandler(conn);
                }
            };

            boolean debugLog = LC.nio_imap_debug_logging.booleanValue();
            try {
                OzServer ozserver = new OzServer("IMAP", IMAP_READ_SIZE_HINT, serverSocket, imapHandlerFactory, debugLog, ZimbraLog.imap);
                ozserver.setProperty(OzImapConnectionHandler.PROPERTY_ALLOW_CLEARTEXT_LOGINS, Boolean.toString(loginOK));
                ozserver.start();
                sImapServer = ozserver;
            } catch (IOException ioe) {
                Zimbra.halt("failed to create OzServer for IMAP", ioe);
            }
        } else {
            ImapServer imapServer = new ImapServer(threads, serverSocket, loginOK, false);
            imapServer.setSSL(false);
            sImapServer = imapServer;
            Thread imap = new Thread(imapServer);
            imap.setName("ImapServer");
            imap.start();
        }
    }

    public synchronized static void startupImapSSLServer() throws ServiceException {
        if (sImapSSLServer != null)
            return;

        Server server = Provisioning.getInstance().getLocalServer();
        int threads = server.getIntAttr(Provisioning.A_zimbraImapNumThreads, 10);
        String address = server.getAttr(Provisioning.A_zimbraImapSSLBindAddress, null);
        int port = server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, Config.D_IMAP_SSL_BIND_PORT);
        
        if (LC.nio_imap_enable.booleanValue()) {
            ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, false);
            
            final boolean debugLogging = LC.nio_imap_debug_logging.booleanValue();
            
            OzConnectionHandlerFactory imapHandlerFactory = new OzConnectionHandlerFactory() {
                public OzConnectionHandler newConnectionHandler(OzConnection conn) {
                    conn.setAlarm(IMAP_UNAUTHED_CONNECTION_MAX_IDLE_MILLISECONDS);
                    conn.setWriteQueueMaxCapacity(IMAP_WRITE_QUEUE_MAX_SIZE_UNAUTH);
                    conn.addFilter(new OzTLSFilter(conn, debugLogging, ZimbraLog.imap));
                    return new OzImapConnectionHandler(conn);
                }
            };
            try {
                OzServer ozserver = new OzServer("IMAPS", IMAP_READ_SIZE_HINT, serverSocket, imapHandlerFactory, debugLogging, ZimbraLog.imap);
                ozserver.setProperty(OzImapConnectionHandler.PROPERTY_SECURE_SERVER, "true");
                ozserver.start();
                sImapSSLServer = ozserver;
            } catch (IOException ioe) {
                Zimbra.halt("failed to create OzServer for IMAPS", ioe);
            }
        } else {
            ServerSocket serverSocket = NetUtil.getBoundServerSocket(address, port, true);
            
            ImapServer imapsServer = new ImapServer(threads, serverSocket, true, true);
            imapsServer.setSSL(true);
            sImapSSLServer = imapsServer;
            Thread imaps = new Thread(imapsServer);
            imaps.setName("ImapSSLServer");
            imaps.start();            
        }
    }

    public synchronized static void shutdownImapServers() {
        if (sImapServer != null) {
            if (sImapServer instanceof ImapServer) {
                ((ImapServer)sImapServer).shutdown(10); // TODO shutdown grace period from config
            } else if (sImapServer instanceof OzServer) {
                ((OzServer)sImapServer).shutdown();
            }
            sImapServer = null;
        }

        if (sImapSSLServer != null) {
            if (sImapSSLServer instanceof ImapServer) {
                ((ImapServer)sImapSSLServer).shutdown(10); // TODO shutdown grace period from config
            } else if (sImapSSLServer instanceof OzServer) {
                ((OzServer)sImapSSLServer).shutdown();
            }
            sImapSSLServer = null;
        }
    }
    
    /**
     * Implementation of <code>RealtimeStatsCallback</code> that returns the number
     * of active handlers for this server.
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        String statName = mConnectionSSL ? ZimbraPerf.RTS_IMAP_SSL_CONN : ZimbraPerf.RTS_IMAP_CONN;
        data.put(statName, numActiveHandlers());
        return data;
    }
    
    public static void main(String args[]) throws ServiceException {
        Zimbra.toolSetup("INFO");
        startupImapServer();
    }
}
