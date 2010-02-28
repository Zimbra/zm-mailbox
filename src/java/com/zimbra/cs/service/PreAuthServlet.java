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
 * Created on Dec 20, 2004
 * @author Greg Solovyev
 * */
package com.zimbra.cs.service;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.SoapEngine;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.net.URLEncoder;

public class PreAuthServlet extends ZimbraServlet {

    public static final String PARAM_PREAUTH = "preauth";
    public static final String PARAM_AUTHTOKEN = "authtoken";
    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_ADMIN = "admin";
    public static final String PARAM_ISREDIRECT = "isredirect";
    public static final String PARAM_BY = "by";
    public static final String PARAM_REDIRECT_URL = "redirectURL";
    public static final String PARAM_TIMESTAMP = "timestamp";
    public static final String PARAM_EXPIRES = "expires";    
    
    private static final HashSet<String> sPreAuthParams = new HashSet<String>();
    
    static {
        sPreAuthParams.add(PARAM_PREAUTH);
        sPreAuthParams.add(PARAM_AUTHTOKEN);
        sPreAuthParams.add(PARAM_ACCOUNT);
        sPreAuthParams.add(PARAM_ADMIN);
        sPreAuthParams.add(PARAM_ISREDIRECT);
        sPreAuthParams.add(PARAM_BY);
        sPreAuthParams.add(PARAM_TIMESTAMP);
        sPreAuthParams.add(PARAM_EXPIRES);
    }

    public void init() throws ServletException {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " starting up");
        super.init();
    }

    public void destroy() {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    private String getRequiredParam(HttpServletRequest req, HttpServletResponse resp, String paramName) throws ServiceException {
        String param = req.getParameter(paramName);
        if (param == null) throw ServiceException.INVALID_REQUEST("missing required param: "+paramName, null);
        else return param;
    }

    private String getOptionalParam(HttpServletRequest req, String paramName, String def) {
        String param = req.getParameter(paramName);
        if (param == null) return def;
        else return param;
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        ZimbraLog.clearContext();
        try {
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getLocalServer();
            String referMode = server.getAttr(Provisioning.A_zimbraMailReferMode, "wronghost");
            
            boolean isRedirect = getOptionalParam(req, PARAM_ISREDIRECT, "0").equals("1");
            String rawAuthToken = getOptionalParam(req, PARAM_AUTHTOKEN, null);
            AuthToken authToken = null;
            if (rawAuthToken != null) {
                authToken = AuthProvider.getAuthToken(rawAuthToken);
                if (authToken == null)
                    throw new AuthTokenException("unable to get auth token from " + PARAM_AUTHTOKEN);
            }
            
            if (rawAuthToken != null) {
                // we've got an auth token in the request:
                // See if we need a redirect to the correct server
                boolean isAdmin = authToken != null && AuthToken.isAnyAdmin(authToken);
                Account acct = prov.get(AccountBy.id, authToken.getAccountId(), authToken);
                if (isAdmin || !needReferral(acct, referMode, isRedirect)) {
                    // no need to redirect to the correct server, just send them off to do business
                    setCookieAndRedirect(req, resp, authToken);
                } else {
                    // redirect to the correct server with the incoming auth token
                    // we no longer send the auth token we generate over when we redirect to the correct server, 
                    // but customer can be sending a token in their preauth URL, in this case, just 
                    // send over the auth token as is.
                    redirectToCorrectServer(req, resp, acct, rawAuthToken);
                }
            } else {
                // no auth token in the request URL.  See if we should redirect this request 
                // to the correct server, or should do the preauth locally.
                
                String preAuth = getRequiredParam(req, resp, PARAM_PREAUTH);            
                String account = getRequiredParam(req, resp, PARAM_ACCOUNT);
                String accountBy = getOptionalParam(req, PARAM_BY, AccountBy.name.name());

                boolean admin = getOptionalParam(req, PARAM_ADMIN, "0").equals("1") && isAdminRequest(req);
                long timestamp = Long.parseLong(getRequiredParam(req, resp, PARAM_TIMESTAMP));
                long expires = Long.parseLong(getRequiredParam(req, resp, PARAM_EXPIRES));
            
                Account acct = null;
                acct = prov.get(AccountBy.fromString(accountBy), account, authToken);                            
            
                if (acct == null)
                    throw AuthFailedServiceException.AUTH_FAILED(account, account, "account not found");

                if (admin) {
                    boolean isDomainAdminAccount = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
                    boolean isAdminAccount = acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
                    boolean isDelegatedAdminAccount = acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false);
                    boolean ok = (isDomainAdminAccount || isAdminAccount || isDelegatedAdminAccount);
                    if (!ok)
                        throw ServiceException.PERM_DENIED("not an admin account");
                }
                
                // all params are well, now see if we should preauth locally or redirect to the correct server.
                
                if (admin || !needReferral(acct, referMode, isRedirect)) {
                    // do preauth locally
                    Map<String, Object> authCtxt = new HashMap<String, Object>();
                    authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
                    authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, account);
                    authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
                    prov.preAuthAccount(acct, account, accountBy, timestamp, expires, preAuth, admin, authCtxt);
                
                    AuthToken at;
    
                    if (admin)
                        at = (expires ==  0) ? AuthProvider.getAuthToken(acct, admin) : AuthProvider.getAuthToken(acct, expires, admin, null);
                    else
                        at = (expires ==  0) ? AuthProvider.getAuthToken(acct) : AuthProvider.getAuthToken(acct, expires);

                    setCookieAndRedirect(req, resp, at);
                    
                } else {
                    // redirect to the correct server.  
                    // Note: we do not send over the generated auth token (the auth token param passed to 
                    // redirectToCorrectServer is null).
                    redirectToCorrectServer(req, resp, acct, null);
                }
            }
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (AuthTokenException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
    
    private boolean needReferral(Account acct, String referMode, boolean isRedirect) throws ServiceException {
        // if this request is already a redirect, don't redirect again
        if (isRedirect)
            return false;
        
        return (Provisioning.MAIL_REFER_MODE_ALWAYS.equals(referMode) ||
                (Provisioning.MAIL_REFER_MODE_WRONGHOST.equals(referMode) && !Provisioning.onLocalServer(acct)));
    }

    private void addQueryParams(HttpServletRequest req, StringBuilder sb, boolean first, boolean nonPreAuthParamsOnly) {
        Enumeration names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            
            if (nonPreAuthParamsOnly && sPreAuthParams.contains(name)) 
                continue;
            
            String values[] = req.getParameterValues(name);
            if (values != null) {
                for (String value : values) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append('&');
                    }
                    try {
                        sb.append(name).append("=").append(URLEncoder.encode(value, "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        // this should never happen...
                        sb.append(name).append("=").append(URLEncoder.encode(value));
                    }
                }
            }
        }
    }
    
    /* 
     * As a fix for bug 35088, the preauth handler(servlet) will no longer send the authtoken 
     * it generates over when it needs to redirect to the correct server, it now sends the original 
     * preauth params instead, validating of the preauth will happen on the home server.
     * In this case the token parameter will be null.
     * 
     * Although we no longer pass authtoken, some existing customers might be using it 
     * as a way to inject an authtoken from a URL into a cookie so we might need to leave it.
     * In this case the token parameter will be non-null.
     *  
     */
    private void redirectToCorrectServer(HttpServletRequest req, HttpServletResponse resp, Account acct, String token) throws ServiceException, IOException {
        StringBuilder sb = new StringBuilder();
        Provisioning prov = Provisioning.getInstance();        
        sb.append(URLUtil.getServiceURL(prov.getServer(acct), req.getRequestURI(), true));
        sb.append('?').append(PARAM_ISREDIRECT).append('=').append('1');
        
        if (token != null) {
            sb.append('&').append(PARAM_AUTHTOKEN).append('=').append(token);
            // send only non-preauth (i.e. customer's) params over, since there is already an auth token, the preauth params would be useless anyway
            addQueryParams(req, sb, false, true);  
        } else {
            // send all incoming params over
            addQueryParams(req, sb, false, false); 
        }
        
        resp.sendRedirect(sb.toString());
    }

    private static final String DEFAULT_MAIL_URL = "/zimbra";
    private static final String DEFAULT_ADMIN_URL = "/zimbraAdmin";

    private void setCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken) throws IOException, ServiceException {
        boolean isAdmin = AuthToken.isAnyAdmin(authToken);
        boolean secureCookie = req.getScheme().equals("https");
        authToken.encode(resp, isAdmin, secureCookie);

        String redirectURL = getOptionalParam(req, PARAM_REDIRECT_URL, null);
        if (redirectURL != null) {
            resp.sendRedirect(redirectURL);
        } else {
            StringBuilder sb = new StringBuilder();
            addQueryParams(req, sb, true, true);
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getLocalServer();
            String redirectUrl;

            if (isAdmin) {
                redirectUrl = server.getAttr(Provisioning.A_zimbraAdminURL, DEFAULT_ADMIN_URL);
            } else {
                redirectUrl = server.getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
                // NB: do we really have to add the mail app to the end?
                if (redirectUrl.charAt(redirectUrl.length() - 1) == '/') {
                    redirectUrl += "mail";
                } else {
                    redirectUrl += "/mail";
                }
            }
            if (sb.length() > 0) {
                resp.sendRedirect(redirectUrl + "?" + sb.toString());
            } else {
                resp.sendRedirect(redirectUrl);
            }
        }
    }

    
}
