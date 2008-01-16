/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Set;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;

public class DomainAccessManager extends AccessManager {

    public boolean isDomainAdminOnly(AuthToken at) {
        return at.isDomainAdmin() && !at.isAdmin();
    }

    public boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        
        accessDomain(target);
        
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
        
        accessDomain(target);
        
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
        if (at.isAdmin()) return true;
        if (!at.isDomainAdmin()) return false;
        return getDomain(at).getName().equalsIgnoreCase(domainName);
    }

    public boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        accessDomain(domainName);
        return canAccessDomainInternal(at, domainName);
    }

    public boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        accessDomain(domain);
        return canAccessDomainInternal(at, domain.getName());
    }

    public boolean canAccessEmail(AuthToken at, String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        
        // check for family mailbox
        Account targetAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, email);
        if (targetAcct != null) {
            if (isParentOf(at, targetAcct))
                return true;
        }
        return canAccessDomain(at, parts[1]);
    }

    public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long quota) throws ServiceException {
        if (!canAccessAccount(at,  targetAccount))
            return false;

        if (at.isAdmin()) return true;
        
        Account adminAccount = Provisioning.getInstance().get(Provisioning.AccountBy.id,  at.getAccountId());
        if (adminAccount == null) return false;

        // 0 is unlimited
        long maxQuota = adminAccount.getLongAttr(Provisioning.A_zimbraDomainAdminMaxMailQuota, -1);

        // return true if they can set quotas to anything
        if (maxQuota == 0)
            return true;

        if (    (maxQuota == -1) ||    // they don't permsission to change any quotas
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
    
    private boolean isParentOf(AuthToken at, Account target) throws ServiceException {
        
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
    private boolean isParentOf(Account credentials, Account target) {
        
        Set<String> childAccts = credentials.getMultiAttrSet(Provisioning.A_zimbraChildAccount);
        String targetId = target.getId();
        
        if (childAccts.contains(targetId))
            return true;

        return false;
    }
    
    protected void accessDomain(Account acct) throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomain(acct);
        accessDomain(domain);
    }
    
    private void accessDomain(String domainName) throws ServiceException {
        Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
        accessDomain(domain);
    }
    
    private void accessDomain(Domain domain) throws ServiceException {
        if (domain != null) {
            if (domain.isSuspended() || domain.isShutdown())
                throw ServiceException.PERM_DENIED("domain is " + domain.getDomainStatus());
        }
    }
}
