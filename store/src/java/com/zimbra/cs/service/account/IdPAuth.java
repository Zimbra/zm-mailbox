/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.service.account;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.listeners.AuthListener;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Redeems a single-use ZCO exchange auth token (usage {@link Usage#ZCO_AUTH}) for a normal
 * session auth token. The presented exchange token is de-registered on first redemption so that a
 * replayed token is rejected.
 */
public class IdPAuth extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element authTokenEl = request.getElement(AccountConstants.E_AUTH_TOKEN);

        AuthToken exchangeToken;
        Account acct;
        try {
            exchangeToken = AuthProvider.getAuthToken(authTokenEl, (Account) null);
            // enforce that the presented token was minted for ZCO exchange only, this also verifies
            // registration (single-use), expiry and account status.
            acct = AuthProvider.validateAuthToken(prov, exchangeToken, false, Usage.ZCO_AUTH);
        } catch (AuthTokenException e) {
            AuthFailedServiceException afse = AuthFailedServiceException.AUTH_FAILED("invalid exchange token");
            AuthListener.invokeOnException(afse);
            throw afse;
        }

        // issue a normal session auth token for the account
        AuthToken sessionToken = AuthProvider.getAuthToken(acct);

        Element response = zsc.createElement(AccountConstants.IDP_AUTH_RESPONSE);
        sessionToken.encodeAuthResp(response, false);
        response.addAttribute(AccountConstants.E_LIFETIME,
                sessionToken.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);

        // also return the session token as an auth cookie in the http response header
        HttpServletRequest httpReq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        HttpServletResponse httpResp = (HttpServletResponse) context.get(SoapServlet.SERVLET_RESPONSE);
        sessionToken.encode(httpReq, httpResp, false, ZimbraCookie.secureCookie(httpReq), false);

        if (Provisioning.onLocalServer(acct)) {
            Session session = updateAuthenticatedAccount(zsc, sessionToken, context, true);
            if (session != null) {
                ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType());
            }
        }

        // single-use: destroy the exchange token so it cannot be replayed
        try {
            exchangeToken.deRegister();
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("cannot de-register ZCO exchange auth token", e);
        }

        AuthListener.invokeOnSuccess(acct);
        ZimbraLog.security.info("ZCO exchange token redeemed for account %s", acct.getName());
        return response;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        // the exchange token itself is the credential
        return false;
    }
}
