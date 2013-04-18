/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.util.ngxlookup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;

public class ZimbraNginxLookUpClient {

    private List<Route> ngxLookUpServers; // Nginx LookUp Handlers 
    private int ngxConnectTimeout;  // in seconds
    private int ngxRetryTimeout;  // in seconds
    private static int rrindex;
    private static final String urlExtension = "/service/extension/nginx-lookup";
    private static final int DEFAULT_PORT = 11211;
    private static final String ngxPassword = "_password_";

    public ZimbraNginxLookUpClient() {
        ngxLookUpServers = null;
        ngxConnectTimeout = 15000;
        ngxRetryTimeout = 600000;
    }

    public void setAttributes(String[] serverList, int connectTimeout,
            int retryTimeout) {
        this.ngxLookUpServers = parseServerList(serverList);
        this.ngxConnectTimeout = connectTimeout;
        this.ngxRetryTimeout = retryTimeout;
    }

    private Route getNginxRouteHandler() throws ServiceException {
        int count = 0;
        int currentIndex = 0;
        Route ngxHandler = null;
        if (ngxLookUpServers != null && ngxLookUpServers.size() > 0) {
            currentIndex = rrindex - 1;
            rrindex = (rrindex + 1) % ngxLookUpServers.size();
            do {
                if (count >= ngxLookUpServers.size()) {
                    throw ServiceException.FAILURE("All Nginx LookUp Handlers are unavailable", null);
                } else {
                    currentIndex = (currentIndex + 1) % ngxLookUpServers.size();
                    count ++;
                    ngxHandler = ngxLookUpServers.get(currentIndex);
                    if (ngxHandler.failureTime != 0) {
                        if (System.nanoTime() < ngxHandler.failureTime || 
                                System.nanoTime() - ngxHandler.failureTime < this.ngxRetryTimeout / 1000) {
                            continue;
                        } else {
                            // Nginx Route handler is available and we can reconnect
                            ngxHandler.failureTime = 0;
                        }
                    }
                    break;
                }
            } while(true);

            return ngxHandler;
        }
        return null;
    }

    public NginxAuthServer getRouteforAccount(String userName, String authMethod, String authProtocol, String clientIP,
            String proxyIP, String virtualHost) 
    throws ServiceException {
        Route nginxLookUpHandler = getNginxRouteHandler();
        if (nginxLookUpHandler != null) {
            GetMethod method = new GetMethod((new StringBuilder().append(urlExtension).
                    append(" HTTP/1.0")).toString());

            method.setRequestHeader("Host", nginxLookUpHandler.ngxServerAddress.getAddress().toString());
            method.setRequestHeader("Auth-Method", authMethod);
            method.setRequestHeader("Auth-User", userName);
            method.setRequestHeader("Auth-Pass", ngxPassword);
            method.setRequestHeader("Auth-Protocol", authProtocol);
            // for web requests, login attempts is always 0
            method.setRequestHeader("Auth-Login-Attempt", "0");
            method.setRequestHeader("X-Proxy-IP", proxyIP);
            method.setRequestHeader("Client-IP", clientIP);
            method.setRequestHeader("X-Proxy-Host", virtualHost);
            HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();

            // TO DO ngxConnectTimeout time-out is not used currently
            try {
                int statusCode = HttpClientUtil.executeMethod(client, method);
                if (statusCode == 200 && method.getResponseHeader("Auth-Status").getValue().equals("OK")) {
                    return new NginxAuthServer(method.getResponseHeader("Auth-Server").getValue(), method.getResponseHeader("Auth-Port").getValue(),
                            method.getResponseHeader("Auth-User").getValue());
                }
            } catch (IOException e) {
                nginxLookUpHandler.failureTime = System.nanoTime();
            } finally {
                method.releaseConnection();
            }
        }
        return null;
    }

    /**
     * Parse a server list.
     * Each server value is hostname:port or just hostname.  Default port is 7072.
     * @param serverList
     * @return
     */
    private List<Route> parseServerList(String[] servers) {
        // Eliminate duplicates and sort case-insensitively.  This negates operator error
        // configuring server list with inconsistent order on different Nginx Route Handler clients.
        TreeSet<String> tset = new TreeSet<String>();  // TreeSet provides deduping and sorting.
        for (int i = 0; i < servers.length; ++i) {
            tset.add(servers[i].toLowerCase());
        }
        servers = tset.toArray(new String[0]);
        if (servers != null) {
            List<Route> addrs = new ArrayList<Route>(servers.length);
            for (String server : servers) {
                if (server.length() == 0)
                    continue;
                String[] parts = server.split(":");
                if (parts != null) {
                    String host;
                    int port = DEFAULT_PORT;
                    if (parts.length == 1) {
                        host = parts[0];
                    } else if (parts.length == 2) {
                        host = parts[0];
                        try {
                            port = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            ZimbraLog.misc.warn("Invalid server " + server);
                            continue;
                        }
                    } else {
                        ZimbraLog.misc.warn("Invalid server " + server);
                        continue;
                    }
                    Route rt = this.new Route(new InetSocketAddress(host, port), 0);
                    addrs.add(rt);
                } else {
                    ZimbraLog.misc.warn("Invalid server " + server);
                    continue;
                }
            }
            return addrs;
        } else {
            return new ArrayList<Route>(0);
        }
    }

    private class Route {
        private InetSocketAddress ngxServerAddress;
        private long failureTime;

        private Route (InetSocketAddress server, long failureTime) {
            this.ngxServerAddress = server;
            this.failureTime = failureTime;
        }
    }
}