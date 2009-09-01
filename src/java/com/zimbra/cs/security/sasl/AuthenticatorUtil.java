/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.security.sasl;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.AuthProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Miscellaneous utility methods to support SASL authentication.
 */
public final class AuthenticatorUtil {
    public static Account authenticate(String username, String authenticateId, String password, AuthContext.Protocol protocol, 
            String origRemoteIp, String userAgent)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account authAccount = prov.get(Provisioning.AccountBy.name, authenticateId);
        if (authAccount == null) {
            ZimbraLog.account.info("authentication failed for " + authenticateId + " (no such account)");
            return null;
        }

        // authenticate the authentication principal
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, origRemoteIp);
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, authenticateId);
        authCtxt.put(AuthContext.AC_USER_AGENT, userAgent);
        prov.authAccount(authAccount, password, protocol, authCtxt);

        return authorize(authAccount, username, true);
    }

    public static Account authenticateKrb5(String username, String principal) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account authAccount = prov.get(Provisioning.AccountBy.krb5Principal, principal);
        if (authAccount == null) {
            ZimbraLog.account.warn("authentication failed (no account associated with Kerberos principal " + principal + ')');
            return null;
        }
        
        Account targetAcct = authorize(authAccount, username, true);
        if (targetAcct != null)
            prov.accountAuthed(authAccount);
        return targetAcct;
    }

    public static Account authenticateZToken(String username, String authenticateId, String authtoken) throws ServiceException {
        if (authenticateId == null || authenticateId.equals(""))
            return null;

        // validate the auth token
        Provisioning prov = Provisioning.getInstance();
        AuthToken at;
        try {
            at = ZimbraAuthToken.getAuthToken(authtoken);
        } catch (AuthTokenException e) {
            return null;
        }
        
        Account authTokenAccount = null;
        try {
            authTokenAccount = AuthProvider.validateAuthToken(prov, at, false);
        } catch (ServiceException e) {
            return null;
        }

        // make sure that the authentication account is valid
        Account authAccount = prov.get(Provisioning.AccountBy.name, authenticateId, at);
        if (authAccount == null)
            return null;

        // make sure the auth token belongs to authenticatedId
        if (!at.getAccountId().equalsIgnoreCase(authAccount.getId()))
            return null;

        // if necessary, check that the authenticated user can authorize as the target user
        Account targetAcct = authorize(authAccount, username, AuthToken.isAnyAdmin(at));
        if (targetAcct != null)
            prov.accountAuthed(authAccount);
        return targetAcct;
    }

    private static Account authorize(Account authAccount, String username, boolean asAdmin) throws ServiceException {
        if (username == null || username.length() == 0)
            return authAccount;

        Provisioning prov = Provisioning.getInstance();
        Account userAcct = prov.get(Provisioning.AccountBy.name, username);
        if (userAcct == null) {
            // if username not found, check username again using the domain associated with the authorization account
            int i = username.indexOf('@');
            if (i != -1) {
                String domain = authAccount.getDomainName();
                if (domain != null) {
                    username = username.substring(0, i) + '@' + domain;
                    userAcct = prov.get(Provisioning.AccountBy.name, username);
                }
            }
        }
        if (userAcct == null) {
            ZimbraLog.account.info("authorization failed for " + username + " (account not found)", username);
            return null;
        }

        // check whether the authenticated user is able to access the target
        if (!authAccount.getId().equals(userAcct.getId()) && !AccessManager.getInstance().canAccessAccount(authAccount, userAcct, asAdmin)) {
            ZimbraLog.account.warn("authorization failed for " + username + " (authenticated user " + authAccount.getName() + " has insufficient rights)");
            return null;
        }
        return userAcct;
    }
}
