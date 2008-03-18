/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.security.sasl;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthContext;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.common.service.ServiceException;

/**
 * Miscellaneous utility methods to support SASL authentication.
 */
public final class AuthenticatorUtil {
    public static Account authenticate(String username, String authenticateId, String password, String protocol, String origRemoteIp)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.name, username);
        Account authAcct = authenticateId == null || authenticateId.equals("") ?
            account : prov.get(Provisioning.AccountBy.name, authenticateId);
        if (account == null || authAcct == null) {
            return null;
        }
        // authenticate the authentication principal
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, origRemoteIp);
        prov.authAccount(authAcct, password, protocol, authCtxt);
        // authorize as the target user
        if (!account.getId().equals(authAcct.getId())) {
            // check domain/global admin if auth credentials != target account
            if (!AccessManager.getInstance().canAccessAccount(authAcct, account)) {
                return null;
            }
        }
        return account;
    }

    public static Account authenticateKrb5(String username, String principal) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.krb5Principal, principal);
        if (account == null) {
            return null;
        }
        if (username != null) {
            // If username (authorization id) is specified, then return the
            // user's account only if it can be accessed by the account
            // associated with the Kerberos principal (authentication id).
            Account userAcct = prov.get(Provisioning.AccountBy.name, username);
            if (userAcct == null) {
                return null;
            }
            AccessManager am = AccessManager.getInstance();
            return am.canAccessAccount(account, userAcct) ? userAcct : null;
        }
        return account;
    }

    public static Account authenticateZToken(String username, String authtoken) throws ServiceException {
        if (username == null || username.equals("")) {
            return null;
        }
        Provisioning prov = Provisioning.getInstance();
        try {
            AuthToken at = ZimbraAuthToken.getAuthToken(authtoken);
            if (at == null || at.isExpired() || !at.isAdmin() || at.getAdminAccountId() != null) {
                return null;
            }
            Account admin = prov.get(Provisioning.AccountBy.id, at.getAccountId(), at);
            if (admin == null || !admin.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE)) {
                return null;
            }
        } catch (AuthTokenException e) {
            return null;
        }
        return prov.get(Provisioning.AccountBy.name, username);
    }
}
