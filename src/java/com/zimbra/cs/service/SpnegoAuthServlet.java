/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.cs.service;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SpnegoUserRealm;
import org.mortbay.jetty.security.UserRealm;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.service.authenticator.SpnegoAuthenticator;
import com.zimbra.cs.service.authenticator.SSOAuthenticator;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.SSOAuthenticatorServiceException;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.ZimbraPrincipal;

public class SpnegoAuthServlet extends SSOServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        ZimbraLog.addUserAgentToContext(req.getHeader("User-Agent"));
        
        boolean isAdminRequest = false;
        boolean isFromZCO = false;
        
        try {
            isAdminRequest = isOnAdminPort(req);
            isFromZCO = isFromZCO(req);
            
            SpnegoUserRealm userRealm = getSpnegoUserRealm();
            SSOAuthenticator authenticator = new SpnegoAuthenticator(req, resp, userRealm);
            ZimbraPrincipal principal = null;
            
            try {
                principal = authenticator.authenticate();
            } catch (SSOAuthenticatorServiceException e) {
                if (SSOAuthenticatorServiceException.SENT_CHALLENGE.equals(e.getCode())) {
                    return;
                } else {
                    throw e;
                }
            }                
                
            AuthToken authToken = authorize(req, authenticator, principal, isAdminRequest);
                
            if (isFromZCO) {
                setAuthTokenCookieAndReturn(req, resp, authToken);
            } else {
                setAuthTokenCookieAndRedirect(req, resp, principal.getAccount(), authToken);
            }
            
        } catch (ServiceException e) {
            if (e instanceof AuthFailedServiceException) {
                AuthFailedServiceException afe = (AuthFailedServiceException)e;
                ZimbraLog.account.info("failed to authenticate by spnego: " + afe.getMessage() + afe.getReason(", %s"));
            } else {
                ZimbraLog.account.info("failed to authenticate by spnego: " + e.getMessage());
            }
            ZimbraLog.account.debug("failed to authenticate by spnego", e);
            
            if (isFromZCO) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } else {
                try {
                    redirectWithoutAuthTokenCookie(req, resp, isAdminRequest);
                } catch (ServiceException se) {
                    ZimbraLog.account.info("failed to redirect to error page: " + se.getMessage());
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
    
    private SpnegoUserRealm getSpnegoUserRealm() throws ServiceException {
        ServletContext servletContext = getServletContext(); 
        if (servletContext instanceof org.mortbay.jetty.handler.ContextHandler.SContext) {
            org.mortbay.jetty.handler.ContextHandler.SContext sContext = (org.mortbay.jetty.handler.ContextHandler.SContext)servletContext;
            
            // get the WebAppContext
            org.mortbay.jetty.handler.ContextHandler contextHandler = sContext.getContextHandler();
            
            Server server = contextHandler.getServer();
            UserRealm[] userRealms = server.getUserRealms();
            for (UserRealm realm : userRealms) {
                String realmName = realm.getName();
                if (realm instanceof SpnegoUserRealm) {
                    ZimbraLog.account.debug("Found spnego user realm: [" + realmName + "]");
                    return (SpnegoUserRealm)realm;
                }
            }
        }
        
        throw ServiceException.FAILURE("no spnego user realm", null);
    }

}
