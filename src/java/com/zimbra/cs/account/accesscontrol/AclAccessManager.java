package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DomainAccessManager;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
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
    
    public boolean canPerform(Account grantee, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return hasRight(grantee, target, rightNeeded, asAdmin, defaultGrant);
    }
    
    public boolean canPerform(AuthToken grantee, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return hasRight(grantee, target, rightNeeded, asAdmin, defaultGrant);
    }
    
    public boolean canPerform(String granteeEmail, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return hasRight(granteeEmail, target, rightNeeded, asAdmin, defaultGrant);
    }
    
    private boolean hasRight(Account grantee, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            if (grantee == null)
                grantee = ACL.ANONYMOUS_ACCT;

            // 1. always allow self
            if (grantee != null) {
                if (target.getId().equals(grantee.getId()))
                    return true;
            }
            
            // 2. check admin access - if the right being asked for is not loginAs
            if (rightNeeded != UserRight.RT_loginAs) {
                if (target instanceof Account) {
                    if (canAccessAccount(grantee, (Account)target, asAdmin))
                        return true;
                }
            }
            
            // 3. check ACL
            ZimbraACL acl = PermUtil.getACL(target);
            if (acl != null && acl.containsRight(rightNeeded))
                return acl.hasRight(grantee, rightNeeded);
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
    
    private boolean hasRight(AuthToken grantee, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            Account granteeAcct;
            if (grantee == null)
                granteeAcct = ACL.ANONYMOUS_ACCT;
            else if (grantee.isZimbraUser())
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
            else
                granteeAcct = new ACL.GuestAccount(grantee);
            
            return hasRight(granteeAcct, target, rightNeeded, asAdmin, defaultGrant);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " +
                                   "grantee=" + grantee.getAccountId() +
                                   ", target=" + target.getLabel() +
                                   ", right=" + rightNeeded.getName() +
                                   " => denied", e);
        }
        
        return false;
    }

    private boolean hasRight(String granteeEmail, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            Account granteeAcct = null;
            
            if (granteeEmail != null)
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, granteeEmail);
            if (granteeAcct == null)
                granteeAcct = ACL.ANONYMOUS_ACCT;
            
            return hasRight(granteeAcct, target, rightNeeded, asAdmin, defaultGrant);
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
