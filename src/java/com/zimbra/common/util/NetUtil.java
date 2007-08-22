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
 * Portions created by Zimbra are Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;

import com.zimbra.common.service.ServiceException;

public class NetUtil {

    public static ServerSocket getTcpServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, false, false);
    }
    
    public static ServerSocket getSslTcpServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, true, /* doesn't matter, but keep it false always */ false);
    }

    public static ServerSocket getOzServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, false, true);
    }
    
    public static void bindTcpServerSocket(String address, int port) throws IOException {
        bindServerSocket(address, port, false, false);
    }
    
    public static void bindSslTcpServerSocket(String address, int port) throws IOException { 
        bindServerSocket(address, port, true, /* doesn't matter, but it false always */ false);
    }

    public static void bindOzServerSocket(String address, int port) throws IOException { 
        bindServerSocket(address, port, false, true);
    }
    
 
    public static synchronized ServerSocket getServerSocket(String address, int port, boolean ssl, boolean useChannels) throws ServiceException {
        ServerSocket serverSocket = getAlreadyBoundServerSocket(address, port, ssl, useChannels);
        if (serverSocket != null) {
            return serverSocket;
        }
        try {
            serverSocket = newBoundServerSocket(address, port, ssl, useChannels);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Could not bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl + " useChannels=" + useChannels, ioe);
        }
        if (serverSocket == null) {
            throw ServiceException.FAILURE("Server socket null after bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl + " useChannels=" + useChannels, null);
        }
        return serverSocket;
    }

    private static ServerSocket newBoundServerSocket(String address, int port, boolean ssl, boolean useChannels) throws IOException {
        ServerSocket serverSocket = null;
        InetAddress bindAddress = null;
        if (address != null && address.length() > 0) {
            bindAddress = InetAddress.getByName(address);
        }

        if (useChannels) {
        	//for SSL channels, it's up to the selector to configure SSL stuff
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false); //I believe the only time we use channels is in NIO
            serverSocket = serverSocketChannel.socket();
        } else {
        	if (ssl) {
                SSLServerSocketFactory fact = (SSLServerSocketFactory)
                SSLServerSocketFactory.getDefault();
                serverSocket = fact.createServerSocket();
        	} else {
        		serverSocket = new ServerSocket();
        	}
        }
        
        serverSocket.setReuseAddress(true);
        InetSocketAddress isa = new InetSocketAddress(bindAddress, port);
        serverSocket.bind(isa, 1024);
        return serverSocket;
    }

    private static Map mBoundSockets = new HashMap();
    
    private static String makeKey(String address, int port, boolean ssl, boolean useChannels) {
        return "[ssl=" + ssl + ";addr=" + address + ";port=" + port + ";useChannels=" + useChannels + "]";
    }
    
    public static void dumpMap() {
        for (Iterator iter = mBoundSockets.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            System.err.println(entry.getKey() + " => " + entry.getValue());
        }
    }
    
    public static synchronized void bindServerSocket(String address, int port, boolean ssl, boolean useChannels) throws IOException {
        // Don't use log4j - when this code is called, log4j might not have been initialized
        // and we do not want to initialize log4j at this time because we are likely still
        // running as root.
        System.err.println("Zimbra server reserving server socket port=" + port + " bindaddr=" + address + " ssl=" + ssl);
        String key = makeKey(address, port, ssl, useChannels);
        ServerSocket serverSocket = NetUtil.newBoundServerSocket(address, port, ssl, useChannels);
        //System.err.println("put table=" + mBoundSockets.hashCode() + " key=" + key + " sock=" + serverSocket);
        mBoundSockets.put(key, serverSocket);
        //dumpMap();
    }

    private static ServerSocket getAlreadyBoundServerSocket(String address, int port, boolean ssl, boolean useChannels) {
        //dumpMap();
        String key = makeKey(address, port, ssl, useChannels);
        ServerSocket serverSocket = (ServerSocket)mBoundSockets.get(key);
        //System.err.println("get table=" + mBoundSockets.hashCode() + " key=" + key + " sock=" + serverSocket);
        return serverSocket;
    }
}
