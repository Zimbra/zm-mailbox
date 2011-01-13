/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;

public class AccessControlUtil {
    
    public static boolean isGlobalAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false));
    }
    
    static boolean isDelegatedAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false));
    }
    
    static public Account authTokenToAccount(AuthToken authToken, Right rightNeeded) {
        Account granteeAcct = null;
        try {
            
            if (authToken == null) {
                if (rightNeeded.isUserRight())
                    granteeAcct = GuestAccount.ANONYMOUS_ACCT;
            } else if (authToken.isZimbraUser())
                granteeAcct = authToken.getAccount();
            else {
                if (rightNeeded.isUserRight())
                    granteeAcct = new GuestAccount(authToken);
            }
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("unable to get account from auth token, id=: " + authToken.getAccountId(), e);
        }
        
        return granteeAcct;
    }
    
    static public Account emailAddrToAccount(String emailAddr, Right rightNeeded) {
        Account granteeAcct = null;
        try {
            if (emailAddr != null)
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, emailAddr);
            if (granteeAcct == null) {
                if (rightNeeded.isUserRight())
                    granteeAcct = GuestAccount.ANONYMOUS_ACCT;
            }
            
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("unable to get account from email address: " + emailAddr, e);
        }
        
        return granteeAcct;
    }
}
