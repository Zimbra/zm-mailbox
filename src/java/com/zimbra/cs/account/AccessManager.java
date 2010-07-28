/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;

public abstract class AccessManager {

    private static AccessManager sManager;
    
    public static AccessManager getInstance() {
        if (sManager == null) {
            
            // 1. try getting it from global config attr zimbraAdminAccessControlMech.
            //    zimbraAdminAccessControlMech can only be set to an access manager that is 
            //    "admin console capable".  That is, it must implement the AdminConsoleCapable
            //    interface.
            try {
                Config config = Provisioning.getInstance().getConfig();
                String accessManager = config.getAttr(Provisioning.A_zimbraAdminAccessControlMech);
                if (accessManager != null) {
                    ZAttrProvisioning.AdminAccessControlMech am = ZAttrProvisioning.AdminAccessControlMech.fromString(accessManager);
                    if (am == ZAttrProvisioning.AdminAccessControlMech.acl)
                        sManager = new com.zimbra.cs.account.accesscontrol.ACLAccessManager();
                    else if (am == ZAttrProvisioning.AdminAccessControlMech.global)
                        sManager = new com.zimbra.cs.account.accesscontrol.GlobalAccessManager();
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to determine access manager from global config attribute " + 
                        Provisioning.A_zimbraAdminAccessControlMech + ", fallback to use LC key " +
                        LC.zimbra_class_accessmanager.key(), e);
            }
            
            //
            // 2. if not set, fallback to localconfig key zimbra_class_accessmanager.
            //    zimbra_class_accessmanager can be set to any class that is an
            //    AccessManager.  No guarantee it will support the admin console.
            //    Allow this path for backward compatibility.
            //
            if (sManager == null) {
                String className = LC.zimbra_class_accessmanager.value();
                
                if (className != null && !className.equals("")) {
                    try {
                        sManager = (AccessManager) Class.forName(className).newInstance();
                    } catch (Exception e) {
                        ZimbraLog.account.error("could not instantiate AccessManager interface of class '" 
                                + className + "'; defaulting to DomainAccessManager", e);
                    }
                }
            }
            
            // 
            // 3. if still null, default to GlobalAccessManager
            //
            if (sManager == null)
            	sManager = new com.zimbra.cs.account.accesscontrol.GlobalAccessManager();
            
            ZimbraLog.account.info("Initialized access manager: " + sManager.getClass().getCanonicalName());
        }
        
        return sManager;
    }
    
    public abstract boolean isDomainAdminOnly(AuthToken at);

    public Account getAccount(AuthToken at) throws ServiceException {
        return Provisioning.getInstance().get(AccountBy.id, at.getAccountId(), at);
    }
    
    protected Account getAdminAccount(AuthToken at) throws ServiceException {
        String adminAcctId = at.getAdminAccountId();
        if (adminAcctId == null)
            return null;
        else
            return Provisioning.getInstance().get(AccountBy.id, adminAcctId, at);
    }

    public Domain getDomain(AuthToken at) throws ServiceException {
        return Provisioning.getInstance().getDomain(getAccount(at));
    }

    public abstract boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) throws ServiceException;
    public abstract boolean canAccessAccount(AuthToken at, Account target) throws ServiceException;

    /** @return Returns whether the specified account's credentials are sufficient
     *  to perform operations on the target account.  <i>Note: This method
     *  checks only for admin access, and passing the same account for
     *  <code>credentials</code> and <code>target</code> will not succeed
     *  for non-admin accounts.</i>
     * @param credentials  The authenticated account performing the action. 
     * @param target       The target account for the proposed action. 
     * @param asAdmin      If the authenticated account is acting as an admin accunt */
    public abstract boolean canAccessAccount(Account credentials, Account target, boolean asAdmin) throws ServiceException;
    public abstract boolean canAccessAccount(Account credentials, Account target) throws ServiceException;

    public abstract boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException;
    public abstract boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException;
    
    public abstract boolean canAccessCos(AuthToken at, Cos cos) throws ServiceException;

    public abstract boolean canAccessEmail(AuthToken at, String email) throws ServiceException;

    public abstract boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException;
    
    /**
     * Returns true if authAccount should be allowed access to private data in appointments owned
     * by targetAccount.  Returns true if authAccount and targetAccount are the same account or if
     * authAccount has admin rights over targetAccount.
     * 
     * @param authAccount
     * @param targetAccount
     * @param asAdmin true if authAccount is authenticated with admin privileges
     * @return
     * @throws com.zimbra.common.service.ServiceException
     */
    public boolean allowPrivateAccess(Account authAccount, Account targetAccount, boolean asAdmin)
    throws ServiceException {
        if (authAccount != null && targetAccount != null) {
            if (authAccount.getId().equalsIgnoreCase(targetAccount.getId()))
                return true;
            if (canAccessAccount(authAccount, targetAccount, asAdmin))
                return true;
        }
        return false;
    }
    
    protected boolean isParentOf(AuthToken at, Account target) throws ServiceException {
        
        /*
         * first, try using the admin account in the token
         */
        /*  uncomment after we allow the parent to grab a delegated auth token.
        try {
            Account adminAcct = getAdminAccount(at);
            if (adminAcct != null) {
                if (isParentOf(adminAcct, target))
                    return true;
            }
        } catch (ServiceException e) {
            // not an admin account 
        }
        */
        Account acct = getAccount(at);
        return isParentOf(acct, target);
    }
    
    /** Returns whether the specified account's credentials indicationg that 
     *  it is the parent account of the target account. 
     *  <i>Note: This method checks only for family parent account access, 
     *  and passing the same account for <code>credentials</code> and
     *  <code>target</code> will not succeed for non-parent accounts.</i>
     * @param credentials  The authenticated account performing the action. 
     * @param target       The target account for the proposed action. */
    protected boolean isParentOf(Account credentials, Account target) {
        
        Set<String> childAccts = credentials.getMultiAttrSet(Provisioning.A_zimbraChildAccount);
        String targetId = target.getId();
        
        if (childAccts.contains(targetId))
            return true;

        return false;
    }
    
    protected void checkDomainStatus(Account acct) throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomain(acct);
        checkDomainStatus(domain);
    }
    
    protected void checkDomainStatus(String domainName) throws ServiceException {
        Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
        checkDomainStatus(domain);
    }
    
    public void checkDomainStatus(Domain domain) throws ServiceException {
        if (domain != null) {
            if (domain.isSuspended() || domain.isShutdown())
                throw ServiceException.PERM_DENIED("domain is " + domain.getDomainStatusAsString());
        }
    }
    
    
    //
    // ACL based methods
    //
    
    /* 
     * ====================
     * user right entrances
     * ====================
     */
    public abstract boolean canDo(Account grantee,     Entry target, Right rightNeeded, boolean asAdmin);
    public abstract boolean canDo(AuthToken grantee,   Entry target, Right rightNeeded, boolean asAdmin);
    public abstract boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin);
    
    
    /* 
     * =====================
     * admin right entrances
     * =====================
     */
    // for admin calls to return the decisive grant that lead to the result 
    public static class ViaGrant {
        private ViaGrant mImpl;
        public void setImpl(ViaGrant impl) { mImpl = impl;}
        public String getTargetType()   { return (mImpl==null)?null:mImpl.getTargetType(); } 
        public String getTargetName()   { return (mImpl==null)?null:mImpl.getTargetName(); } 
        public String getGranteeType()  { return (mImpl==null)?null:mImpl.getGranteeType(); } 
        public String getGranteeName()  { return (mImpl==null)?null:mImpl.getGranteeName(); } 
        public String getRight()        { return (mImpl==null)?null:mImpl.getRight(); } 
        public boolean isNegativeGrant(){ return (mImpl==null)?null:mImpl.isNegativeGrant(); } 
        
        public boolean available() { return mImpl != null; }
    }
    
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin, 
            ViaGrant viaGrant) throws ServiceException {
        return canDo(grantee, target, rightNeeded, asAdmin);
    }
    
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin, 
            ViaGrant viaGrant) throws ServiceException {
        return canDo(grantee, target, rightNeeded, asAdmin);
    }
    
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin, 
            ViaGrant viaGrant) throws ServiceException {
        return canDo(granteeEmail, target, rightNeeded, asAdmin);
    }
    

    /**
     * returns if the specified account's credentials can get the specified attrs on target
     *
     * @param credentials The authenticated account performing the action. 
     * @param target      The target entry.
     * @param attrs       Attrs to get.
     * @param asAdmin     If the authenticated account is acting as an admin account.
     * @return
     * @throws ServiceException
     */
    public abstract boolean canGetAttrs(Account credentials,   Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;
    public abstract boolean canGetAttrs(AuthToken credentials, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;
    

    public interface AttrRightChecker {
        /**
         * returns if the specified attr is allowed
         * 
         * @param attrName
         * @return
         * @throws ServiceException
         */
        public boolean allowAttr(String attrName);
    }
    
    /**
     * returns an AttrRightChecker for the specified target
     * 
     * @param credentials
     * @param target
     * @param asAdmin
     * @return
     * @throws ServiceException
     */
    public AttrRightChecker canGetAttrs(Account credentials,   Entry target, boolean asAdmin) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }
    
    public AttrRightChecker canGetAttrs(AuthToken credentials, Entry target, boolean asAdmin) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }

    
    /**
     * 
     * returns if the specified account's credentials can set the specified attrs on target
     * constraints are not checked
     *
     * @param credentials The authenticated account performing the action. 
     * @param target      The target entry.
     * @param attrs       Attrs to set.
     * @param asAdmin     If the authenticated account is acting as an admin account.
     * @return
     * @throws ServiceException
     */
    public abstract boolean canSetAttrs(Account credentials,   Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;
    public abstract boolean canSetAttrs(AuthToken credentials, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;
    

    /**
     * returns if the specified account's credentials can set the specified attrs to the specified values on target.
     * constraints are checked.
     *
     * @param credentials The authenticated account performing the action.
     * @param target      The target entry.
     * @param attrs       Attrs/values to set.
     * @param asAdmin     If the authenticated account is acting as an admin account.
     * @return
     * @throws ServiceException
     */
    public abstract boolean canSetAttrs(Account credentials,   Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException;
    public abstract boolean canSetAttrs(AuthToken credentials, Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException;
    
    public boolean canSetAttrsOnCreate(Account credentials, TargetType targetType, String entryName, 
            Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }
    
    // for access manager internal use and unittest only, do not call this API, use the canDo API instead.
    public boolean canPerform(Account credentials, Entry target, Right rightNeeded, boolean canDelegate, 
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }
    
    // for access manager internal use and unittest only, do not call this API, use the canDo API instead.
    public boolean canPerform(AuthToken credentials, Entry target, Right rightNeeded, boolean canDelegate, 
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }

}
