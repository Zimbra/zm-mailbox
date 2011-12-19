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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.names.NameUtil;

public class DomainAccessManager extends AccessManager {

    public boolean isDomainAdminOnly(AuthToken at) {
        return at.isDomainAdmin() && !at.isAdmin();
    }

    @Override
    public boolean isAdequateAdminAccount(Account acct) {
        return acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false) ||
               acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
    }
    
    public boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        
        checkDomainStatus(target);
        
        if (asAdmin && at.isAdmin()) return true;
        if (isParentOf(at, target)) return true;
        if (!(asAdmin && at.isDomainAdmin())) return false;
        // don't allow a domain-only admin to access a global admin's account
        if (target.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false)) return false;
        Provisioning prov = Provisioning.getInstance();
        return getDomain(at).getId().equals(prov.getDomain(target).getId());
    }
    
    public boolean canAccessAccount(AuthToken at, Account target) throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    /** Returns whether the specified account's credentials are sufficient
     *  to perform operations on the target account.  This occurs when the
     *  credentials belong to an admin or when the credentials are for an
     *  appropriate domain admin.  <i>Note: This method checks only for admin
     *  access, and passing the same account for <code>credentials</code> and
     *  <code>target</code> will not succeed for non-admin accounts.</i>
     * @param credentials  The authenticated account performing the action. 
     * @param target       The target account for the proposed action. 
     * @param asAdmin      If the authenticated account is acting as an admin accunt*/
    public boolean canAccessAccount(Account credentials, Account target, boolean asAdmin) throws ServiceException {
        if (credentials == null)
            return false;
        
        checkDomainStatus(target);
        
        // admin auth account will always succeed
        if (asAdmin && credentials.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false))
            return true;
        // parent auth account will always succeed
        if (isParentOf(credentials, target))
            return true;
        // don't allow access if the authenticated account is not acting as an admin
        if (!asAdmin)
            return false;
        // don't allow a domain-only admin to access a global admin's account
        if (target.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false))
            return false;
        // domain admins succeed if the target is in the same domain
        if (target.getDomainName() != null && target.getDomainName().equals(credentials.getDomainName()))
            return credentials.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        // everyone else is out of luck
        return false;
    }
    
    public boolean canAccessAccount(Account credentials, Account target) throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }

    private boolean canAccessDomainInternal(AuthToken at, String domainName) throws ServiceException {
        if (at.isAdmin()) {
            return true;
        }
        if (!at.isDomainAdmin()) {
            return false;
        }
        return getDomain(at).getName().equalsIgnoreCase(domainName);
    }

    public boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        checkDomainStatus(domainName);
        return canAccessDomainInternal(at, domainName);
    }

    public boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        checkDomainStatus(domain);
        return canAccessDomainInternal(at, domain.getName());
    }
    
    public boolean canAccessCos(AuthToken at, Cos cos) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        
        if (at.isAdmin()) return true;
        if (!at.isDomainAdmin()) return false;
        
        String cosId = cos.getId();
        
        Domain domain = getDomain(at);
        Set<String> allowedCoses = domain.getMultiAttrSet(Provisioning.A_zimbraDomainCOSMaxAccounts);
        for (String c : allowedCoses) {
            String[] parts = c.split(":");
            if (parts.length != 2)
                continue;  // bad value skip
            String id = parts[0];
            if (id.equals(cosId))
                return true;
        }
        return false;
    }
    

    @Override
    public boolean canCreateGroup(AuthToken at, String groupEmail)
            throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canCreateGroup(Account credentials, String groupEmail)
            throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canAccessGroup(AuthToken at, Group group)
            throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canAccessGroup(Account credentials, Group group, boolean asAdmin)
            throws ServiceException {
        return false;
    }

    public boolean canAccessEmail(AuthToken at, String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        
        // check for family mailbox
        Account targetAcct = Provisioning.getInstance().get(Key.AccountBy.name, email, at);
        if (targetAcct != null) {
            if (isParentOf(at, targetAcct))
                return true;
        }
        return canAccessDomain(at, parts[1]);
    }

    public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException {
        if (!canAccessAccount(at,  targetAccount))
            return false;
        
        return canSetMailQuota(at, targetAccount, mailQuota);
    }
    
    // public static because of bug 42896.
    // change back to non-static protected when we support constraints on a per admin basis
    public static boolean canSetMailQuota(AuthToken at, Account targetAccount, long quota) throws ServiceException {
        if (at.isAdmin()) return true;
        
        Account adminAccount = Provisioning.getInstance().get(Key.AccountBy.id,  at.getAccountId(), at);
        if (adminAccount == null) return false;

        // 0 is unlimited
        long maxQuota = adminAccount.getLongAttr(Provisioning.A_zimbraDomainAdminMaxMailQuota, -1);

        // return true if they can set quotas to anything
        if (maxQuota == 0)
            return true;

        if ((maxQuota == -1) ||    // they don't permsission to change any quotas
            (quota == 0) ||        // they don't have permission to assign unlimited quota
            (quota > maxQuota)     // the quota they are tying to assign is too big
           ) {
            ZimbraLog.account.warn(String.format("invalid attempt to change quota: admin(%s) account(%s) quota(%d) max(%d)",
                    adminAccount.getName(), targetAccount.getName(), quota, maxQuota));
            return false;
        } else {
            return true;    
        }
    }
    
    /* ===========================================================================================
     * ACL based access methods
     * 
     * - not supported by DomainAccessManager
     * - DomainAccessManager will be retired after ACL based access control is fully implemented.
     * 
     * ===========================================================================================
     */
    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        return false;
    }
    
    @Override
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        return false;
    }
    
    @Override
    public boolean canDo(String grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        return false;
    }

    @Override
    public boolean canGetAttrs(Account grantee,   Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canGetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canSetAttrs(Account grantee,   Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canSetAttrs(Account grantee,   Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        return false;
    }
    
    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        return false;
    }
    
}
