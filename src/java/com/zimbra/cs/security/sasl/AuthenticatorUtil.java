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

    public static Account authenticateKrb5(String username, String principle)
            throws ServiceException, IOException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.krb5Principal, principle);
        if (account == null) return null;
        if (username != null && !principle.equals(username)
                             && !principle.startsWith(username + '@')) {
            Account userAcct = prov.get(Provisioning.AccountBy.name, username);
            if (userAcct == null) return null;
            AccessManager am = AccessManager.getInstance();
            if (!am.canAccessAccount(account, userAcct)) return null;
        }
        return account;
    }
}
