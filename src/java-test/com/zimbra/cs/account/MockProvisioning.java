/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource.Type;
import com.zimbra.cs.account.NamedEntry.Visitor;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.mime.MimeTypeInfo;

/**
 * Mock implementation of {@link Provisioning} for testing.
 *
 * @author ysasaki
 */
public class MockProvisioning extends Provisioning {

    private Map<String, Account> id2account = new HashMap<String, Account>();
    private Map<String, Account> name2account = new HashMap<String, Account>();
    private Map<String, List<MimeTypeInfo>> mimeConfig =
        new HashMap<String, List<MimeTypeInfo>>();
    private Config config = new Config(new HashMap<String, Object>(), this);

    @Override
    public Account createAccount(String email, String password,
            Map<String, Object> attrs) throws ServiceException {
        validate(ProvisioningValidator.CREATE_ACCOUNT, email, null, attrs);

        Account account = new Account(email, email, attrs, null, this);
        try {
            name2account.put(email, account);
            id2account.put(account.getId(), account);
            return account;
        } finally {
            validate(ProvisioningValidator.CREATE_ACCOUNT_SUCCEEDED, email, account);
        }
    }

    @Override
    public Account get(AccountBy keyType, String key) {
        switch (keyType) {
            case name:
                return name2account.get(key);
            case id:
            default:
                return id2account.get(key);
        }
    }

    @Override
    public List<MimeTypeInfo> getMimeTypes(String mime) {
        return mimeConfig.get(mime);
    }

    @Override
    public List<MimeTypeInfo> getAllMimeTypes() {
        List<MimeTypeInfo> result = new ArrayList<MimeTypeInfo>();
        for (List<MimeTypeInfo> entry : mimeConfig.values()) {
            result.addAll(entry);
        }
        return result;
    }

    public void addMimeType(String mime, MimeTypeInfo info) {
        List<MimeTypeInfo> list = mimeConfig.get(mime);
        if (list == null) {
            list = new ArrayList<MimeTypeInfo>();
            mimeConfig.put(mime, list);
        }
        list.add(info);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public List<Zimlet> getObjectTypes() {
        return Collections.emptyList();
    }

    @Override
    public void modifyAttrs(Entry entry, Map<String, ? extends Object> attrs,
            boolean checkImmutable) {
        entry.setAttrs(new HashMap<String, Object>(attrs));
    }

    @Override
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs,
            boolean checkImmutable, boolean allowCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reload(Entry e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean inDistributionList(Account acct, String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getDistributionLists(Account acct) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DistributionList> getDistributionLists(Account acct,
            boolean directOnly, Map<String, String> via) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DistributionList> getDistributionLists(DistributionList list,
            boolean directOnly, Map<String, String> via) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean healthCheck() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GlobalGrant getGlobalGrant() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Account restoreAccount(String emailAddress, String password,
            Map<String, Object> attrs, Map<String, Object> origAttrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAccount(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameAccount(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NamedEntry> searchAccounts(String query, String[] returnAttrs,
            String sortAttr, boolean sortAscending, int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Account> getAllAdminAccounts() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCOS(Account acct, Cos cos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyAccountStatus(Account acct, String newStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void authAccount(Account acct, String password, Protocol proto) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void authAccount(Account acct, String password, Protocol proto,
            Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void preAuthAccount(Account acct, String accountName,
            String accountBy, long timestamp, long expires, String preAuth,
            Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(Account acct, String currentPassword,
            String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPassword(Account acct, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkPasswordStrength(Account acct, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlias(Account acct, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAlias(Account acct, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Domain createDomain(String name, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Domain get(DomainBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Domain> getAllDomains() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDomain(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cos createCos(String name, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cos copyCos(String srcCosId, String destCosName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameCos(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cos get(CosBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Cos> getAllCos() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCos(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server getLocalServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server createServer(String name, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server get(ServerBy keyName, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Server> getAllServers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Server> getAllServers(String service) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteServer(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributionList createDistributionList(String listAddress,
            Map<String, Object> listAttrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributionList get(DistributionListBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDistributionList(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAlias(DistributionList dl, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAlias(DistributionList dl, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameDistributionList(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Zimlet getZimlet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Zimlet> listAllZimlets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Zimlet createZimlet(String name, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteZimlet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CalendarResource createCalendarResource(String emailAddress,
            String password, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCalendarResource(String zimbraId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameCalendarResource(String zimbraId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CalendarResource get(CalendarResourceBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NamedEntry> searchCalendarResources(EntrySearchFilter filter,
            String[] returnAttrs, String sortAttr, boolean sortAscending) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getAllAccounts(Domain d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllAccounts(Domain d, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllAccounts(Domain d, Server s, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getAllCalendarResources(Domain d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllCalendarResources(Domain d, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getAllCalendarResources(Domain d, Server s, Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<?> getAllDistributionLists(Domain d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NamedEntry> searchAccounts(Domain d, String query,
            String[] returnAttrs, String sortAttr, boolean sortAscending,
            int flags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NamedEntry> searchDirectory(SearchOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchGalResult searchGal(Domain d, String query,
            GalSearchType type, String token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchGalResult autoCompleteGal(Domain d, String query,
            GalSearchType type, int limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NamedEntry> searchCalendarResources(Domain d,
            EntrySearchFilter filter, String[] returnAttrs, String sortAttr,
            boolean sortAscending) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMembers(DistributionList list, String[] members) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMembers(DistributionList list, String[] member) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity createIdentity(Account account, String identityName,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity restoreIdentity(Account account, String identityName,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyIdentity(Account account, String identityName,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIdentity(Account account, String identityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Identity> getAllIdentities(Account account) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity get(Account account, IdentityBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature createSignature(Account account, String signatureName,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature restoreSignature(Account account, String signatureName,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifySignature(Account account, String signatureId,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSignature(Account account, String signatureId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Signature> getAllSignatures(Account account) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature get(Account account, SignatureBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource createDataSource(Account account, Type type,
            String dataSourceName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource createDataSource(Account account, Type type,
            String dataSourceName, Map<String, Object> attrs,
            boolean passwdAlreadyEncrypted) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource restoreDataSource(Account account, Type type,
            String dataSourceName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyDataSource(Account account, String dataSourceId,
            Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDataSource(Account account, String dataSourceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataSource> getAllDataSources(Account account) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource get(Account account, DataSourceBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMPPComponent createXMPPComponent(String name, Domain domain,
            Server server, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMPPComponent get(XMPPComponentBy keyName, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<XMPPComponent> getAllXMPPComponents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteXMPPComponent(XMPPComponent comp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushCache(CacheEntryType type, CacheEntry[] entries) {
        throw new UnsupportedOperationException();
    }

}
