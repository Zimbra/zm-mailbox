/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.servlet.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.ZimbraServlet;

public class AuthUtil {
    private static Log mLog = LogFactory.getLog(AuthUtil.class);

    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
   
    
    /**
     * Checks to see if this is an admin request
     * @param req
     * @return
     * @throws ServiceException
     */
    public static boolean isAdminRequest(HttpServletRequest req) throws ServiceException {
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

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, false, false);
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, false, doNotSendHttpError);
    }

    public static AuthToken getAuthTokenFromHttpReq(HttpServletRequest req, boolean isAdminReq) {
        AuthToken authToken = null;
        try {
            authToken = AuthProvider.getAuthToken(req, isAdminReq);
            if (authToken == null)
                return null;
            
            if (authToken.isExpired())
                return null;
    
            return authToken;
        } catch (AuthTokenException e) {
            return null;
        }
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

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req) {
        return getAuthTokenFromHttpReq(req, true);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, true, false);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, true, doNotSendHttpError);
    }

    public static Account basicAuthRequest(HttpServletRequest req, 
                                           HttpServletResponse resp,
                                           ZimbraServlet servlet,
                                           boolean sendChallenge)
    throws IOException, ServiceException {
    
        if (!AuthProvider.allowBasicAuth(req, servlet))
            return null;
    
        String auth = req.getHeader("Authorization");
    
        // TODO: more liberal parsing of Authorization value...
        if (auth == null || !auth.startsWith("Basic ")) {
            if (sendChallenge) {
                resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));            
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
                resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));
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
                resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader(req, prov.getDomain(acct)));
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
            }
            return null;
        }
        return acct;
    }

    public AuthToken cookieAuthRequest(HttpServletRequest req, HttpServletResponse resp) 
    throws IOException, ServiceException {
        AuthToken at = AuthUtil.isAdminRequest(req) ? AuthUtil.getAdminAuthTokenFromCookie(req, resp, true) : AuthUtil.getAuthTokenFromCookie(req, resp, true);
        return at;
    }

    public static String getRealmHeader(HttpServletRequest req, Domain domain)  { 
        String realm = null;
        
        if (domain == null) {
            // get domain by virtual host
            String host = HttpUtil.getVirtualHost(req);
            if (host != null) {
                // to defend against DOS attack, use the negative domain cache
                try {
                    domain = Provisioning.getInstance().getDomain(DomainBy.virtualHostname, host.toLowerCase(), true);
                } catch (ServiceException e) {
                    mLog.warn("caught exception while getting domain by virtual host: " + host, e);
                }
            }
        }
        
        if (domain != null)
            realm = domain.getBasicAuthRealm();
    
        return getRealmHeader(realm);
    }

    public static String getRealmHeader(String realm)  { 
        if (realm == null)
            realm = "Zimbra";
        return "BASIC realm=\"" + realm + "\""; 
    }

}
