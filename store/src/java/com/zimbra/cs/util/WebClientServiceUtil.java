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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

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
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(clientBuilder);
        for (Server server : servers) {
            if (isServerAtLeast8dot5(server)) {
                HttpRequestBase method = null;
                try {
                    method = new HttpGet(URLUtil.getServiceURL(server, serviceUrl, false));
                    ZimbraLog.misc.debug("connecting to ui node %s", server.getName());
                    try {
                        method.addHeader(PARAM_AUTHTOKEN, authToken.getEncoded());
                    } catch (AuthTokenException e) {
                        ZimbraLog.misc.warn(e);
                    }
                    HttpResponse httpResp = HttpClientUtil.executeMethod(clientBuilder.build(), method);
                    int respCode = httpResp.getStatusLine().getStatusCode();
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
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(clientBuilder);
        AuthToken authToken = AuthProvider.getAdminAuthToken();
        ZimbraLog.misc.debug("got admin auth token");
        String resp = "";
        for (Server server : servers) {
            if (isServerAtLeast8dot5(server)) {
                HttpRequestBase method = null;
                try {
                    method = new HttpGet(URLUtil.getServiceURL(server, serviceUrl, false));
                    ZimbraLog.misc.debug("connecting to ui node %s", server.getName());
                    method.addHeader(PARAM_AUTHTOKEN, authToken.getEncoded());
                    // Add all the req headers passed to this function as well
                    for (Map.Entry<String, String> entry : reqHeaders.entrySet()) {
                        method.addHeader(entry.getKey(), entry.getValue());
                        ZimbraLog.misc.debug("adding request header %s=%s", entry.getKey(), entry.getValue());
                    }
                    HttpResponse httpResp = HttpClientUtil.executeMethod(clientBuilder.build(), method);
                    int result = httpResp.getStatusLine().getStatusCode();
                    ZimbraLog.misc.debug("resp: %d", result);
                    resp = EntityUtils.toString(httpResp.getEntity());
                    ZimbraLog.misc.debug("got response from ui node: %s", resp);
                    break; //try ui nodes one by one until one succeeds.
                } catch (IOException | HttpException e) {
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
            HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(clientBuilder);
            AuthToken authToken = AuthProvider.getAdminAuthToken();
            ZimbraLog.misc.debug("got admin auth token");
            String resp = "";
            HttpRequestBase method = null;
            try {
                method = new HttpGet(URLUtil.getServiceURL(server, serviceUrl, false));
                ZimbraLog.misc.debug("connecting to ui node %s", server.getName());
                method.addHeader(PARAM_AUTHTOKEN, authToken.getEncoded());
                HttpResponse httpResp = HttpClientUtil.executeMethod(clientBuilder.build(), method);
                int result = httpResp.getStatusLine().getStatusCode();
                ZimbraLog.misc.debug("resp: %d", result);
                resp = EntityUtils.toString(httpResp.getEntity());
                ZimbraLog.misc.debug("got response from ui node: %s", resp);
            } catch (IOException | HttpException e) {
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

    private static void postToUiNode(Server server, HttpPost method, String authToken) throws ServiceException {
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpProxyUtil.configureProxy(clientBuilder);
        try {
            method.addHeader(PARAM_AUTHTOKEN, authToken);
            ZimbraLog.zimlet.debug("connecting to ui node %s", server.getName());
            HttpResponse httpResp = HttpClientUtil.executeMethod(clientBuilder.build(), method);
            int respCode = httpResp.getStatusLine().getStatusCode();
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
            HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(clientBuilder);
            HttpPost method = new HttpPost(URLUtil.getServiceURL(server, "/fromservice/deployzimlet", false));
            method.addHeader(ZimletUtil.PARAM_ZIMLET, zimlet);
            ZimbraLog.zimlet.info("connecting to ui node %s, data size %d", server.getName(), data.length);

            method.setEntity(new ByteArrayEntity(data));
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
            HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
            HttpProxyUtil.configureProxy(clientBuilder);
            HttpPost method = new HttpPost(URLUtil.getServiceURL(server, "/fromservice/undeployzimlet", false));
            method.addHeader(ZimletUtil.PARAM_ZIMLET, zimlet);
            postToUiNode(server, method, authToken);
        }
    }
}
