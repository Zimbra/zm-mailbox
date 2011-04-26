/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.milter;

import java.util.concurrent.ExecutorService;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.mina.MinaCodecFactory;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaSession;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.server.ServerManager;

public class MinaMilterServer extends MinaServer implements MilterServer {

    public MinaMilterServer(ServerConfig config, ExecutorService pool) throws ServiceException {
        super(config, pool);
        registerMinaStatsMBean("MinaMilterServer");
    }
    
    @Override public MinaHandler createHandler(MinaSession session) {
        return new MinaMilterHandler(this, session);
    }

    @Override protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new MinaCodecFactory() {
            @Override public ProtocolDecoder getDecoder() {
                return new MinaMilterDecoder(getStats());
            }
            
            @Override public ProtocolEncoder getEncoder() {
                return new MinaMilterEncoder(getStats());
            }
        };
    }
    
    @Override public MilterConfig getConfig() {
        return (MilterConfig) super.getConfig();
    }
    
    @Override public Log getLog() {
        return ZimbraLog.milter;
    }
    
    /* for running standalone milter server */
    
    private static ExecutorService milterNioHandlerPool;
    private static MilterServer milterServer;
    
    private static class MilterShutdownHook extends Thread {
        private MilterServer server;

        public MilterShutdownHook(MilterServer server) {
            this.server = server;
        }
        
        public void run() {
            try {
                ZimbraLog.milter.info("Shutting down milter server");
                server.stop();
            } catch (ServiceException e) {
                ZimbraLog.milter.error("Server shutdown error: " + e.getMessage());    
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            Provisioning prov = Provisioning.getInstance();
            if (prov instanceof LdapProvisioning) {
                ZimbraLdapContext.waitForServer();
            }
            
            MilterConfig config = new MilterConfig();
            milterNioHandlerPool = ServerManager.newNioHandlerPool(config);
            milterServer = new MinaMilterServer(config, milterNioHandlerPool);
            
            MilterShutdownHook shutdownHook = new MilterShutdownHook(milterServer);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            
            ZimbraLog.milter.info("Starting milter server");
            milterServer.start();
        } catch (ServiceException e) {
            ZimbraLog.milter.error("Unable to start milter server: " + e.getMessage());
        }
    }
}
