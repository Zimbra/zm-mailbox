/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import org.eclipse.jetty.security.SpnegoLoginService;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.service.authenticator.SSOAuthenticator;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.SSOAuthenticatorServiceException;
import com.zimbra.cs.service.authenticator.SpnegoAuthenticator;

public class SpnegoFilter implements Filter {
    private static final String PARAM_PASS_THRU_ON_FAILURE_URI = "passThruOnFailureUri";
    private static final String ERROR_401_PAGE = "error401Page";
    private static String error401Page ;

    private URI passThruOnFailureUri = null;
    private SpnegoLoginService spnegoUserRealm = null;
    
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
        error401Page = filterConfig.getInitParameter(ERROR_401_PAGE);
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
        SSOAuthenticator authenticator = new SpnegoAuthenticator(req, resp, spnegoUserRealm, error401Page);
        authenticator.authenticate();
    }
    
    private SpnegoLoginService getSpnegoUserRealm(FilterConfig filterConfig) {
        // ServletContext servletContext = getServletContext(); 
        ServletContext servletContext = filterConfig.getServletContext();
        if (servletContext instanceof ServletContextHandler.Context) {
        	ServletContextHandler.Context sContext = (ServletContextHandler.Context)servletContext;
            // get the WebAppContext
            ServletContextHandler contextHandler = (ServletContextHandler) sContext.getContextHandler();
            SpnegoLoginService realm = contextHandler.getServer().getBean(SpnegoLoginService.class);
            if (realm != null) {
                ZimbraLog.account.debug("Found spnego user realm: [" + realm.getName() + "]");
            }
            return realm;
        }
        // throw ServiceException.FAILURE("no spnego user realm", null);
        return null;
    }

}
