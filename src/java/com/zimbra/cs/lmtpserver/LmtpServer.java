/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.NetUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mina.MinaThreadFactory;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServer;
import com.zimbra.cs.util.Zimbra;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LmtpServer extends TcpServer {
    private static com.zimbra.cs.server.Server lmtpServer;

    private static final String HANDLER_THREAD_NAME = "LmtpHandler";
    
    public LmtpServer(LmtpConfig config) throws ServiceException {
        super("LmtpServer", config);
    }

    protected ProtocolHandler newProtocolHandler() {
        return new TcpLmtpHandler(this);
    }

    public static void bindServerSocket(String addr, int port)
            throws IOException {
        NetUtil.bindServerSocket(addr, port, false, MinaLmtpServer.isEnabled());
    }
    
    public static void startupLmtpServer() throws ServiceException {
        if (lmtpServer != null) return;

        LmtpConfig config = new LmtpConfig(Provisioning.getInstance());
        ExecutorService pool = Executors.newFixedThreadPool(
            config.getNumThreads(), new MinaThreadFactory(HANDLER_THREAD_NAME));
        if (MinaLmtpServer.isEnabled()) {
            try {
                lmtpServer = new MinaLmtpServer(config, pool);
            } catch (IOException e) {
                Zimbra.halt("failed to create MinaLmtpServer", e);
            }
        } else {
            lmtpServer = new LmtpServer(config);
        }
        try {
            lmtpServer.start();
        } catch (IOException e) {
            Zimbra.halt("failed to start LmtpServer", e);
        }

    }

    public static void shutdownLmtpServer() {
        if (lmtpServer != null) {
            lmtpServer.shutdown(10); // TODO shutdown grace period from config
            lmtpServer = null;
        }
    }
}
