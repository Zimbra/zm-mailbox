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
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.ZimbraPrincipal;

public class SpnegoAuthServlet extends SSOServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        addUAToLoggingContext(req);
        
        boolean isAdminRequest = false;
        boolean isFromZCO = false;
        
        try {
            isAdminRequest = isOnAdminPort(req);
            isFromZCO = isFromZCO(req);
            
            Principal principal = req.getUserPrincipal();
            
            if (principal == null) {
                throw AuthFailedServiceException.AUTH_FAILED("no principal");
            }
            
            if (!(principal instanceof ZimbraPrincipal)) {
                throw AuthFailedServiceException.AUTH_FAILED(principal.getName(), "not ZimbraPrincipal", (Throwable)null);
            }
            
            ZimbraPrincipal zimbraPrincipal = (ZimbraPrincipal)principal;   
            AuthToken authToken = authorize(req, AuthContext.Protocol.spnego, zimbraPrincipal, isAdminRequest);
                
            if (isFromZCO) {
                setAuthTokenCookieAndReturn(req, resp, authToken);
            } else {
                setAuthTokenCookieAndRedirect(req, resp, zimbraPrincipal.getAccount(), authToken);
            }
            
        } catch (ServiceException e) {
            if (e instanceof AuthFailedServiceException) {
                AuthFailedServiceException afe = (AuthFailedServiceException)e;
                ZimbraLog.account.info("spnego auth failed: " + afe.getMessage() + afe.getReason(", %s"));
            } else {
                ZimbraLog.account.info("spnego auth failed: " + e.getMessage());
            }
            ZimbraLog.account.debug("spnego auth failed", e);
            
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
}
