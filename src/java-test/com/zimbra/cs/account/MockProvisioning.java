/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.ShareLocatorBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.NamedEntry.Visitor;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.mime.MockMimeTypeInfo;
import com.zimbra.cs.mime.handler.UnknownTypeHandler;
import com.zimbra.cs.redolog.MockRedoLogProvider;
import com.zimbra.soap.admin.type.DataSourceType;

/**
 * Mock implementation of {@link Provisioning} for testing.
 *
 * @author ysasaki
 */
public final class MockProvisioning extends Provisioning {
    public static final String DEFAULT_ACCOUNT_ID = new UUID(0L, 0L).toString();

    private final Map<String, Account> id2account = new HashMap<String, Account>();
    private final Map<String, Account> name2account = new HashMap<String, Account>();
    private final Map<String, List<MimeTypeInfo>> mimeConfig =
        new HashMap<String, List<MimeTypeInfo>>();
    private final Config config = new Config(new HashMap<String, Object>(), this);
    private final Map<String, ShareLocator> shareLocators = new HashMap<String, ShareLocator>();
    private final Server localhost;

    public MockProvisioning() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "localhost");
        attrs.put(Provisioning.A_zimbraRedoLogProvider, MockRedoLogProvider.class.getName());
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraMailMode, Provisioning.MailMode.http.toString());
        localhost = new Server("localhost", "localhost", attrs, Collections.<String, Object>emptyMap(), this);
    }

    @Override
    public Account createAccount(String email, String password, Map<String, Object> attrs) throws ServiceException {
        validate(ProvisioningValidator.CREATE_ACCOUNT, email, null, attrs);
        if (!attrs.containsKey(Provisioning.A_zimbraId)) {
            attrs.put(Provisioning.A_zimbraId, DEFAULT_ACCOUNT_ID);
        }
        if (!attrs.containsKey(Provisioning.A_zimbraMailHost)) {
            attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        }
        if (!attrs.containsKey(Provisioning.A_zimbraAccountStatus)) {
            attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        }
        if (!attrs.containsKey(Provisioning.A_zimbraDumpsterEnabled)) {
            attrs.put(Provisioning.A_zimbraDumpsterEnabled, Provisioning.TRUE);
        }
        attrs.put(Provisioning.A_zimbraBatchedIndexingSize, Integer.MAX_VALUE); // suppress indexing
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
        List<MimeTypeInfo> result = mimeConfig.get(mime);
        if (result != null) {
            return result;
        } else {
            MockMimeTypeInfo info = new MockMimeTypeInfo();
            info.setHandlerClass(UnknownTypeHandler.class.getName());
            return Collections.<MimeTypeInfo>singletonList(info);
        }
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
    public void modifyAttrs(Entry entry, Map<String, ? extends Object> attrs, boolean checkImmutable) {
        Map<String, Object> map = entry.getAttrs(false);
        for (Map.Entry<String, ? extends Object> attr : attrs.entrySet()) {
            if (attr.getValue() != null) {
                map.put(attr.getKey(), attr.getValue());
            } else {
                map.remove(attr.getKey());
            }
        }
    }

    @Override
    public Server getLocalServer() {
        return localhost;
    }

    @Override
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs,
            boolean checkImmutable, boolean allowCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reload(Entry e) {
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
        Account account = id2account.remove(zimbraId);
        if (account != null) {
            name2account.remove(account.getName());
        }
    }

    @Override
    public void renameAccount(String zimbraId, String newName) {
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
    public void authAccount(Account acct, String password, Protocol proto, Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires,
            String preAuth, Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ssoAuthAccount(Account acct, AuthContext.Protocol proto, Map<String, Object> authCtxt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(Account acct, String currentPassword, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SetPasswordResult setPassword(Account acct, String newPassword) {
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
    public Domain get(Key.DomainBy keyType, String key) {
        return null;
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
    public Cos get(Key.CosBy keyType, String key) {
        return new MockCos(key, UUID.randomUUID().toString(), new HashMap<String,Object>(), this);
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
    public Server createServer(String name, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server get(Key.ServerBy keyName, String key) {
        switch (keyName) {
            case id:
                return localhost.getId().equals(key) ? localhost : null;
            case name:
                return localhost.getName().equals(key) ? localhost : null;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public List<Server> getAllServers() {
        return Arrays.asList(localhost);
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
    public DistributionList createDistributionList(String listAddress, Map<String, Object> listAttrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DistributionList get(Key.DistributionListBy keyType, String key) {
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
        return Collections.emptyList();
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
    public CalendarResource createCalendarResource(String emailAddress, String password, Map<String, Object> attrs) {
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
    public CalendarResource get(Key.CalendarResourceBy keyType, String key) {
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
    public void addMembers(DistributionList list, String[] members) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMembers(DistributionList list, String[] member) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity getDefaultIdentity(Account account) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(A_zimbraPrefIdentityName, ProvisioningConstants.DEFAULT_IDENTITY_NAME);
        attrs.put(A_zimbraPrefIdentityId, account.getId());
        return new Identity(account, ProvisioningConstants.DEFAULT_IDENTITY_NAME, account.getId(), attrs, this);
    }

    @Override
    public Identity createIdentity(Account account, String identityName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Identity restoreIdentity(Account account, String identityName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyIdentity(Account account, String identityName, Map<String, Object> attrs) {
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
    public Identity get(Account account, Key.IdentityBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature createSignature(Account account, String signatureName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signature restoreSignature(Account account, String signatureName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifySignature(Account account, String signatureId, Map<String, Object> attrs) {
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
    public Signature get(Account account, Key.SignatureBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource createDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource createDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs,
            boolean passwdAlreadyEncrypted) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSource restoreDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs) {
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
        // Don't throw UnsupportedOperationException because Mailbox.updateRssDataSource()
        // calls this method.
        return Collections.emptyList();
    }

    @Override
    public DataSource get(Account account, Key.DataSourceBy keyType, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMPPComponent createXMPPComponent(String name, Domain domain, Server server, Map<String, Object> attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMPPComponent get(Key.XMPPComponentBy keyName, String key) {
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

    @Override
    public ShareLocator get(ShareLocatorBy keyType, String key) throws ServiceException {
        return shareLocators.get(key);
    }

    @Override
    public ShareLocator createShareLocator(String id, Map<String, Object> attrs) throws ServiceException {
        ShareLocator shloc = new ShareLocator(id, attrs, this);
        shareLocators.put(id, shloc);
        return shloc;
    }

    @Override
    public void deleteShareLocator(String id) throws ServiceException {
        shareLocators.remove(id);
    }

}
