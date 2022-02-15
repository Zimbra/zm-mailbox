/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.service.admin.AdminAccessControl;
import com.zimbra.cs.servlet.ZimbraServlet;

public abstract class AuthProvider {

    private static Log sLog = LogFactory.getLog(AuthProvider.class);

    // registered/installed providers
    private static Map<String, AuthProvider> registeredProviders = new HashMap<String, AuthProvider>();

    // ordered list of enabled providers
    private static List<AuthProvider> enabledProviders = null;

    static {
        register(new ZimbraAuthProvider());
        register(new ZimbraAuthProviderForOAuth());
        refresh();
    }

    public synchronized static void register(AuthProvider ap) {
        String name = ap.getName();
        logger().info("Adding auth provider: " + name + " " + ap.getClass().getName());

        if (registeredProviders.get(name) == null) {
            registeredProviders.put(name, ap);
        } else {
            logger().error("auth provider " + name + " already exists, not adding " +
                    ap.getClass().getName());
        }
    }

    /**
     * refresh the enabled cache
     *
     * TODO, can be called from zmprov flushCache to flush the enabled cache
     */
    public static void refresh() {
        List<AuthProvider> providerList = new ArrayList<AuthProvider>();
        String[] providers = LC.zimbra_auth_provider.value().split(",");
        for (String provider : providers) {

            provider = provider.trim();
            if (!Strings.isNullOrEmpty(provider)) {
                AuthProvider ap = registeredProviders.get(provider);
                if (ap != null) {
                    providerList.add(ap);
                }
            }
        }

        // always add the zimbra providers if there is no provider configured.
        if (providerList.size() == 0) {
            providerList.add(registeredProviders.get(ZimbraAuthProvider.ZIMBRA_AUTH_PROVIDER));
            // providerList.add(registeredProviders.get(ZimbraOAuthProvider.ZIMBRA_OAUTH_PROVIDER));
        }

        setProviders(providerList);
    }

    private static synchronized void setProviders(List<AuthProvider> providers) {
        enabledProviders = providers;
    }

    private static synchronized List<AuthProvider> getProviders() {
        return enabledProviders;
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
     * Throws AuthTokenException if auth data for the provider is present but cannot be
     * resolved into a valid AuthToken
     *
     * @param req
     * @param isAdmin
     * @return
     * @throws AuthTokenException
     */
    protected abstract AuthToken authToken(HttpServletRequest req, boolean isAdminReq)
    throws AuthProviderException, AuthTokenException;

    /**
     * Returns an AuthToken by auth data in http request
     *
     * Should never return null.
     * Throws AuthProviderException.NO_AUTH_TOKEN if auth data for the provider is not present
     * Throws AuthTokenException if auth data for the provider is present but cannot be
     * resolved into a valid AuthToken
     *
     * @param soapCtxt
     * @param engineCtxt
     * @param isAdmin
     * @return
     * @throws AuthTokenException
     */
    protected abstract AuthToken authToken(Element soapCtxt, Map engineCtxt)
    throws AuthProviderException, AuthTokenException;

    protected AuthToken jwToken(Element soapCtxt, Map engineCtxt)
    throws AuthProviderException, AuthTokenException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    protected AuthToken jwToken(String encoded, String currentSalt)
    throws AuthProviderException, AuthTokenException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    /**
     * Returns an AuthToken by auth data in the <authToken> element.
     *
     * @param authTokenElem
     * @param acct  TODO: This may not be needed if we can get account info from the
     *              OAuth access token.
     * @return
     * @throws AuthProviderException
     * @throws AuthTokenException
     */
    protected AuthToken authToken(Element authTokenElem, Account acct)
    throws AuthProviderException, AuthTokenException {
        // default implementation just extracts the text and calls the authToken(String)
        // the acct parameter is ignored.
        String token = authTokenElem.getText();
        return authToken(token);
    }

    /**
     *
     * Returns an AuthToken from an encoded String.
     *
     * This API is for servlets that support auth from a non-cookie channel, where it
     * honors a String token from a specific element in the request, which is neither
     * a cookie nor a SOAP context header.  e.g. a query param.
     *
     * By default, an AuthProvider do not need to implement this method.
     * The default implementation is throwing AuthProviderException.NOT_SUPPORTED.
     *
     * Should never return null.
     * Throws AuthProviderException.NO_SUPPORTED if this API is not supported by the auth provider
     * Throws AuthProviderException.NO_AUTH_TOKEN if auth data for the provider is not present
     * Throws AuthTokenException if auth data for the provider is present but cannot be
     * resolved into a valid AuthToken
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
        return authToken(acct, false, null);
    }

    /**
     * Returns an AuthToken from the account object of type tokenType
     * Should never return null.
     *
     * Throws AuthProviderException.NO_SUPPORTED if this API is not supported by the auth provider
     * @param acct
     * @return
     * @throws AuthProviderException
     */
    protected AuthToken authToken(Account acct, TokenType tokenType) throws AuthProviderException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    /**
     * Returns an AuthToken from the account object.
     * Should never return null.
     *
     * @param acct
     * @param isAdmin
     * @param authMech
     * @return
     * @throws AuthProviderException
     */
    protected AuthToken authToken(Account acct, boolean isAdmin, AuthMech authMech)
    throws AuthProviderException {
        if (acct == null) {
            throw AuthProviderException.NOT_SUPPORTED();
        }

        long lifetime = isAdmin ?
                acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime,
                        AuthToken.DEFAULT_AUTH_LIFETIME * 1000) :
                acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime,
                        AuthToken.DEFAULT_AUTH_LIFETIME * 1000);
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

    protected AuthToken authToken(Account acct, long expires, TokenType tokenType) throws AuthProviderException {
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
    protected AuthToken authToken(Account acct, long expires, boolean isAdmin, Account adminAcct)
    throws AuthProviderException {
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
     * The static getAuthToken methods go through all the providers, trying them in order
     * until one returns an AuthToken.
     *
     * If any provider in the chain throws AuthTokenException,
     * it will be stored and re-thrown to caller at the end.
     *
     * If more than one provider throws AuthTokenException then exception reported
     * by last provider will be thrown to caller.
     *
     * If AuthProviderException is thrown by provider then-
     *    - For AuthProviderException that is ignorable(AuthProviderException.NO_AUTH_TOKEN, AuthProviderException.NOT_SUPPORTED),
     *      it will be logged and next provider will be tried.
     *    - For AuthProviderExceptions that is not ignorable, AuthTokenException is generated and stored,
     *      thrown at the end if all provider fails.
     *
     * Return null when all providers fails to get AuthToken and no exception thrown by any provider.
     */

    /**
     * @param req http request
     * @return an AuthToken object, or null if auth data is not present for any of the enabled providers
     * @throws ServiceException
     */
    public static AuthToken getAuthToken(HttpServletRequest req, boolean isAdminReq)
    throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();

        AuthTokenException authTokenExp = null;

        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(req, isAdminReq);
                if (at == null) {
                    authTokenExp = new AuthTokenException("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authTokenExp = new AuthTokenException("auth provider error", e);
                }
            } catch (AuthTokenException e) {
                //log and store exception reference
                authTokenExp = e;
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
            }
        }

        //If AuthTokenException has occurred while traversing Auth providers then it should be thrown.
        //If multiple auth providers caused AuthTokenException, then last exception is rethrown from here.
        if (null != authTokenExp) {
            throw authTokenExp;
        }

        // there is no auth data for any of the enabled providers
        return null;
    }

    /**
     * For SOAP, we do not pass in isAdminReq, because with the current flow in SoapEngine,
     * at the point when the SOAP context(ZimbraSoapContext) is examined, we haven't looked
     * at the SOAP body yet.  Whether admin auth is required is based on the SOAP command,
     * which has to be extracted from the body.  ZimbraAuthProvider always retrieves the
     * encoded auth token from the fixed tag, so does YahooYT auth.
     * This should be fine for now.
     * If any provider in the chain throws AuthTokenException,
     * it will be thrown at the end.
     * If more than one provider throws AuthTokenException then exception reported
     * by last provider will be thrown to caller.
     *
     * @param soapCtxt <context> element in SOAP header
     * @param engineCtxt soap engine context
     * @return an AuthToken object, or null if auth data is not present for any of the enabled providers
     * @throws AuthTokenException
     */
    public static AuthToken getAuthToken(Element soapCtxt, Map engineCtxt)
    throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();

        AuthTokenException authTokenExp = null;

        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(soapCtxt, engineCtxt);
                if (at == null) {
                    authTokenExp = new AuthTokenException("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authTokenExp = new AuthTokenException("auth provider error", e);
                }
            } catch (AuthTokenException e) {
                //log and store exception reference
                authTokenExp = e;
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
            }
        }

        //If AuthTokenException has occurred while traversing Auth providers then it should be thrown.
        //If multiple auth providers caused AuthTokenException, then last exception is rethrown from here.
        if (null != authTokenExp) {
            throw authTokenExp;
        }

        // there is no auth data for any of the enabled providers
        return null;
    }

    public static AuthToken getJWToken(Element soapCtxt, Map engineCtxt)
    throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();

        AuthTokenException authTokenExp = null;

        for (AuthProvider ap : providers) {
            try {
                at = ap.jwToken(soapCtxt, engineCtxt);
                if (at == null) {
                    authTokenExp = new AuthTokenException(String.format("auth provider %s returned null", ap.getName()));
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore()) {
                    logger().debug("auth provider failure %s : %s", ap.getName(), e.getMessage());
                } else {
                    authTokenExp = new AuthTokenException("auth provider error", e);
                }
            } catch (AuthTokenException e) {
                //log and store exception reference
                authTokenExp = e;
                logger().debug("getJWToken error: provider= %s, err= %s", ap.getName(), e.getMessage(), e);
            }
        }

        //If AuthTokenException has occurred while traversing Auth providers then it should be thrown.
        //If multiple auth providers caused AuthTokenException, then last exception is rethrown from here.
        if (null != authTokenExp) {
            throw authTokenExp;
        }

        // there is no auth data for any of the enabled providers
        return null;
    }

    public static AuthToken getAuthToken(Element authTokenElem, Account acct)
    throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        AuthTokenException authTokenExp = null;

        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(authTokenElem, acct);
                if (at == null) {
                    authTokenExp = new AuthTokenException("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authTokenExp = new AuthTokenException("auth provider error", e);
                }
            } catch (AuthTokenException e) {
                //log and store exception reference
                authTokenExp = e;
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
            }
        }

        //If AuthTokenException has occurred while traversing Auth providers then it should be thrown.
        //If multiple auth providers caused AuthTokenException, then last exception is rethrown from here.
        if (null != authTokenExp) {
            throw authTokenExp;
        }

        // there is no auth data for any of the enabled providers
        return null;
    }

    /**
     * Creates an AuthToken object from token string.
     *
     * @param encoded
     * @return
     * @throws AuthTokenException
     * @see #authToken(String)
     */
    public static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();
        AuthTokenException authTokenExp = null;

        for (AuthProvider ap : providers) {
            try {
                at = ap.authToken(encoded);
                if (at == null) {
                    authTokenExp = new AuthTokenException("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore()) {
                    logger().warn(ap.getName() + ":" + e.getMessage());
                } else {
                    authTokenExp = new AuthTokenException("auth provider error", e);
                }
            } catch (AuthTokenException e) {
                //log and store exception reference
                authTokenExp = e;
                logger().debug("getAuthToken error: provider=" + ap.getName() + ", err=" + e.getMessage(), e);
            }
        }

        //If AuthTokenException has occurred while traversing Auth providers then it should be thrown.
        //If multiple auth providers caused AuthTokenException, then last exception is rethrown from here.
        if (null != authTokenExp) {
            throw authTokenExp;
        }

        // there is no auth data for any of the enabled providers
        logger().error("unable to get AuthToken from encoded " + encoded);
        return null;
    }

    public static AuthToken getJWToken(String encoded, String currentSalt) throws AuthTokenException {
        AuthToken at = null;
        List<AuthProvider> providers = getProviders();

        AuthTokenException authTokenExp = null;

        for (AuthProvider ap : providers) {
            try {
                at = ap.jwToken(encoded, currentSalt);
                if (at == null) {
                    authTokenExp = new AuthTokenException(String.format("auth provider %s returned null", ap.getName()));
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                // if there is no auth data for this provider, log and continue with next provider
                if (e.canIgnore()) {
                    logger().debug("auth provider failure %s : %s", ap.getName(), e.getMessage());
                } else {
                    authTokenExp = new AuthTokenException("auth provider error", e);
                }
            } catch (AuthTokenException e) {
                //log and store exception reference
                authTokenExp = e;
                logger().debug("getJWToken error: provider= %s, err= %s", ap.getName(), e.getMessage(), e);
            }
        }

        //If AuthTokenException has occurred while traversing Auth providers then it should be thrown.
        //If multiple auth providers caused AuthTokenException, then last exception is rethrown from here.
        if (null != authTokenExp) {
            throw authTokenExp;
        }

        // there is no auth data for any of the enabled providers
        return null;
    }

    public static AuthToken getAuthToken(Account acct) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }
        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acct.getName());
    }

    public static AuthToken getAuthToken(Account acct, TokenType tokenType) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, tokenType);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE(String.format("auth provider %s returned null", ap.getName()));
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug("auth provider failure %s : %s", ap.getName(), e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }
        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE(String.format("cannot get authtoken from account ", acct.getName()));
    }

    public static AuthToken getAuthToken(Account acct, boolean isAdmin)
    throws AuthProviderException {
        return getAuthToken(acct, isAdmin, null);
    }

    public static AuthToken getAuthToken(Account acct, boolean isAdmin, AuthMech authMech)
    throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, isAdmin, authMech);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }

        String acctName = acct != null ? acct.getName() : "null";
        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acctName);
    }

    public static AuthToken getAdminAuthToken() throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.adminName, LC.zimbra_ldap_user.value());
        return AuthProvider.getAuthToken(acct, true);
    }

    public static AuthToken getAuthToken(Account acct, long expires) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, expires);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }
        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acct.getName());
    }

    public static AuthToken getAuthToken(Account acct, long expires, TokenType tokenType) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, expires, tokenType);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE(String.format("auth provider %s returned null", ap.getName()));
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug("auth provider failure %s : %s", ap.getName(), e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }
        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE(String.format("cannot get authtoken from account ", acct.getName()));
    }

    public static AuthToken getAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct)
    throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(acct, expires, isAdmin, adminAcct);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }

        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + acct.getName());
    }

    public static AuthToken getAuthToken(Account account, Usage usage) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(account, usage);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE("auth provider " + ap.getName() + " returned null");
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug(ap.getName() + ":" + e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }

        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE("cannot get authtoken from account " + account.getName());
    }

    public static AuthToken getAuthToken(Account account, Usage usage, TokenType tokenType) throws AuthProviderException {
        List<AuthProvider> providers = getProviders();
        AuthProviderException authProviderExp = null;

        for (AuthProvider ap : providers) {
            try {
                AuthToken at = ap.authToken(account, usage, tokenType);
                if (at == null) {
                    authProviderExp = AuthProviderException.FAILURE(String.format("auth provider %s returned null", ap.getName()));
                } else {
                    return at;
                }
            } catch (AuthProviderException e) {
                if (e.canIgnore()) {
                    logger().debug("auth provider failure %s : %s", ap.getName(), e.getMessage());
                } else {
                    authProviderExp = e;
                }
            }
        }

        if (null != authProviderExp) {
            throw authProviderExp;
        }
        throw AuthProviderException.FAILURE(String.format("cannot get authtoken from account ", account.getName()));
    }

    /**
     * This method is used to create AuthToken for account
     * @param account
     * @param isAdmin is true then token will be for Admin Account
     * @param usage is type 2FA then token returned will be used for 2FA
     * @param tokenType
     * @return AuthToken
     * @throws AuthProviderException
     */
    public static AuthToken getAuthToken(Account account, boolean isAdmin, Usage usage, TokenType tokenType) throws AuthProviderException {
        for (AuthProvider ap : getProviders()) {
            try {
                AuthToken at = ap.authToken(account, isAdmin, usage, tokenType);
                if (at != null) {
                    return at;
                }
            } catch (AuthProviderException e) {
                logger().debug("auth provider failure %s : %s", ap.getName(), e.getMessage());
            }
        }

        throw AuthProviderException.FAILURE(String.format("cannot get authtoken from account ", account.getName()));
    }

    protected AuthToken authToken(Account acct, Usage usage) throws AuthProviderException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    protected AuthToken authToken(Account acct, Usage usage, TokenType tokenType) throws AuthProviderException {
        throw AuthProviderException.NOT_SUPPORTED();
    }
    
    /**
     * If there is no implementation of AuthProvider this method is used.
     * @param acct
     * @param isAdmin
     * @param usage
     * @param tokenType
     * @return
     * @throws AuthProviderException.NOT_SUPPORTED Exception
     */
    protected AuthToken authToken(Account acct, boolean isAdmin, Usage usage, TokenType tokenType) throws AuthProviderException {
        throw AuthProviderException.NOT_SUPPORTED();
    }

    public static boolean allowBasicAuth(HttpServletRequest req, ZimbraServlet servlet) {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            if (ap.allowHttpBasicAuth(req, servlet)) {
                return true;
            }
        }
        return false;
    }

    public static boolean allowAccessKeyAuth(HttpServletRequest req, ZimbraServlet servlet) {
        List<AuthProvider> providers = getProviders();
        for (AuthProvider ap : providers) {
            if (ap.allowURLAccessKeyAuth(req, servlet)) {
                return true;
            }
        }
        return false;
    }

    public static Account validateAuthToken(Provisioning prov, AuthToken at,
            boolean addToLoggingContext) throws ServiceException {
        return validateAuthToken(prov, at, addToLoggingContext, Usage.AUTH);
    }

    public static Account validateAuthToken(Provisioning prov, AuthToken at,
            boolean addToLoggingContext, Usage usage) throws ServiceException {
        if (at.getUsage() == Usage.RESET_PASSWORD && usage == Usage.AUTH) {
            validateAuthTokenInternal(prov, at, addToLoggingContext, Usage.RESET_PASSWORD);
            throw AccountServiceException.RESET_PASSWORD();
        }
        try {
            return validateAuthTokenInternal(prov, at, addToLoggingContext, usage);
        } catch (ServiceException e) {
            if (ServiceException.AUTH_EXPIRED.equals(e.getCode())) {
                // we may not want to expose the details to malicious caller
                // debug log the message and throw a vanilla AUTH_EXPIRED
                ZimbraLog.account.debug("auth token validation failed", e);
                throw ServiceException.AUTH_EXPIRED();
            } else {
                // rethrow the same exception
                throw e;
            }
        }
    }

    private static Account validateAuthTokenInternal(Provisioning prov, AuthToken at,
            boolean addToLoggingContext, Usage usage) throws ServiceException {
        if (prov == null) {
            prov = Provisioning.getInstance();
        }

        if (at.getUsage() != usage) {
            throw ServiceException.AUTH_EXPIRED("invalid usage value");
        }

        if (at.isExpired()) {
            if(at.isRegistered()) {
                try {
                     at.deRegister();
                } catch (AuthTokenException e) {
                     ZimbraLog.account.error(e);
                }
            }
            throw ServiceException.AUTH_EXPIRED();
        }

        if(!at.isRegistered()) {
            throw ServiceException.AUTH_EXPIRED();
        }

        // make sure that the authenticated account is still active and has not been deleted since the last request
        String acctId = at.getAccountId();
        Account acct = prov.get(AccountBy.id, acctId, at);

        if (acct == null && at.isZMGAppBootstrap()) {
            return null;
        }

        if (acct == null) {
            throw ServiceException.AUTH_EXPIRED("account " + acctId + " not found");
        }

        if (addToLoggingContext) {
            ZimbraLog.addAccountNameToContext(acct.getName());
        }

        if (!acct.checkAuthTokenValidityValue(at)) {
            throw ServiceException.AUTH_EXPIRED("invalid validity value");
        }

        boolean delegatedAuth = at.isDelegatedAuth();
        String acctStatus = acct.getAccountStatus(prov);

        if (!delegatedAuth && !Provisioning.ACCOUNT_STATUS_ACTIVE.equals(acctStatus)) {
            if (at.getUsage() == Usage.TWO_FACTOR_AUTH) {
                // if this is a 2FA token, attempting to log into an inactive account
                // should throw the same error as when authenticating with a username/password
                // for an inactive account
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), "account not active");
            } else {
                throw ServiceException.AUTH_EXPIRED("account not active");
            }
        }

        // if using delegated auth, make sure the "admin" is really an active admin account
        if (delegatedAuth) {

            // note that delegated auth allows access unless the account's in maintenance mode
            if (Provisioning.ACCOUNT_STATUS_MAINTENANCE.equals(acctStatus)) {
                throw ServiceException.AUTH_EXPIRED("delegated account in MAINTENANCE mode");
            }

            Account admin = prov.get(AccountBy.id, at.getAdminAccountId());
            if (admin == null) {
                throw ServiceException.AUTH_EXPIRED("delegating account " + at.getAdminAccountId() + " not found");
            }

            boolean isAdmin = AdminAccessControl.isAdequateAdminAccount(admin);
            if (!isAdmin) {
                throw ServiceException.PERM_DENIED("not an admin for delegated auth");
            }

            if (!Provisioning.ACCOUNT_STATUS_ACTIVE.equals(admin.getAccountStatus(prov))) {
                throw ServiceException.AUTH_EXPIRED("delegating account is not active");
            }
        }

        return acct;
    }
}