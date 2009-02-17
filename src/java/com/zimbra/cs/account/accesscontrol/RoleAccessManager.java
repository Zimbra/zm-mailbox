package com.zimbra.cs.account.accesscontrol;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ldap.LdapUtil;
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
        // there is no such thing as domain admin
        return false;
    }
    
    @Override
    public boolean canAccessAccount(AuthToken at, Account target,
            boolean asAdmin) throws ServiceException {
        if (asAdmin)
            return canDo(at, target, AdminRight.R_adminLoginAs, asAdmin, false);
        else
            return canDo(at, target, UserRight.R_loginAs, asAdmin, false);
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target)
            throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target,
            boolean asAdmin) throws ServiceException {
        if (asAdmin)
            return canDo(credentials, target, AdminRight.R_adminLoginAs, asAdmin, false);
        else
            return canDo(credentials, target, UserRight.R_loginAs, asAdmin, false);
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
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return canDo(grantee, target, rightNeeded, asAdmin, defaultGrant, null);
    }
    
    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return canDo(grantee, target, rightNeeded, asAdmin, defaultGrant, null);
    }
    
    @Override
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return canDo(granteeEmail, target, rightNeeded, asAdmin, defaultGrant, null);
    }
    
    @Override
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant, ViaGrant via) {
        
        // always allow system admin access
        if (RightChecker.isSystemAdmin(grantee, asAdmin))
            return true;
        
        return checkPresetRight(grantee, target, rightNeeded, false, asAdmin, defaultGrant, via);
    }
    
    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant, ViaGrant via) {
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
            
            return canDo(granteeAcct, target, rightNeeded, asAdmin, defaultGrant, via);
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
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant, ViaGrant via) {
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
            
            return canDo(granteeAcct, target, rightNeeded, asAdmin, defaultGrant, via);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " + 
                                   "grantee=" + granteeEmail + 
                                   ", target=" + target.getLabel() + 
                                   ", right=" + rightNeeded.getName() + 
                                   " => denied", e);
        }
        
        return false;
    }
    
    @Override
    public boolean canGetAttrs(Account grantee, Entry target, Set<String> attrsNeeded, boolean asAdmin) throws ServiceException {
        if (RightChecker.isSystemAdmin(grantee, asAdmin))
            return true;
        
        return canGetAttrsInternal(grantee, target, attrsNeeded, false);
    }
    
    @Override
    public boolean canGetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return canGetAttrs(getAccountFromAuthToken(grantee), target, attrs, asAdmin);
    }
    
    
    @Override
    // this API does not check constraints
    public boolean canSetAttrs(Account grantee, Entry target, Set<String> attrsNeeded, boolean asAdmin) throws ServiceException {
        if (RightChecker.isSystemAdmin(grantee, asAdmin))
            return true;
        
        return canSetAttrsInternal(grantee, target, attrsNeeded, false);
    }
    
    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return canSetAttrs(getAccountFromAuthToken(grantee), target, attrs, asAdmin);
    }
    
    @Override
    // this API does check constraints
    public boolean canSetAttrs(Account grantee, Entry target, Map<String, Object> attrsNeeded, boolean asAdmin) throws ServiceException {
        if (RightChecker.isSystemAdmin(grantee, asAdmin))
            return true;
        
        RightChecker.AllowedAttrs allowedAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, false);
        return RightChecker.canSetAttrs(allowedAttrs, grantee, target, attrsNeeded);
    }
    
    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        return canSetAttrs(getAccountFromAuthToken(grantee), target, attrs, asAdmin);
    }
    
    public boolean canSetAttrsOnCreate(AuthToken grantee, TargetType targetType, String entryName, Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        DomainBy domainBy = null;
        String domainStr = null;
        CosBy cosBy = null;
        String cosStr = null;
        
        if (targetType == TargetType.account ||
            targetType == TargetType.calresource ||
            targetType == TargetType.dl) {
            String parts[] = EmailUtil.getLocalPartAndDomain(entryName);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+entryName, null);
            
            domainBy = DomainBy.name;
            domainStr = parts[1];
        }
        
        if (targetType == TargetType.account ||
            targetType == TargetType.calresource) {
            cosStr = (String)attrs.get(Provisioning.A_zimbraCOSId);
            if (cosStr != null) {
                if (LdapUtil.isValidUUID(cosStr))
                    cosBy = cosBy.id;
                else
                    cosBy = cosBy.name;
            }
        }
        
        Entry target = RightChecker.createPseudoTarget(Provisioning.getInstance(),
                                                      targetType, 
                                                      domainBy, domainStr,
                                                      cosBy, cosStr);
        return canSetAttrs(grantee, target, attrs, asAdmin);
    }
    
    @Override
    public boolean canPerform(Account grantee, Entry target, 
                              Right rightNeeded, boolean canDelegateNeeded, 
                              Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant) throws ServiceException {
        
        if (RightChecker.isSystemAdmin(grantee, asAdmin))
            return true;
        
        boolean allowed = false;
        if (rightNeeded.isPresetRight()) {
            allowed = checkPresetRight(grantee, target, rightNeeded, canDelegateNeeded, asAdmin, false, viaGrant);
        
        } else if (rightNeeded.isAttrRight()) {
            AttrRight attrRight = (AttrRight)rightNeeded;
            allowed = checkAttrRight(grantee, target, (AttrRight)rightNeeded, canDelegateNeeded, attrs, asAdmin);
            
        } else if (rightNeeded.isComboRight()) {
            // throw ServiceException.FAILURE("checking right for combo right is not supported", null);
            
            ComboRight comboRight = (ComboRight)rightNeeded;
            // check all directly and indirectly contained rights
            for (Right right : comboRight.getAllRights()) {
                // via is not set for combo right. maybe we should just get rid of via 
                if (!canPerform(grantee, target, right, canDelegateNeeded, attrs, asAdmin, null)) 
                    return false;
            }
            allowed = true;
        }
        
        return allowed;
    }
    
    // all user and admin preset rights go through here 
    private boolean checkPresetRight(Account grantee, Entry target, 
                                     Right rightNeeded, boolean canDelegateNeeded, 
                                     boolean asAdmin, boolean defaultGrant, ViaGrant via) {
        try {
            if (grantee == null) {
                if (canDelegateNeeded)
                    return false;
                
                if (rightNeeded.isUserRight())
                    grantee = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            }

            // 1. always allow self for user right, self can also delegate(i.e. grant) the right
            if (rightNeeded.isUserRight() && target instanceof Account) {
                if (((Account)target).getId().equals(grantee.getId()))
                    return true;
            }
            
            // 3. check ACL
            Boolean result = RightChecker.checkPresetRight(grantee, target, rightNeeded, canDelegateNeeded, via);
            if (result != null)
                return result.booleanValue();
            else {
                if (canDelegateNeeded)
                    return false;
                
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
    
    private boolean checkAttrRight(Account grantee, Entry target, 
                                   AttrRight rightNeeded, boolean canDelegateNeeded, 
                                   Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        
        TargetType targetType = TargetType.getTargetType(target);
        if (!RightChecker.rightApplicableOnTargetType(targetType, rightNeeded, canDelegateNeeded))
            return false;
        
        boolean allowed = false;
        
        if (rightNeeded.getRightType() == Right.RightType.getAttrs) {
            Set<String> attrsToGet = rightNeeded.getAttrs();
            allowed = canGetAttrsInternal(grantee, target, attrsToGet, canDelegateNeeded);
        } else {
            if (attrs == null || attrs.isEmpty()) {
                // no attr/value map, just check if all attrs in the right are covered (constraints are not checked)
                Set<String> attrsToSet = rightNeeded.getAttrs();
                allowed = canSetAttrsInternal(grantee, target, attrsToSet, canDelegateNeeded);
            } else {
                // attr/value map is provided, check it (constraints are checked)
                
                // sanity check, we should *not* be needing "can delegate"
                if (canDelegateNeeded)
                    throw ServiceException.FAILURE("internal error", null);
                
                allowed = canSetAttrs(grantee, target, attrs, asAdmin);
            }
        }
        
        return allowed;
    }
    
    private RightChecker.AllowedAttrs canAccessAttrs(Account grantee, Entry target, AdminRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
        // Do NOT check for self.  If an admin auth as an admin and want to get/set  
        // his own attrs, he has to have the proper right to do so.
            
        // check ACL
        return RightChecker.canAccessAttrs(grantee, target, rightNeeded, canDelegateNeeded);
    }
    
    private boolean canGetAttrsInternal(Account grantee, Entry target, Set<String> attrsNeeded, boolean canDelegateNeeded) throws ServiceException {
        RightChecker.AllowedAttrs allowedAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_GET_ATTRS, canDelegateNeeded);
        return RightChecker.canAccessAttrs(allowedAttrs, attrsNeeded);
    }
    
    private boolean canSetAttrsInternal(Account grantee, Entry target, Set<String> attrsNeeded, boolean canDelegateNeeded) throws ServiceException {
        RightChecker.AllowedAttrs allowedAttrs = canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, canDelegateNeeded);
        return RightChecker.canAccessAttrs(allowedAttrs, attrsNeeded);
    }

    //
    // util methods
    //
    
    /*
     * get the authed account from an auth token
     */
    private Account getAccountFromAuthToken(AuthToken authToken) throws ServiceException {
        String acctId = authToken.getAccountId();
        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.id, acctId);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);
        
        return acct;
    }

    
    //
    // end util methods
    //
    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
