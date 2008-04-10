/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import javax.net.ssl.SSLServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;

import com.zimbra.common.service.ServiceException;

public class NetUtil {

    public static ServerSocket getTcpServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, false, false, null);
    }
    
    public static ServerSocket getSslTcpServerSocket(String address, int port, String[] excludeCiphers) throws ServiceException {
        return getServerSocket(address, port, true, /* doesn't matter, but keep it false always */ false, excludeCiphers);
    }

    public static ServerSocket getNioServerSocket(String address, int port) throws ServiceException {
        return getServerSocket(address, port, false, true, null);
    }

    public static void bindTcpServerSocket(String address, int port) throws IOException {
        bindServerSocket(address, port, false, false, null);
    }
    
    public static void bindSslTcpServerSocket(String address, int port, String[] excludeCiphers) throws IOException { 
        bindServerSocket(address, port, true, /* doesn't matter, but it false always */ false, excludeCiphers);
    }

    public static void bindNioServerSocket(String address, int port) throws IOException { 
        bindServerSocket(address, port, false, true, null);
    }
    
 
    public static synchronized ServerSocket getServerSocket(String address, int port, boolean ssl, boolean useChannels, String[] excludeCiphers) throws ServiceException {
        ServerSocket serverSocket = getAlreadyBoundServerSocket(address, port, ssl, useChannels);
        if (serverSocket != null) {
            return serverSocket;
        }
        try {
            serverSocket = newBoundServerSocket(address, port, ssl, useChannels, excludeCiphers);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Could not bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl + " useChannels=" + useChannels, ioe);
        }
        if (serverSocket == null) {
            throw ServiceException.FAILURE("Server socket null after bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl + " useChannels=" + useChannels, null);
        }
        return serverSocket;
    }

    private static ServerSocket newBoundServerSocket(String address, int port, boolean ssl, boolean useChannels,
	    String[] excludeCiphers) throws IOException {
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
                setSSLEnabledCipherSuites((SSLServerSocket)serverSocket, excludeCiphers);
        	} else {
        		serverSocket = new ServerSocket();
        	}
        }
        
        serverSocket.setReuseAddress(true);
        InetSocketAddress isa = new InetSocketAddress(bindAddress, port);
        serverSocket.bind(isa, 1024);
        return serverSocket;
    }

    private static void setSSLEnabledCipherSuites(SSLServerSocket socket, String[] excludeCiphers) {
	
	if (excludeCiphers != null && excludeCiphers.length > 0) {
	    List<String> excludedCSList = Arrays.asList(excludeCiphers);
            String[] enabledCipherSuites = socket.getEnabledCipherSuites();
            List<String> enabledCSList = new ArrayList<String>(Arrays.asList(enabledCipherSuites));
            
            for (String cipher : excludedCSList) {
                if (enabledCSList.contains(cipher))
                    enabledCSList.remove(cipher);
            }

            String[] enabledCiphers = enabledCSList.toArray(new String[enabledCSList.size()]);
            socket.setEnabledCipherSuites(enabledCiphers);
        }
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
    
    public static synchronized void bindServerSocket(String address, int port, boolean ssl, boolean useChannels, String[] excludeCiphers) throws IOException {
        // Don't use log4j - when this code is called, log4j might not have been initialized
        // and we do not want to initialize log4j at this time because we are likely still
        // running as root.
        System.err.println("Zimbra server reserving server socket port=" + port + " bindaddr=" + address + " ssl=" + ssl);
        String key = makeKey(address, port, ssl, useChannels);
        ServerSocket serverSocket = NetUtil.newBoundServerSocket(address, port, ssl, useChannels, excludeCiphers);
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
