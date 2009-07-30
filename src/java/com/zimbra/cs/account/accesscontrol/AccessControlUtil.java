package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class AccessControlUtil {
    
    public static boolean isGlobalAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false));
    }
    
    static boolean isDelegatedAdmin(Account acct, boolean asAdmin) {
        return (asAdmin && acct != null && acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false));
    }
    
    /**
     * entry point for each and every ACL checking calls.
     * 
     * Currently, the only check is if the authed account is a system admin.
     * 
     * @param authedAcct
     * @param target
     * @return
     * @throws ServiceException
     */
    public static Boolean checkHardRules(Account authedAcct, boolean asAdmin, Entry target, Right right) throws ServiceException {
        if (AccessControlUtil.isGlobalAdmin(authedAcct, asAdmin)) {
            return Boolean.TRUE;
        } else {
            boolean isAdminRight = (right == null || !right.isUserRight());
            
            // We are checking an admin right
            if (isAdminRight) {
                // 1. ensure the authed account must be a delegated admin
                if (!AccessControlUtil.isDelegatedAdmin(authedAcct, asAdmin))
                    throw ServiceException.PERM_DENIED("not an eligible admin account");
                
                // 2. don't allow a delegated-only admin to access a global admin's account,
                //    no matter how much rights it has
                if (target instanceof Account) {
                    if (AccessControlUtil.isGlobalAdmin((Account)target, true))
                        // return Boolean.FALSE;
                        throw ServiceException.PERM_DENIED("delegated admin is not allowed to access a global admin's account");
                }
            }
        }
        
        // hard rules are not applicable
        return null;
    }
}
