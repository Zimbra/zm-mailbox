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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.util.NetUtil;
import com.zimbra.cs.server.Server;

public class Pop3Server extends TcpServer implements RealtimeStatsCallback {
    private static com.zimbra.cs.server.Server sPopServer;
    private static com.zimbra.cs.server.Server sPopSSLServer;

    private Pop3Server(Pop3Config config) throws ServiceException {
        super(config.isSSLEnabled() ? "Pop3SSLServer" : "Pop3Server", config);
        ZimbraPerf.addStatsCallback(this);
    }

    protected ProtocolHandler newProtocolHandler() {
        return new TcpPop3Handler(this);
    }

    public static void bindServerSocket(String addr, int port, boolean ssl)
            throws IOException {
        NetUtil.bindServerSocket(addr, port, ssl, MinaPop3Server.isEnabled());
    }
    
    public synchronized static void startupPop3Server() throws ServiceException {
        if (sPopServer == null) {
            sPopServer = startServer(false);
        }
    }

    public synchronized static void startupPop3SSLServer() throws ServiceException {
        if (sPopSSLServer == null) {
            sPopSSLServer = startServer(true);
        }
    }

    private static Server startServer(boolean ssl) throws ServiceException {
        Pop3Config config = new Pop3Config(Provisioning.getInstance(), ssl);
        Server server;
        if (MinaPop3Server.isEnabled()) {
             try {
                 server = new MinaPop3Server(config);
             } catch (IOException e) {
                 Zimbra.halt("failed to create MinaPop3Server", e);
                 return null;
             }
        } else {
            server = new Pop3Server(config);
        }
        try {
            server.start();
        } catch (IOException e) {
            Zimbra.halt("failed to start Pop3Server", e);
        }
        return server;
    }

    public synchronized static void shutdownPop3Servers() {
        if (sPopServer != null) {
            sPopServer.shutdown(10); // TODO shutdown grace period from config
            sPopServer = null;
        }
        if (sPopSSLServer != null) {
            sPopSSLServer.shutdown(10); // TODO shutdown grace period from config
            sPopSSLServer = null;
        }
    }

    /**
     * Implementation of <code>RealtimeStatsCallback</code> that returns the number
     * of active handlers for this server.
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        String statName = getConfig().isSSLEnabled() ?
            ZimbraPerf.RTS_POP_SSL_CONN : ZimbraPerf.RTS_POP_CONN;
        data.put(statName, numActiveHandlers());
        return data;
    }
}