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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Server;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.SkinUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.common.util.ZimbraLog;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author schemers
 */
public class Auth extends AccountDocumentHandler {

    /** Returns (or creates) the in-memory {@link Session} object appropriate
     *  for this request.<p>
     * 
     *  Auth commands do not create a session by default, as issues with the 
     *  ordering of operations might cause the new session to be for the old
     *  credentials rather than for the new ones.
     * 
     * @return <code>null</code> in all cases */
    public Session getSession(Map context) {
        return null;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element authTokenEl = request.getOptionalElement(AccountConstants.E_AUTH_TOKEN);
        if (authTokenEl != null) {
            try {
                String token = authTokenEl.getText();
                AuthToken at = AuthToken.getAuthToken(token);
                if (at.isExpired())
                    throw ServiceException.AUTH_EXPIRED();
                // make sure that the authenticated account is active and has not been deleted/disabled since the last request
                Account acct = Provisioning.getInstance().get(AccountBy.id, at.getAccountId());
                if (acct == null || !acct.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                    throw ServiceException.AUTH_EXPIRED();
                return doResponse(request, at, zsc, acct);
            } catch (AuthTokenException e) {
                throw ServiceException.AUTH_REQUIRED();
            }
        } else {
            Element acctEl = request.getElement(AccountConstants.E_ACCOUNT);
            String value = acctEl.getText();
            String byStr = acctEl.getAttribute(AccountConstants.A_BY, AccountBy.name.name());
            Element preAuthEl = request.getOptionalElement(AccountConstants.E_PREAUTH);
            String password = request.getAttribute(AccountConstants.E_PASSWORD, null);

            Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
            String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();

            AccountBy by = AccountBy.fromString(byStr);

            if (by == AccountBy.name) {
                if (virtualHost != null && value.indexOf('@') == -1) {
                    Domain d = prov.get(DomainBy.virtualHostname, virtualHost);
                    if (d != null)
                        value = value + "@" + d.getName();
                }
            }

            Account acct = prov.get(by, value);
            if (acct == null)
                throw AuthFailedServiceException.AUTH_FAILED(value, "account not found");

            long expires = 0;

            if (password != null) {
                prov.authAccount(acct, password, "soap");
            } else if (preAuthEl != null) {
                long timestamp = preAuthEl.getAttributeLong(AccountConstants.A_TIMESTAMP);
                expires = preAuthEl.getAttributeLong(AccountConstants.A_EXPIRES, 0);
                String preAuth = preAuthEl.getTextTrim();
                prov.preAuthAccount(acct, value, byStr, timestamp, expires, preAuth);
            } else {
                throw ServiceException.INVALID_REQUEST("must specify "+AccountConstants.E_PASSWORD, null);
            }

            AuthToken at = expires ==  0 ? AuthToken.getAuthToken(acct) : AuthToken.getAuthToken(acct, expires);
            return doResponse(request, at, zsc, acct);
        }
    }

    private Element doResponse(Element request, AuthToken at, ZimbraSoapContext zsc, Account acct) throws ServiceException {
        Element response = zsc.createElement(AccountConstants.AUTH_RESPONSE);
        at.encodeAuthResp(response, false);
        
        response.addAttribute(AccountConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
        boolean isCorrectHost = Provisioning.onLocalServer(acct);
        if (isCorrectHost) {
            Session session = updateAuthenticatedAccount(zsc, at, true);
            if (session != null)
                ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType(), true);
        }
        
        Server localhost = Provisioning.getInstance().getLocalServer();
        String referMode = localhost.getAttr(Provisioning.A_zimbraMailReferMode, "wronghost");
        // if (!isCorrectHost || LC.zimbra_auth_always_send_refer.booleanValue()) {
        if (Provisioning.MAIL_REFER_MODE_ALWAYS.equals(referMode) ||
            (Provisioning.MAIL_REFER_MODE_WRONGHOST.equals(referMode) && !isCorrectHost)) {
            response.addAttribute(AccountConstants.E_REFERRAL, acct.getAttr(Provisioning.A_zimbraMailHost), Element.Disposition.CONTENT);
        }

		Element prefsRequest = request.getOptionalElement(AccountConstants.E_PREFS);
		if (prefsRequest != null) {
			Element prefsResponse = response.addUniqueElement(AccountConstants.E_PREFS);
			GetPrefs.handle(prefsRequest, prefsResponse, acct);
		}

        Element attrsRequest = request.getOptionalElement(AccountConstants.E_ATTRS);
        if (attrsRequest != null) {
            Element attrsResponse = response.addUniqueElement(AccountConstants.E_ATTRS);
            Set<String> attrList = AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.accountInfo);
            for (Iterator it = attrsRequest.elementIterator(AccountConstants.E_ATTR); it.hasNext(); ) {
                Element e = (Element) it.next();
                String name = e.getAttribute(AccountConstants.A_NAME);
                if (name != null && attrList.contains(name)) {
                    Object v = acct.getUnicodeMultiAttr(name);
                    if (v != null) GetInfo.doAttr(attrsResponse, name, v);
                }
            }
        }

		Element requestedSkinEl = request.getOptionalElement(AccountConstants.E_REQUESTED_SKIN);
		String requestedSkin = requestedSkinEl != null ? requestedSkinEl.getText() : null;  
		String skin = SkinUtil.chooseSkin(acct, requestedSkin);
		ZimbraLog.webclient.debug("chooseSkin() returned "+skin );
		if (skin != null) {
			response.addElement(AccountConstants.E_SKIN).setText(skin);
		}

		return response;
    }

    public boolean needsAuth(Map<String, Object> context) {
		return false;
	}
}
