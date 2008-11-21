/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.MemberOf;

import java.util.Map;
import java.util.List;

/**
 * @author schemers
 */
public class Account extends ZAttrAccount  {
    
    public Account(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void addAlias(String alias) throws ServiceException {
        getProvisioning().addAlias(this, alias);
    }

    public void authAccount(String password, String proto) throws ServiceException {
        getProvisioning().authAccount(this, password, proto);
    }

    public void changePassword(String currentPassword, String newPassword) throws ServiceException {
        getProvisioning().changePassword(this, currentPassword, newPassword);
    }

    public void checkPasswordStrength(String password) throws ServiceException {
        getProvisioning().checkPasswordStrength(this, password);
    }

    public List<DataSource> getAllDataSources() throws ServiceException {
        return getProvisioning().getAllDataSources(this);
    }

    public List<Identity> getAllIdentities() throws ServiceException {
        return getProvisioning().getAllIdentities(this);
    }

    public List<MemberOf> getAclGroups() throws ServiceException {
        return getProvisioning().getAclGroups(this);
    }

    public List<Signature> getAllSignatures() throws ServiceException {
        return getProvisioning().getAllSignatures(this);
    }

    public Cos getCOS() throws ServiceException {
        return getProvisioning().getCOS(this);
    }

    public String getAccountStatus(Provisioning prov) {
        
        String domainStatus = null;
        String accountStatus = getAttr(Provisioning.A_zimbraAccountStatus);
        
        boolean isAdmin = getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        boolean isDomainAdmin = getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        isAdmin = (isAdmin && !isDomainAdmin);
        if (isAdmin)
            return accountStatus;
            
        
        if (mDomain != null) {
            try {
                Domain domain = prov.getDomain(this);
                if (domain != null) {
                    domainStatus = domain.getDomainStatusAsString();
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to get domain for account " + getName(), e);
                return accountStatus;
            }
        }
        
        if (domainStatus == null || domainStatus.equals(Provisioning.DOMAIN_STATUS_ACTIVE))
            return accountStatus;
        else if (domainStatus.equals(Provisioning.DOMAIN_STATUS_LOCKED)) {
            if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE) ||
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED))
                return accountStatus;
            else
                return Provisioning.ACCOUNT_STATUS_LOCKED;
        } else if (domainStatus.equals(Provisioning.DOMAIN_STATUS_MAINTENANCE) ||
                   domainStatus.equals(Provisioning.DOMAIN_STATUS_SUSPENDED) ||
                   domainStatus.equals(Provisioning.DOMAIN_STATUS_SHUTDOWN)) {
            if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED))
                return accountStatus;
            else
                return Provisioning.ACCOUNT_STATUS_MAINTENANCE;
        } else {
            assert(domainStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED));
            return Provisioning.ACCOUNT_STATUS_CLOSED;
        }
    }
}
