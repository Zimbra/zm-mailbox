/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2019 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AuthMode;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.listeners.AuthListener;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class Auth extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        boolean csrfSupport = request.getAttributeBool(AccountConstants.A_CSRF_SUPPORT, false);
        String name = request.getAttribute(AdminConstants.E_NAME, null);
        Element acctEl = request.getOptionalElement(AccountConstants.E_ACCOUNT);
        String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
        AuthToken at = null;
        Account acct = null;
        boolean isCredProvided = false;
        String value = null;

        if (name == null && acctEl == null) {
            at = getAuthToken(request, prov, zsc);
            // make sure that the authenticated account is active and has not been deleted/disabled since the last request
            acct = prov.get(AccountBy.id, at.getAccountId(), at);
            if (acct == null || !acct.getAccountStatus(prov).equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                throw ServiceException.AUTH_EXPIRED();
        } else {
            if (name != null && acctEl != null)
                throw ServiceException.INVALID_REQUEST("only one of <name> or <account> can be specified", null);
            if (name == null && acctEl == null)
                throw ServiceException.INVALID_REQUEST("missing <name> or <account>", null);

            AccountBy by;
            String valuePassedIn;
            if (name != null) {
                valuePassedIn = name;
                by = AccountBy.name;
            } else {
                valuePassedIn = acctEl.getText();
                String byStr = acctEl.getAttribute(AccountConstants.A_BY, AccountBy.name.name());
                by = AccountBy.fromString(byStr);
            }
            value = valuePassedIn;
            acct = getAccountForCredentials(by, value, valuePassedIn, request, acct, prov, zsc);
            isCredProvided = true;
        }
        
        // make sure the authenticated account is an admin account
        checkAdmin(acct);
        
        if (isCredProvided) {
            Map<String, Object> authCtxt = getAdminAuthContext(context, value, zsc);
            
            if (password != null) {
                ZimbraLog.account.debug("Authenticating account using password.");
                prov.authAccount(acct, password, AuthContext.Protocol.soap, authCtxt);
            }

            if (isTwoFactorAccount(acct)) {
                String twoFactorCode = request.getAttribute(AccountConstants.E_TWO_FACTOR_CODE, null);
                Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
                String reqTokenType = request.getAttribute(AccountConstants.A_TOKEN_TYPE, "");
                TokenType tokenType = TokenType.fromCode(reqTokenType);

                context.put(Provisioning.AUTH_MODE_KEY, AuthMode.PASSWORD);
                if (authTokenEl == null && StringUtil.isNullOrEmpty(password)) {
                    throw ServiceException.INVALID_REQUEST("missing required attribute: " + AccountConstants.E_PASSWORD, null);
                }

                if (password != null && twoFactorCode == null) {
                 // Will return a temporary Auth token that will be sent again with two factor code by client
                    return getTwoFactorTempAuthToken(acct, zsc, tokenType, context, csrfSupport);
                }
                
                if (password == null) {
                    //As Password is null so will authenticity of user via temporary Auth token
                    validateAuthToken(authTokenEl, prov, acct);
                }

                //Validation of TOTP will be performed here.
                authenticateTwoFactorCode(twoFactorCode, acct);
            }
            
            AuthMech authedByMech = (AuthMech) authCtxt.get(AuthContext.AC_AUTHED_BY_MECH);
            ZimbraLog.account.debug("Retrieving Auth Token for account %s ", value);
            at = AuthProvider.getAuthToken(acct, true, authedByMech);
        }
        
        return doResponse(request, at, zsc, context, acct, csrfSupport);
    }

    private Account getAccountForCredentials(AccountBy by, String value, String valuePassedIn, Element request, Account acct, Provisioning prov, ZimbraSoapContext zsc) throws ServiceException{
        Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
        String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
        
        if (by == AccountBy.name && value.indexOf("@") == -1) {
            // first try to get by adminName, which resolves the account under cn=admins,cn=zimbra
            // and does not need a domain
            acct = prov.get(AccountBy.adminName, value, zsc.getAuthToken());

            // not found, try applying virtual host name
            if (acct == null) {
                if (virtualHost != null) {
                    Domain d = prov.get(Key.DomainBy.virtualHostname, virtualHost);
                    if (d != null)
                        value = value + "@" + d.getName();
                }
            }
        }

        if (acct == null)
            acct = prov.get(by, value);

        if (acct == null)
            throw AuthFailedServiceException.AUTH_FAILED(value, valuePassedIn, "account not found");
        
        return acct;
    }

    private AuthToken getAuthToken(Element request, Provisioning prov, ZimbraSoapContext zsc) throws ServiceException{
        //get an auth token from cookie
        AuthToken at = zsc.getAuthToken();
        ZimbraLog.account.debug("Fetching auth token from cookies.");
        if(at == null) {
            //if auth token is not in the cookie check for auth token in SOAP
            Element authTokenEl = request.getOptionalElement(AdminConstants.E_AUTH_TOKEN);
            if(authTokenEl != null) {
                try {
                    ZimbraLog.account.debug("Auth token was not present in cookies so trying to get it from soap request");
                    at = AuthProvider.getAuthToken(request, new HashMap<String, Object>());
                } catch (AuthTokenException e) {
                    throw ServiceException.AUTH_REQUIRED();
                }
            }
        }

        if (at == null) {
            //neither login credentials nor valid auth token could be retrieved
            throw ServiceException.AUTH_REQUIRED();
        }
        com.zimbra.cs.service.account.Auth.addAccountToLogContextByAuthToken(prov, at);

        if (at.isExpired())
            throw ServiceException.AUTH_EXPIRED();

        if(!at.isRegistered())
            throw ServiceException.AUTH_EXPIRED("authtoken is invalid");

        return at;
    }

    /**
     * This method will check that dmin account must not be a system and resource account
     * as well as it check that two factor is enable for this admin account.
     * @param acct
     * @return
     * @throws ServiceException
     */
    private boolean isTwoFactorAccount(Account acct) throws ServiceException {
        if (!acct.isIsSystemResource() && !acct.isIsSystemAccount()) {
            TwoFactorAuth twoFactorManager = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);
            return twoFactorManager.twoFactorAuthRequired() && twoFactorManager.twoFactorAuthEnabled();
        }
        return false;
    }
    
    private Map<String, Object> getAdminAuthContext(Map<String, Object> context, String valuePassedIn,
            ZimbraSoapContext zsc) {
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, context.get(SoapEngine.ORIG_REQUEST_IP));
        authCtxt.put(AuthContext.AC_REMOTE_IP, context.get(SoapEngine.SOAP_REQUEST_IP));
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, valuePassedIn);
        authCtxt.put(AuthContext.AC_USER_AGENT, zsc.getUserAgent());
        authCtxt.put(AuthContext.AC_AS_ADMIN, Boolean.TRUE);
        return authCtxt;
    }

    /**
     * This method will return the Auth token that will used by client to make 
     * request with two factor code + will return CSRF token if CSRF is enabled
     * @param account
     * @param twoFactorManager
     * @param zsc
     * @param tokenType
     * @param context
     * @param csrfSupport
     * @return
     * @throws ServiceException
     */
    private Element getTwoFactorTempAuthToken(Account account, ZimbraSoapContext zsc,
            TokenType tokenType, Map<String, Object> context, boolean csrfSupport) throws ServiceException {
        ZimbraLog.account.debug("Generating auth token used to validate two factor code.");
        Element response = zsc.createElement(AccountConstants.AUTH_RESPONSE);
        AuthToken twoFactorToken = AuthProvider.getAuthToken(account, Usage.TWO_FACTOR_AUTH, tokenType);
        response.addUniqueElement(AccountConstants.E_TWO_FACTOR_AUTH_REQUIRED).setText("true");
        response.addAttribute(AccountConstants.E_LIFETIME, twoFactorToken.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
        twoFactorToken.encodeAuthResp(response, false);

        HttpServletRequest httpReq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        HttpServletResponse httpResp = (HttpServletResponse) context.get(SoapServlet.SERVLET_RESPONSE);
        setCSRFToken(httpReq, httpResp, twoFactorToken, csrfSupport, response);
        try {
            ZimbraCookie.addHttpOnlyCookie(httpResp, ZimbraCookie.COOKIE_ZM_ADMIN_AUTH_TOKEN, twoFactorToken.getEncoded(), ZimbraCookie.PATH_ROOT, -1, true);
        } catch (AuthTokenException e) {
            throw ServiceException.AUTH_REQUIRED();
        }
        ZimbraCookie.addHttpOnlyCookie(httpResp, Constants.CSRF_TOKEN,
                httpResp.getHeader(Constants.CSRF_TOKEN), ZimbraCookie.PATH_ROOT, -1, true);
        return response;
    }

    /**
     * This method is used to verify the two factor code
     * This will only called when 2FA is enabled
     * @param twoFactorCode
     * @param acct
     * @throws ServiceException
     */
    private void authenticateTwoFactorCode(String twoFactorCode, Account acct) throws ServiceException{
        TwoFactorAuth manager = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);
        if (twoFactorCode != null) {
            ZimbraLog.account.debug("Verifying the Two Factor Code for account %s ", acct.getName());
            manager.authenticate(twoFactorCode);
            ZimbraLog.account.debug("Two Factor Code has been matched successfully for account %s ", acct.getName());
        } else {
            AuthFailedServiceException e = AuthFailedServiceException
                    .AUTH_FAILED("no two-factor code provided");
            AuthListener.invokeOnException(e);
            throw e;
        }
    }

    /**
     * This method is used to validate Auth Token
     * This method will only called when client provide Auth Token only for 2FA
     * @param authTokenEl
     * @param prov
     * @param acct
     * @throws ServiceException
     */
    private void validateAuthToken(Element authTokenEl, Provisioning prov, Account acct) throws ServiceException{
        try {
            ZimbraLog.account.debug("Validating two factor Auth token with for account %s ", acct.getName());
            AuthToken twoFactorToken = AuthProvider.getAuthToken(authTokenEl, acct);
            Account twoFactorTokenAcct = AuthProvider.validateAuthToken(prov, twoFactorToken, false, Usage.TWO_FACTOR_AUTH);
            boolean verifyAccount = authTokenEl.getAttributeBool(AccountConstants.A_VERIFY_ACCOUNT, false);
            if (verifyAccount && !twoFactorTokenAcct.getId().equalsIgnoreCase(acct.getId())) {
                throw new AuthTokenException("two-factor auth token doesn't match the named account");
            }
            ZimbraLog.account.debug("two-factor auth token has been matched for account %s ", acct.getName());
        } catch (AuthTokenException e) {
            AuthFailedServiceException exception = AuthFailedServiceException
                    .AUTH_FAILED("bad auth token");
            AuthListener.invokeOnException(exception);
            throw exception;
        }
    }

    private void checkAdmin(Account acct) throws ServiceException {
        boolean isDomainAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        boolean isAdmin= acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        boolean isDelegatedAdmin= acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false);
        boolean ok = (isDomainAdmin || isAdmin || isDelegatedAdmin);
        if (!ok)
            throw ServiceException.PERM_DENIED("not an admin account");
    }

    private Element doResponse(Element request, AuthToken at, ZimbraSoapContext zsc, Map<String, Object> context, Account acct, boolean csrfSupport) throws ServiceException {
        at.setCsrfTokenEnabled(csrfSupport);
        HttpServletRequest httpReq = (HttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        httpReq.setAttribute(CsrfFilter.AUTH_TOKEN, at);
        Element response = zsc.createElement(AdminConstants.AUTH_RESPONSE);
        at.encodeAuthResp(response, true);

        /*
         * bug 67078
         * also return auth token cookie in http header
         */
        HttpServletResponse httpResp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
        boolean rememberMe = request.getAttributeBool(AdminConstants.A_PERSIST_AUTH_TOKEN_COOKIE, false);
        at.encode(httpResp, true, ZimbraCookie.secureCookie(httpReq), rememberMe);

        response.addAttribute(AdminConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);

        Session session = updateAuthenticatedAccount(zsc, at, context, true);
        if (session != null) {
            ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType());
        }

        setCSRFToken(httpReq, httpResp, at, csrfSupport, response);

        return response;
    }

    private void setCSRFToken(HttpServletRequest httpReq, HttpServletResponse httpResp, AuthToken authToken, boolean csrfSupport, Element response) throws ServiceException {
        boolean csrfCheckEnabled = false;
        if (httpReq.getAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled) != null) {
            csrfCheckEnabled = (Boolean) httpReq.getAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled);
        }
        if (csrfSupport && csrfCheckEnabled) {
            String accountId = authToken.getAccountId();
            long authTokenExpiration = authToken.getExpires();
            int tokenSalt = (Integer) httpReq.getAttribute(CsrfFilter.CSRF_SALT);
            String token = CsrfUtil.generateCsrfToken(accountId, authTokenExpiration, tokenSalt, authToken);
            Element csrfResponse = response.addUniqueElement(HeaderConstants.E_CSRFTOKEN);
            csrfResponse.addText(token);
            httpResp.setHeader(Constants.CSRF_TOKEN, token);
        }
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        // can't require auth on auth request
        return false;
    }

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        // can't require auth on auth request
        return false;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}
