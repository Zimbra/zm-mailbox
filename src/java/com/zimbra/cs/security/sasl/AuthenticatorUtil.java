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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.common.service.ServiceException;

import java.io.IOException;

/**
 * Miscellaneous utility methods to support SASL authentication.
 */
public final class AuthenticatorUtil {
    public static Account authenticate(String username, String authenticateId,
                                       String password, String protocol, String origRemoteIp)
            throws ServiceException, IOException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.name, username);
        Account authacct = authenticateId == null || authenticateId.equals("") ?
            account : prov.get(Provisioning.AccountBy.name, authenticateId);
        if (account == null || authacct == null) {
            return null;
        }
        // authenticate the authentication principal
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, origRemoteIp);
        prov.authAccount(authacct, password, protocol, authCtxt);

        // authorize as the target user
        if (!account.getId().equals(authacct.getId())) {
            // check domain/global admin if auth credentials != target account
            if (!AccessManager.getInstance().canAccessAccount(authacct, account)) {
                return null;
            }
        }
        return account;
    }

    public static Account authenticateKrb5(String username, String principal)
            throws ServiceException, IOException {
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
}
