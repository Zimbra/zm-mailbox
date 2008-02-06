/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.SoapServlet;

public abstract class AuthProvider {
    protected static final String ZIMBRA_AUTH_PROVIDER = "zimbra";
    
    private static Log sLog = LogFactory.getLog(AuthProvider.class);
  
    // registered/installed providers
    private static Map<String, AuthProvider> sRegisteredProviders = new HashMap<String, AuthProvider>();
    
    // ordered list of enabled providers
    private static List<AuthProvider> sEnabledProviders = null;
    
    static {
        register(new ZimbraAuthProvider());
        refresh();
    }
    
    public synchronized static void register(AuthProvider ap) {
        String name = ap.getName();
        logger().info("Adding auth provider: " + name + " " + ap.getClass().getName());
            
        if (sRegisteredProviders.get(name) == null)
            sRegisteredProviders.put(name, ap);
        else
            logger().error("auth provider " + name + " already exists, not adding " + ap.getClass().getName());
    }

    /**
     * refresh the enabled cache
     * 
     * TODO, can be called from zmprov flushCache to flush the enabled cache
     */
    public static void refresh() {
        List<AuthProvider> providerList = new ArrayList<AuthProvider>();
        String[] providers = LC.zimbra_auth_provider.value().split(",");
        for (String p : providers) {
            AuthProvider ap = sRegisteredProviders.get(p);
            if (ap != null)
                providerList.add(ap);
        }
        
        // always add the zimbra provider if there is no provider configured. 
        if (providerList.size() == 0)
            providerList.add(sRegisteredProviders.get(ZIMBRA_AUTH_PROVIDER));

        setProviders(providerList);
    }
    
    private static synchronized void setProviders(List<AuthProvider> providers) {
        sEnabledProviders = providers;
    }
    
    private static synchronized List<AuthProvider> getProviders() {
        return sEnabledProviders;
    }
    
    private String mName;
    
    protected AuthProvider(String name) {
        mName = name;
    }
    
    private String getName() {
        return mName;
    }
    
    protected static Log logger() {
        return sLog;
    }
    
    
    /**
     * invoked by implementations to determine if the request requires admin auth
     * 
     * @param req
     * @return
     * @throws ServiceException
     */
    /*
    protected boolean isAdminRequest(HttpServletRequest req) throws ServiceException  {
        int adminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, -1);
        return (req.getLocalPort() == adminPort);
    }
    */
    
    /**
     * invoked by implementations to determine if the request requires admin auth
     * 
     * @param engineCtxt
     * @return
     * @throws ServiceException
     */
    /*
    protected boolean isAdminRequest(Map engineCtxt) throws ServiceException  {
        return isAdminRequest((HttpServletRequest)engineCtxt.get(SoapServlet.SERVLET_REQUEST));
    }
    */
    
    /**
     * Returns an AuthToken based on http request
     * 
     * @param req
     * @param isAdmin
     * @return
     * @throws AuthTokenException
     */
    protected abstract AuthToken authToken(HttpServletRequest req, boolean isAdmin) throws AuthTokenException;

    /**
     * Returns an AuthToken based on SOAP conext header
     * 
     * @param soapCtxt
     * @param engineCtxt
     * @param isAdmin
     * @return
     * @throws AuthTokenException
     */
    protected abstract AuthToken authToken(Element soapCtxt, Map engineCtxt) throws AuthTokenException;
    
    
    /**
     * goes through all the providers, trying them in order until one returns a non-null auth token.
     * could return null if no provider can construct an AuthToken from req.
     * 
     * @param req http request
     * @return
     * @throws ServiceException
     */
    public static AuthToken getAuthToken(HttpServletRequest req, boolean isAdmin) throws ServiceException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(req, isAdmin);
            } catch (AuthTokenException e) {
                // log and continue with next provider
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
            }
            if (at != null)
                return at;
        }
        return null;
    }

    /**
     * goes through all the providers, trying them in order until one returns a non-null auth token.
     * could return null if no provider can construct an AuthToken from soapCtxt/engineCtxt
     * 
     * AP-TODO-2:
     * For SOAP, we currently do not pass in isAdmin, because with the current flow in SoapEngine, 
     * at the point when the SOAP context(ZimbraSoapContext) is examined, we haven't looked at the SOAP 
     * body yet.  Whether admin auth is required is based on the SOAP command, which has to be extracted 
     * from the body.  Zimbra auth provider always retrieves the raw auth token from the fixed tag, so 
     * does YahooYT auth.  This should be fine for now.
     *    
     * @param soapCtxt <context> element in SOAP header
     * @param engineCtxt soap engine context
     * @return
     * @throws ServiceException
     */
    public static AuthToken getAuthToken(Element soapCtxt, Map engineCtxt) throws ServiceException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(soapCtxt, engineCtxt);
            } catch (AuthTokenException e) {
                // log and continue with next provider
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
            }
            if (at != null)
                return at;
        }
        return null;
    }
    
    public static void main(String[] args) throws Exception {
        AuthToken at = getAuthToken(null, null);
    }

}