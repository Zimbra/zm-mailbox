/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mina.MinaThreadFactory;
import com.zimbra.cs.server.Server;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Zimbra;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pop3Server extends TcpServer implements RealtimeStatsCallback {
    private static com.zimbra.cs.server.Server sPopServer;
    private static com.zimbra.cs.server.Server sPopSSLServer;

    private static final String HANDLER_THREAD_NAME = "Pop3Handler";
    
    private Pop3Server(Pop3Config config) throws ServiceException {
        super(config.isSslEnabled() ? "Pop3SSLServer" : "Pop3Server", config);
        ZimbraPerf.addStatsCallback(this);
    }

    protected ProtocolHandler newProtocolHandler() {
        return new TcpPop3Handler(this);
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

    private static ExecutorService sPop3HandlerThreadPool;        

    private static Server startServer(boolean ssl) throws ServiceException {
        Pop3Config config = new Pop3Config(ssl);
        Server server;
        if (MinaPop3Server.isEnabled()) {
             if (sPop3HandlerThreadPool == null) {
                sPop3HandlerThreadPool = Executors.newFixedThreadPool(
                    config.getNumThreads(), new MinaThreadFactory(HANDLER_THREAD_NAME)); 
             }
             try {
                server = new MinaPop3Server(config, sPop3HandlerThreadPool);
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
            sPopServer.shutdown();
            sPopServer = null;
        }
        if (sPopSSLServer != null) {
            sPopSSLServer.shutdown();
            sPopSSLServer = null;
        }
    }

    /**
     * Implementation of <code>RealtimeStatsCallback</code> that returns the number
     * of active handlers for this server.
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        String statName = getConfig().isSslEnabled() ?
            ZimbraPerf.RTS_POP_SSL_CONN : ZimbraPerf.RTS_POP_CONN;
        data.put(statName, numActiveHandlers());
        return data;
    }
}