/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioServer;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.server.ServerConfig;

public class NioMilterServer extends NioServer implements MilterServer {
    private final ProtocolDecoder decoder = new NioMilterDecoder();
    private final ProtocolEncoder encoder = new NioMilterEncoder();

    public NioMilterServer(ServerConfig config) throws ServiceException {
        super(config);
        registerMBean("NioMilterServer");
    }

    @Override
    public NioHandler createHandler(NioConnection conn) {
        return new NioMilterHandler(this, conn);
    }

    @Override
    protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new ProtocolCodecFactory() {
            @Override
            public ProtocolDecoder getDecoder(IoSession session) {
                return decoder;
            }

            @Override
            public ProtocolEncoder getEncoder(IoSession session) {
                return encoder;
            }
        };
    }

    @Override
    public MilterConfig getConfig() {
        return (MilterConfig) super.getConfig();
    }

    @Override
    public Log getLog() {
        return ZimbraLog.milter;
    }

    /* for running standalone milter server */

    private static MilterServer milterServer;

    private static class MilterShutdownHook extends Thread {
        private MilterServer server;

        public MilterShutdownHook(MilterServer server) {
            this.server = server;
        }

        @Override
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
            milterServer = new NioMilterServer(config);

            MilterShutdownHook shutdownHook = new MilterShutdownHook(milterServer);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            ZimbraLog.milter.info("Starting milter server");
            milterServer.start();
        } catch (ServiceException e) {
            ZimbraLog.milter.error("Unable to start milter server: " + e.getMessage());
        }
    }
}
