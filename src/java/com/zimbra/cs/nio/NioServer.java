/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.nio;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.server.Server;
import com.zimbra.cs.server.ServerConfig;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;

public abstract class NioServer implements Server {
    private final ServerConfig config;
    private final ExecutorService handlerPool;
    private SSLInfo sslInfo;

    protected NioServer(ServerConfig config, ExecutorService pool) {
        this.config = config;
        handlerPool = pool;
    }

    public SSLInfo getSSLInfo() {
        if (sslInfo == null) {
            sslInfo = new SSLInfo(getConfig());
        }
        return sslInfo;
    }

    public ServerConfig getConfig() {
        return config;
    }

    protected ExecutorService getHandlerPool() {
        return handlerPool;
    }

    public abstract NioStatsMBean getStats();

    public void stop() throws ServiceException {
        stop(getConfig().getShutdownGraceSeconds());
    }
    
    protected void registerStatsMBean(String type) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(getStats(), new ObjectName("ZimbraCollaborationSuite:type=" + type));
        } catch (Exception e) {
            // getLog().warn("Unable to register MinaStats mbean", e);
        }
    }
}
