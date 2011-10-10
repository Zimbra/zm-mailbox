/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Provisioning.SearchAccountsOptions;
import com.zimbra.cs.account.Provisioning.SearchDirectoryObjectType;
import com.zimbra.cs.account.Provisioning.SearchObjectsOptions;
import com.zimbra.cs.account.Provisioning.SearchOptions;
import com.zimbra.cs.account.Provisioning.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.Provisioning.SearchObjectsOptions.MakeObjectOpt;
import com.zimbra.cs.account.Provisioning.SearchObjectsOptions.SortOpt;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.mailbox.ACL;

public class TestLdapProvAccount extends TestLdap {
    
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = Provisioning.getInstance();
        domain = TestLdapProvDomain.createDomain(prov, baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestLdapProvAccount.class.getName().toLowerCase();
    }
    
    static Account createAccount(Provisioning prov, String localPart, 
            Domain domain, Map<String, Object> attrs)
    throws Exception {
        String acctName = TestUtil.getAddress(localPart, domain.getName());
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.name, acctName);
        assertNull(acct);
                
        acct = prov.createAccount(acctName, "test123", attrs);
        assertNotNull(acct);
        
        prov.flushCache(CacheEntryType.account, null);
        acct = prov.get(AccountBy.name, acctName);
        assertNotNull(acct);
        assertEquals(acctName.toLowerCase(), acct.getName().toLowerCase());
        
        return acct;
    }
    
    static CalendarResource createCalendarResource(Provisioning prov, String localPart,
            Domain domain, Map<String, Object> attrs)
    throws Exception {
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_displayName, localPart);
            attrs.put(Provisioning.A_zimbraCalResType, Provisioning.CalResType.Equipment.name());
        }
        
        String crName = TestUtil.getAddress(localPart, domain.getName());
        prov.flushCache(CacheEntryType.account, null);
        CalendarResource cr = prov.get(CalendarResourceBy.name, crName);
        assertNull(cr);
                
        cr = prov.createCalendarResource(crName, "test123", attrs);
        assertNotNull(cr);
        
        prov.flushCache(CacheEntryType.account, null);
        cr = prov.get(CalendarResourceBy.name, crName);
        assertNotNull(cr);
        assertEquals(crName.toLowerCase(), cr.getName().toLowerCase());
        
        return cr;
    }
    
    static void deleteAccount(Provisioning prov, Account acct) throws Exception {
        String acctId = acct.getId();
        prov.deleteAccount(acctId);
        prov.flushCache(CacheEntryType.account, null);
        acct = prov.get(AccountBy.id, acctId);
        assertNull(acct);
    }

    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        return createAccount(prov, localPart, domain, attrs);
    }
    
    private CalendarResource createCalendarResource(String localPart) throws Exception {
        return createCalendarResource(localPart, null);
    }
    
    private CalendarResource createCalendarResource(String localPart, Map<String, Object> attrs) throws Exception {
        return createCalendarResource(prov, localPart, domain, attrs);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        deleteAccount(prov, acct);
    }
    
    @Test
    public void createAccount() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("createAccount");
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        deleteAccount(acct);
    }
    
    @Test
    public void createAccountAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("createAccountAlreadyExists");
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        
        boolean caughtException = false;
        try {
            prov.createAccount(acct.getName(), "test123", null);
        } catch (AccountServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteAccount(acct);
    }
    
    private void getAccountByAdminName(String adminName) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.adminName, adminName);
        assertNotNull(acct);
    }
    
    private void getAccountByAppAdminName(String appAdminName) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.appAdminName, appAdminName);
        assertNotNull(acct);
    }
    
    private void getAccountById(String acctId) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.id, acctId);
        assertNotNull(acct);
    }
    
    private void getAccountByName(String name) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.name, name);
        assertNotNull(acct);
    }
    
    private void getAccountByForeignPrincipal(String foreignPrincipal) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.foreignPrincipal, foreignPrincipal);
        assertNotNull(acct);
    }
    
    private void getAccountByKrb5Principal(String krb5Principal) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        Account acct = prov.get(AccountBy.krb5Principal, krb5Principal);
        assertNotNull(acct);
    }
    
    @Test
    public void getAccount() throws Exception {
        String ACCT_NAME = "getAccount";
        
        String FOREIGN_PRINCIPAL = "test:foreignPrincipal";
        String KRB5_PRINCIPAL = "krb5Principal";
        String KRB5_PRINCIPAL_ATTR_VALUE = Provisioning.FP_PREFIX_KERBEROS5 + KRB5_PRINCIPAL;
        Map<String, Object> attrs = new HashMap<String, Object>();
        StringUtil.addToMultiMap(attrs, Provisioning.A_zimbraForeignPrincipal, FOREIGN_PRINCIPAL);
        StringUtil.addToMultiMap(attrs, Provisioning.A_zimbraForeignPrincipal, KRB5_PRINCIPAL_ATTR_VALUE);
        Account acct = createAccount(ACCT_NAME, attrs);
        
        getAccountByAdminName(LC.zimbra_ldap_user.value());
        getAccountByAppAdminName("zmnginx");
        getAccountById(acct.getId());
        getAccountByName(acct.getName());
        getAccountByForeignPrincipal(FOREIGN_PRINCIPAL);
        getAccountByKrb5Principal(KRB5_PRINCIPAL);
        
        deleteAccount(acct);
    }
    
    @Test
    public void getAllAdminAccounts() throws Exception {
        String ADMIN_ACCT_NAME_1 = "getAllAdminAccounts-1";
        String ADMIN_ACCT_NAME_2 = "getAllAdminAccounts-2";
        
        Map<String, Object> acct1Attrs1 = new HashMap<String, Object>();
        acct1Attrs1.put(Provisioning.A_zimbraIsAdminAccount, LdapConstants.LDAP_TRUE);
        Map<String, Object> acct1Attrs2 = new HashMap<String, Object>();
        acct1Attrs2.put(Provisioning.A_zimbraIsDelegatedAdminAccount, LdapConstants.LDAP_TRUE);
        
        Account adminAcct1 = createAccount(ADMIN_ACCT_NAME_1, acct1Attrs1);
        Account adminAcct2 = createAccount(ADMIN_ACCT_NAME_2, acct1Attrs2);
        
        List<Account> allAdminAccts = prov.getAllAdminAccounts();
        
        Set<String> allAdminAcctIds = new HashSet<String>();
        for (Account acct : allAdminAccts) {
            allAdminAcctIds.add(acct.getId());
        }
        
        assertTrue(allAdminAcctIds.contains(adminAcct1.getId()));
        assertTrue(allAdminAcctIds.contains(adminAcct2.getId()));
        
        deleteAccount(adminAcct1);
        deleteAccount(adminAcct2);
    }
    
    private DataSource createDataSource(Account acct, String dataSourceName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "123");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        attrs.put(Provisioning.A_zimbraDataSourceHost, "zimbra.com");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        DataSource ds = prov.createDataSource(acct, DataSourceType.pop3, dataSourceName, attrs);
        return ds;
    }
    
    /*
     * This test does not work with JNDI.  The trailing space in data source name 
     * got stripped after the rename.
     */
    @Test
    public void renameAccount() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("renameAccount");
        String ACCT_NEW_NAME = TestUtil.getAddress(
                TestLdap.makeAccountNameLocalPart("renameAccount-new"), 
                domain.getName()).toLowerCase();
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String acctId = acct.getId();
        
        // create some children
        String DATA_SOURCE_NAME_1;
        String DATA_SOURCE_NAME_2;
        String DATA_SOURCE_NAME_3;
        
        if (getCurrentTestConfig() == TestConfig.UBID) {
            DATA_SOURCE_NAME_1 = TestLdap.makeDataSourceName("ds1");
            DATA_SOURCE_NAME_2 = TestLdap.makeDataSourceName("ds2");
            DATA_SOURCE_NAME_3 = TestLdap.makeDataSourceName("ds3");
        } else {
            DATA_SOURCE_NAME_1 = "ds1";
            DATA_SOURCE_NAME_2 = "ds2";
            DATA_SOURCE_NAME_3 = "ds3";
        }
        
        DataSource ds1 = createDataSource(acct, DATA_SOURCE_NAME_1);
        DataSource ds2 = createDataSource(acct, DATA_SOURCE_NAME_2);
        DataSource ds3 = createDataSource(acct, DATA_SOURCE_NAME_3);
        String DATA_SOURCE_ID_1 = ds1.getId();
        String DATA_SOURCE_ID_2 = ds2.getId();
        String DATA_SOURCE_ID_3 = ds3.getId();
        
        prov.renameAccount(acctId, ACCT_NEW_NAME);
        
        prov.flushCache(CacheEntryType.account, null);
        Account renamedAcct = prov.get(AccountBy.name, ACCT_NEW_NAME);
        
        assertEquals(acctId, renamedAcct.getId());
        assertEquals(ACCT_NEW_NAME, renamedAcct.getName());
        
        // make sure children are moved
        assertEquals(DATA_SOURCE_ID_1, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_1).getId());
        assertEquals(DATA_SOURCE_ID_2, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_2).getId());
        assertEquals(DATA_SOURCE_ID_3, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_3).getId());
        
        deleteAccount(renamedAcct);
    }
    
    @Test
    public void renameAccountDomainChanged() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("renameAccountDomainChanged");
        
        String NEW_DOMAIN_NAME = "renameAccountDomainChanged." + baseDomainName();
        Domain newDomain = TestLdapProvDomain.createDomain(prov, NEW_DOMAIN_NAME, null);
        String ACCT_NEW_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("renameAccountDomainChanged-new");
        String ACCT_NEW_NAME =  
            TestUtil.getAddress(ACCT_NEW_NAME_LOCALPART, NEW_DOMAIN_NAME).toLowerCase();
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String acctId = acct.getId();
        
        // create some children
        String DATA_SOURCE_NAME_1;
        String DATA_SOURCE_NAME_2;
        String DATA_SOURCE_NAME_3;
        
        if (getCurrentTestConfig() == TestConfig.UBID) {
            DATA_SOURCE_NAME_1 = TestLdap.makeDataSourceName("ds1");
            DATA_SOURCE_NAME_2 = TestLdap.makeDataSourceName("ds2");
            DATA_SOURCE_NAME_3 = TestLdap.makeDataSourceName("ds3");
        } else {
            DATA_SOURCE_NAME_1 = "ds1";
            DATA_SOURCE_NAME_2 = "ds2";
            DATA_SOURCE_NAME_3 = "ds3";
        }
        
        DataSource ds1 = createDataSource(acct, DATA_SOURCE_NAME_1);
        DataSource ds2 = createDataSource(acct, DATA_SOURCE_NAME_2);
        DataSource ds3 = createDataSource(acct, DATA_SOURCE_NAME_3);
        String DATA_SOURCE_ID_1 = ds1.getId();
        String DATA_SOURCE_ID_2 = ds2.getId();
        String DATA_SOURCE_ID_3 = ds3.getId();
        
        prov.renameAccount(acctId, ACCT_NEW_NAME);
        
        prov.flushCache(CacheEntryType.account, null);
        Account renamedAcct = prov.get(AccountBy.name, ACCT_NEW_NAME);
        
        assertEquals(acctId, renamedAcct.getId());
        assertEquals(ACCT_NEW_NAME, renamedAcct.getName());
        
        // make sure children are moved
        assertEquals(DATA_SOURCE_ID_1, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_1).getId());
        assertEquals(DATA_SOURCE_ID_2, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_2).getId());
        assertEquals(DATA_SOURCE_ID_3, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_3).getId());
        
        deleteAccount(renamedAcct);
        TestLdapProvDomain.deleteDomain(prov, newDomain);
    }
    
    @Test
    public void renameAccountAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("renameAccountAlreadyExists");
        String ACCT_NAME_EXISTS_LOCALPART = TestLdap.makeAccountNameLocalPart("renameAccountAlreadyExists-exists");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Account acctExists = createAccount(ACCT_NAME_EXISTS_LOCALPART);
        String acctId = acct.getId();
        
        String ACCT_NEW_NAME = acctExists.getName();
        
        boolean caughtException = false;
        try {
            prov.renameAccount(acctId, ACCT_NEW_NAME);
        } catch (AccountServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteAccount(acct);
        deleteAccount(acctExists);
    }
    
    @Test
    public void searchAccountsOnServer() throws Exception {
        // create a search domain
        String DOMAIN_NAME = "searchAccountsOnServer." + baseDomainName();
        Domain searchDomain = TestLdapProvDomain.createDomain(prov, DOMAIN_NAME, null);
        
        // create an account and a calendar resource on the domain
        String ACCT_LOCALPART = TestLdap.makeAccountNameLocalPart("searchAccountsOnServer-acct");
        String CR_LOCALPART = TestLdap.makeAccountNameLocalPart("searchAccountsOnServer-cr");
        
        Map<String, Object> crAttrs = Maps.newHashMap();
        crAttrs.put(Provisioning.A_displayName, "ACCT_LOCALPART");
        crAttrs.put(Provisioning.A_zimbraCalResType, Provisioning.CalResType.Equipment.name());
        Account acct = createAccount(prov, ACCT_LOCALPART, searchDomain, null);
        CalendarResource cr = createCalendarResource(prov, CR_LOCALPART, searchDomain, crAttrs);
        
        Server server = prov.getLocalServer();
        List<NamedEntry> result;
        SearchAccountsOptions opts;
        
        // 1. test search accounts, including cr
        opts = new SearchAccountsOptions(searchDomain, new String[]{Provisioning.A_zimbraId});
        opts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        result = prov.searchAccountsOnServer(server, opts);
        assertEquals(2, result.size());
        
        // 2. test maxResults
        boolean caughtTooManySearchResultsException = false;
        try {
            opts = new SearchAccountsOptions(searchDomain, new String[]{Provisioning.A_zimbraId});
            opts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
            opts.setMaxResults(1);
            result = prov.searchAccountsOnServer(server, opts);
        } catch (ServiceException e) {
            if (AccountServiceException.TOO_MANY_SEARCH_RESULTS.equals(e.getCode())) {
                caughtTooManySearchResultsException = true;
            }
        }
        assertTrue(caughtTooManySearchResultsException);
        
        
        // 3. search accounts only
        opts = new SearchAccountsOptions(searchDomain, new String[]{Provisioning.A_zimbraId});
        opts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        opts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        result = prov.searchAccountsOnServer(server, opts);
        assertEquals(1, result.size());
        
        // 3. test sorting
        opts = new SearchAccountsOptions(searchDomain, new String[]{Provisioning.A_zimbraId});
        opts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        opts.setSortOpt(SortOpt.SORT_DESCENDING);
        result = prov.searchAccountsOnServer(server, opts);
        assertEquals(2, result.size());
        assertEquals(cr.getName(), result.get(0).getName());
        
        opts = new SearchAccountsOptions(searchDomain, new String[]{Provisioning.A_zimbraId});
        opts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
        opts.setSortOpt(SortOpt.SORT_ASCENDING);
        result = prov.searchAccountsOnServer(server, opts);
        assertEquals(2, result.size());
        assertEquals(acct.getName(), result.get(0).getName());
    }
    
    @Test
    public void getAllAccounts() throws Exception {
        Account acct3 = createAccount("getAllAccounts-3");
        Account acct2 = createAccount("getAllAccounts-2");
        Account acct1 = createAccount("getAllAccounts-1");
        CalendarResource cr = createCalendarResource("getAllAccounts-cr");
        
        List<NamedEntry> accounts = prov.getAllAccounts(domain);
        assertEquals(3, accounts.size());
        assertEquals(acct1.getName(), accounts.get(0).getName());
        assertEquals(acct2.getName(), accounts.get(1).getName());
        assertEquals(acct3.getName(), accounts.get(2).getName());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
    }
    
    @Test
    public void searchDirectory_accountsByExternalGrant() throws Exception {
        String EXT_USER_EMAIL = "user@test.com";
        
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraSharedItem, "granteeId:" + EXT_USER_EMAIL + "blah blah");
        Account acct = createAccount("searchDirectory_accountsByExternalGrant", attrs);
        
        SearchAccountsOptions searchOpts = new SearchAccountsOptions(
                domain, new String[] {
                        Provisioning.A_zimbraId,
                        Provisioning.A_displayName,
                        Provisioning.A_zimbraSharedItem });
        
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().accountsByExternalGrant(EXT_USER_EMAIL);
        searchOpts.setFilter(filter);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(1, accounts.size());
        assertEquals(acct.getName(), accounts.get(0).getName());
        
        deleteAccount(acct);
    }
    
    @Test
    public void searchDirectory_accountsByGrants() throws Exception {
        
        String GRANTEE_ID_1 = LdapUtilCommon.generateUUID();
        String GRANTEE_ID_2 = LdapUtilCommon.generateUUID();
        String GRANTEE_ID_3 = LdapUtilCommon.generateUUID();
        List<String> GRANTEE_IDS = Lists.newArrayList(GRANTEE_ID_1, GRANTEE_ID_2, GRANTEE_ID_3);
        
        Map<String, Object> attrs1 = Maps.newHashMap();
        attrs1.put(Provisioning.A_zimbraSharedItem, "granteeId:" + GRANTEE_ID_3 + "blah blah");
        Account acct1 = createAccount("searchDirectory_accountsByGrants-1", attrs1);
        
        Map<String, Object> attrs2 = Maps.newHashMap();
        attrs2.put(Provisioning.A_zimbraSharedItem, "blah" + "granteeType:pub" + " blah");
        Account acct2 = createAccount("searchDirectory_accountsByGrants-2", attrs2);
        
        Map<String, Object> attrs3 = Maps.newHashMap();
        attrs3.put(Provisioning.A_zimbraSharedItem, "blah" + "granteeType:all" + " blah");
        Account acct3 = createAccount("searchDirectory_accountsByGrants-3", attrs3);
        
        SearchAccountsOptions searchOpts = new SearchAccountsOptions(
                 new String[] {
                        Provisioning.A_zimbraId,
                        Provisioning.A_displayName,
                        Provisioning.A_zimbraSharedItem });
        
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().accountsByGrants(GRANTEE_IDS, true, false);
        searchOpts.setFilter(filter);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING); // so our assertion below will always work
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(2, accounts.size());
        assertEquals(acct1.getName(), accounts.get(0).getName());
        assertEquals(acct2.getName(), accounts.get(1).getName());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
    }
    
    @Test
    public void searchDirectory_accountsOnServerAndCosHasSubordinates() throws Exception {
        String COS_ID = prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME).getId();
        
        Account acct = createAccount("searchDirectory_accountsOnServerAndCosHasSubordinates");
        Signature sig1 = prov.createSignature(acct, "sig1", new HashMap<String, Object>());
        Signature sig2 = prov.createSignature(acct, "sig2", new HashMap<String, Object>());
                
        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().accountsOnServerAndCosHasSubordinates(
                prov.getLocalServer().getServiceHostname(), COS_ID);
        searchOpts.setFilter(filter);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(1, accounts.size());
        assertEquals(acct.getName(), accounts.get(0).getName());
        deleteAccount(acct);
    }
    
    @Test
    public void searchDirectory_CMBSearchAccountsOnly() throws Exception {
        Account acct1 = createAccount("searchDirectory_CMBSearchAccountsOnly-1");
        
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "TRUE");
        Account acct2 = createAccount("searchDirectory_CMBSearchAccountsOnly-2", acct2Attrs);
        
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        Account acct3 = createAccount("searchDirectory_CMBSearchAccountsOnly-3", acct3Attrs);
        
        String [] returnAttrs = {Provisioning.A_displayName, Provisioning.A_zimbraId, Provisioning.A_uid,
                Provisioning.A_zimbraArchiveAccount, Provisioning.A_zimbraMailHost};
        
        // use domain so our assertion will work, production code does not a domain
        SearchAccountsOptions searchOpts = new SearchAccountsOptions(domain, returnAttrs);
        searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        searchOpts.setSortOpt(SortOpt.SORT_DESCENDING);
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().CMBSearchAccountsOnly();
        searchOpts.setFilter(filter);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(2, accounts.size());
        assertEquals(acct3.getName(), accounts.get(0).getName());
        assertEquals(acct1.getName(), accounts.get(1).getName());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
        
        
        /*
        // legacy code and LDAP trace before refactoring
        List<NamedEntry> accounts = prov.searchAccounts(
                "(|(!(" + Provisioning.A_zimbraExcludeFromCMBSearch + "=*))(" +
                Provisioning.A_zimbraExcludeFromCMBSearch + "=FALSE))",
                attrs, null, false, Provisioning.searchDirectoryStringToMask("accounts"));
        
        Oct  9 13:00:09 pshao-macbookpro-2 slapd[73952]: conn=1327 op=101 SRCH base="" scope=2 deref=0 filter="(&(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))"
        Oct  9 13:00:09 pshao-macbookpro-2 slapd[73952]: conn=1327 op=101 SRCH attr=zimbraCOSId objectClass zimbraDomainName zimbraACE displayName zimbraId uid zimbraArchiveAccount zimbraMailHost
        */
        
        /*
         * LDAP trace after reactoring
         * 
         Oct  9 13:43:26 pshao-macbookpro-2 slapd[73952]: conn=1345 op=107 SRCH base="ou=people,dc=com,dc=zimbra,dc=qa,dc=unittest,dc=testldapprovaccount" scope=2 deref=0 filter="(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))"
         Oct  9 13:43:26 pshao-macbookpro-2 slapd[73952]: conn=1345 op=107 SRCH attr=zimbraCOSId objectClass zimbraDomainName zimbraACE displayName zimbraId uid zimbraArchiveAccount zimbraMailHost
         */
    }
    
    
    @Test
    public void searchDirectory_CMBSearchAccountsOnlyWithArchive() throws Exception {
        
        Account acct1 = createAccount("searchDirectory_CMBSearchAccountsOnlyWithArchive-1");
        
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "TRUE");
        Account acct2 = createAccount("searchDirectory_CMBSearchAccountsOnlyWithArchive-2", acct2Attrs);
        
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        Account acct3 = createAccount("searchDirectory_CMBSearchAccountsOnlyWithArchive-3", acct3Attrs);
        
        Map<String, Object> acct4Attrs = Maps.newHashMap();
        acct4Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        acct4Attrs.put(Provisioning.A_zimbraArchiveAccount, "archive@test.com");
        Account acct4 = createAccount("searchDirectory_CMBSearchAccountsOnlyWithArchive-4", acct4Attrs);
        
        String [] returnAttrs = {Provisioning.A_displayName, Provisioning.A_zimbraId, Provisioning.A_uid,
                Provisioning.A_zimbraArchiveAccount, Provisioning.A_zimbraMailHost};
        
        // use domain so our assertion will work, production code does not a domain
        SearchAccountsOptions searchOpts = new SearchAccountsOptions(domain, returnAttrs);
        searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        searchOpts.setSortOpt(SortOpt.SORT_DESCENDING);
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().CMBSearchAccountsOnlyWithArchive();
        searchOpts.setFilter(filter);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(1, accounts.size());
        assertEquals(acct4.getName(), accounts.get(0).getName());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
        deleteAccount(acct4);
        
        /*
        // legacy code and LDAP trace before refactoring
        List<NamedEntry> accounts = prov.searchAccounts(
                "(&(" + Provisioning.A_zimbraArchiveAccount + "=*)(|(!(" +
                Provisioning.A_zimbraExcludeFromCMBSearch + "=*))(" +
                Provisioning.A_zimbraExcludeFromCMBSearch + "=FALSE)))",
                returnAttrs,null,false,Provisioning.searchDirectoryStringToMask("accounts"));
    
        Oct  9 16:40:05 pshao-macbookpro-2 slapd[73952]: conn=1388 op=172 SRCH base="" scope=2 deref=0 filter="(&(&(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))"
        Oct  9 16:40:05 pshao-macbookpro-2 slapd[73952]: conn=1388 op=172 SRCH attr=zimbraCOSId objectClass zimbraDomainName zimbraACE displayName zimbraId uid zimbraArchiveAccount zimbraMailHost
        */
        
        /* 
         * LDAP trace after reactoring
         * 
        Oct  9 17:03:11 pshao-macbookpro-2 slapd[73952]: conn=1413 op=125 SRCH base="ou=people,dc=com,dc=zimbra,dc=qa,dc=unittest,dc=testldapprovaccount" scope=2 deref=0 filter="(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))"
        Oct  9 17:03:11 pshao-macbookpro-2 slapd[73952]: conn=1413 op=125 SRCH attr=zimbraCOSId objectClass zimbraDomainName zimbraACE displayName zimbraId uid zimbraArchiveAccount zimbraMailHost
        */
    }

    
    @Test
    public void searchDirectory_CMBSearchNonSystemResourceAccountsOnly() throws Exception {
        Account acct1 = createAccount("searchDirectory_CMBSearchNonSystemResourceAccountsOnly-1");
        
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "TRUE");
        Account acct2 = createAccount("searchDirectory_CMBSearchNonSystemResourceAccountsOnly-2", acct2Attrs);
        
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        Account acct3 = createAccount("searchDirectory_CMBSearchNonSystemResourceAccountsOnly-3", acct3Attrs);
        
        Map<String, Object> acct4Attrs = Maps.newHashMap();
        acct4Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        acct4Attrs.put(Provisioning.A_zimbraIsSystemResource, "TRUE");
        Account acct4 = createAccount("searchDirectory_CMBSearchNonSystemResourceAccountsOnly-4", acct4Attrs);

        Map<String, Object> acct5Attrs = Maps.newHashMap();
        acct5Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        acct5Attrs.put(Provisioning.A_zimbraIsSystemResource, "FALSE");
        Account acct5 = createAccount("searchDirectory_CMBSearchNonSystemResourceAccountsOnly-5", acct5Attrs);

        
        String [] returnAttrs = {Provisioning.A_displayName, Provisioning.A_zimbraId, Provisioning.A_uid,
                Provisioning.A_zimbraArchiveAccount, Provisioning.A_zimbraMailHost};
        
        // use domain so our assertion will work, production code does not a domain
        SearchAccountsOptions searchOpts = new SearchAccountsOptions(domain, returnAttrs);
        searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        searchOpts.setSortOpt(SortOpt.SORT_DESCENDING);
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().CMBSearchNonSystemResourceAccountsOnly();
        searchOpts.setFilter(filter);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(3, accounts.size());
        assertEquals(acct5.getName(), accounts.get(0).getName());
        assertEquals(acct3.getName(), accounts.get(1).getName());
        assertEquals(acct1.getName(), accounts.get(2).getName());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
        deleteAccount(acct4);
        deleteAccount(acct5);
        
        /*
        // legacy code and LDAP trace before refactoring
        List<NamedEntry> accounts = prov.searchAccounts(
                "(&(!(" + Provisioning.A_zimbraIsSystemResource + "=*))(|(!(" +
                Provisioning.A_zimbraExcludeFromCMBSearch + "=*))(" +
                Provisioning.A_zimbraExcludeFromCMBSearch + "=FALSE)))",
                returnAttrs, null, false, Provisioning.searchDirectoryStringToMask("accounts"));
                
        Oct  9 14:55:09 pshao-macbookpro-2 slapd[73952]: conn=1352 op=172 SRCH base="" scope=2 deref=0 filter="(&(&(!(zimbraIsSystemResource=*))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))"
        Oct  9 14:55:09 pshao-macbookpro-2 slapd[73952]: conn=1352 op=172 SRCH attr=zimbraCOSId objectClass zimbraDomainName zimbraACE displayName zimbraId uid zimbraArchiveAccount zimbraMailHost
        */
        
        /*
         * LDAP trace after reactoring
         * 
        Oct  9 16:18:04 pshao-macbookpro-2 slapd[73952]: conn=1381 op=127 SRCH base="ou=people,dc=com,dc=zimbra,dc=qa,dc=unittest,dc=testldapprovaccount" scope=2 deref=0 filter="(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(!(zimbraIsSystemResource=TRUE))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))"
        Oct  9 16:18:04 pshao-macbookpro-2 slapd[73952]: conn=1381 op=127 SRCH attr=zimbraCOSId objectClass zimbraDomainName zimbraACE displayName zimbraId uid zimbraArchiveAccount zimbraMailHost
        */                
    }
    
    @Test
    public void searchDirectory_sslClientCertPrincipalMap() throws Exception {
        Account acct1 = createAccount("searchDirectory_sslClientCertPrincipalMap-1");
        
        String filterStr = "(uid=searchDirectory_sslClientCertPrincipalMap*)";
        
        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        searchOpts.setMaxResults(1);
        searchOpts.setFilterString(FilterId.ACCOUNT_BY_SSL_CLENT_CERT_PRINCIPAL_MAP, filterStr);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(1, accounts.size());
        assertEquals(acct1.getName(), accounts.get(0).getName());
        
        // create another account with same uid prefix
        Account acct2 = createAccount("searchDirectory_sslClientCertPrincipalMap-2");
        
        searchOpts = new SearchAccountsOptions();
        searchOpts.setMaxResults(1);
        searchOpts.setFilterString(FilterId.ACCOUNT_BY_SSL_CLENT_CERT_PRINCIPAL_MAP, filterStr);
        
        boolean caughtTooManySearchResultsException = false;
        try {
            accounts = prov.searchDirectory(searchOpts);
        } catch (ServiceException e) {
            if (AccountServiceException.TOO_MANY_SEARCH_RESULTS.equals(e.getCode())) {
                caughtTooManySearchResultsException = true;
            }
        }
        assertTrue(caughtTooManySearchResultsException);
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        
        /*
        // legacy code and LDAP trace before refactoring
         * 
        
        String filterStr = "(uid=searchDirectory_sslClientCertPrincipalMap)";
        filter = "(&" + ZLdapFilterFactory.getInstance().allAccounts().toFilterString() + filterStr + ")";
                
        SearchOptions options = new SearchOptions();
        options.setMaxResults(1);
        options.setFlags(Provisioning.SO_NO_FIXUP_OBJECTCLASS);
        options.setQuery(filter);
        
        // should return at most one entry.  If more than one entries were matched,
        // TOO_MANY_SEARCH_RESULTS will be thrown
        List<NamedEntry> entries = prov.searchDirectory(options);
        
        Oct  9 19:28:20 pshao-macbookpro-2 slapd[73952]: conn=1417 op=165 SRCH base="" scope=2 deref=0 filter="(&(objectClass=zimbraAccount)(uid=searchdirectory_sslclientcertprincipalmap))"
        Oct  9 19:28:20 pshao-macbookpro-2 slapd[73952]: conn=1417 op=165 SEARCH RESULT tag=101 err=0 nentries=0 text=
        */
        
        /*
         // LDAP trace after refactoring
         Oct  9 19:58:39 pshao-macbookpro-2 slapd[73952]: conn=1438 op=220 SRCH base="" scope=2 deref=0 filter="(&(|(objectClass=zimbraAccount)(objectClass=zimbraCalendarResource))(uid=searchdirectory_sslclientcertprincipalmap))"

         */
    }
    
    @Test
    public void searchDirectory() throws Exception {
        String granteeId = LdapUtilCommon.generateUUID();
        byte gtype = ACL.GRANTEE_USER;
        
        SearchObjectsOptions opts = new SearchObjectsOptions();
        if (gtype == ACL.GRANTEE_USER) {
            opts.addType(SearchDirectoryObjectType.accounts);
            opts.addType(SearchDirectoryObjectType.resources);
        } else if (gtype == ACL.GRANTEE_GROUP) {
            opts.addType(SearchDirectoryObjectType.distributionlists);
        } else if (gtype == ACL.GRANTEE_COS) {
            opts.addType(SearchDirectoryObjectType.coses);
        } else if (gtype == ACL.GRANTEE_DOMAIN) {
            opts.addType(SearchDirectoryObjectType.domains);
        } else {
            throw ServiceException.INVALID_REQUEST("invalid grantee type for revokeOrphanGrants", null);
        }
        
        String query = "(" + Provisioning.A_zimbraId + "=" + granteeId + ")";
        opts.setFilterString(FilterId.SEARCH_GRANTEE, query);
        opts.setOnMaster(true);  // search the grantee on LDAP master

        Provisioning prov = Provisioning.getInstance();
        List<NamedEntry> entries = prov.searchDirectory(opts);
        
        
        /*
        // logacy code and LDAP trace
        int flags = 0;
        if (gtype == ACL.GRANTEE_USER)
            flags |= (Provisioning.SD_ACCOUNT_FLAG | Provisioning.SD_CALENDAR_RESOURCE_FLAG) ;
        else if (gtype == ACL.GRANTEE_GROUP)
            flags |= Provisioning.SD_DISTRIBUTION_LIST_FLAG;
        else if (gtype == ACL.GRANTEE_COS)
            flags |= Provisioning.SD_COS_FLAG;
        else if (gtype == ACL.GRANTEE_DOMAIN)
            flags |= Provisioning.SD_DOMAIN_FLAG;
        else
            throw ServiceException.INVALID_REQUEST("invalid grantee type for revokeOrphanGrants", null);

        String query = "(" + Provisioning.A_zimbraId + "=" + granteeId + ")";

        Provisioning.SearchOptions opts = new SearchOptions();
        opts.setFlags(flags);
        opts.setQuery(query);
        opts.setOnMaster(true);  // search the grantee on LDAP master

        Provisioning prov = Provisioning.getInstance();
        List<NamedEntry> entries = prov.searchDirectory(opts);
        
        Oct  9 23:09:26 pshao-macbookpro-2 slapd[73952]: conn=1531 op=209 SRCH base="" scope=2 deref=0 filter="(&(zimbraId=8b435b63-40c3-4de7-b105-869cbafea29b)(|(objectClass=zimbraAccount)(objectClass=zimbraCalendarResource)))"
        */
        
        /*
         // LDAP trace after refactoring
        Oct  9 23:26:59 pshao-macbookpro-2 slapd[73952]: conn=1535 op=209 SRCH base="" scope=2 deref=0 filter="(&(|(objectClass=zimbraAccount)(objectClass=zimbraCalendarResource))(zimbraId=561fcc6d-6a09-432e-8346-3f1752eea3f9))"

         */
    }

}
