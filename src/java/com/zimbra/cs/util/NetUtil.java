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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

//} catch (IOException ioe) {
//    Zimbra.halt();
//}

public class NetUtil {

    public static synchronized ServerSocket getBoundServerSocket(String address, int port, boolean ssl) throws ServiceException {
        ServerSocket serverSocket = getAlreadyBoundServerSocket(address, port, ssl);
        if (serverSocket != null) {
            return serverSocket;
        }
        try {
            serverSocket = newBoundServerSocket(address, port, ssl);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Could not bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl, ioe);
        }
        if (serverSocket == null) {
            throw ServiceException.FAILURE("Server socket null after bind to port=" + port + " bindaddr=" + address + " ssl=" + ssl, null);
        }
        return serverSocket;
    }

    private static ServerSocket newBoundServerSocket(String address, int port, boolean ssl) throws IOException {
        ServerSocket serverSocket = null;
        InetAddress bindAddress = null;
        if (address != null && address.length() > 0) {
            bindAddress = InetAddress.getByName(address);
        }
        if (!ssl) {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocket = serverSocketChannel.socket();
        } else {
            SSLServerSocketFactory fact = (SSLServerSocketFactory)
            SSLServerSocketFactory.getDefault();
            serverSocket = fact.createServerSocket();
        }
        serverSocket.setReuseAddress(true);
        InetSocketAddress isa = new InetSocketAddress(bindAddress, port);
        serverSocket.bind(isa, 1024);
        return serverSocket;
    }

    private static Map mBoundSockets = new HashMap();
    
    private static String makeKey(String address, int port, boolean ssl) {
        return "[ssl=" + ssl + ";addr=" + address + ";port=" + port + "]";
    }
    
    private static void dumpMap() {
        for (Iterator iter = mBoundSockets.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            System.err.println(entry.getKey() + " => " + entry.getValue());
        }
    }
    
    public static synchronized void reserveServerSocket(String address, int port, boolean ssl) throws IOException {
        // Don't use log4j - when this code is called, log4j might not have been initialized
        // and we do not want to initialize log4j at this time because we are likely still
        // running as root.
        System.err.println("Zimbra server reserving server socket port=" + port + " bindaddr=" + address + " ssl=" + ssl);
        String key = makeKey(address, port, ssl);
        ServerSocket serverSocket = NetUtil.newBoundServerSocket(address, port, ssl);
        //System.err.println("put table=" + mBoundSockets.hashCode() + " key=" + key + " sock=" + serverSocket);
        mBoundSockets.put(makeKey(address, port, ssl), serverSocket);
        //dumpMap();
    }

    private static ServerSocket getAlreadyBoundServerSocket(String address, int port, boolean ssl) {
        //dumpMap();
        String key = makeKey(address, port, ssl);
        ServerSocket serverSocket = (ServerSocket)mBoundSockets.get(key);
        //System.err.println("get table=" + mBoundSockets.hashCode() + " key=" + key + " sock=" + serverSocket);
        return serverSocket;
    }
    
    private static String sProxyUrl = null;
    private static URI sProxyUri = null;
    private static AuthScope sProxyAuthScope = null;
    private static UsernamePasswordCredentials sProxyCreds = null;
    
    public static synchronized void configureProxy(HttpClient client) {
        try {
            String url = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraHttpProxyURL, null);
            if (url == null) return;

            // need to initializae all the statics
            if (sProxyUrl == null || !sProxyUrl.equals(url)) {
                sProxyUrl = url;
                sProxyUri = new URI(url);
                sProxyAuthScope = null;
                sProxyCreds = null;
                String userInfo = sProxyUri.getUserInfo();                
                if (userInfo != null) {
                    int i = userInfo.indexOf(':');
                    if (i != -1) {
                        sProxyAuthScope = new AuthScope(sProxyUri.getHost(), sProxyUri.getPort(), null); 
                        sProxyCreds = new UsernamePasswordCredentials(userInfo.substring(0, i), userInfo.substring(i+1));
                    }
                }
            }
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("setting proxy: "+url);
            }
            client.getHostConfiguration().setProxy(sProxyUri.getHost(), sProxyUri.getPort());
            if (sProxyAuthScope != null && sProxyCreds != null) 
                client.getState().setProxyCredentials(sProxyAuthScope, sProxyCreds);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Unable to configureProxy: "+e.getMessage(), e);
        } catch (URISyntaxException e) {
            ZimbraLog.misc.warn("Unable to configureProxy: "+e.getMessage(), e);
        }
    }
}
