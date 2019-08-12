/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on 2005. 4. 5.
 */
package com.zimbra.cs.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.JWTUtil;
import com.zimbra.cs.servlet.util.AuthUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.SoapServlet;

/**
 * Superclass for all Zimbra servlets.  Supports port filtering and
 * provides some utility methods to subclasses.
 */
public class ZimbraServlet extends HttpServlet {
    private static final long serialVersionUID = 5025244890767551679L;

    private static Log mLog = LogFactory.getLog(ZimbraServlet.class);

    private static final String PARAM_ALLOWED_PORTS  = "allowed.ports";

    public static final String QP_ZAUTHTOKEN = "zauthtoken";
    public static final String QP_ZJWT = "zjwt";

    protected String getRealmHeader(String realm)  {
        if (realm == null)
            realm = "Zimbra";
        return "BASIC realm=\"" + realm + "\"";
    }

    protected String getRealmHeader(HttpServletRequest req, Domain domain)  {
        String realm = null;

        if (domain == null) {
            // get domain by virtual host
            String host = HttpUtil.getVirtualHost(req);
            if (host != null) {
                // to defend against DOS attack, use the negative domain cache
                try {
                    domain = Provisioning.getInstance().getDomain(Key.DomainBy.virtualHostname, host.toLowerCase(), true);
                } catch (ServiceException e) {
                    mLog.warn("caught exception while getting domain by virtual host: " + host, e);
                }
            }
        }

        if (domain != null)
            realm = domain.getBasicAuthRealm();

        return getRealmHeader(realm);
    }

    public static final String ZIMBRA_FAULT_CODE_HEADER = "X-Zimbra-Fault-Code";
    public static final String ZIMBRA_FAULT_MESSAGE_HEADER = "X-Zimbra-Fault-Message";

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
        return getAuthTokenFromHttpReq(req, resp, false, false);
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, false, doNotSendHttpError);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, true, false);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, true, doNotSendHttpError);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req) {
        return getAuthTokenFromHttpReq(req, true);
    }

    public static AuthToken getAuthTokenFromHttpReq(HttpServletRequest req,
                                                    HttpServletResponse resp,
                                                    boolean isAdminReq,
                                                    boolean doNotSendHttpError) throws IOException {
        AuthToken authToken = null;
        try {
            authToken = getAuthToken(req, isAdminReq);
            if (authToken == null) {
                if (!doNotSendHttpError)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "no authtoken cookie");
                return null;
            }

            if (authToken.isExpired() || !authToken.isRegistered()) {
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

    private static AuthToken getAuthToken(HttpServletRequest req, boolean isAdminReq) throws AuthTokenException {
        AuthToken authToken = AuthProvider.getAuthToken(req, isAdminReq);
        if (authToken == null) {
            Map <Object, Object> engineCtxt = new HashMap<Object, Object>();
            engineCtxt.put(SoapServlet.SERVLET_REQUEST, req);
            authToken = AuthProvider.getJWToken(null, engineCtxt);
        }
        return authToken;
    }

    public static AuthToken getAuthTokenFromHttpReq(HttpServletRequest req, boolean isAdminReq) {
        AuthToken authToken = null;
        try {
            authToken = getAuthToken(req, isAdminReq);
            if (authToken == null)
                return null;

            if (authToken.isExpired())
                return null;

            if (!authToken.isRegistered())
                return null;

            return authToken;
        } catch (AuthTokenException e) {
            return null;
        }
    }

    public static void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, String accountId)
    throws IOException, ServiceException, HttpException {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, accountId);
        if (acct == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such user");
            return;
        }
        proxyServletRequest(req, resp, prov.getServer(acct), null);
    }

    public static void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, Server server, AuthToken authToken)
    throws IOException, ServiceException, HttpException {
        String uri = req.getRequestURI();
        String qs = req.getQueryString();
        if (qs != null) {
            uri += '?' + qs;
        }
        proxyServletRequest(req, resp, server, uri, authToken);
    }

    public static void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, Server server, String uri, AuthToken authToken)
    throws IOException, ServiceException, HttpException {
        if (server == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "cannot find remote server");
            return;
        }
        HttpRequestBase method;
        String url = getProxyUrl(req, server, uri);
        mLog.debug("Proxy URL = %s", url);
        if (req.getMethod().equalsIgnoreCase("GET")) {
            method = new HttpGet(url);
        } else if (req.getMethod().equalsIgnoreCase("POST") || req.getMethod().equalsIgnoreCase("PUT")) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new InputStreamEntity(req.getInputStream()));
            method = post;
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "cannot proxy method: " + req.getMethod());
            return;
        }
        BasicCookieStore state = new BasicCookieStore();
        String hostname = method.getURI().getHost();
        if (authToken != null) {
            authToken.encode(state, false, hostname);
            if (JWTUtil.isJWT(authToken)) {
                try {
                    method.addHeader(Constants.AUTH_HEADER, Constants.BEARER + " " + authToken.getEncoded());
                } catch (AuthTokenException e) {
                    mLog.debug("auth header not set during request proxy");
                }
            }
        }
        try {
            proxyServletRequest(req, resp, method, state);
        } finally {
            method.releaseConnection();
        }
    }

    private static boolean hasZimbraAuthCookie(BasicCookieStore state) {
        List<Cookie> cookies = state.getCookies();
        if (cookies == null)
            return false;

        for (Cookie c: cookies) {
            if (c.getName().equals(ZimbraCookie.COOKIE_ZM_AUTH_TOKEN))
                return true;
        }
        return false;
    }

    // TO DO HTTP
    private static boolean hasJWTSaltCookie(BasicCookieStore state) {
        List<Cookie>  cookies = state == null? null : state.getCookies();
        if (cookies == null) {
            return false;
        }

        for (Cookie c: cookies) {
            if (c.getName().equals(ZimbraCookie.COOKIE_ZM_JWT)) {
                return true;
            }
        }
        return false;
    }

    public static void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, HttpRequestBase method, BasicCookieStore state)
    throws IOException, ServiceException, HttpException {
        // create an HTTP client with the same cookies
        javax.servlet.http.Cookie cookies[] = req.getCookies();
        String hostname = method.getURI().getHost();
        boolean hasZMAuth = hasZimbraAuthCookie(state);
        boolean hasJwtSalt = hasJWTSaltCookie(state);
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if ((cookies[i].getName().equals(ZimbraCookie.COOKIE_ZM_AUTH_TOKEN) && hasZMAuth) ||
                        (hasJwtSalt && cookies[i].getName().equals(ZimbraCookie.COOKIE_ZM_JWT)))
                    continue;
                BasicClientCookie cookie = new BasicClientCookie(cookies[i].getName(), cookies[i].getValue());
                cookie.setDomain(hostname);
                cookie.setPath("/");
                cookie.setSecure(false);
                state.addCookie(cookie);
            }
        }
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        if (state != null)
            clientBuilder.setDefaultCookieStore(state);

        int hopcount = 0;
        for (Enumeration<?> enm = req.getHeaderNames(); enm.hasMoreElements(); ) {
            String hname = (String) enm.nextElement(), hlc = hname.toLowerCase();
            if (hlc.equals("x-zimbra-hopcount"))
                try { hopcount = Math.max(Integer.parseInt(req.getHeader(hname)), 0); } catch (NumberFormatException e) { }
            else if (hlc.startsWith("x-") || hlc.startsWith("content-") || hlc.equals("authorization"))
                method.addHeader(hname, req.getHeader(hname));
        }
        if (hopcount >= MAX_PROXY_HOPCOUNT)
            throw ServiceException.TOO_MANY_HOPS(HttpUtil.getFullRequestURL(req));
        method.addHeader("X-Zimbra-Hopcount", Integer.toString(hopcount + 1));
        if (method.getFirstHeader("X-Zimbra-Orig-Url") == null)
            method.addHeader("X-Zimbra-Orig-Url", req.getRequestURL().toString());
        String ua = req.getHeader("User-Agent");
        if (ua != null)
            method.addHeader("User-Agent", ua);

        // dispatch the request and copy over the results
        int statusCode = -1;
        HttpClient client = clientBuilder.build();
        HttpResponse httpResp = null;
        for (int retryCount = 3; statusCode == -1 && retryCount > 0; retryCount--) {
            httpResp = HttpClientUtil.executeMethod(client, method);
            statusCode = httpResp.getStatusLine().getStatusCode();
        }
        if (statusCode == -1) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "retry limit reached");
            return;
        } else if (statusCode >= 300) {
            resp.sendError(statusCode, httpResp.getStatusLine().getReasonPhrase());
            return;
        }

        Header[] headers = httpResp.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            String hname = headers[i].getName(), hlc = hname.toLowerCase();
            if (hlc.startsWith("x-") || hlc.startsWith("content-") || hlc.startsWith("www-"))
                resp.addHeader(hname, headers[i].getValue());
        }
        InputStream responseStream = httpResp.getEntity().getContent();
        if (responseStream == null || resp.getOutputStream() == null)
            return;
        ByteUtil.copy(httpResp.getEntity().getContent(), false, resp.getOutputStream(), false);

    }

    protected boolean isAdminRequest(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, -1);
        if (req.getLocalPort() == adminPort) {
            //can still be in offline server where port=adminPort
            int mailPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailPort, -1);
            if (mailPort == adminPort) //we are in offline, so check cookie
                return getAdminAuthTokenFromCookie(req) != null;
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

    /**
     * Note that if this method returns null, it isn't clear whether resp.sendError has been called or not.
     * For that reason, it has been deprecated.  Believe there is no Zimbra code which still calls it but
     * left in case customization code uses it.
     */
    @Deprecated
    public Account basicAuthRequest(HttpServletRequest req, HttpServletResponse resp, boolean sendChallenge)
    throws IOException, ServiceException {
        return AuthUtil.basicAuthRequest(req, resp, this, sendChallenge);
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

    public static String getClientIp(HttpServletRequest req) {
        RemoteIP remoteIp = new RemoteIP(req, getTrustedIPs());
        return remoteIp.getClientIP();
    }

    public static void addRemoteIpToLoggingContext(HttpServletRequest req) {
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

    public static void addUAToLoggingContext(HttpServletRequest req) {
        ZimbraLog.addUserAgentToContext(req.getHeader("User-Agent"));
    }
}
