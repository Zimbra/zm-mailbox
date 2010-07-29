/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;

public class ACLAccessManager extends AccessManager implements AdminConsoleCapable {

    public ACLAccessManager() throws ServiceException {
        // initialize RightManager
        RightManager.getInstance();
    }
    
    private Account actualTargetForAdminLoginAs(Account target) throws ServiceException {
        if (target.isCalendarResource())
            // need a CalendarResource instance for RightChecker
            return Provisioning.getInstance().get(CalendarResourceBy.id, target.getId());
        else
            return target;
    }
    
    private AdminRight actualRightForAdminLoginAs(Account target) {
        if (target.isCalendarResource())
            return Admin.R_adminLoginCalendarResourceAs;
        else
            return Admin.R_adminLoginAs;
    }
    
    @Override
    public boolean isDomainAdminOnly(AuthToken at) {
        // there is no such thing as domain admin in the realm of ACL checking.
        return false;
    }
    
    @Override
    public boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) throws ServiceException {
         
        checkDomainStatus(target);
        
        if (isParentOf(at, target))
            return true;
        
        if (asAdmin)
            return canDo(at, actualTargetForAdminLoginAs(target), actualRightForAdminLoginAs(target), asAdmin);
        else
            return canDo(at, target, User.R_loginAs, asAdmin);
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target) throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target, boolean asAdmin) throws ServiceException {
        
        checkDomainStatus(target);
        
        if (isParentOf(credentials, target))
            return true;
        
        if (asAdmin)
            return canDo(credentials, actualTargetForAdminLoginAs(target), actualRightForAdminLoginAs(target), asAdmin);
        else
            return canDo(credentials, target, User.R_loginAs, asAdmin);
    }
    
    @Override
    public boolean canAccessAccount(Account credentials, Account target) throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }

    @Override
    public boolean canAccessCos(AuthToken at, Cos cos) throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException {
        // TODO Auto-generated method stub
        // return false;
        
        throw ServiceException.FAILURE("internal error", null);  // should never be called
    }

    @Override
    public boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException {
        // TODO Auto-generated method stub
        // return false;
        
        throw ServiceException.FAILURE("internal error", null);  // should never be called
    }

    @Override
    public boolean canAccessEmail(AuthToken at, String email) throws ServiceException {
        // TODO Auto-generated method stub
        // return false;
        throw ServiceException.FAILURE("internal error", null);  // should never be called
    }

    @Override
    public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException {
        // throw ServiceException.FAILURE("internal error", null);  // should never be called
        
        // for bug 42896, we now have to do the same check on zimbraDomainAdminMaxMailQuota  
        // until we come up with a framework to support constraints on a per admin basis.
        // the following call is ugly!
        return com.zimbra.cs.account.DomainAccessManager.canSetMailQuota(at, targetAccount, mailQuota);
    }
    
    @Override
    /**
     * User right entrance - do not throw
     */
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        try {
            return canDo(grantee, target, rightNeeded, asAdmin, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("right denied", e);
            return false;
        }
    }
    
    @Override
    /**
     * User right entrance - do not throw
     */
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        try {
            return canDo(grantee, target, rightNeeded, asAdmin, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("right denied", e);
            return false;
        }
    }
    
    @Override
    /**
     * User right entrance - do not throw
     */
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin) {
        try {
            return canDo(granteeEmail, target, rightNeeded, asAdmin, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("right denied", e);
            return false;
        }
    }
    
    @Override
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, 
            boolean asAdmin, ViaGrant via) throws ServiceException {
        
        // check hard rules
        Boolean hardRulesResult = AccessControlUtil.checkHardRules(grantee, asAdmin, target, rightNeeded);
        if (hardRulesResult != null)
            return hardRulesResult.booleanValue();
        
        // check pseudo rights
        if (asAdmin) {
            if (rightNeeded == AdminRight.PR_ALWAYS_ALLOW)
                return true;
            else if (rightNeeded == AdminRight.PR_SYSTEM_ADMIN_ONLY)
                return false;
        }
        
        return checkPresetRight(grantee, target, rightNeeded, false, asAdmin, via);
    }
    
    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, 
            boolean asAdmin, ViaGrant via) throws ServiceException {
        try {
            Account granteeAcct = AccessControlUtil.authTokenToAccount(grantee, rightNeeded);
            if (granteeAcct != null)
                return canDo(granteeAcct, target, rightNeeded, asAdmin, via);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("ACL checking failed", e);
        }
        
        return false;
    }

    @Override
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, 
            boolean asAdmin, ViaGrant via) throws ServiceException {
        try {
            Account granteeAcct = AccessControlUtil.emailAddrToAccount(granteeEmail, rightNeeded);
            if (granteeAcct != null)
                return canDo(granteeAcct, target, rightNeeded, asAdmin, via);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("ACL checking failed", e);
        }
        
        return false;
    }
    
    @Override
    public boolean canGetAttrs(Account grantee, Entry target, Set<String> attrsNeeded, boolean asAdmin) throws ServiceException {
        
        // check hard rules
        Boolean hardRulesResult = AccessControlUtil.checkHardRules(grantee, asAdmin, target, null);
        if (hardRulesResult != null)
            return hardRulesResult.booleanValue();
        
        return canGetAttrsInternal(grantee, target, attrsNeeded, false);
    }
    
    @Override
    public boolean canGetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin) 
    throws ServiceException {
        return canGetAttrs(grantee.getAccount(), target, attrs, asAdmin);
    }

    @Override
    public AttrRightChecker canGetAttrs(Account credentials,   Entry target, boolean asAdmin) throws ServiceException {
        Boolean hardRulesResult = AccessControlUtil.checkHardRules(credentials, asAdmin, target, null);
        
        if (hardRulesResult == Boolean.TRUE)
            return AllowedAttrs.ALLOW_ALL_ATTRS();
        else if (hardRulesResult == Boolean.FALSE)
            return AllowedAttrs.DENY_ALL_ATTRS();
        else
            return CheckAttrRight.accessibleAttrs(new Grantee(credentials), target, AdminRight.PR_GET_ATTRS, false);
    }
    
    @Override
    public AttrRightChecker canGetAttrs(AuthToken credentials, Entry target, boolean asAdmin) throws ServiceException {
        return canGetAttrs(credentials.getAccount(), target, asAdmin);
    }
    
    
    @Override
    // this API does not check constraints
    public boolean canSetAttrs(Account grantee, Entry target, Set<String> attrsNeeded, boolean asAdmin) throws ServiceException {
        
        // check hard rules
        Boolean hardRulesResult = AccessControlUtil.checkHardRules(grantee, asAdmin, target, null);
        if (hardRulesResult != null)
            return hardRulesResult.booleanValue();
        
        return canSetAttrsInternal(grantee, target, attrsNeeded, false);
    }
    
    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return canSetAttrs(grantee.getAccount(), target, attrs, asAdmin);
    }
    
    @Override
    // this API does check constraints
    public boolean canSetAttrs(Account granteeAcct, Entry target, Map<String, Object> attrsNeeded, boolean asAdmin) throws ServiceException {
        
        // check hard rules
        Boolean hardRulesResult = AccessControlUtil.checkHardRules(granteeAcct, asAdmin, target, null);
        if (hardRulesResult != null)
            return hardRulesResult.booleanValue();
        
        Grantee grantee = new Grantee(granteeAcct);
        AllowedAttrs allowedAttrs = CheckAttrRight.accessibleAttrs(grantee, target, AdminRight.PR_SET_ATTRS, false);
        return allowedAttrs.canSetAttrs(grantee, target, attrsNeeded);
    }
    
    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        return canSetAttrs(grantee.getAccount(), target, attrs, asAdmin);
    }
    
    public boolean canSetAttrsOnCreate(Account grantee, TargetType targetType, String entryName, 
            Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
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
        
        Entry target = PseudoTarget.createPseudoTarget(Provisioning.getInstance(),
                                                       targetType, 
                                                       domainBy, domainStr, false,
                                                       cosBy, cosStr,
                                                       entryName);
        return canSetAttrs(grantee, target, attrs, asAdmin);
    }
    
    @Override
    public boolean canPerform(Account grantee, Entry target, 
            Right rightNeeded, boolean canDelegateNeeded, 
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant) 
    throws ServiceException {
        
        // check hard rules
        Boolean hardRulesResult = AccessControlUtil.checkHardRules(grantee, asAdmin, target, rightNeeded);
        if (hardRulesResult != null)
            return hardRulesResult.booleanValue();
        
        boolean allowed = false;
        if (rightNeeded.isPresetRight()) {
            allowed = checkPresetRight(grantee, target, rightNeeded, canDelegateNeeded, asAdmin, viaGrant);
        
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
    
    @Override
    public boolean canPerform(AuthToken grantee, Entry target, Right rightNeeded, boolean canDelegate, 
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant) throws ServiceException {
        Account authedAcct = grantee.getAccount();
        return canPerform(authedAcct, target, rightNeeded, canDelegate, 
                          attrs, asAdmin, viaGrant);
    }
    
    
    // all user and admin preset rights go through here 
    private boolean checkPresetRight(Account grantee, Entry target, 
                                     Right rightNeeded, boolean canDelegateNeeded, 
                                     boolean asAdmin, ViaGrant via) {
        try {
            if (grantee == null) {
                if (canDelegateNeeded)
                    return false;
                
                if (rightNeeded.isUserRight())
                    grantee = GuestAccount.ANONYMOUS_ACCT;
                else
                    return false;
            }

            //
            // user right treatment
            //
            if (rightNeeded.isUserRight()) {
                if (target instanceof Account) {
                    // always allow self for user right, self can also delegate(i.e. grant) the right
                    if (((Account)target).getId().equals(grantee.getId()))
                        return true;
                    
                    // check the loginAs right and family access - if the right being asked for is not loginAs
                    if (rightNeeded != Rights.User.R_loginAs)
                        if (canAccessAccount(grantee, (Account)target, asAdmin))
                            return true;
                }
            } else {
                // not a user right, must have a target object
                // if it is a user right, let it fall through to return the default permission.
                if (target == null)
                    return false;
            }
            
            
            //
            // check ACL
            //
            Boolean result = null;
            if (target != null)
                result = CheckPresetRight.check(grantee, target, rightNeeded, canDelegateNeeded, via);
            
            if (result != null && result.booleanValue()) 
                return result.booleanValue();  // // allowed by ACL
            else {
                // either no matching ACL for the right or is now allowed by ACL
                
                if (canDelegateNeeded)
                    return false;
                
                // call the fallback if there is one for the right
                CheckRightFallback fallback = rightNeeded.getFallback();
                if (fallback != null) {
                    Boolean fallbackResult = fallback.checkRight(grantee, target, asAdmin);
                    if (fallbackResult != null)
                        return fallbackResult.booleanValue();
                }
                
                if (result == null) {
                    // no matching ACL for the right, and no callback (or no callback result), 
                    // see if there is a configured default 
                    Boolean defaultValue = rightNeeded.getDefault();
                    if (defaultValue != null)
                        return defaultValue.booleanValue();
                }
            }
                
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("ACL checking failed: " + 
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
        if (!CheckRight.rightApplicableOnTargetType(targetType, rightNeeded, canDelegateNeeded))
            return false;
        
        boolean allowed = false;
        
        if (rightNeeded.getRightType() == Right.RightType.getAttrs) {
            allowed = checkAttrRight(grantee, target, rightNeeded, canDelegateNeeded);
        } else {
            if (attrs == null || attrs.isEmpty()) {
                // no attr/value map, just check if all attrs in the right are covered (constraints are not checked)
                allowed = checkAttrRight(grantee, target, rightNeeded, canDelegateNeeded);
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
    
    private boolean checkAttrRight(Account granteeAcct, Entry target, 
            AttrRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
        AllowedAttrs allowedAttrs = 
            CheckAttrRight.accessibleAttrs(new Grantee(granteeAcct), target, rightNeeded, canDelegateNeeded);
        return allowedAttrs.canAccessAttrs(rightNeeded.getAttrs(), target);
    }
    
    private boolean canGetAttrsInternal(Account granteeAcct, Entry target, 
            Set<String> attrsNeeded, boolean canDelegateNeeded) throws ServiceException {
        AllowedAttrs allowedAttrs = 
            CheckAttrRight.accessibleAttrs(new Grantee(granteeAcct), target, AdminRight.PR_GET_ATTRS, canDelegateNeeded);
        return allowedAttrs.canAccessAttrs(attrsNeeded, target);
    }
    
    private boolean canSetAttrsInternal(Account granteeAcct, Entry target, 
            Set<String> attrsNeeded, boolean canDelegateNeeded) throws ServiceException {
        AllowedAttrs allowedAttrs = 
            CheckAttrRight.accessibleAttrs(new Grantee(granteeAcct), target, AdminRight.PR_SET_ATTRS, canDelegateNeeded);
        return allowedAttrs.canAccessAttrs(attrsNeeded, target);
    }

    // ============
    // util methods
    // ============
    
    // ===========================
    // AdminConsoleCapable methods
    // ===========================
    
    public void getAllEffectiveRights(RightBearer rightBearer, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights result) throws ServiceException {
        CollectAllEffectiveRights.getAllEffectiveRights(rightBearer, expandSetAttrs, expandGetAttrs, result);
    }
    
    public void getEffectiveRights(RightBearer rightBearer, Entry target, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {
        CollectEffectiveRights.getEffectiveRights(rightBearer, target, expandSetAttrs, expandGetAttrs, result);
        
    }
    
    public Set<TargetType> targetTypesForGrantSearch() {
        // we want all target types
        return new HashSet<TargetType>(Arrays.asList(TargetType.values()));
    }

}
