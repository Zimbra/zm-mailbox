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

package com.zimbra.cs.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SpnegoUserRealm;
import org.mortbay.jetty.security.UserRealm;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.service.authenticator.SSOAuthenticator;
import com.zimbra.cs.service.authenticator.SpnegoAuthenticator;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.SSOAuthenticatorServiceException;

public class SpnegoFilter implements Filter {
    private static final String PARAM_PASS_THRU_ON_FAILURE_URI = "passThruOnFailureUri";
    
    private URI passThruOnFailureUri = null;
    private SpnegoUserRealm spnegoUserRealm = null;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String uri = filterConfig.getInitParameter(PARAM_PASS_THRU_ON_FAILURE_URI);
        if (uri != null) {
            try {
                passThruOnFailureUri = new URI(uri);
            } catch (URISyntaxException e) {
                throw new ServletException("Malformed URI: " + uri, e);
            }
            
        }
        spnegoUserRealm = getSpnegoUserRealm(filterConfig);
    }
    
    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest hreq = (HttpServletRequest) req;
        HttpServletResponse hresp = (HttpServletResponse) resp;
        
        try {
            try {
                authenticate(hreq, hresp);
            } catch (SSOAuthenticatorServiceException e) {
                if (SSOAuthenticatorServiceException.SENT_CHALLENGE.equals(e.getCode())) {
                    return;
                } else {
                    throw e;
                }
            }  
            chain.doFilter(req, resp);
        } catch (ServiceException e) {
            ZimbraServlet.addRemoteIpToLoggingContext(hreq);
            ZimbraServlet.addUAToLoggingContext(hreq);
            if (e instanceof AuthFailedServiceException) {
                AuthFailedServiceException afe = (AuthFailedServiceException)e;
                ZimbraLog.account.info("spnego auth failed: " + afe.getMessage() + afe.getReason(", %s"));
            } else {
                ZimbraLog.account.info("spnego auth failed: " + e.getMessage());
            }
            ZimbraLog.account.debug("spnego auth failed", e);
            ZimbraLog.clearContext();
            
            if (passThruOnAuthFailure(hreq)) {
                chain.doFilter(req, resp);
            } else {    
                hresp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            }
        }
        
    }
    
    private boolean passThruOnAuthFailure(HttpServletRequest hreq) {
        if (passThruOnFailureUri != null) {
            try {
                URI reqUri = new URI(hreq.getRequestURI());
                return passThruOnFailureUri.equals(reqUri);
            } catch (URISyntaxException e) {
            }
        }
        return false;
    }

    private void authenticate(HttpServletRequest req, HttpServletResponse resp) 
    throws ServiceException {
        if (spnegoUserRealm == null) {
            throw ServiceException.FAILURE("no spnego user realm", null);
        }
        SSOAuthenticator authenticator = new SpnegoAuthenticator(req, resp, spnegoUserRealm);
        authenticator.authenticate();
    }
    
    private SpnegoUserRealm getSpnegoUserRealm(FilterConfig filterConfig) {
        // ServletContext servletContext = getServletContext(); 
        ServletContext servletContext = filterConfig.getServletContext();
        if (servletContext instanceof org.mortbay.jetty.handler.ContextHandler.SContext) {
            org.mortbay.jetty.handler.ContextHandler.SContext sContext = (org.mortbay.jetty.handler.ContextHandler.SContext)servletContext;
            
            // get the WebAppContext
            org.mortbay.jetty.handler.ContextHandler contextHandler = sContext.getContextHandler();
            
            Server server = contextHandler.getServer();
            UserRealm[] userRealms = server.getUserRealms();
            if (userRealms != null) {
                for (UserRealm realm : userRealms) {
                    String realmName = realm.getName();
                    if (realm instanceof SpnegoUserRealm) {
                        ZimbraLog.account.debug("Found spnego user realm: [" + realmName + "]");
                        return (SpnegoUserRealm)realm;
                    }
                }
            }
        }
        
        // throw ServiceException.FAILURE("no spnego user realm", null);
        return null;
    }

}
