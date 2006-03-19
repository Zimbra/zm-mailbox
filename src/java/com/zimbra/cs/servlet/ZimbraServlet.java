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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 4. 5.
 */
package com.zimbra.cs.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.*;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapProtocol;

/**
 * @author jhahm
 *
 * Superclass for all Zimbra servlets.  Supports port filtering and
 * provides some utility methods to subclasses.
 */
public class ZimbraServlet extends HttpServlet {

    private static Log mLog = LogFactory.getLog(ZimbraServlet.class);

    public static final String USER_SERVICE_URI  = "/service/soap/";
    public static final String ADMIN_SERVICE_URI = "/service/admin/soap/";

    public static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    public static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN"; 

    private static final String PARAM_ALLOWED_PORTS  = "allowed.ports";

    protected static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    protected String getRealmHeader()  { return "BASIC realm=\"Zimbra\""; }

    private static final int MAX_PROXY_HOPCOUNT = 3;

    private static Map<String, ZimbraServlet> sServlets = new HashMap<String, ZimbraServlet>();

    private int[] mAllowedPorts;

    public void init() throws ServletException {
        String portsCSV = getInitParameter(PARAM_ALLOWED_PORTS);
        if (portsCSV != null) {
            // Split on zero-or-more spaces followed by comma followed by
            // zero-or-more spaces.
            String[] vals = portsCSV.split("\\s*,\\s*");
            if (vals == null || vals.length == 0)
                throw new ServletException("Must specify comma-separated list of port numbers for " +
                                           PARAM_ALLOWED_PORTS + " parameter");
            mAllowedPorts = new int[vals.length];
            for (int i = 0; i < vals.length; i++) {
                try {
                    mAllowedPorts[i] = Integer.parseInt(vals[i]);
                } catch (NumberFormatException e) {
                    throw new ServletException("Invalid port number \"" + vals[i] + "\" in " +
                                               PARAM_ALLOWED_PORTS + " parameter");
                }
                if (mAllowedPorts[i] < 1)
                    throw new ServletException("Invalid port number " + mAllowedPorts[i] + " in " +
                                               PARAM_ALLOWED_PORTS + " parameter; port number must be greater than zero");
            }
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
    }
    
    public static ZimbraServlet getServlet(String name) {
        synchronized (sServlets) {
            return sServlets.get(name);
        }
    }

    /**
     * Filter the request based on incoming port.  If the allowed.ports
     * parameter is specified for the servlet, the incoming port must
     * match one of the listed ports.
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        if (mAllowedPorts != null && mAllowedPorts.length > 0) {
            int incoming = request.getServerPort();
            boolean allowed = false;
            for (int i = 0; i < mAllowedPorts.length; i++) {
                if (mAllowedPorts[i] == incoming) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                SoapProtocol soapProto = SoapProtocol.Soap12;
                Element fault = SoapProtocol.Soap12.soapFault(
                        ServiceException.FAILURE("Request not allowed on port " + incoming, null));
                Element envelope = SoapProtocol.Soap12.soapEnvelope(fault);
                byte[] soapBytes = envelope.toUTF8();
                response.setContentType(soapProto.getContentType());
                response.setBufferSize(soapBytes.length + 2048);
                response.setContentLength(soapBytes.length);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getOutputStream().write(soapBytes);
                return;
            }
        }
        super.service(request, response);
    }

    /**
     * read until EOF is reached
     */
    protected byte[] readUntilEOF(InputStream input) throws IOException {
        final int SIZE = 2048;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(SIZE);
        byte[] buffer = new byte[SIZE];

        int n = 0;
        while ((n = input.read(buffer, 0, SIZE)) > 0)
            baos.write(buffer, 0, n);
        return baos.toByteArray();
    }

    protected void readFully(InputStream in, byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
            throw new java.io.EOFException();
            n += count;
        }
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req,
                                                      HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, COOKIE_ZM_AUTH_TOKEN, false);
    }

    protected static AuthToken getAuthTokenFromCookie(HttpServletRequest req,
                                                      HttpServletResponse resp,
                                                      boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, COOKIE_ZM_AUTH_TOKEN, doNotSendHttpError);
    }

    protected static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req,
                                                           HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromCookieImpl(req, resp, COOKIE_ZM_ADMIN_AUTH_TOKEN, false);
    }

    private static AuthToken getAuthTokenFromCookieImpl(HttpServletRequest req,
                                                        HttpServletResponse resp,
                                                        String cookieName,
                                                        boolean doNotSendHttpError)
    throws IOException {
        String authTokenStr = null;
        javax.servlet.http.Cookie cookies[] =  req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(cookieName)) {
                    authTokenStr = cookies[i].getValue();
                    break;
                }
            }
        }

        if (authTokenStr == null) {
            if (!doNotSendHttpError)
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "no authtoken cookie");
            return null;
        }

        try {
            AuthToken authToken = AuthToken.getAuthToken(authTokenStr);
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
    throws IOException, ServletException {
		try {
            Provisioning prov = Provisioning.getInstance();
            Account acct = prov.getAccountById(accountId);
    		if (acct == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such user");
                return;
            }
            proxyServletRequest(req, resp, acct.getServer(), null);
		} catch (ServiceException e) {
			throw new ServletException(e);
		}
    }

    protected void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, Server server, String authToken)
    throws IOException, ServletException {
        String uri = req.getRequestURI(), qs = req.getQueryString();
        if (qs != null)
            uri += '?' + qs;
        proxyServletRequest(req, resp, server, uri, authToken);
    }
    protected void proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, Server server, String uri, String authToken)
    throws IOException, ServletException {
        if (server == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "cannot find remote server");
            return;
        }

        try {
            // determine the URI for the remote ContentServlet
            int port = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
            boolean useHTTP = port > 0;
            if (!useHTTP)
                port = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
            if (port <= 0)
                throw ServiceException.FAILURE("remote server " + server.getName() + " has neither http nor https port enabled", null);
            String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);

            StringBuffer url = new StringBuffer(useHTTP ? "http" : "https");
            url.append("://").append(hostname).append(':').append(port).append(uri);

            // create an HTTP client with the same cookies
            HttpState state = new HttpState();
            javax.servlet.http.Cookie cookies[] = req.getCookies();
            if (cookies != null)
                for (int i = 0; i < cookies.length; i++)
                    state.addCookie(new Cookie(hostname, cookies[i].getName(), cookies[i].getValue(), "/", null, false));
            if (authToken != null)
                state.addCookie(new Cookie(hostname, COOKIE_ZM_AUTH_TOKEN, authToken, "/", null, false));                

            HttpClient client = new HttpClient();
            client.setState(state);

            // create a duplicate request (same method, same content, same X-headers)
            HttpMethod method = null;
            int hopcount = 0;
            if (req.getMethod().equalsIgnoreCase("GET"))
                method = new GetMethod(url.toString());
            else if (req.getMethod().equalsIgnoreCase("POST") || req.getMethod().equalsIgnoreCase("PUT")) {
                PostMethod post = new PostMethod(url.toString());
                post.setRequestBody(req.getInputStream());
                method = post;
            } else
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "cannot proxy method: " + req.getMethod());
            for (Enumeration enm = req.getHeaderNames(); enm.hasMoreElements(); ) {
                String hname = (String) enm.nextElement(), hlc = hname.toLowerCase();
                if (hlc.equals("x-zimbra-hopcount"))
                    try { hopcount = Math.max(Integer.parseInt(req.getHeader(hname)), 0); } catch (NumberFormatException e) { }
                else if (hlc.startsWith("x-") || hlc.startsWith("content-") || hlc.equals("authorization"))
                    method.addRequestHeader(hname, req.getHeader(hname));
            }
            if (hopcount >= MAX_PROXY_HOPCOUNT)
                throw ServiceException.TOO_MANY_HOPS();
            method.addRequestHeader("X-Zimbra-Hopcount", Integer.toString(hopcount + 1));

            // dispatch the request and copy over the results
            int statusCode = -1;
            for (int retryCount = 3; statusCode == -1 && retryCount > 0; retryCount--)
                try {
                    statusCode = client.executeMethod(method);
                } catch (HttpRecoverableException e) { }
            if (statusCode != HttpStatus.SC_OK) {
                if (statusCode == -1)
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "retry limit reached");
                else
                	resp.sendError(statusCode, method.getStatusText());
                return;
            }
            Header[] headers = method.getResponseHeaders();
            for (int i = 0; i < headers.length; i++) {
                String hname = headers[i].getName(), hlc = hname.toLowerCase();
                if (hlc.startsWith("x-") || hlc.startsWith("content-") || hlc.startsWith("www-"))
                    resp.addHeader(hname, headers[i].getValue());
            }
            ByteUtil.copy(method.getResponseBodyAsStream(), resp.getOutputStream());

            method.releaseConnection();
        } catch (ServiceException e) {
            throw new ServletException(e);
        }
    }
    

    protected Account cookieAuthRequest(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError) 
        throws IOException, ServletException, ServiceException 
    {
        AuthToken at = getAuthTokenFromCookie(req, resp, doNotSendHttpError);
        return at == null ? null : Provisioning.getInstance().getAccountById(at.getAccountId()); 
    }
    
    protected Account basicAuthRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException, ServiceException {
        String auth = req.getHeader("Authorization");

        // TODO: more liberal parsing of Authorization value...
        if (auth == null || !auth.startsWith("Basic ")) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());            
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "must authenticate");
            return null;
        }

        // 6 comes from "Basic ".length();
        String userPass = new String(Base64.decodeBase64(auth.substring(6).getBytes()));

        int loc = userPass.indexOf(":"); 
        if (loc == -1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid basic auth credentials");
            return null;
        }
        
        String user = userPass.substring(0, loc);
        String pass = userPass.substring(loc + 1);

        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.getAccountByName(user);
        if (acct == null) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
            return null;
        }
        try {
            prov.authAccount(acct, pass);
        } catch (ServiceException se) {
            resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader());
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
            return null;
        }
        return acct;
    }
}
