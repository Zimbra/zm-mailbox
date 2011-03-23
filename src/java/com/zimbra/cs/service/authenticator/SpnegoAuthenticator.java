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

package com.zimbra.cs.service.authenticator;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.SpnegoUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.security.SpnegoUserRealm.SpnegoUser;
import org.mortbay.log.Log;
import org.mortbay.util.URIUtil;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning.AccountBy;

public class SpnegoAuthenticator extends SSOAuthenticator {

    private SpnegoUserRealm spnegoUserRealm;
    private String _errorPage; // TODO: initialize
    private String _errorPath; // TODO: initialize
    
    public SpnegoAuthenticator(HttpServletRequest req, HttpServletResponse resp, SpnegoUserRealm spnegoUserRealm) {
        super(req, resp);
        this.spnegoUserRealm = spnegoUserRealm;
    }
    
    @Override
    public String getAuthMethod() {
        return "Spnego";
    }
    
    @Override
    public ZimbraPrincipal authenticate() throws ServiceException {
        Principal principal = getPrincipal();
        Account acct = getAccountByPrincipal(principal);
        return new ZimbraPrincipal(principal.getName(), acct);
    }
    
    private Principal getPrincipal() throws ServiceException {
        Request request = (req instanceof Request) ? (Request)req : null;
                    
        if (request == null) {
            throw ServiceException.FAILURE("not supported", null);
        }
        
        String errorPage = Provisioning.getInstance().getConfig().getSpnegoAuthErrorURL();
        Principal principal = null;
        
        try {
            principal = authenticate(spnegoUserRealm, errorPage, request, resp);
        } catch (IOException e) {
            throw AuthFailedServiceException.AUTH_FAILED("spnego authenticate failed", e); 
        }
        
        if (principal == null) {
            throw AuthFailedServiceException.AUTH_FAILED("spnego authenticate failed", (Throwable)null); 
        }
        
        return principal;
    }
    
    
    private Account getAccountByPrincipal(Principal principal) throws ServiceException {
        
        String krb5Name = principal.getName();
        
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.krb5Principal, krb5Name);
        return acct;
    }
    
    /* =========================================================
     * 
     * Based on org.mortbay.jetty.security.SpnegoAuthenticator
     * 
     * =========================================================
     */
    private Principal authenticate(UserRealm realm, String pathInContext, Request request, HttpServletResponse response) 
    throws ServiceException, IOException
    {   
        /*
         * if the request is for the error page, return nobody and it should present
         */
        if ( isErrorPage(pathInContext) )
        {
            return SecurityHandler.__NOBODY;
        }
        
        Principal user = null;
        
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        /*
         * if the header is null then we need to challenge...this is after the error page check
         */
        if (header == null)
        {
            sendChallenge(realm,request,response);
            throw SSOAuthenticatorServiceException.SENT_CHALLENGE();
        } 
        else if (header != null && header.startsWith(HttpHeaders.NEGOTIATE)) 
        {           
            /*
             * we have gotten a negotiate header to try and authenticate
             */
            
            String username = header.substring(10);
            
            user = realm.authenticate(username, null, request);
            
            if (user != null)
            {
                Log.debug("SpengoAuthenticator: obtained principal: " + user.getName());

                // request.setAuthType(Constraint.__SPNEGO_AUTH);
                // request.setUserPrincipal(user);
                
                response.addHeader(HttpHeaders.WWW_AUTHENTICATE, HttpHeaders.NEGOTIATE + " " + ((SpnegoUser)user).getToken());
                
                return user;
            }
            else
            {
                /*
                 * no user was returned from the authentication which means something failed
                 * so process error logic
                 */
                if(Log.isDebugEnabled())
                {
                    Log.debug("SpengoAuthenticator: no user found, authentication failed");
                }
                
                if (_errorPage==null)
                {
                    if (response != null)
                    {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }
                }
                else
                {
                    if (response != null)
                    {
                        response.setContentLength(0);
                        
                        response.sendRedirect(response.encodeRedirectURL
                                          (URIUtil.addPaths(request.getContextPath(),
                                                        _errorPage)));                  
                    }
                }
                
                return null;
            }
        }
        /*
         * the header was not null, but we didn't get a negotiate so process error logic
         */
        else
        {
            if(Log.isDebugEnabled())
            {
                Log.debug("SpengoAuthenticator: authentication failed, unknown header (browser is likely misconfigured for SPNEGO)");
            }
            
            if (_errorPage==null)
            {
                if (response != null)
                {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            }
            else
            {
                if (response != null)
                {
                    response.setContentLength(0);
                    
                    response.sendRedirect(response.encodeRedirectURL
                                      (URIUtil.addPaths(request.getContextPath(),
                                                    _errorPage)));
                }
            }
                     
            return null;
        }        
    }
    
    public void sendChallenge(UserRealm realm, Request request, HttpServletResponse response) throws IOException
    {
        Log.debug("SpengoAuthenticator: sending challenge");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, HttpHeaders.NEGOTIATE);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
    
    public boolean isErrorPage(String pathInContext)
    {
        return pathInContext!=null && (pathInContext.equals(_errorPath));
    }
}
