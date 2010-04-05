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

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mina.MinaThreadFactory;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Zimbra;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LmtpServer extends TcpServer {
    private static Server server;

    private static final String HANDLER_THREAD_NAME = "LmtpHandler";
    
    public LmtpServer(LmtpConfig config) throws ServiceException {
        super("LmtpServer", config);
    }

    protected ProtocolHandler newProtocolHandler() {
        return new TcpLmtpHandler(this);
    }

    public static void startupLmtpServer() throws ServiceException {
        if (server != null) return;

        LmtpConfig config = LmtpConfig.getInstance();
        ExecutorService pool = Executors.newFixedThreadPool(
            config.getNumThreads(), new MinaThreadFactory(HANDLER_THREAD_NAME));
        if (MinaLmtpServer.isEnabled()) {
            try {
                server = new MinaLmtpServer(config, pool);
            } catch (IOException e) {
                Zimbra.halt("failed to create MinaLmtpServer", e);
            }
        } else {
            server = new LmtpServer(config);
        }
        try {
            server.start();
        } catch (IOException e) {
            Zimbra.halt("failed to start LmtpServer", e);
        }

    }

    public static void shutdownLmtpServer() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }
    
    public static LmtpServer getInstance() {
	return (LmtpServer) server;
    }

    public LmtpConfig getLmtpConfig() {
	return (LmtpConfig) getConfig();
    }
}
