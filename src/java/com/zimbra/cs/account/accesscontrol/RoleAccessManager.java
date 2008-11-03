package com.zimbra.cs.account.accesscontrol;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;

/*
 * TODO: when things are stable:
 *       rename AclAccessManager to DomainAclAccessManager, then
 *       rename RoleAccessManager to AclAccessManager
 */

public class RoleAccessManager extends AccessManager {

    public RoleAccessManager() throws ServiceException {
        // initialize RightManager
        RightManager.getInstance();
    }
    
    @Override
    public boolean isDomainAdminOnly(AuthToken at) {
        /*
         * returning true to essentially trigger all permission checks 
         * for all admins, not just domain admins.  
         * 
         * Should probably retire this call when we will never ever 
         * go back to the domain admin paradigm.
         */
        return true;
    }
    
    @Override
    public boolean canAccessAccount(AuthToken at, Account target,
            boolean asAdmin) throws ServiceException {
        return canPerform(at, target, UserRight.RT_loginAs, asAdmin, false);
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target)
            throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target,
            boolean asAdmin) throws ServiceException {
        return canPerform(credentials, target, UserRight.RT_loginAs, asAdmin, false);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target)
            throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }

    @Override
    public boolean canAccessCos(AuthToken at, String cosId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessDomain(AuthToken at, String domainName)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessDomain(AuthToken at, Domain domain)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessEmail(AuthToken at, String email)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canPerform(Account grantee, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            if (grantee == null) {
                if (rightNeeded.isUserRight())
                    grantee = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            }

            // 1. always allow self
            if (grantee != null) {
                if (target.getId().equals(grantee.getId()))
                    return true;
            }
            
            Provisioning prov = Provisioning.getInstance();
            
            // 2. check ACL
            Set<ZimbraACE> effectiveACEs = TargetType.expandTarget(prov, target, rightNeeded);
            if (effectiveACEs.size() > 0)
                return RightChecker.canDo(effectiveACEs, grantee, rightNeeded);
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
    
    @Override
    public boolean canPerform(AuthToken grantee, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            Account granteeAcct;
            if (grantee == null) {
                if (rightNeeded.isUserRight())
                    granteeAcct = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            } else if (grantee.isZimbraUser())
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
            else {
                if (rightNeeded.isUserRight())
                    granteeAcct = new ACL.GuestAccount(grantee);
                else
                    return false;
            }
            
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

    @Override
    public boolean canPerform(String granteeEmail, NamedEntry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        try {
            Account granteeAcct = null;
            
            if (granteeEmail != null)
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, granteeEmail);
            if (granteeAcct == null) {
                if (rightNeeded.isUserRight())
                    granteeAcct = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            }
            
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
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
