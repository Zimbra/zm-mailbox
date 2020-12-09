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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning.AuthMode;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.TrustedDevice;
import com.zimbra.cs.account.TrustedDeviceToken;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswords;
import com.zimbra.cs.account.auth.twofactor.TrustedDevices;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.listeners.AuthListener;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class Auth extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        AuthToken at = null;
        Account acct = null;
        String acctValuePassedIn = null;

        Provisioning prov = Provisioning.getInstance();
        boolean csrfSupport = request.getAttributeBool(AccountConstants.A_CSRF_SUPPORT, false);
        String name = request.getAttribute(AdminConstants.E_NAME, null);
        Element acctEl = request.getOptionalElement(AccountConstants.E_ACCOUNT);

        //only perform auth-token authentication if other credentials are not provided
        if (name == null && acctEl == null) {
            //get an auth token from cookie
            at = zsc.getAuthToken();
            if(at == null) {
                //if auth token is not in the cookie check for auth token in SOAP
                Element authTokenEl = request.getOptionalElement(AdminConstants.E_AUTH_TOKEN);
                if(authTokenEl != null) {
                    try {
                        at = AuthProvider.getAuthToken(request, new HashMap<String, Object>());
                    } catch (AuthTokenException e) {
                        throw ServiceException.AUTH_REQUIRED();
                    }
                }
            }

            if(at == null) {
                //neither login credentials nor valid auth token could be retrieved
                throw ServiceException.AUTH_REQUIRED();
            }
            com.zimbra.cs.service.account.Auth.addAccountToLogContextByAuthToken(prov, at);

            if (at.isExpired())
                throw ServiceException.AUTH_EXPIRED();

            if(!at.isRegistered())
                throw ServiceException.AUTH_EXPIRED("authtoken is invalid");

            // make sure that the authenticated account is active and has not been deleted/disabled since the last request
            acct = prov.get(AccountBy.id, at.getAccountId(), at);
            if (acct == null || !acct.getAccountStatus(prov).equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                throw ServiceException.AUTH_EXPIRED();

            // make sure the authenticated account is an admin account
            checkAdmin(acct);
        } else {
            /*
             * only one of
             *     <name>...</name>
             * or
             *     <account by="name|id|foreignPrincipal">...</account>
             * can/must be specified
             */
            if (name != null && acctEl != null)
                throw ServiceException.INVALID_REQUEST("only one of <name> or <account> can be specified", null);
            if (name == null && acctEl == null)
                throw ServiceException.INVALID_REQUEST("missing <name> or <account>", null);

            String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
            String recoveryCode = request.getAttribute(AccountConstants.E_RECOVERY_CODE, null);
            String twoFactorCode = request.getAttribute(AccountConstants.E_TWO_FACTOR_CODE, null);
            Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
            String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();

            String valuePassedIn;
            AccountBy by;
            String value;
            if (name != null) {
                valuePassedIn = name;
                by = AccountBy.name;
            } else {
                valuePassedIn = acctEl.getText();
                String byStr = acctEl.getAttribute(AccountConstants.A_BY, AccountBy.name.name());
                by = AccountBy.fromString(byStr);
            }
            value = valuePassedIn;

            try {

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

                AccountUtil.addAccountToLogContext(prov, acct.getId(), ZimbraLog.C_NAME, ZimbraLog.C_ID, null);

                ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "AdminAuth","account", value}));

                Map<String, Object> authCtxt = new HashMap<String, Object>();
                authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, context.get(SoapEngine.ORIG_REQUEST_IP));
                authCtxt.put(AuthContext.AC_REMOTE_IP, context.get(SoapEngine.SOAP_REQUEST_IP));
                authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, valuePassedIn);
                authCtxt.put(AuthContext.AC_USER_AGENT, zsc.getUserAgent());
                authCtxt.put(AuthContext.AC_AS_ADMIN, Boolean.TRUE);          
                
                String deviceId = request.getAttribute(AccountConstants.E_DEVICE_ID, null);
                Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
                String reqTokenType = request.getAttribute(AccountConstants.A_TOKEN_TYPE, "");
                TokenType tokenType = TokenType.fromCode(reqTokenType);
                boolean generateDeviceId = request.getAttributeBool(AccountConstants.A_GENERATE_DEVICE_ID, false);
                String newDeviceId = generateDeviceId? UUIDUtil.generateUUID(): null;
                
                AuthMode mode = AuthMode.PASSWORD;
                String code = password;
                if (StringUtils.isEmpty(password) && StringUtils.isNotEmpty(recoveryCode)) {
                    mode = AuthMode.RECOVERY_CODE;
                    code = recoveryCode;
                }
                authCtxt.put(Provisioning.AUTH_MODE_KEY, mode);
                
                TrustedDeviceToken trustedToken = null;
                if (acct != null) {
                	if(acctEl != null) {
                		acctValuePassedIn = acctEl.getText();
                	}
                    TrustedDevices trustedDeviceManager = TwoFactorAuth.getFactory().getTrustedDevices(acct);
                    if (trustedDeviceManager != null) {
                        trustedToken = trustedDeviceManager.getTokenFromRequest(request, context);
                        if (trustedToken != null && trustedToken.isExpired()) {
                            TrustedDevice device = trustedDeviceManager.getTrustedDeviceByTrustedToken(trustedToken);
                            if (device != null) {
                                device.revoke();
                            }
                        }
                    }
                }
                
                Boolean registerTrustedDevice = false;
                TwoFactorAuth twoFactorManager = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);
                if (twoFactorManager.twoFactorAuthEnabled()) {
                    registerTrustedDevice = trustedToken == null && request.getAttributeBool(AccountConstants.A_TRUSTED_DEVICE, false);
                }
                
                boolean trustedDeviceOverride = false;
                if (trustedToken != null && acct.isFeatureTrustedDevicesEnabled()) {
                    if (trustedToken.isExpired()) {
                        ZimbraLog.account.debug("trusted token is expired");
                        registerTrustedDevice = false;
                    } else {
                        Map<String, Object> attrs = getTrustedDeviceAttrs(zsc, deviceId);
                        try {
                            verifyTrustedDevice(acct, trustedToken, attrs);
                            trustedDeviceOverride = true;
                        } catch (AuthFailedServiceException e) {
                            AuthListener.invokeOnException(e);
                            ZimbraLog.account.info("trusted device not verified");
                        }
                    }
                }
                boolean usingTwoFactorAuth = acct != null && twoFactorManager.twoFactorAuthRequired() && !trustedDeviceOverride;
                boolean twoFactorAuthWithToken = usingTwoFactorAuth && authTokenEl != null;
                if (password != null || recoveryCode != null || twoFactorAuthWithToken) {
                    // authentication logic can be reached with either a password, or a 2FA auth token
                    if (usingTwoFactorAuth && twoFactorCode == null && (password != null || recoveryCode != null)) {
                        int mtaAuthPort = acct.getServer().getMtaAuthPort();
                        boolean supportsAppSpecificPaswords =  acct.isFeatureAppSpecificPasswordsEnabled() && zsc.getPort() == mtaAuthPort;
                        if (supportsAppSpecificPaswords && password != null) {
                            // if we are here, it means we are authenticating SMTP,
                            // so app-specific passwords are accepted. Other protocols (pop, imap)
                            // doesn't touch this code, so their authentication happens in ZimbraAuth.
                            AppSpecificPasswords appPasswords = TwoFactorAuth.getFactory().getAppSpecificPasswords(acct, acctValuePassedIn);
                            appPasswords.authenticate(password);
                        } else {
                            prov.authAccount(acct, code, AuthContext.Protocol.soap, authCtxt);
                            return needTwoFactorAuth(acct, twoFactorManager, zsc, tokenType);
                        }
                    } else {
                        if (password != null || recoveryCode != null) {
                            prov.authAccount(acct, code, AuthContext.Protocol.soap, authCtxt);
                        } else {
                            // it's ok to not have a password if the client is using a 2FA auth token for the 2nd step of 2FA
                            if (!twoFactorAuthWithToken) {
                                throw ServiceException.AUTH_REQUIRED();
                            }
                        }
                        if (usingTwoFactorAuth) {
                            // check that 2FA has been enabled, in case the client is passing in a twoFactorCode prior to setting up 2FA
                            if (!twoFactorManager.twoFactorAuthEnabled()) {
                                throw AccountServiceException.TWO_FACTOR_SETUP_REQUIRED();
                            }
                            AuthToken twoFactorToken = null;
                            if (password == null) {
                                try {
                                    twoFactorToken = AuthProvider.getAuthToken(authTokenEl, acct);
                                    Account twoFactorTokenAcct = AuthProvider.validateAuthToken(prov, twoFactorToken, false, Usage.TWO_FACTOR_AUTH);
                                    boolean verifyAccount = authTokenEl.getAttributeBool(AccountConstants.A_VERIFY_ACCOUNT, false);
                                    if (verifyAccount && !twoFactorTokenAcct.getId().equalsIgnoreCase(acct.getId())) {
                                        throw new AuthTokenException("two-factor auth token doesn't match the named account");
                                    }
                                } catch (AuthTokenException e) {
                                    AuthFailedServiceException exception = AuthFailedServiceException
                                        .AUTH_FAILED("bad auth token");
                                    AuthListener.invokeOnException(exception);
                                    throw exception;
                                }
                            }
                            TwoFactorAuth manager = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);
                            if (twoFactorCode != null) {
                                manager.authenticate(twoFactorCode);
                            } else {
                                AuthFailedServiceException e = AuthFailedServiceException
                                    .AUTH_FAILED("no two-factor code provided");
                                AuthListener.invokeOnException(e);
                                throw e;
                            }
                            if (twoFactorToken != null) {
                                try {
                                    twoFactorToken.deRegister();
                                } catch (AuthTokenException e) {
                                    throw ServiceException.FAILURE("cannot de-register two-factor auth token", e);
                                }
                            }
                        }
                    }
                } 

                if (registerTrustedDevice && (trustedToken == null || trustedToken.isExpired())) {
                    //generate a new trusted device token if there is no existing one or if the current one is no longer valid
                    Map<String, Object> attrs = getTrustedDeviceAttrs(zsc, newDeviceId == null? deviceId: newDeviceId);
                    TrustedDevices trustedDeviceManager = TwoFactorAuth.getFactory().getTrustedDevices(acct);
                    if (trustedDeviceManager != null) {
                        trustedToken = trustedDeviceManager.registerTrustedDevice(attrs);
                    }
                }
                checkAdmin(acct);
                AuthMech authedByMech = (AuthMech) authCtxt.get(AuthContext.AC_AUTHED_BY_MECH);
                at = AuthProvider.getAuthToken(acct, true, authedByMech);
            } catch (ServiceException se) {
                ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "AdminAuth","account", value, "error", se.getMessage()}));
                throw se;
            }
        }
        if(at != null) {
            at.setCsrfTokenEnabled(csrfSupport);
        }
        ServletRequest httpReq = (ServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        httpReq.setAttribute(CsrfFilter.AUTH_TOKEN, at);
        return doResponse(request, at, zsc, context, acct, csrfSupport);
    }
    
    private Map<String, Object> getTrustedDeviceAttrs(ZimbraSoapContext zsc, String deviceId) {
        Map<String, Object> deviceAttrs = new HashMap<String, Object>();
        deviceAttrs.put(AuthContext.AC_DEVICE_ID, deviceId);
        deviceAttrs.put(AuthContext.AC_USER_AGENT, zsc.getUserAgent());
        return deviceAttrs;
    }
    
    private void verifyTrustedDevice(Account account, TrustedDeviceToken td, Map<String, Object> attrs) throws ServiceException {
        TrustedDevices trustedDeviceManager = TwoFactorAuth.getFactory().getTrustedDevices(account);
        trustedDeviceManager.verifyTrustedDevice(td, attrs);
    }

    private Element needTwoFactorAuth(Account account, TwoFactorAuth auth, ZimbraSoapContext zsc, TokenType tokenType) throws ServiceException {
        /* two cases here:
         * 1) the user needs to provide a two-factor code.
         *    in this case, the server returns a two-factor auth token in the response header that the client
         *    must send back, along with the code, in order to finish the authentication process.
         * 2) the user needs to set up two-factor auth.
         *    this can happen if it's required for the account but the user hasn't received a secret yet.
         */
        if (!auth.twoFactorAuthEnabled()) {
            throw AccountServiceException.TWO_FACTOR_SETUP_REQUIRED();
        } else {
            Element response = zsc.createElement(AccountConstants.AUTH_RESPONSE);
            AuthToken twoFactorToken = AuthProvider.getAuthToken(account, Usage.TWO_FACTOR_AUTH, tokenType);
            response.addUniqueElement(AccountConstants.E_TWO_FACTOR_AUTH_REQUIRED).setText("true");
            response.addAttribute(AccountConstants.E_LIFETIME, twoFactorToken.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
            twoFactorToken.encodeAuthResp(response, false);
            response.addUniqueElement(AccountConstants.E_TRUSTED_DEVICES_ENABLED).setText(account.isFeatureTrustedDevicesEnabled() ? "true" : "false");
            return response;
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

    private Element doResponse(Element request, AuthToken at, ZimbraSoapContext zsc,
            Map<String, Object> context, Account acct, boolean csrfSupport) throws ServiceException {
        Element response = zsc.createElement(AdminConstants.AUTH_RESPONSE);
        at.encodeAuthResp(response, true);

        /*
         * bug 67078
         * also return auth token cookie in http header
         */
        HttpServletRequest httpReq = (HttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        HttpServletResponse httpResp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);
        boolean rememberMe = request.getAttributeBool(AdminConstants.A_PERSIST_AUTH_TOKEN_COOKIE, false);
        at.encode(httpResp, true, ZimbraCookie.secureCookie(httpReq), rememberMe);

        response.addAttribute(AdminConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);

        Session session = updateAuthenticatedAccount(zsc, at, context, true);
        if (session != null) {
            ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType());
        }

        boolean csrfCheckEnabled = false;
        if (httpReq.getAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled) != null) {
            csrfCheckEnabled = (Boolean) httpReq.getAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled);
        }
        if (csrfSupport && csrfCheckEnabled) {
            String accountId = at.getAccountId();
            long authTokenExpiration = at.getExpires();
            int tokenSalt = (Integer)httpReq.getAttribute(CsrfFilter.CSRF_SALT);
            String token = CsrfUtil.generateCsrfToken(accountId,
                authTokenExpiration, tokenSalt, at);
            Element csrfResponse = response.addUniqueElement(HeaderConstants.E_CSRFTOKEN);
            csrfResponse.addText(token);
            httpResp.setHeader(Constants.CSRF_TOKEN, token);
        }
        return response;
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
