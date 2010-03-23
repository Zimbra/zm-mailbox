/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.server;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.NetUtil;
import com.zimbra.cs.account.Provisioning;

import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

public abstract class ServerConfig {
    private String protocol;
    private boolean ssl;

    private static final int SHUTDOWN_GRACE_PERIOD = 60;
    private static final int NUM_THREADS = 20;
    private static final int MAX_IDLE_SECONDS = 600;
    private static final int NIO_MAX_SESSIONS = 200;
    private static final int NIO_WRITE_CHUNK_SIZE = 8192;
    private static final int NIO_WRITE_TIMEOUT = 60;
    private static final int NIO_MAX_SCHEDULED_WRITE_BYTES = 1024 * 1024;
    
    public ServerConfig(String protocol, boolean ssl) {
        this.protocol = protocol;
        this.ssl = ssl;
    }

    public String getServerName() {
        return LC.zimbra_server_hostname.value();
    }
    
    public String getServerVersion() {
        return null;
    }
    
    public String getBindAddress() {
        return null;
    }
    
    public abstract int getBindPort();
    public abstract Log getLog();
    
    public int getMaxIdleSeconds() {
        return MAX_IDLE_SECONDS;
    }

    public int getNumThreads() {
        return NUM_THREADS;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isSslEnabled() {
        return ssl;
    }

    public String getGreeting() {
        return getDescription() + " ready";
    }

    public String getGoodbye() {
        return getDescription() + " closing connection";
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        String name = getServerName();
        if (name != null && name.length() > 0) {
            sb.append(name).append(' ');
        }
        sb.append("Zimbra ");
        String version = getServerVersion();
        if (version != null && version.length() > 0) {
            sb.append(version).append(' ');
        }
        return sb.append(getProtocol()).append(" server").toString();
    }

    public String[] getSslExcludedCiphers() {
        String key = Provisioning.A_zimbraSSLExcludeCipherSuites;
        try {
            return Provisioning.getInstance().getConfig().getMultiAttr(key);
        } catch (ServiceException e) {
            getLog().warn("Unable to get global attribute: " + key, e);
            return null;
        }
    }

    public int getShutdownGracePeriod() {
       return SHUTDOWN_GRACE_PERIOD;
    }

    public int getNioMaxSessions() {
        return NIO_MAX_SESSIONS;
    }

    public int getNioWriteChunkSize() {
        return NIO_WRITE_CHUNK_SIZE;
    }

    public int getNioWriteTimeout() {
        return NIO_WRITE_TIMEOUT;
    }

    public int getNioMaxScheduledWriteBytes() {
        return NIO_MAX_SCHEDULED_WRITE_BYTES;
    }
    
    public ServerSocket getServerSocket() throws ServiceException {
        return isSslEnabled() ?
            NetUtil.getSslTcpServerSocket(getBindAddress(), getBindPort(), getSslExcludedCiphers()) :
            NetUtil.getTcpServerSocket(getBindAddress(), getBindPort());
    }

    public ServerSocketChannel getServerSocketChannel() throws ServiceException {
        return NetUtil.getNioServerSocket(getBindAddress(), getBindPort()).getChannel();
    }

    protected String getAttr(String key, String defaultValue) {
        try {
            return getLocalServer().getAttr(key, defaultValue);
        } catch (ServiceException e) {
            getLog().warn("Unable to get server attribute: " + key, e);
            return defaultValue;
        }
    }

    protected int getIntAttr(String key, int defaultValue) {
        try {
            return getLocalServer().getIntAttr(key, defaultValue);
        } catch (ServiceException e) {
            getLog().warn("Unable to get server attribute: " + key, e);
            return defaultValue;
        }
    }

    protected boolean getBooleanAttr(String key, boolean defaultValue) {
        try {
            return getLocalServer().getBooleanAttr(key, defaultValue);
        } catch (ServiceException e) {
            getLog().warn("Unable to get server attribute: " + key, e);
            return defaultValue;
        }
    }

    protected com.zimbra.cs.account.Server getLocalServer() throws ServiceException {
        return Provisioning.getInstance().getLocalServer();
    }

    protected com.zimbra.cs.account.Config getGlobalConfig() throws ServiceException {
        return Provisioning.getInstance().getConfig();
    }
}

