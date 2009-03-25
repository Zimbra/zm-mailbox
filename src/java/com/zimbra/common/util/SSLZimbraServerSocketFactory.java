/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.common.util;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * SSL server socket factory wrapper allowing simple setup of excluded ciphers
 * 
 * @author pshao
 */
public class SSLZimbraServerSocketFactory extends SSLServerSocketFactory {

    private SSLServerSocketFactory mFactory;
    private String[] mEnabledCipherSuites;

    private SSLZimbraServerSocketFactory(String[] excludedCipherSuites) throws ServiceException {
	mFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
	      
        if (excludedCipherSuites != null && excludedCipherSuites.length > 0) {
            List<String> excludedCiphers = Arrays.asList(excludedCipherSuites);
            String[] defaultCipherSuites = mFactory.getDefaultCipherSuites();
            List<String> enabledCiphers = new ArrayList<String>(Arrays.asList(defaultCipherSuites));

            for (String cipher : excludedCiphers) {
                if (enabledCiphers.contains(cipher)) {
                    enabledCiphers.remove(cipher);
                }
            }
            if (enabledCiphers.size() == 0)
        	throw ServiceException.FAILURE("no enabled cipher suites after excluding cipher suites " + excludedCipherSuites, null);
            mEnabledCipherSuites = enabledCiphers.toArray(new String[enabledCiphers.size()]);
        }
    }

    private void initSocket(ServerSocket ss) {
	SSLServerSocket sslServerSocket = (SSLServerSocket)ss;
	if (mEnabledCipherSuites != null)
	    sslServerSocket.setEnabledCipherSuites(mEnabledCipherSuites);
    }
    
    public ServerSocket createServerSocket(int port) throws IOException {
	ServerSocket socket = mFactory.createServerSocket(port);
	initSocket(socket);
	return socket;
    }

    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
	ServerSocket socket = mFactory.createServerSocket(port, backlog);
	initSocket(socket);
	return socket;
    }

    public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
	ServerSocket socket = mFactory.createServerSocket(port, backlog, ifAddress);
        initSocket(socket);
	return socket;
    }

    public String[] getDefaultCipherSuites() {
        if (mEnabledCipherSuites == null)
	    return mFactory.getDefaultCipherSuites();
	else
	    return mEnabledCipherSuites;
    }

    public String[] getSupportedCipherSuites() {
        return mFactory.getSupportedCipherSuites();
    }
    
    public static void main(String[] args) {
	// TODO Auto-generated method stub

    }
}
