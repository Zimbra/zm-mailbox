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
import com.zimbra.common.util.NetUtil;

import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

public class ServerConfig {
    private String mName;
    private int mMaxIdleSeconds = 0;
    private String mBindAddress;
    private int mBindPort = -1;
    private int mNumThreads = -1;
    private boolean mSSLEnabled;
    private String[] mSSLExcludeCiphers;

    private static final int NIO_WRITE_CHUNK_SIZE = 8192;
    private static final int NIO_WRITE_TIMEOUT = 60;
    private static final int NIO_MAX_SCHEDULED_WRITE_BYTES = 1024 * 1024;
    
    public ServerConfig() {
        String name = LC.zimbra_server_hostname.value();
        if (name != null) setName(name);
    }

    public void validate() throws ServiceException {
        if (mName == null) failure("missing configuration name");
        if (mMaxIdleSeconds < 0) failure("invalid MaxIdleSeconds value: " + mMaxIdleSeconds);
        if (mBindPort < 0) failure("invalid BindPort value: " + mBindPort);
        if (mNumThreads < 0) failure("invalid NumThreads value: " + mNumThreads);
    }

    protected void failure(String msg) throws ServiceException {
        throw ServiceException.FAILURE(msg, null);
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setMaxIdleSeconds(int secs) {
        mMaxIdleSeconds = secs;
    }

    public int getMaxIdleSeconds() {
        return mMaxIdleSeconds;
    }

    public void setBindAddress(String addr) {
        mBindAddress = addr;
    }

    public String getBindAddress() {
        return mBindAddress;
    }

    public void setBindPort(int port) {
        mBindPort = port;
    }

    public int getBindPort() {
        return mBindPort;
    }

    public void setNumThreads(int numThreads) {
        mNumThreads = numThreads;
    }

    public int getNumThreads() {
        return mNumThreads;
    }

    public void setSSLEnabled(boolean enabled) {
        mSSLEnabled = enabled;
    }

    public boolean isSSLEnabled() {
        return mSSLEnabled;
    }
    
    public void setSSLExcludeCiphers(String[] excludeCiphers) {
        mSSLExcludeCiphers = excludeCiphers;
    }
    
    public String[] getSSLExcludeCiphers() {
        return mSSLExcludeCiphers;
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
        return isSSLEnabled() ?
            NetUtil.getSslTcpServerSocket(getBindAddress(), getBindPort(), getSSLExcludeCiphers()) :
            NetUtil.getTcpServerSocket(getBindAddress(), getBindPort());
    }

    public ServerSocketChannel getServerSocketChannel()
            throws ServiceException {
        return NetUtil.getNioServerSocket(getBindAddress(),
                                          getBindPort()).getChannel();
    }
}

