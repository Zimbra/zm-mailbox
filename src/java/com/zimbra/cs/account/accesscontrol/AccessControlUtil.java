/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.Provisioning;

public class AccessControlUtil {

    public static boolean isGlobalAdmin(Account acct) {
        return isGlobalAdmin(acct, true);
    }

    public static boolean isGlobalAdmin(MailTarget target, boolean asAdmin) {
        if (!asAdmin || target == null || !(target instanceof Account)) {
            return false;
        }
        return ((Account)target).isIsAdminAccount();
    }

    public static boolean isGlobalAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.isIsAdminAccount());
    }

    static boolean isDelegatedAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.isIsDelegatedAdminAccount());
    }

    static public Account authTokenToAccount(AuthToken authToken, Right rightNeeded) {
        Account granteeAcct = null;
        try {

            if (authToken == null) {
                if (rightNeeded.isUserRight()) {
                    granteeAcct = GuestAccount.ANONYMOUS_ACCT;
                }
            } else if (authToken.isZimbraUser()) {
                granteeAcct = authToken.getAccount();
            } else {
                if (rightNeeded.isUserRight()) {
                    granteeAcct = new GuestAccount(authToken);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("unable to get account from auth token, id=: " +
                    authToken.getAccountId(), e);
        }

        return granteeAcct;
    }

    static public MailTarget emailAddrToMailTarget(String emailAddr, Right rightNeeded) {
        MailTarget grantee = null;
        if (emailAddr != null) {
            try {
                grantee = Provisioning.getInstance().get(Key.AccountBy.name, emailAddr);
            } catch (ServiceException e) {
            }
            if (grantee == null) {
                try {
                    grantee = Provisioning.getInstance().get(Key.DistributionListBy.name, emailAddr);
                } catch (ServiceException e) {
                    ZimbraLog.acl.warn("unable to get account or distribution list from email address: " + emailAddr, e);
                }
            }
        }
        if (grantee == null) {
            // not an internal user or distribution list
            if (rightNeeded.isUserRight()) {
                if (emailAddr != null) {
                    grantee = new GuestAccount(emailAddr, null);
                } else {
                    grantee = GuestAccount.ANONYMOUS_ACCT;
                }
            }
        }
        return grantee;
    }

    static public Account emailAddrToAccount(String emailAddr, Right rightNeeded) {
        Account granteeAcct = null;
        try {
            if (emailAddr != null) {
                granteeAcct = Provisioning.getInstance().get(Key.AccountBy.name, emailAddr);
            }

            if (granteeAcct == null) {
                // not an internal user
                if (rightNeeded.isUserRight()) {
                    if (emailAddr != null) {
                        granteeAcct = new GuestAccount(emailAddr, null);
                    } else {
                        granteeAcct = GuestAccount.ANONYMOUS_ACCT;
                    }
                }
            }

        } catch (ServiceException e) {
            ZimbraLog.acl.warn("unable to get account from email address: " + emailAddr, e);
        }

        return granteeAcct;
    }
}
