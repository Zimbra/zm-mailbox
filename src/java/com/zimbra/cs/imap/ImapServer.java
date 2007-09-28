/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 9, 2005
 */
package com.zimbra.cs.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.common.util.NetUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.server.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dkarp
 */
public class ImapServer extends TcpServer implements RealtimeStatsCallback {
    private static Server sImapServer;
    private static Server sImapSSLServer;

    private ImapServer(ImapConfig config) throws ServiceException {
        super(config.isSSLEnabled() ? "ImapSSLServer" : "ImapServer", config);
        ZimbraPerf.addStatsCallback(this);
    }

    @Override
    protected ProtocolHandler newProtocolHandler() {
        return new TcpImapHandler(this);
    }

    public static void bindServerSocket(String addr, int port, boolean ssl)
            throws IOException {
        NetUtil.bindServerSocket(addr, port, ssl, MinaImapServer.isEnabled());
    }
    
    public synchronized static void startupImapServer() throws ServiceException {
        if (sImapServer == null) sImapServer = startServer(false);
    }

    public synchronized static void startupImapSSLServer() throws ServiceException {
        if (sImapSSLServer == null) sImapSSLServer = startServer(true);
    }

    private static Server startServer(boolean ssl) throws ServiceException {
        ImapConfig config = new ImapConfig(Provisioning.getInstance(), ssl);
        Server server;
        if (MinaImapServer.isEnabled()) {
            try {
                server = new MinaImapServer(config);
            } catch (IOException e) {
                Zimbra.halt("failed to start MinaImapServer", e);
                return null;
            }
        } else {
            server = new ImapServer(config);
        }
        try {
            server.start();
        } catch (IOException e) {
            Zimbra.halt("failed to start ImapSSLServer", e);
            return null;
        }
        return server;
    }

    public synchronized static void shutdownImapServers() {
        if (sImapServer != null) {
            sImapServer.shutdown(10);
            sImapServer = null;
        }
        if (sImapSSLServer != null) {
            sImapSSLServer.shutdown(10);
            sImapSSLServer = null;
        }
    }

    /**
     * Implementation of <code>RealtimeStatsCallback</code> that returns the number
     * of active handlers for this server.
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        String statName = getConfig().isSSLEnabled() ?
            ZimbraPerf.RTS_IMAP_SSL_CONN : ZimbraPerf.RTS_IMAP_CONN;
        data.put(statName, numActiveHandlers());
        return data;
    }
}
