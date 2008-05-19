package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DomainAccessManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;

/*
 * subclass DomainAccessManager for now
 * will replace DomainAccessManager after ACL based access controls are fully implemented.
 * 
 */
public class AclAccessManager extends DomainAccessManager {
    
    public boolean canPerform(Account grantee, Account target, Right rightNeeded, boolean defaultGrant) {
        return hasRight(grantee, target, rightNeeded, defaultGrant);
    }
    
    public boolean canPerform(AuthToken grantee, Account target, Right rightNeeded, boolean defaultGrant) {
        return hasRight(grantee, target, rightNeeded, defaultGrant);
    }
    
    public boolean canPerform(String granteeEmail, Account target, Right rightNeeded, boolean defaultGrant) {
        return hasRight(granteeEmail, target, rightNeeded, defaultGrant);
    }
    
    public boolean canPerform(Account grantee, CalendarResource target, Right rightNeeded, boolean defaultGrant) {
        return hasRight(grantee, target, rightNeeded, defaultGrant);
    }
    
    public boolean canPerform(AuthToken grantee, CalendarResource target, Right rightNeeded, boolean defaultGrant) {
        return hasRight(grantee, target, rightNeeded, defaultGrant);
    }
    
    public boolean canPerform(String granteeEmail, CalendarResource target, Right rightNeeded, boolean defaultGrant) {
        return hasRight(granteeEmail, target, rightNeeded, defaultGrant);
    }
    
    private boolean hasRight(Account grantee, Entry target, Right rightNeeded, boolean defaultGrant) {
        try {
            ZimbraACL acl = target.getACL();
            if (acl != null)
                return acl.hasRight(grantee, rightNeeded);
            else {
                // no ACL, return default requested by the callsite
                return defaultGrant;
            }
                
        } catch (ServiceException e) {
            ZimbraLog.account.warn("failed checking ACL for: " + 
                                   "grantee=" + grantee.getName() + 
                                   "target=" + target.getLabel() + 
                                   "right=" + rightNeeded.toString() + 
                                   "=> denied", e);
        }
        return false;
    }
    
    private boolean hasRight(AuthToken grantee, Entry target, Right rightNeeded, boolean defaultGrant) {
        try {
            Account granteeAcct;
            if (grantee.isZimbraUser())
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
            else
                granteeAcct = new ACL.GuestAccount(grantee);
            
            return hasRight(granteeAcct, target, rightNeeded, defaultGrant);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("failed checking ACL for: " +
                                   "grantee=" + grantee.getAccountId() +
                                   "target=" + target.getLabel() +
                                   "right=" + rightNeeded.toString() +
                                   "=> denied", e);
        }
        
        return false;
    }

    private boolean hasRight(String granteeEmail, Entry target, Right rightNeeded, boolean defaultGrant) {
        try {
            Account granteeAcct = null;
            
            if (granteeEmail != null)
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, granteeEmail);
            if (granteeAcct == null)
                granteeAcct = ACL.ANONYMOUS_ACCT;
            
            return hasRight(granteeAcct, target, rightNeeded, defaultGrant);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("failed checking ACL for: " + 
                                   "grantee=" + granteeEmail + 
                                   "target=" + target.getLabel() + 
                                   "right=" + rightNeeded.toString() + 
                                   "=> denied", e);
        }
        
        return false;
    }


}
