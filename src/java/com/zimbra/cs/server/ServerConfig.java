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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.server;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.NetUtil;

import java.net.ServerSocket;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

public class ServerConfig {
    private String mName;
    private int mMaxIdleSeconds = -1;
    private String mBindAddress;
    private int mBindPort = -1;
    private int mNumThreads = -1;
    private boolean mSSLEnabled;

    public ServerConfig() {
        String name = LC.zimbra_server_hostname.value();
        if (name != null) setName(name);
    }

    public void validate() throws ServiceException {
        if (mName == null) failure("missing configuration name");
        if (mBindPort < 0) failure("invalid bind port value " + mBindPort);
        if (mNumThreads < 0) failure("invalid number of threads " + mNumThreads);
    }

    protected void failure(String msg) throws ServiceException {
        ServiceException.FAILURE(msg, null);
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

    public ServerSocket getServerSocket() throws ServiceException {
        return isSSLEnabled() ?
            NetUtil.getSslTcpServerSocket(getBindAddress(), getBindPort()) :
            NetUtil.getTcpServerSocket(getBindAddress(), getBindPort());
    }

    public ServerSocketChannel getServerSocketChannel()
            throws ServiceException {
        return NetUtil.getNioServerSocket(getBindAddress(),
                                          getBindPort()).getChannel();
    }
}

