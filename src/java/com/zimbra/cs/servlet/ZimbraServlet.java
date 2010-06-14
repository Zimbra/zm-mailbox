/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on 2005. 4. 5.
 */
package com.zimbra.cs.servlet;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.Zimbra;

/**
 * Superclass for all Zimbra servlets.  Supports port filtering and
 * provides some utility methods to subclasses.
 */
public class ZimbraServlet extends HttpServlet {
    private static final long serialVersionUID = 5025244890767551679L;

    private static Log mLog = LogFactory.getLog(ZimbraServlet.class);

    public static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    public static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN"; 

    private static final String PARAM_ALLOWED_PORTS  = "allowed.ports";

    protected static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    protected String getRealmHeader()  { return "BASIC realm=\"Zimbra\""; }
    
    protected static final String ZIMBRA_FAULT_CODE_HEADER    = "X-Zimbra-Fault-Code";
    protected static final String ZIMBRA_FAULT_MESSAGE_HEADER = "X-Zimbra-Fault-Message";

    private static final int MAX_PROXY_HOPCOUNT = 3;

    private static Map<String, ZimbraServlet> sServlets = new HashMap<String, ZimbraServlet>();

    private int[] mAllowedPorts;

    @Override public void init() throws ServletException {
        try {
            String portsCSV = getInitParameter(PARAM_ALLOWED_PORTS);
            if (portsCSV != null) {
                // Split on zero-or-more spaces followed by comma followed by
                // zero-or-more spaces.
                String[] vals = portsCSV.split("\\s*,\\s*");
                if (vals == null || vals.length == 0)
                    throw new ServletException("Must specify comma-separated list of port numbers for " +
                                               PARAM_ALLOWED_PORTS + " parameter");
                List<Integer> allowedPorts = new ArrayList<Integer>();
                int port;
                for (int i = 0; i < vals.length; i++) {
                    try {
                	port = Integer.parseInt(vals[i]);
                    } catch (NumberFormatException e) {
                        throw new ServletException("Invalid port number \"" + vals[i] + "\" in " +
                                                   PARAM_ALLOWED_PORTS + " parameter");
                    }
                    if (port < 0)
                	throw new ServletException("Invalid port number " + vals[i] + " in " +
                                                   PARAM_ALLOWED_PORTS + " parameter; port number must be greater than zero");
                    else if (port != 0)  // 0 is a legit value for those ports that are disabled
                	allowedPorts.add(port);
                }
                
                mAllowedPorts = new int[allowedPorts.size()];
                for (int i=0; i<allowedPorts.size(); i++)
                    mAllowedPorts[i] = allowedPorts.get(i);
            }
            
            // Store reference to this servlet for accessor 
            synchronized (sServlets) {
                String name = getServletName();
                if (sServlets.containsKey(name)) {
                    Zimbra.halt("Attempted to instantiate a second instance of " + name);
                }
                sServlets.put(getServletName(), this);
                mLog.debug("Added " + getServletName() + " to the servlet list");
            }
        } catch (Throwable t) {
            Zimbra.halt("Unable to initialize servlet " + getServletName() + "; halting", t);
        }
    }
    
    public static ZimbraServlet getServlet(String name) {
        synchronized (sServlets) {
            return sServlets.get(name);
        }
    }

    protected boolean isRequestOnAllowedPort(HttpServletRequest request) {
        if (mAllowedPorts != null && mAllowedPorts.length > 0) {
            int incoming = request.getLocalPort();
            for (int i = 0; i < mAllowedPorts.length; i++) {
                if (mAllowedPorts[i] == incoming) {
                	return true;
                }
            }
            return false;
        }
        return true;
    }
    /**
     * Filter the request based on incoming port.  If the allowed.ports
     * parameter is specified for the servlet, the incoming port must
     * match one of the listed ports.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        boolean allowed = isRequestOnAllowedPort(request);
        if (!allowed) {
            SoapProtocol soapProto = SoapProtocol.Soap12;
            ServiceException e = ServiceException.FAILURE("Request not allowed on port " + request.getLocalPort(), null);
            ZimbraLog.soap.warn(null, e);
            Element fault = SoapProtocol.Soap12.soapFault(e);
            Element envelope = SoapProtocol.Soap12.soapEnvelope(fault);
            byte[] soapBytes = envelope.toUTF8();
            response.setContentType(soapProto.getContentType());
            response.setBufferSize(soapBytes.length + 2048);
            response.setContentLength(soapBytes.length);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().write(soapBytes);
            return;
        }
        super.service(request, response);
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, false, false);
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, false, doNotSendHttpError);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, true, false);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, true, doNotSendHttpError);
    }

    private static AuthToken getAuthTokenFromCookieImpl(HttpServletRequest req,
            HttpServletResponse resp,
            boolean isAdminReq,
            boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, isAdminReq,  doNotSendHttpError);
    }
    
    public static AuthToken getAuthTokenFromHttpReq(HttpServletRequest req,
                                                    HttpServletResponse resp,
                                                    boolean isAdminReq,
                                                    boolean doNotSendHttpError) throws IOException {
        AuthToken authToken = null;
        try {
            authToken = AuthProvider.getAuthToken(req, isAdminReq);
            if (authToken == null) {
                if (!doNotSendHttpError)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "no authtoken cookie");
                return null;
            }

            if (authToken.isExpired()) {
                if (!doNotSendHttpError)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "authtoken expired");
                return null;
            }
            return authToken;
        } catch (AuthTokenException e) {
            if (!doNotSendHttpError)
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "unable to parse authtoken");
            return null;
        }
    }


    protected void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, String accountId)
    throws IOException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, accountId);
        if (acct == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such user");
            return;
        }
        proxyServletRequest(req, resp, prov.getServer(acct), null);
    }

    protected void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, Server server, AuthToken authToken)
    throws IOException, ServiceException {
        proxyServletRequest(req, resp, server, HttpUtil.getFullRequestURL(req), authToken);
    }

    protected void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, Server server, String uri, AuthToken authToken)
    throws IOException, ServiceException {
        if (server == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "cannot find remote server");
            return;
        }
        HttpMethod method;
        String url = getProxyUrl(req, server, uri);
        if (req.getMethod().equalsIgnoreCase("GET")) {
            method = new GetMethod(url.toString());
        } else if (req.getMethod().equalsIgnoreCase("POST") || req.getMethod().equalsIgnoreCase("PUT")) {
            PostMethod post = new PostMethod(url.toString());
            post.setRequestEntity(new InputStreamRequestEntity(req.getInputStream()));
            method = post;
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "cannot proxy method: " + req.getMethod());
            return;
        }
        HttpState state = new HttpState();
        String hostname = method.getURI().getHost();
        if (authToken != null)
            authToken.encode(state, false, hostname);

        try {
            proxyServletRequest(req, resp, method, state);
        } finally {
            method.releaseConnection();
        }
    }

    private boolean hasZimbraAuthCookie(HttpState state) {
        Cookie[] cookies = state.getCookies();
        if (cookies == null)
            return false;
        
        for (Cookie c: cookies) {
            if (c.getName().equals(COOKIE_ZM_AUTH_TOKEN))
                return true;
        }
        return false;
    }
    
    protected void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, HttpMethod method, HttpState state)
    throws IOException, ServiceException {
        // create an HTTP client with the same cookies
        javax.servlet.http.Cookie cookies[] = req.getCookies();
        String hostname = method.getURI().getHost();
        boolean hasZMAuth = hasZimbraAuthCookie(state);
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(COOKIE_ZM_AUTH_TOKEN) && hasZMAuth)
                    continue;
                state.addCookie(new Cookie(hostname, cookies[i].getName(), cookies[i].getValue(), "/", null, false));
            }
        }
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        if (state != null)
            client.setState(state);

        int hopcount = 0;
        for (Enumeration<?> enm = req.getHeaderNames(); enm.hasMoreElements(); ) {
            String hname = (String) enm.nextElement(), hlc = hname.toLowerCase();
            if (hlc.equals("x-zimbra-hopcount"))
                try { hopcount = Math.max(Integer.parseInt(req.getHeader(hname)), 0); } catch (NumberFormatException e) { }
            else if (hlc.startsWith("x-") || hlc.startsWith("content-") || hlc.equals("authorization"))
                method.addRequestHeader(hname, req.getHeader(hname));
        }
        if (hopcount >= MAX_PROXY_HOPCOUNT)
            throw ServiceException.TOO_MANY_HOPS(HttpUtil.getFullRequestURL(req));
        method.addRequestHeader("X-Zimbra-Hopcount", Integer.toString(hopcount + 1));
        String ua = req.getHeader("User-Agent");
        if (ua != null)
            method.setRequestHeader("User-Agent", ua);

        // dispatch the request and copy over the results
        int statusCode = -1;
        for (int retryCount = 3; statusCode == -1 && retryCount > 0; retryCount--) {
            statusCode = HttpClientUtil.executeMethod(client, method);
        }
        if (statusCode == -1) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "retry limit reached");
            return;
        } else if (statusCode >= 300) {
            resp.sendError(statusCode, method.getStatusText());
            return;
        }

        Header[] headers = method.getResponseHeaders();
        for (int i = 0; i < headers.length; i++) {
            String hname = headers[i].getName(), hlc = hname.toLowerCase();
            if (hlc.startsWith("x-") || (hlc.startsWith("content-") && !hlc.equals("content-length"))  || hlc.startsWith("www-"))    
                resp.addHeader(hname, headers[i].getValue());
        }
        InputStream responseStream = method.getResponseBodyAsStream();
        if (responseStream == null || resp.getOutputStream() == null)
            return;
        ByteUtil.copy(method.getResponseBodyAsStream(), false, resp.getOutputStream(), false);
    }

    protected boolean isAdminRequest(HttpServletRequest req) throws ServiceException, IOException {
        int adminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, -1);
        if (req.getLocalPort() == adminPort) {
            //can still be in offline server where port=adminPort
            int mailPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailPort, -1);
            if (mailPort == adminPort) //we are in offline, so check cookie
                return getAdminAuthTokenFromCookie(req, null, true) != null;
            else
                return true;
        }
        return false;
    }

    public AuthToken cookieAuthRequest(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServiceException {
        AuthToken at = isAdminRequest(req) ? getAdminAuthTokenFromCookie(req, resp, true) : getAuthTokenFromCookie(req, resp, true);
        return at;
    }

    public Account basicAuthRequest(HttpServletRequest req, HttpServletResponse resp, boolean sendChallenge)
    throws IOException, ServiceException {

        if (!AuthProvider.allowBasicAuth(req, this))
            return null;

        String auth = req.getHeader("Authorization");

        // TODO: more liberal parsing of Authorization value...
        if (auth == null || !auth.startsWith("Basic ")) {
            if (sendChallenge) {
                resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());            
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must authenticate");
            }
            return null;
        }

        // 6 comes from "Basic ".length();
        String userPass = new String(Base64.decodeBase64(auth.substring(6).getBytes()), "UTF-8");

        int loc = userPass.indexOf(":"); 
        if (loc == -1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid basic auth credentials");
            return null;
        }

        String userPassedIn = userPass.substring(0, loc);
        String user = userPassedIn;
        String pass = userPass.substring(loc + 1);

        Provisioning prov = Provisioning.getInstance();

        if (user.indexOf('@') == -1) {
            String host = HttpUtil.getVirtualHost(req);
            if (host != null) {
                Domain d = prov.get(DomainBy.virtualHostname, host.toLowerCase());
                if (d != null) user += "@" + d.getName();
            }
        }

        Account acct = prov.get(AccountBy.name, user);
        if (acct == null) {
            if (sendChallenge) {
                resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
            }
            return new GuestAccount(user, pass);
        }
        try {
            Map<String, Object> authCtxt = new HashMap<String, Object>();
            authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
            authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, userPassedIn);
            authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
            prov.authAccount(acct, pass, AuthContext.Protocol.http_basic, authCtxt);
        } catch (ServiceException se) {
            if (sendChallenge) {
                resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
            }
            return null;
        }
        return acct;
    }


    public static String getAccountPath(Account acct) {
        return "/" + acct.getName();
    }
    
    public static String getServiceUrl(Account acct, String path) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getServer(acct);

        if (server == null) {
            throw ServiceException.FAILURE("unable to retrieve server for account" + acct.getName(), null);
        }

        return getServiceUrl(server, prov.getDomain(acct), path + getAccountPath(acct));
    }


    public static String getServiceUrl(Server server, Domain domain, String path) throws ServiceException {
        return URLUtil.getPublicURLForDomain(server, domain, path, true);
    }

    protected static String getProxyUrl(HttpServletRequest req, Server server, String path) throws ServiceException {
        int servicePort = (req == null) ? -1 : req.getLocalPort();
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        if (!prov.isOfflineProxyServer(server) && servicePort == localServer.getIntAttr(Provisioning.A_zimbraAdminPort, 0))
            return URLUtil.getAdminURL(server, path);
        else
            return URLUtil.getServiceURL(server, path, servicePort == localServer.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0));
    }

    protected void returnError(HttpServletResponse resp, ServiceException e) {
        resp.setHeader(ZIMBRA_FAULT_CODE_HEADER, e.getCode());
        resp.setHeader(ZIMBRA_FAULT_MESSAGE_HEADER, e.getMessage());
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public static String getOrigIp(HttpServletRequest req) {
        RemoteIP remoteIp = new RemoteIP(req, getTrustedIPs());
        return remoteIp.getOrigIP();
    }

    protected void addRemoteIpToLoggingContext(HttpServletRequest req) {
        RemoteIP remoteIp = new RemoteIP(req, getTrustedIPs());
        remoteIp.addToLoggingContext();
    }

    public static RemoteIP.TrustedIPs getTrustedIPs() {
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            return new RemoteIP.TrustedIPs(server.getMultiAttr(Provisioning.A_zimbraMailTrustedIP));
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("failed to get trusted IPs, only localhost will be trusted", e);
        }
        return new RemoteIP.TrustedIPs(null);
    }
}
