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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AuthMode;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.cs.servlet.util.AuthUtil;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;
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
        String twoFactorCode = request.getAttribute(AccountConstants.E_TWO_FACTOR_CODE, null);
        Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
        AuthToken at = null;
        Account acct = null;
        String acctName = null;

        if (password != null) {
            if (name == null && acctEl == null || name != null && acctEl != null) {
                throw ServiceException.INVALID_REQUEST("invalid request parameter", null);
            }
            
            AccountBy by;
            if (name != null) {
                acctName = name;
                by = AccountBy.name;
            } else {
                acctName = acctEl.getText();
                String byStr = acctEl.getAttribute(AccountConstants.A_BY, AccountBy.name.name());
                by = AccountBy.fromString(byStr);
            }

            Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
            String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
            acct = AccountUtil.getAccount(by, acctName, virtualHost, zsc.getAuthToken(), prov);
            // make sure that the authenticated account is active and has not been deleted/disabled since the last request
            if (acct == null || !acct.getAccountStatus(prov).equals(Provisioning.ACCOUNT_STATUS_ACTIVE)) {
                throw ServiceException.PERM_DENIED("Error in Authentication");
            }

            // check account is admin
            AccountUtil.isAdminAccount(acct);

            Map<String, Object> authCtxt = AccountUtil.getAdminAuthContext(context, acctName, zsc);
            context.put(Provisioning.AUTH_MODE_KEY, AuthMode.PASSWORD);

            prov.authAccount(acct, password, AuthContext.Protocol.soap, authCtxt);

            AuthMech authedByMech = (AuthMech) authCtxt.get(AuthContext.AC_AUTHED_BY_MECH);
            Usage usage = Usage.AUTH;
            TokenType tokenType = null;

            if (AccountUtil.isTwoFactorAccount(acct)) {
                if (StringUtil.isNullOrEmpty(twoFactorCode)) {
                    String reqTokenType = request.getAttribute(AccountConstants.A_TOKEN_TYPE, "");
                    tokenType = TokenType.fromCode(reqTokenType);
                    usage = Usage.TWO_FACTOR_AUTH;
                } else {
                    // validation of TOTP will be performed here.
                    AccountUtil.authenticateTwoFactorCode(twoFactorCode, acct);
                }
            }
            at = AuthUtil.getAuthToken(acct, true, usage, tokenType, authedByMech);
        } else {
            at = AuthUtil.getAuthToken(request, prov, zsc);

            acct = AuthProvider.validateAuthToken(prov, at, true, at.getUsage());
            
            // check this Auth token is for admin account and Zimbra user
            if (!(AuthToken.isAnyAdmin(at) && at.isZimbraUser())) {
                throw ServiceException.PERM_DENIED("Error in Authentication");
            }

            if (at.getUsage() == Usage.TWO_FACTOR_AUTH && !StringUtil.isNullOrEmpty(twoFactorCode)) {
                if (acctEl != null && authTokenEl != null && authTokenEl.getAttributeBool(AccountConstants.A_VERIFY_ACCOUNT, false)) {
                    AuthUtil.validateAuthTokenWithNamedAccount(authTokenEl, acctEl, request, acct, prov, at);
                }

                // validation of TOTP will be performed here.
                AccountUtil.authenticateTwoFactorCode(twoFactorCode, acct);

                Map<String, Object> authCtxt = AccountUtil.getAdminAuthContext(context, acctName, zsc);
                AuthMech authedByMech = (AuthMech) authCtxt.get(AuthContext.AC_AUTHED_BY_MECH);
                at = AuthUtil.getAuthToken(acct, true, null, null, authedByMech);
            }
        }
        
        return doResponse(request, at, zsc, context, acct, csrfSupport);
    }

    private Element doResponse(Element request, AuthToken at, ZimbraSoapContext zsc, Map<String, Object> context, Account acct, boolean csrfSupport) throws ServiceException {
        HttpServletRequest httpReq = (HttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        HttpServletResponse httpResp = (HttpServletResponse)context.get(SoapServlet.SERVLET_RESPONSE);

        Element response = zsc.createElement(AdminConstants.AUTH_RESPONSE);
        response.addAttribute(AdminConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
        at.setCsrfTokenEnabled(csrfSupport);
        if (at.getUsage() == Usage.TWO_FACTOR_AUTH) {
            response.addUniqueElement(AccountConstants.E_TWO_FACTOR_AUTH_REQUIRED).setText("true");
        } else {
            boolean rememberMe = request.getAttributeBool(AdminConstants.A_PERSIST_AUTH_TOKEN_COOKIE, false);
            at.encode(httpResp, true, ZimbraCookie.secureCookie(httpReq), rememberMe);
            httpReq.setAttribute(CsrfFilter.AUTH_TOKEN, at);
            Session session = updateAuthenticatedAccount(zsc, at, context, true);
            if (session != null) {
                ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType());
            }
        }
        at.encodeAuthResp(response, true);

        CsrfUtil.setCSRFToken(httpReq, httpResp, at, csrfSupport, response);

        return response;
    }

    @Deprecated
    private void checkAdmin(Account acct) throws ServiceException {
        boolean isDomainAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        boolean isAdmin= acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        boolean isDelegatedAdmin= acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false);
        boolean ok = (isDomainAdmin || isAdmin || isDelegatedAdmin);
        if (!ok)
            throw ServiceException.PERM_DENIED("not an admin account");
    }
    
    @Deprecated
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
