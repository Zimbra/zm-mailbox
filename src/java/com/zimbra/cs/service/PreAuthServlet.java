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
 * Created on Dec 20, 2004
 * @author Greg Solovyev
 * */
package com.zimbra.cs.service;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.account.Auth;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ZimbraLog;

public class PreAuthServlet extends ZimbraServlet {

    public static final String PARAM_PREAUTH = "preauth";
    public static final String PARAM_AUTHTOKEN = "authtoken";
    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_ISREDIRECT = "isredirect";
    public static final String PARAM_BY = "by";
    public static final String PARAM_TIMESTAMP = "timestamp";
    public static final String PARAM_EXPIRES = "expires";    
    
    private static final String DEFAULT_MAIL_URL = "/zimbra/mail";
    
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

    private String getOptionalParam(HttpServletRequest req, String paramName, String def) throws ServiceException {
        String param = req.getParameter(paramName);
        if (param == null) return def;
        else return param;
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        try {
            String isRedirect = getOptionalParam(req, PARAM_ISREDIRECT, "0");
            if (isRedirect.equals("1")) {
                String authToken = getRequiredParam(req, resp, PARAM_AUTHTOKEN);
                setCookieAndRedirect(resp, authToken);
                return;
            } else {
                String preAuth = getRequiredParam(req, resp, PARAM_PREAUTH);            
                String account = getRequiredParam(req, resp, PARAM_ACCOUNT);
                String accountBy = getOptionalParam(req, PARAM_BY, Auth.BY_NAME);
                long timestamp = Long.parseLong(getRequiredParam(req, resp, PARAM_TIMESTAMP));
                long expires = Long.parseLong(getRequiredParam(req, resp, PARAM_EXPIRES));
            
                Provisioning prov = Provisioning.getInstance();
            
                Account acct = null;
                if (accountBy.equals(Auth.BY_NAME)) {
                    acct = prov.getAccountByName(account);            
                } else if (accountBy.equals(Auth.BY_ID)) {
                    acct = prov.getAccountById(account);
                } else if (accountBy.equals(Auth.BY_FOREIGN_PRINCIPAL)) {
                    acct = prov.getAccountByForeignPrincipal(account);
                }
            
                if (acct == null)
                    throw AccountServiceException.AUTH_FAILED(account);
                
                prov.preAuthAccount(acct, account, accountBy, timestamp, expires, preAuth);
            
                AuthToken at =  expires ==  0 ? new AuthToken(acct) : new AuthToken(acct, expires);
                try {
                    String authToken = at.getEncoded();
                    
                    if (acct.isCorrectHost()) {
                        setCookieAndRedirect(resp, at.getEncoded());
                    } else {
                        redirectToCorrectServer(req, resp, acct, at.getEncoded());
                    }
                } catch (AuthTokenException e) {
                    throw  ServiceException.FAILURE("unable to encode auth token", e);
                }
            }
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
    }

    private void redirectToCorrectServer(HttpServletRequest req, HttpServletResponse resp, Account acct, String token) throws ServiceException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(URLUtil.getMailURL(acct.getServer(), req.getRequestURI(), true));
        sb.append('?').append(PARAM_ISREDIRECT).append('=').append('1');
        sb.append('&').append(PARAM_AUTHTOKEN).append('=').append(token);
        resp.sendRedirect(sb.toString());
    }

    private void setCookieAndRedirect(HttpServletResponse resp, String authToken) throws IOException {
        Cookie c = new Cookie(COOKIE_ZM_AUTH_TOKEN, authToken);
        c.setPath("/");
        resp.addCookie(c);
        resp.sendRedirect(DEFAULT_MAIL_URL);
    }

}
