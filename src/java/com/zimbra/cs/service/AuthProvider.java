/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.servlet.ZimbraServlet;

public abstract class AuthProvider {
    public static final String ZIMBRA_AUTH_PROVIDER = "zimbra";
    
    private static Log sLog = LogFactory.getLog(AuthProvider.class);
  
    // registered/installed providers
    private static Map<String, AuthProvider> sRegisteredProviders = new HashMap<String, AuthProvider>();
    
    // ordered list of enabled providers
    private static List<AuthProvider> sEnabledProviders = null;
    
    static {
        register(new ZimbraAuthProvider());
        // register(new com.zimbra.qa.unittest.TestAccessKeyGrant.DummyAuthProvider());
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
     * Returns an AuthToken by auth data in http request
     * 
     * Should never return null.
     * Throws AuthProviderException.NO_AUTH_TOKEN if auth data for the provider is not present 
     * Throws AuthTokenException if auth data for the provider is present but cannot be resolved into a valid AuthToken
     * 
     * @param req
     * @param isAdmin
     * @return
     * @throws AuthTokenException
     */
    protected abstract AuthToken authToken(HttpServletRequest req, boolean isAdminReq) throws AuthProviderException, AuthTokenException;

    /**
     * Returns an AuthToken by auth data in http request
     * 
     * Should never return null.
     * Throws AuthProviderException.NO_AUTH_TOKEN if auth data for the provider is not present 
     * Throws AuthTokenException if auth data for the provider is present but cannot be resolved into a valid AuthToken
     * 
     * @param soapCtxt
     * @param engineCtxt
     * @param isAdmin
     * @return 
     * @throws AuthTokenException
     */
    protected abstract AuthToken authToken(Element soapCtxt, Map engineCtxt) throws AuthProviderException, AuthTokenException;
    
    /**
     *
     * Returns an AuthToken from an encoded String.
     * 
     * This API is for servlets that support auth from a non-cookie channel, where it honors a String token from 
     * a specific element in the request, which is neither a cookie nor a SOAP context header.  e.g. a query param.
     * 
     * By default, an AuthProvider do not need to implement this method.  The default implementation is throwing
     * AuthProviderException.NOT_SUPPORTED.
     * 
     * Should never return null.
     * Throws AuthProviderException.NO_SUPPORTED if this API is not supported by the auth provider 
     * Throws AuthProviderException.NO_AUTH_TOKEN if auth data for the provider is not present 
     * Throws AuthTokenException if auth data for the provider is present but cannot be resolved into a valid AuthToken
     *
     * @param encoded
     * @return
     * @throws AuthProviderException
     * @throws AuthTokenException
     */
    protected AuthToken authToken(String encoded) throws AuthProviderException, AuthTokenException {
        throw AuthProviderException.NOT_SUPPORTED();
    }
    
    /**
     * Returns an AuthToken from the account object.
     * Should never return null.
     * 
     * @param acct
     * @return
     * @throws AuthProviderException
     */
    protected AuthToken authToken(Account acct) throws AuthProviderException {
        return authToken(acct, false);
    }
    
    /**
     * Returns an AuthToken from the account object.
     * Should never return null.
     * 
     * @param acct
     * @param isAdmin
     * @return
     * @throws AuthProviderException
     */
    protected AuthToken authToken(Account acct, boolean isAdmin) throws AuthProviderException {
        if (acct == null)
            throw AuthProviderException.NOT_SUPPORTED();
        
        long lifetime = isAdmin ?
                acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, AuthToken.DEFAULT_AUTH_LIFETIME * 1000) :                                    
                acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, AuthToken.DEFAULT_AUTH_LIFETIME * 1000);
        return authToken(acct, lifetime);        
    }
    
    /**
     * Returns an AuthToken from the account object with specified lifetime.
     * Should never return null.
     * 
     * @param acct
     * @param expires
     * @return
     * @throws AuthProviderException
     */
    protected AuthToken authToken(Account acct, long expires) throws AuthProviderException {
        throw AuthProviderException.NOT_SUPPORTED();
    }
    
    /**
     * Returns an AuthToken from the account object with specified expiration.
     * Should never return null.
     * 
     * @param acct
     * @param expires
     * @param isAdmin
     * @param adminAcct
     * 
     * @param acct account authtoken will be valid for
     * @param expires when the token expires
     * @param isAdmin true if acct is using its admin privileges
     * @param adminAcct the admin account accessing acct's information, if this token was created by an admin. 
     *        
     * @return
     * @throws AuthProviderException
     */
    protected AuthToken authToken(Account acct, long expires, boolean isAdmin, Account adminAcct) throws AuthProviderException {
        throw AuthProviderException.NOT_SUPPORTED();
    }
    
    /**
     * 
     * @param req
     * @return whether http basic authentication is allowed
     */
    protected boolean allowHttpBasicAuth(HttpServletRequest req, ZimbraServlet servlet) {
        return true;
    }
    
    /**
     * 
     * @param req
     * @return whether accesskey authentication is allowed
     */
    protected boolean allowURLAccessKeyAuth(HttpServletRequest req, ZimbraServlet servlet) {
        return false;
    }

    /**
     * The static getAuthToken methods go through all the providers, trying them in order until one returns an AuthToken
     * Return null when there is no auth data for any of the enabled providers
     * Throw AuthTokenException if any provider in the chain throws an AuthTokenException.
     * 
     * Note: 1. we proceed to try the next provider if the provider throws an AuthProviderException that is ignorable
     * (AuthProviderException.canIgnore()).  for example: AuthProviderException.NO_AUTH_TOKEN, AuthProviderException.NOT_SUPPORTED.
     *
     *       2. in all other cases, we stop processing and throws an AuthTokenException to our caller.
     *             In particular, when a provider:
     *             - returns null -> it should not.  Treat it as a provider error and throws AuthTokenException
     *             - throws AuthTokenException, this means auth data is present for a provider but it cannot be 
     *               resolved into a valid AuthToken.
     *             - Any AuthProviderException that is not ignorable.
     * 
     */
    
    /** 
     * @param req http request
     * @return an AuthToken object, or null if auth data is not present for any of the enabled providers
     * @throws ServiceException
     */
    public static AuthToken getAuthToken(HttpServletRequest req, boolean isAdminReq) throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(req, isAdminReq);
                // sanity check, should not be null, if a provider returns null we throw AuthTokenException here
                if (at == null)
                    throw new AuthTokenException("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore())
                    logger().debug(ap.getName() + ":" + e.getMessage());
                else
                    throw new AuthTokenException("auth provider error", e);
            } catch (AuthTokenException e) {
                // log and rethrow
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
                throw e;
            }
        }
        
        // there is no auth data for any of the enabled providers
        return null;
    }

    /**
     * For SOAP, we do not pass in isAdminReq, because with the current flow in SoapEngine, 
     * at the point when the SOAP context(ZimbraSoapContext) is examined, we haven't looked at the SOAP 
     * body yet.  Whether admin auth is required is based on the SOAP command, which has to be extracted 
     * from the body.  ZimbraAuthProvider always retrieves the encoded auth token from the fixed tag, so 
     * does YahooYT auth.  This should be fine for now.
     *    
     * @param soapCtxt <context> element in SOAP header
     * @param engineCtxt soap engine context
     * @return an AuthToken object, or null if auth data is not present for any of the enabled providers
     * @throws AuthTokenException
     */
    public static AuthToken getAuthToken(Element soapCtxt, Map engineCtxt) throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(soapCtxt, engineCtxt);
                // sanity check, should not be null, if a provider returns null we throw AuthTokenException here
                if (at == null)
                    throw new AuthTokenException("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore())
                    logger().debug(ap.getName() + ":" + e.getMessage());
                else
                    throw new AuthTokenException("auth provider error", e);
            } catch (AuthTokenException e) {
                // log and rethrow
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
                throw e;
            }
        }
        
        // there is no auth data for any of the enabled providers
        return null;
    }
    
    /**
     * 
     * Note: this API for now is only supported by ZimbraAuthProvider.
     *      
     * Callsites of this API:    
     *     1. qp auth in UserServlet(REST)
     *     2. authtoken query param in PreAuthServlet
     *     
     * Also see doc for AuthToken authToken(String encoded)
     * 
     * @param encoded
     * @param isAdminReq
     * @return
     * @throws AuthTokenException
     */
    public static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(encoded);
                // sanity check, should not be null, if a provider returns null we throw AuthTokenException here
                if (at == null)
                    throw new AuthTokenException("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore())
                    logger().warn(ap.getName() + ":" + e.getMessage());
                else
                    throw new AuthTokenException("auth provider error", e);
            } catch (AuthTokenException e) {
                // log and rethrow
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
                throw e;
            }
        }
        
        // there is no auth data for any of the enabled providers
        logger().error("unable to get AuthToken from encoded " + encoded);
        return null;
    }
    
    public static AuthToken getAuthToken(Account acct) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct);
                // sanity check, should not be null, if a provider returns null we throw AuthProviderException here
                if (at == null)
                    throw AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                if (e.canIgnore())
                    logger().debug(ap.getName() + ":" + e.getMessage());
                else
                    throw e;
            } 
        }
        
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acct.getName());
    }
    
    public static AuthToken getAuthToken(Account acct, boolean isAdmin) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, isAdmin);
                // sanity check, should not be null, if a provider returns null we throw AuthTokenException here
                if (at == null)
                    throw AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                if (e.canIgnore())
                    logger().debug(ap.getName() + ":" + e.getMessage());
                else
                    throw e;
            } 
        }

        String acctName = acct != null ? acct.getName() : "null";
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acctName);
    }
    
    public static AuthToken getAdminAuthToken() throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.adminName, LC.zimbra_ldap_user.value());
        return AuthProvider.getAuthToken(acct, true);
    }
    
    public static AuthToken getAuthToken(Account acct, long expires) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, expires);
                // sanity check, should not be null, if a provider returns null we throw AuthTokenException here
                if (at == null)
                    throw AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                if (e.canIgnore())
                    logger().debug(ap.getName() + ":" + e.getMessage());
                else
                    throw e;
            } 
        }
        
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acct.getName());
    }
    
    public static AuthToken getAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, expires, isAdmin, adminAcct);
                // sanity check, should not be null, if a provider returns null we throw AuthTokenException here
                if (at == null)
                    throw AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                else
                    return at;
            } catch (AuthProviderException e) {
                if (e.canIgnore())
                    logger().debug(ap.getName() + ":" + e.getMessage());
                else
                    throw e;
            } 
        }
        
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acct.getName());
    }
    
    public static boolean allowBasicAuth(HttpServletRequest req, ZimbraServlet servlet) {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            if (ap.allowHttpBasicAuth(req, servlet))
                return true;
        }
        return false;
    }
    
    public static boolean allowAccessKeyAuth(HttpServletRequest req, ZimbraServlet servlet) {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            if (ap.allowURLAccessKeyAuth(req, servlet))
                return true;
        }
        return false;
    }
    
    public static Account validateAuthToken(Provisioning prov, AuthToken at, boolean addToLoggingContext) throws ServiceException {
        if (prov == null)
            prov = Provisioning.getInstance();
        
        if (at.isExpired())
            throw ServiceException.AUTH_EXPIRED();
        // make sure that the authenticated account is active and has not been deleted/disabled since the last request
        Account acct = prov.get(AccountBy.id, at.getAccountId(), at);
        
        if (addToLoggingContext && acct != null) {
            ZimbraLog.addAccountNameToContext(acct.getName());
        }
        
        /*
         * should we throw more specific exception?  Before this consolidation,
         * FileUploadServlet did, but other places didn't.
         
           if (acct == null)
               throw AccountServiceException.NO_SUCH_ACCOUNT(at.getAccountId());
               
           if (acct.getAuthTokenValidityValue() != at.getValidityValue())
               throw AccountServiceException.AUTH_EXPIRED()     
                     
           if (acctStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE))
               throw AccountServiceException.MAINTENANCE_MODE();
           else if (!acctStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
               throw AccountServiceException.ACCOUNT_INACTIVE(acct.getName());
         */
        if (acct == null || 
            !acct.getAccountStatus(prov).equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
            !acct.checkAuthTokenValidityValue(at))
            throw ServiceException.AUTH_EXPIRED();
        
        return acct;
    }
    
    
    public static void main(String[] args) throws Exception {
        AuthToken at = getAuthToken(null, null);
    }

}