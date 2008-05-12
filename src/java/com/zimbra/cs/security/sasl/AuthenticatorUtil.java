/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server * Copyright (C) 2007 Zimbra, Inc.
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
import com.zimbra.common.util.ZimbraLog;

/**
 * Miscellaneous utility methods to support SASL authentication.
 */
public final class AuthenticatorUtil {
    public static Account authenticate(String username, String authenticateId, String password, String protocol, String origRemoteIp)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.name, authenticateId);
        if (account == null) {
            ZimbraLog.account.warn("No account associated with authentication id '%s'", authenticateId);
            return null;
        }
        // authenticate the authentication principal
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, origRemoteIp);
        prov.authAccount(account, password, protocol, authCtxt);
        return authorize(account, username);
    }

    public static Account authenticateKrb5(String username, String principal) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.krb5Principal, principal);
        if (account == null) {
            ZimbraLog.account.warn("No account associated with Kerberos principle '%s'", principal);
            return null;
        }
        return authorize(account, username);
    }

    public static Account authenticateZToken(String username, String authtoken) throws ServiceException {
        if (username == null || username.equals(""))
            return null;

        Provisioning prov = Provisioning.getInstance();
        AuthToken at;
        try {
            at = ZimbraAuthToken.getAuthToken(authtoken);
        } catch (AuthTokenException e) {
            return null;
        }
        if (at == null || at.isExpired() || !at.isAdmin() || at.getAdminAccountId() != null)
            return null;

        Account admin = prov.get(Provisioning.AccountBy.id, at.getAccountId(), at);
        if (admin == null || !admin.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
            return null;

        return authorize(admin, username);
    }

    private static Account authorize(Account account, String username) throws ServiceException {
        if (username == null || username.length() == 0)
            return account;

        Provisioning prov = Provisioning.getInstance();
        Account userAcct = prov.get(Provisioning.AccountBy.name, username);
        if (userAcct == null) {
            // if username not found, check username again using the domain associated with the authorization account
            int i = username.indexOf('@');
            if (i != -1) {
                String domain = account.getDomainName();
                if (domain != null) {
                    username = username.substring(0, i) + '@' + domain;
                    userAcct = prov.get(Provisioning.AccountBy.name, username);
                }
            }
        }
        if (userAcct == null) {
            ZimbraLog.account.warn("No account associated with username '%s'", username);
            return null;
        }
        if (!account.getUid().equals(userAcct.getUid()) && !AccessManager.getInstance().canAccessAccount(account, userAcct)) {
            ZimbraLog.account.warn("Account for username '%s' cannot be accessed by '%s'", username, account.getName());
            return null;
        }
        return userAcct;
    }
}
