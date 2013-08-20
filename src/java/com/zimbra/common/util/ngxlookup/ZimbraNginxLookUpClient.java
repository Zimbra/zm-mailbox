/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util.ngxlookup;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;

public class ZimbraNginxLookUpClient {

    private List<Route> ngxLookUpServers; // Nginx Upstream LookUp Handlers
    private List<Route> upstreamMailServers; // Upstream Mail service servers
    private int ngxConnectTimeout;
    private int ngxRetryTimeout;
    private static int nginxRRindex;
    private static int upstreamMailServerRRindex;
    private static final String urlExtension = "/service/extension/nginx-lookup";
    private static final int DEFAULT_NGINX_HANDLER_PORT = 7072;
    private static final int DEFAULT_UPSTREAM_MAIL_SERVER_PORT = 7070;
    private static final String ngxPassword = "_password_";

    public ZimbraNginxLookUpClient() {
        ngxLookUpServers = null;
        upstreamMailServers = null;
        ngxConnectTimeout = 15000;
        ngxRetryTimeout = 600000;
    }

    public void setAttributes(String[] lookUpServers, String[] upstreamMailServers, int connectTimeout,
            int retryTimeout) {
        this.ngxLookUpServers = parseServerList(lookUpServers, DEFAULT_NGINX_HANDLER_PORT);
        this.upstreamMailServers = parseServerList(upstreamMailServers, DEFAULT_UPSTREAM_MAIL_SERVER_PORT);
        this.ngxConnectTimeout = connectTimeout;
        this.ngxRetryTimeout = retryTimeout;
    }

    public String getUpstreamMailServer(String protocol) throws ServiceException {
        int count = 0;
        int currentIndex = 0;
        Route upstreamMailServer = null;
        if (upstreamMailServers != null && upstreamMailServers.size() > 0) {
            currentIndex = upstreamMailServerRRindex - 1;
            upstreamMailServerRRindex = (upstreamMailServerRRindex + 1) % upstreamMailServers.size();

            do {
                if (count >= upstreamMailServers.size()) {
                    throw ServiceException.FAILURE("All Upstream Mail Servers are unavailable", null);
                } else {
                    currentIndex = (currentIndex + 1) % upstreamMailServers.size();
                    count++;
                    upstreamMailServer = upstreamMailServers.get(currentIndex);
                    String url = (new StringBuilder(protocol).append("://").append(upstreamMailServer.ngxServerAddress.getHostName()).
                            append(":").append(upstreamMailServer.ngxServerAddress.getPort())).toString();
                    if (!ping(url, 60000)) {
                        continue;
                    }
                    break;
                }
            } while(true);

            return new StringBuilder(upstreamMailServer.ngxServerAddress.getHostName()).append(":").
                    append(upstreamMailServer.ngxServerAddress.getPort()).toString();
        } else {
            throw ServiceException.FAILURE("Upstream mail servers are not configured or set", null);
        }
    }

    private Route getNginxRouteHandler() throws ServiceException {
         // Return nginx handlers using RR algorithm
        int count = 0;
        int currentIndex = 0;
        Route ngxHandler = null;
        if (ngxLookUpServers != null && ngxLookUpServers.size() > 0) {
            currentIndex = nginxRRindex - 1;
            nginxRRindex = (nginxRRindex + 1) % ngxLookUpServers.size();

            do {
                if (count >= ngxLookUpServers.size()) {
                    throw ServiceException.FAILURE("All Nginx LookUp Handlers are unavailable", null);
                } else {
                    currentIndex = (currentIndex + 1) % ngxLookUpServers.size();
                    count++;
                    ngxHandler = ngxLookUpServers.get(currentIndex);
                    if (ngxHandler.failureTime != 0) {
                        if (System.nanoTime() - ngxHandler.failureTime < this.ngxRetryTimeout / 1000) {
                            continue;
                        } else {
                            ngxHandler.failureTime = 0;
                        }
                    }
                    // Ping the Upstream handler to check whether its up
                    // if the handler is not reachable, mark is down
                    String url = (new StringBuilder("http://").append(ngxHandler.ngxServerAddress.getHostName()).
                            append(":").append(ngxHandler.ngxServerAddress.getPort()).append(urlExtension)).toString();
                    if (!ping(url, ngxConnectTimeout)) {
                        ngxHandler.failureTime = System.nanoTime();
                        continue;
                    }
                    break;
                }
            } while(true);

            return ngxHandler;
        }else {
            throw ServiceException.FAILURE("Nginx LookUp Handlers are not configured or set", null);
        }
    }

    /**
     * Pings a HTTP URL. This effectively sends a GET request and returns <code>true</code> if the response code is 200
     * @param url The HTTP URL to be pinged.
     * @param timeout The timeout in millis for both the connection timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200 within the
     * given timeout, otherwise <code>false</code>.
     */
    public static boolean ping(String url, int timeout) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            return (connection.getResponseCode() == 200) ? true : false;
        } catch (IOException exception) {
            return false;
        }
    }

    public NginxAuthServer getRouteforAccount(String userName, String authMethod, String authProtocol, String clientIP,
            String proxyIP, String virtualHost) throws ServiceException {
        Route nginxLookUpHandler = getNginxRouteHandler();
        if (nginxLookUpHandler != null) {
            GetMethod method = new GetMethod((new StringBuilder("http://").append(nginxLookUpHandler.ngxServerAddress.getHostName()).
                    append(":").append(nginxLookUpHandler.ngxServerAddress.getPort()).append(urlExtension)).toString());

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
            // currently we use default httpclient_internal_connmgr_connection_timeout instead of ngxConnectTimeout
            client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);

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
     * Each server value is hostname:port or just hostname.
     * @param serverList
     * @return
     */
    private List<Route> parseServerList(String[] servers, int defaultPort) {
        // Eliminate duplicates and sort case-insensitively.  This negates operator error
        // configuring server list with inconsistent order on different Nginx Route Handler clients.
        // TreeSet provides deduping and sorting.
        TreeSet<String> tset = new TreeSet<String>();
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
                    int port = defaultPort;
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