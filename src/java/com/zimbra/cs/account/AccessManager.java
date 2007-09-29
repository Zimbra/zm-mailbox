/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning.AccountBy;

public abstract class AccessManager {

    private static AccessManager sManager = new DomainAccessManager();
    
    public static AccessManager getInstance() {
        return sManager;
    }
    
    public abstract boolean isDomainAdminOnly(AuthToken at);

    public Account getAccount(AuthToken at) throws ServiceException {
        return Provisioning.getInstance().get(AccountBy.id, at.getAccountId());
    }

    public Domain getDomain(AuthToken at) throws ServiceException {
        return Provisioning.getInstance().getDomain(getAccount(at));
    }

    public abstract boolean canAccessAccount(AuthToken at, Account target) throws ServiceException;

    /** @return Returns whether the specified account's credentials are sufficient
     *  to perform operations on the target account.  <i>Note: This method
     *  checks only for admin access, and passing the same account for
     *  <code>credentials</code> and <code>target</code> will not succeed
     *  for non-admin accounts.</i>
     * @param credentials  The authenticated account performing the action. 
     * @param target       The target account for the proposed action. */
    public abstract boolean canAccessAccount(Account credentials, Account target);

    public abstract boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException;

    public abstract boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException;

    public abstract boolean canAccessEmail(AuthToken at, String email) throws ServiceException;

    public abstract boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException;
}
