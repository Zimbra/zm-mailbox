package com.zimbra.cs.account.accesscontrol;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DomainAccessManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightChecker.EffectiveACL;
import com.zimbra.cs.mailbox.ACL;

/*
 * subclass DomainAccessManager for now
 * will replace DomainAccessManager after ACL based access controls are fully implemented.
 * 
 */
public class AclAccessManager extends DomainAccessManager {
    
    public AclAccessManager() throws ServiceException {
        // initialize RightManager
        RightManager.getInstance();
    }
    
    public boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) throws ServiceException {
        if (super.canAccessAccount(at, target, asAdmin))
            return true;
        else
            return canPerform(at, target, UserRight.RT_loginAs, asAdmin, false);
    }
    
    public boolean canAccessAccount(AuthToken at, Account target) throws ServiceException {
        return canAccessAccount(at, target, true);
    }
    
    public boolean canAccessAccount(Account credentials, Account target, boolean asAdmin) throws ServiceException {
        if (super.canAccessAccount(credentials, target, asAdmin))
            return true;
        else
            return canPerform(credentials, target, UserRight.RT_loginAs, asAdmin, false);
    }
    
    public boolean canAccessAccount(Account credentials, Account target) throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }
    
    public boolean canPerform(Account grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            if (grantee == null)
                grantee = ACL.ANONYMOUS_ACCT;

            // 1. always allow self
            if (target instanceof Account) {
                if (((Account)target).getId().equals(grantee.getId()))
                    return true;
            }
            
            // 2. check admin access - if the right being asked for is not loginAs
            if (rightNeeded != UserRight.RT_loginAs) {
                if (target instanceof Account) {
                    if (canAccessAccount(grantee, (Account)target, asAdmin))
                        return true;
                }
            }
            
            Provisioning prov = Provisioning.getInstance();
            
            // 3. check ACL
            List<EffectiveACL> effectiveACLs = TargetType.expandTarget(prov, target, rightNeeded);
            if (effectiveACLs.size() > 0)
                return RightChecker.canDo(effectiveACLs, grantee, rightNeeded, null);
            else {
                // no ACL, see if there is a configured default 
                Boolean defaultValue = rightNeeded.getDefault();
                if (defaultValue != null)
                    return defaultValue.booleanValue();
                
                // no configured default, return default requested by the callsite
                return defaultGrant;
            }
                
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " + 
                                   "grantee=" + grantee.getName() + 
                                   ", target=" + target.getLabel() + 
                                   ", right=" + rightNeeded.getName() + 
                                   " => denied", e);
        }
        return false;
    }
    
    public boolean canPerform(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            Account granteeAcct;
            if (grantee == null)
                granteeAcct = ACL.ANONYMOUS_ACCT;
            else if (grantee.isZimbraUser())
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
            else
                granteeAcct = new ACL.GuestAccount(grantee);
            
            return canPerform(granteeAcct, target, rightNeeded, asAdmin, defaultGrant);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " +
                                   "grantee=" + grantee.getAccountId() +
                                   ", target=" + target.getLabel() +
                                   ", right=" + rightNeeded.getName() +
                                   " => denied", e);
        }
        
        return false;
    }

    public boolean canPerform(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            Account granteeAcct = null;
            
            if (granteeEmail != null)
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, granteeEmail);
            if (granteeAcct == null)
                granteeAcct = ACL.ANONYMOUS_ACCT;
            
            return canPerform(granteeAcct, target, rightNeeded, asAdmin, defaultGrant);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " + 
                                   "grantee=" + granteeEmail + 
                                   ", target=" + target.getLabel() + 
                                   ", right=" + rightNeeded.getName() + 
                                   " => denied", e);
        }
        
        return false;
    }


}
