/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.WebSplitUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.HttpProxyUtil;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.zimlet.ZimletUtil;

/**
 * Util class for sending service related requests from service node to ui nodes
 */
public class WebClientServiceUtil {

    public static final String PARAM_AUTHTOKEN = "authtoken";
    private static final String FLUSH_UISTRINGS_ON_UI_NODE = "/fromservice/flushuistrings";

    /**
     * @return true if server is in split mode or LC key debug_local_split is set to true
     */
    public static boolean isServerInSplitMode() {
        return DebugConfig.debugLocalSplit || WebSplitUtil.isZimbraServiceSplitEnabled();
    }

    /**
     * send service request to every ui node
     * @param serviceUrl the url that should be matched and handled by ServiceServlet in ZimbraWebClient
     * @throws ServiceException
     */
    public static void sendServiceRequestToEveryUiNode(String serviceUrl) throws ServiceException {
        List<Server> servers = Provisioning.getInstance().getAllServers(Provisioning.SERVICE_WEBCLIENT);
        if (servers == null || servers.isEmpty()) {
            servers.add(Provisioning.getInstance().getLocalServer());
        }
        AuthToken authToken = AuthProvider.getAdminAuthToken();
        ZimbraLog.misc.debug("got admin auth token");
        //sequentially flush each node
        HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(client);
        for (Server server : servers) {
            if (isServerAtLeast8dot5(server)) {
                HttpMethod method = null;
                try {
                    method = new GetMethod(URLUtil.getServiceURL(server, serviceUrl, false));
                    ZimbraLog.misc.debug("connecting to ui node %s", server.getName());
                    try {
                        method.addRequestHeader(PARAM_AUTHTOKEN, authToken.getEncoded());
                    } catch (AuthTokenException e) {
                        ZimbraLog.misc.warn(e);
                    }
                    int respCode = HttpClientUtil.executeMethod(client, method);
                    if (respCode != 200) {
                        ZimbraLog.misc.warn("service failed, return code: %d", respCode);
                    }
                } catch (Exception e) {
                    ZimbraLog.misc.warn("service failed for node %s", server.getName(), e);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            }
        }
        if (authToken != null && authToken.isRegistered()) {
            try {
                authToken.deRegister();
                ZimbraLog.misc.debug("de-registered auth token, isRegistered?%s", authToken.isRegistered());
            } catch (AuthTokenException e) {
                ZimbraLog.misc.warn("failed to de-register auth token", e);
            }
        }
    }

    /**
     * send service request to one random ui node, keep trying until succeeds.
     * @param serviceUrl the url that should be matched and handled by ServiceServlet in ZimbraWebClient
     * @return response from ui node in String
     * @throws ServiceException
     */
    public static String sendServiceRequestToOneRandomUiNode(String serviceUrl) throws ServiceException {
        return sendServiceRequestToOneRandomUiNode(serviceUrl, Collections.<String,String>emptyMap());
    }

    /**
     * send service request to one random ui node, keep trying until succeeds.
     * @param serviceUrl the url that should be matched and handled by ServiceServlet in ZimbraWebClient
     *        reqHeaders the map of req/rsp attributes that need to be set by the UI node
     * @return response from ui node in String
     * @throws ServiceException
     */
    public static String sendServiceRequestToOneRandomUiNode(String serviceUrl, Map<String, String> reqHeaders) throws ServiceException {
        List<Server> servers = Provisioning.getInstance().getAllServers(Provisioning.SERVICE_WEBCLIENT);
        if (servers == null || servers.isEmpty()) {
            servers.add(Provisioning.getInstance().getLocalServer());
        }
        HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(client);
        AuthToken authToken = AuthProvider.getAdminAuthToken();
        ZimbraLog.misc.debug("got admin auth token");
        String resp = "";
        for (Server server : servers) {
            if (isServerAtLeast8dot5(server)) {
                HttpMethod method = null;
                try {
                    method = new GetMethod(URLUtil.getServiceURL(server, serviceUrl, false));
                    ZimbraLog.misc.debug("connecting to ui node %s", server.getName());
                    method.addRequestHeader(PARAM_AUTHTOKEN, authToken.getEncoded());
                    // Add all the req headers passed to this function as well
                    for (Map.Entry<String, String> entry : reqHeaders.entrySet()) {
                        method.addRequestHeader(entry.getKey(), entry.getValue());
                        ZimbraLog.misc.debug("adding request header %s=%s", entry.getKey(), entry.getValue());
                    }
                    int result = HttpClientUtil.executeMethod(client, method);
                    ZimbraLog.misc.debug("resp: %d", result);
                    resp = method.getResponseBodyAsString();
                    ZimbraLog.misc.debug("got response from ui node: %s", resp);
                    break; //try ui nodes one by one until one succeeds.
                } catch (IOException e) {
                    ZimbraLog.misc.warn("failed to get response from ui node", e);
                } catch (AuthTokenException e) {
                    ZimbraLog.misc.warn("failed to get authToken", e);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }
            }
        }
        if (authToken != null && authToken.isRegistered()) {
            try {
                authToken.deRegister();
                ZimbraLog.misc.debug("de-registered auth token, isRegistered?%s", authToken.isRegistered());
            } catch (AuthTokenException e) {
                ZimbraLog.misc.warn("failed to de-register authToken", e);
            }
        }
        return resp;
    }

    public static String sendServiceRequestToUiNode(Server server, String serviceUrl) throws ServiceException {
        if (isServerAtLeast8dot5(server)) {
            HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(client);
            AuthToken authToken = AuthProvider.getAdminAuthToken();
            ZimbraLog.misc.debug("got admin auth token");
            String resp = "";
            HttpMethod method = null;
            try {
                method = new GetMethod(URLUtil.getServiceURL(server, serviceUrl, false));
                ZimbraLog.misc.debug("connecting to ui node %s", server.getName());
                method.addRequestHeader(PARAM_AUTHTOKEN, authToken.getEncoded());
                int result = HttpClientUtil.executeMethod(client, method);
                ZimbraLog.misc.debug("resp: %d", result);
                resp = method.getResponseBodyAsString();
                ZimbraLog.misc.debug("got response from ui node: %s", resp);
            } catch (IOException e) {
                ZimbraLog.misc.warn("failed to get response from ui node", e);
            } catch (AuthTokenException e) {
                ZimbraLog.misc.warn("failed to get authToken", e);
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
            if (authToken != null && authToken.isRegistered()) {
                try {
                    authToken.deRegister();
                    ZimbraLog.misc.debug("de-registered auth token, isRegistered?%s", authToken.isRegistered());
                } catch (AuthTokenException e) {
                    ZimbraLog.misc.warn("failed to de-register authToken", e);
                }
            }
            return resp;
        }
        return "";
    }

    public static void flushUistringsCache() throws ServiceException {
        sendServiceRequestToEveryUiNode(FLUSH_UISTRINGS_ON_UI_NODE);
    }

    public static void sendFlushZimletRequestToUiNode(Server server) throws ServiceException {
        sendServiceRequestToUiNode(server, "/fromservice/flushzimlets");
    }

    private static void postToUiNode(Server server, PostMethod method, String authToken) throws ServiceException {
        HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(client);
        try {
            method.addRequestHeader(PARAM_AUTHTOKEN, authToken);
            ZimbraLog.zimlet.debug("connecting to ui node %s", server.getName());
            int respCode = HttpClientUtil.executeMethod(client, method);
            if (respCode != 200) {
                ZimbraLog.zimlet.warn("operation failed, return code: %d", respCode);
            }
        } catch (Exception e) {
            ZimbraLog.zimlet.warn("operation failed for node %s", server.getName(), e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    public static void sendDeployZimletRequestToUiNode(Server server, String zimlet, String authToken, byte[] data)
            throws ServiceException {
        if (isServerAtLeast8dot5(server)) {
            HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(client);
            PostMethod method = new PostMethod(URLUtil.getServiceURL(server, "/fromservice/deployzimlet", false));
            method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, zimlet);
            ZimbraLog.zimlet.info("connecting to ui node %s, data size %d", server.getName(), data.length);
            method.setRequestBody(new ByteArrayInputStream(data));
            postToUiNode(server, method, authToken);
        }
    }

    private static boolean isServerAtLeast8dot5(Server server) {
        if (server.getServerVersion() == null) {
            ZimbraLog.misc.info("ui node %s is on version pre 8.5, aborting", server.getName());
            return false;
        }
        return true;
    }

    public static void sendUndeployZimletRequestToUiNode(Server server, String zimlet, String authToken)
            throws ServiceException {
        if (isServerAtLeast8dot5(server)) {
            HttpClient client = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(client);
            PostMethod method = new PostMethod(URLUtil.getServiceURL(server, "/fromservice/undeployzimlet", false));
            method.addRequestHeader(ZimletUtil.PARAM_ZIMLET, zimlet);
            postToUiNode(server, method, authToken);
        }
    }
}
