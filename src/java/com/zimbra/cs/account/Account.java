/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.account.Provisioning.SignatureBy;
import com.zimbra.cs.account.auth.AuthContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author schemers
 */
public class Account extends ZAttrAccount  {
    
    public Account(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    public void deleteAccount() throws ServiceException {
        getProvisioning().deleteAccount(this.getId());
    }

    public void rename(String newName) throws ServiceException {
        getProvisioning().renameAccount(this.getId(), newName);
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void addAlias(String alias) throws ServiceException {
        getProvisioning().addAlias(this, alias);
    }

    public void removeAlias(String alias) throws ServiceException {
        getProvisioning().removeAlias(this, alias);
    }

    public void authAccount(String password, AuthContext.Protocol proto) throws ServiceException {
        getProvisioning().authAccount(this, password, proto);
    }

    public void changePassword(String currentPassword, String newPassword) throws ServiceException {
        getProvisioning().changePassword(this, currentPassword, newPassword);
    }

    public void checkPasswordStrength(String password) throws ServiceException {
        getProvisioning().checkPasswordStrength(this, password);
    }

    public void setPassword(String password) throws ServiceException {
        getProvisioning().setPassword(this, password);
    }

    public AclGroups getAclGroups(boolean adminGroupsOnly) throws ServiceException {
        return getProvisioning().getAclGroups(this, adminGroupsOnly);
    }

    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this account (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException on error
     */
    public boolean inDistributionList(String zimbraId) throws ServiceException {
        return getProvisioning().inDistributionList(this, zimbraId);
    }

    /**
     * @return set of all the zimbraId's of lists this account belongs to, including any list in other list.
     * @throws ServiceException on error
     */
    public Set<String> getDistributionLists() throws ServiceException {
        return getProvisioning().getDistributionLists(this);
    }

    /**
     *
     * @param directOnly return only DLs this account is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from a DL name to the DL it was a member of, if
     *            member was indirect.
     * @return all the DLs
     * @throws ServiceException on error
     */
    public List<DistributionList> getDistributionLists(boolean directOnly, Map<String,String> via) throws ServiceException {
        return getProvisioning().getDistributionLists(this, directOnly, via);
    }

    public void preAuthAccount(String accountName, String accountBy, long timestamp, long expires, String preAuth, Map<String, Object> authCtxt) throws ServiceException {
        getProvisioning().preAuthAccount(this, accountName, accountBy, timestamp, expires, preAuth, authCtxt);
    }

    public void preAuthAccount(String accountName, String accountBy, long timestamp, long expires,
                                        String preAuth,
                                        boolean admin,
                                        Map<String, Object> authCtxt) throws ServiceException
    {
        getProvisioning().preAuthAccount(this, accountName, accountBy, timestamp, expires, preAuth, admin, authCtxt);
    }


    public Cos getCOS() throws ServiceException {
        return getProvisioning().getCOS(this);
    }

    public DataSource createDataSource(DataSource.Type type, String dataSourceName, Map<String, Object> attrs) throws ServiceException {
        return getProvisioning().createDataSource(this, type, dataSourceName, attrs);
    }
    
    public DataSource createDataSource(DataSource.Type type, String dataSourceName, Map<String, Object> attrs, boolean passwdAlreadyEncrypted) throws ServiceException {
        return getProvisioning().createDataSource(this, type, dataSourceName, attrs, passwdAlreadyEncrypted);
    }

    public void modifyDataSource(String dataSourceId, Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyDataSource(this, dataSourceId, attrs);
    }

    public void deleteDataSource(String dataSourceId) throws ServiceException {
        getProvisioning().deleteDataSource(this, dataSourceId);
    }

    public List<DataSource> getAllDataSources() throws ServiceException {
        return getProvisioning().getAllDataSources(this);
    }

    public DataSource get(DataSourceBy keyType, String key) throws ServiceException {
        return getProvisioning().get(this, keyType, key);
    }

    public DataSource getDataSourceByName(String name) throws ServiceException {
        return get(DataSourceBy.name, name);
    }

    public DataSource getDataSourceById(String id) throws ServiceException {
        return get(DataSourceBy.id, id);
    }

    public Identity createIdentity(String identityName, Map<String, Object> attrs) throws ServiceException {
        return getProvisioning().createIdentity(this, identityName, attrs);
    }

    public void modifyIdentity(String identityName, Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyIdentity(this, identityName, attrs);
    }

    public void deleteIdentity(String identityName) throws ServiceException {
        getProvisioning().deleteIdentity(this, identityName);
    }

    public List<Identity> getAllIdentities() throws ServiceException {
        return getProvisioning().getAllIdentities(this);
    }

    public Identity get(IdentityBy keyType, String key) throws ServiceException {
        return getProvisioning().get(this, keyType, key);
    }

    public Identity getIdentityByName(String name) throws ServiceException {
        return get(IdentityBy.name, name);
    }

    public Identity getIdentityById(String id) throws ServiceException {
        return get(IdentityBy.id, id);
    }
    public Identity getDefaultIdentity() throws ServiceException {
        return getProvisioning().getDefaultIdentity(this);
    }

    public Signature createSignature(String signatureName, Map<String, Object> attrs) throws ServiceException {
        return getProvisioning().createSignature(this, signatureName, attrs);
    }

    public void modifySignature(String signatureId, Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifySignature(this, signatureId, attrs);
    }

    public void deleteSignature(String signatureId) throws ServiceException {
        getProvisioning().deleteSignature(this, signatureId);
    }

    public List<Signature> getAllSignatures() throws ServiceException {
        return getProvisioning().getAllSignatures(this);
    }

    public Signature get(SignatureBy keyType, String key) throws ServiceException {
        return getProvisioning().get(this, keyType, key);
    }

    public Signature getSignatureByName(String key) throws ServiceException {
        return get(SignatureBy.name, key);
    }

    public Signature getSignatureById(String key) throws ServiceException {
        return get(SignatureBy.id, key);
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
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_PENDING) ||
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED))
                return accountStatus;
            else
                return Provisioning.ACCOUNT_STATUS_LOCKED;
        } else if (domainStatus.equals(Provisioning.DOMAIN_STATUS_MAINTENANCE) ||
                   domainStatus.equals(Provisioning.DOMAIN_STATUS_SUSPENDED) ||
                   domainStatus.equals(Provisioning.DOMAIN_STATUS_SHUTDOWN)) {
            if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_PENDING) ||
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED))
                return accountStatus;
            else
                return Provisioning.ACCOUNT_STATUS_MAINTENANCE;
        } else {
            // domainStatus is Provisioning.DOMAIN_STATUS_CLOSED
            return Provisioning.ACCOUNT_STATUS_CLOSED;
        }
    }

    // needed when when cal resource is loaded as an account and we need to know if 
    // it actually is a cal resource.
    public boolean isCalendarResource() {
        return getAttr(Provisioning.A_zimbraCalResType) != null;
    }
    
    /*
    public boolean isGalSyncAccount() throws ServiceException {
        Boolean isGalSyncAcct = (Boolean)getCachedData(EntryCacheDataKey.ACCOUNT_IS_GAL_SYNC_ACCOUNT.getKeyName());
        if (isGalSyncAcct == null) {
            isGalSyncAcct = Boolean.FALSE;
            if (isIsSystemResource()) {
                // see if there is a GalDataSource in the account 
                List<DataSource> dataSources = getProvisioning().getAllDataSources(this);
                for (DataSource ds : dataSources) {
                    if (DataSource.Type.gal == ds.getType()) {
                        isGalSyncAcct = Boolean.TRUE;
                        break;
                    }
                }
            }
            setCachedData(EntryCacheDataKey.ACCOUNT_IS_GAL_SYNC_ACCOUNT.getKeyName(), isGalSyncAcct);
        }
        return isGalSyncAcct.booleanValue();
    }
    */
    
}
 