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
package com.zimbra.qa.unittest.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.entry.LdapDomain;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.qa.unittest.TestUtil;

public class TestLdapProvSearchDirectory extends LdapTest {
    
    private static ProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new ProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Account createAccount(Provisioning prov, String localPart, 
            Domain domain, Map<String, Object> attrs) throws Exception {
        return provUtil.createAccount(localPart, domain, attrs);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return createAccount(prov, localPart, domain, attrs);
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(prov, localPart, domain, null);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }
    
    private CalendarResource createCalendarResource(String localPart,
            Domain domain, Map<String, Object> attrs) throws Exception {
        return provUtil.createCalendarResource(localPart, domain, attrs);
    }
    
    private CalendarResource createCalendarResource(String localPart) throws Exception {
        return createCalendarResource(localPart, domain, null);
    }
    
    private DistributionList createDistributionList(String localPart, 
            Domain domain, Map<String, Object> attrs) throws Exception {
        return provUtil.createDistributionList(localPart, domain, attrs);
    }
    
    private DistributionList createDistributionList(String localPart, Domain domain) 
    throws Exception {
        return createDistributionList(localPart, domain, null);
    }
    
    private DistributionList createDistributionList(String localPart)
    throws Exception {
        return createDistributionList(localPart, domain, null);
    }
    
    private DynamicGroup createDynamicGroup(String localPart, Domain domain) throws Exception {
        return provUtil.createDynamicGroup(localPart, domain, null);
    }
    
    private DynamicGroup createDynamicGroup(String localPart) throws Exception {
        return createDynamicGroup(localPart, domain);
    }
    
    private void deleteGroup(Group group) throws Exception {
        provUtil.deleteGroup(group);
    }
    
    private Server createServer(String serverName, Map<String, Object> attrs) 
    throws Exception {
        return provUtil.createServer(serverName, attrs);
    }
    
    private Server createServer(String serverName) 
    throws Exception {
        return provUtil.createServer(serverName, null);
    }
    
    private void deleteServer(Server server) throws Exception {
        provUtil.deleteServer(server);
    }
    
    @Test
    public void searchAccountsOnServer() throws Exception {
        // create a search domain
        String DOMAIN_NAME = "searchAccountsOnServer." + baseDomainName();
        Domain searchDomain = provUtil.createDomain(DOMAIN_NAME, null);
        
        // create an account and a calendar resource on the domain
        String ACCT_LOCALPART = Names.makeAccountNameLocalPart("searchAccountsOnServer-acct");
        String CR_LOCALPART = Names.makeAccountNameLocalPart("searchAccountsOnServer-cr");
        
        Map<String, Object> crAttrs = Maps.newHashMap();
        crAttrs.put(Provisioning.A_displayName, "ACCT_LOCALPART");
        crAttrs.put(Provisioning.A_zimbraCalResType, Provisioning.CalResType.Equipment.name());
        Account acct = createAccount(prov, ACCT_LOCALPART, searchDomain, null);
        CalendarResource cr = createCalendarResource(CR_LOCALPART, searchDomain, crAttrs);
        
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
        
        // test the visitor interface
        final List<NamedEntry> acctsByVisitor = Lists.newArrayList();
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {

            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                acctsByVisitor.add(entry);
                
            }
        };
        prov.getAllAccounts(domain, visitor);
        assertEquals(3, acctsByVisitor.size());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
        deleteAccount(cr);
    }
    
    @Test
    public void getAllAccountsByDomainAndServer() throws Exception {
        Account acct3 = createAccount("getAllAccounts-3");
        Account acct2 = createAccount("getAllAccounts-2");
        Account acct1 = createAccount("getAllAccounts-1");
        CalendarResource cr = createCalendarResource("getAllAccounts-cr");
        
        final List<NamedEntry> acctsVisitor = Lists.newArrayList();
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {

            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                acctsVisitor.add(entry);
                
            }
        };
        
        Server server = prov.getLocalServer();
        prov.getAllAccounts(domain, server, visitor);
        assertEquals(3, acctsVisitor.size());
        
        acctsVisitor.clear();
        Server otherServer = createServer("getAllAccountsByDomainAndServer");
        prov.getAllAccounts(domain, otherServer, visitor);
        assertEquals(0, acctsVisitor.size());
        
        deleteAccount(acct1);
        deleteAccount(acct2);
        deleteAccount(acct3);
        deleteAccount(cr);
    }
    
    @Test
    public void accountsByExternalGrant() throws Exception {
        String EXT_USER_EMAIL = "user@test.com";
        
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraSharedItem, "granteeId:" + EXT_USER_EMAIL + "blah blah");
        Account acct = createAccount("accountsByExternalGrant", attrs);
        
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
    public void accountsByGrants() throws Exception {
        
        String GRANTEE_ID_1 = LdapUtil.generateUUID();
        String GRANTEE_ID_2 = LdapUtil.generateUUID();
        String GRANTEE_ID_3 = LdapUtil.generateUUID();
        List<String> GRANTEE_IDS = Lists.newArrayList(GRANTEE_ID_1, GRANTEE_ID_2, GRANTEE_ID_3);
        
        Map<String, Object> attrs1 = Maps.newHashMap();
        attrs1.put(Provisioning.A_zimbraSharedItem, "granteeId:" + GRANTEE_ID_3 + "blah blah");
        Account acct1 = createAccount("accountsByGrants-1", attrs1);
        
        Map<String, Object> attrs2 = Maps.newHashMap();
        attrs2.put(Provisioning.A_zimbraSharedItem, "blah" + "granteeType:pub" + " blah");
        Account acct2 = createAccount("accountsByGrants-2", attrs2);
        
        Map<String, Object> attrs3 = Maps.newHashMap();
        attrs3.put(Provisioning.A_zimbraSharedItem, "blah" + "granteeType:all" + " blah");
        Account acct3 = createAccount("accountsByGrants-3", attrs3);
        
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
    public void accountsOnServerAndCosHasSubordinates() throws Exception {
        /*
         *  X.501: 14.4.4 Has Subordinates operational attribute
         *  is not supported in ubid InMemoryLdapServer
         */
        SKIP_IF_IN_MEM_LDAP_SERVER("Subordinates operational attribute is not supported by InMemoryDirectoryServer");
        
        String COS_ID = prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME).getId();
        
        Account acct = createAccount("accountsOnServerAndCosHasSubordinates");
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
    public void CMBSearchAccountsOnly() throws Exception {
        Account acct1 = createAccount("CMBSearchAccountsOnly-1");
        
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "TRUE");
        Account acct2 = createAccount("CMBSearchAccountsOnly-2", acct2Attrs);
        
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        Account acct3 = createAccount("CMBSearchAccountsOnly-3", acct3Attrs);
        
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
    public void CMBSearchAccountsOnlyWithArchive() throws Exception {
        
        Account acct1 = createAccount("CMBSearchAccountsOnlyWithArchive-1");
        
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "TRUE");
        Account acct2 = createAccount("CMBSearchAccountsOnlyWithArchive-2", acct2Attrs);
        
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        Account acct3 = createAccount("CMBSearchAccountsOnlyWithArchive-3", acct3Attrs);
        
        Map<String, Object> acct4Attrs = Maps.newHashMap();
        acct4Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        acct4Attrs.put(Provisioning.A_zimbraArchiveAccount, "archive@test.com");
        Account acct4 = createAccount("CMBSearchAccountsOnlyWithArchive-4", acct4Attrs);
        
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
    public void CMBSearchNonSystemResourceAccountsOnly() throws Exception {
        Account acct1 = createAccount("CMBSearchNonSystemResourceAccountsOnly-1");
        
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "TRUE");
        Account acct2 = createAccount("CMBSearchNonSystemResourceAccountsOnly-2", acct2Attrs);
        
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        Account acct3 = createAccount("CMBSearchNonSystemResourceAccountsOnly-3", acct3Attrs);
        
        Map<String, Object> acct4Attrs = Maps.newHashMap();
        acct4Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        acct4Attrs.put(Provisioning.A_zimbraIsSystemResource, "TRUE");
        Account acct4 = createAccount("CMBSearchNonSystemResourceAccountsOnly-4", acct4Attrs);

        Map<String, Object> acct5Attrs = Maps.newHashMap();
        acct5Attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "FALSE");
        acct5Attrs.put(Provisioning.A_zimbraIsSystemResource, "FALSE");
        Account acct5 = createAccount("CMBSearchNonSystemResourceAccountsOnly-5", acct5Attrs);

        
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
    public void sslClientCertPrincipalMap() throws Exception {
        Account acct1 = createAccount("sslClientCertPrincipalMap-1");
        
        String filterStr = "(uid=sslClientCertPrincipalMap*)";
        
        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        searchOpts.setMaxResults(1);
        searchOpts.setFilterString(FilterId.ACCOUNT_BY_SSL_CLENT_CERT_PRINCIPAL_MAP, filterStr);
        List<NamedEntry> accounts = prov.searchDirectory(searchOpts);
        
        assertEquals(1, accounts.size());
        assertEquals(acct1.getName(), accounts.get(0).getName());
        
        // create another account with same uid prefix
        Account acct2 = createAccount("sslClientCertPrincipalMap-2");
        
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
        
        String filterStr = "(uid=sslClientCertPrincipalMap)";
        filter = "(&" + ZLdapFilterFactory.getInstance().allAccounts().toFilterString() + filterStr + ")";
                
        SearchOptions options = new SearchOptions();
        options.setMaxResults(1);
        options.setFlags(Provisioning.SO_NO_FIXUP_OBJECTCLASS);
        options.setQuery(filter);
        
        // should return at most one entry.  If more than one entries were matched,
        // TOO_MANY_SEARCH_RESULTS will be thrown
        List<NamedEntry> entries = prov.searchDirectory(options);
        
        Oct  9 19:28:20 pshao-macbookpro-2 slapd[73952]: conn=1417 op=165 SRCH base="" scope=2 deref=0 filter="(&(objectClass=zimbraAccount)(uid=sslclientcertprincipalmap))"
        Oct  9 19:28:20 pshao-macbookpro-2 slapd[73952]: conn=1417 op=165 SEARCH RESULT tag=101 err=0 nentries=0 text=
        */
        
        /*
         // LDAP trace after refactoring
         Oct  9 19:58:39 pshao-macbookpro-2 slapd[73952]: conn=1438 op=220 SRCH base="" scope=2 deref=0 filter="(&(|(objectClass=zimbraAccount)(objectClass=zimbraCalendarResource))(uid=sslclientcertprincipalmap))"

         */
    }
    
    @Test
    public void searchGrantee() throws Exception {
        String granteeId = LdapUtil.generateUUID();
        byte gtype = ACL.GRANTEE_USER;
        
        SearchDirectoryOptions opts = new SearchDirectoryOptions();
        if (gtype == ACL.GRANTEE_USER) {
            opts.addType(SearchDirectoryOptions.ObjectType.accounts);
            opts.addType(SearchDirectoryOptions.ObjectType.resources);
        } else if (gtype == ACL.GRANTEE_GROUP) {
            opts.addType(SearchDirectoryOptions.ObjectType.distributionlists);
        } else if (gtype == ACL.GRANTEE_COS) {
            opts.addType(SearchDirectoryOptions.ObjectType.coses);
        } else if (gtype == ACL.GRANTEE_DOMAIN) {
            opts.addType(SearchDirectoryOptions.ObjectType.domains);
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
    
    @Test
    public void getAllCalendarResources() throws Exception {
        Account acct = createAccount("getAllCalendarResources-acct");
        CalendarResource cr = createCalendarResource("getAllCalendarResources-cr");
        
        List<CalendarResource> crs = prov.getAllCalendarResources(domain);
        assertEquals(1, crs.size());
        assertEquals(cr.getName(), crs.get(0).getName());
        
        deleteAccount(acct);
        deleteAccount(cr);
    }
    
    @Test
    public void getAllCalendarResourcesOnServer() throws Exception {
        Account acct = createAccount("getAllCalendarResourcesOnServer-acct");
        CalendarResource cr = createCalendarResource("getAllCalendarResourcesOnServer-cr");
        
        final List<NamedEntry> crs = Lists.newArrayList();
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                crs.add(entry);
            }
        };
        
        Server server = prov.getLocalServer();
        prov.getAllCalendarResources(domain, server, visitor);
        assertEquals(1, crs.size());
        assertEquals(cr.getName(), crs.get(0).getName());
        
        crs.clear();
        Server otherServer = createServer("getAllCalendarResourcesOnServer");
        assertNotNull(otherServer);
        prov.getAllCalendarResources(domain, otherServer, visitor);
        assertEquals(0, crs.size());
        
        deleteAccount(acct);
        deleteAccount(cr);
        deleteServer(otherServer);
    }
    
    @Test
    public void getAllDistributionLists() throws Exception {
        DistributionList dl = createDistributionList("getAllDistributionList");
        
        List<DistributionList> dls = prov.getAllDistributionLists(domain);
        assertEquals(1, dls.size());
        assertEquals(dl.getName(), dls.get(0).getName());
        
        deleteGroup(dl);
    }
    
    @Test
    public void getAllGroups() throws Exception {
        DistributionList dl = createDistributionList("getAllGroups-dl");
        DynamicGroup dg = createDynamicGroup("getAllGroups-dg");
        
        // create a sub domain
        String SUB_DOMAIN_NAME = "sub." + baseDomainName();
        Domain subDomain = provUtil.createDomain(SUB_DOMAIN_NAME, null);
        
        // create a DL and a DG in the sub domain
        DistributionList dlSub = createDistributionList("getAllGroups-dl-sub", subDomain);
        DynamicGroup dgSub = createDynamicGroup("getAllGroups-dg-sub", subDomain);
        
        List<Group> groups = prov.getAllGroups(domain);
        
        assertEquals(2, groups.size());
        
        // should be sorted asc
        assertEquals(dg.getName(), groups.get(0).getName());
        assertEquals(dl.getName(), groups.get(1).getName());
        
        deleteGroup(dl);
        deleteGroup(dg);
    }
    
    @Test
    public void renameDomainSearchAcctCrDl() throws Exception {
        Account acct = createAccount("renameDomainSearchAcctCrDl-acct");
        CalendarResource cr = createCalendarResource("renameDomainSearchAcctCrDl-cr");
        DistributionList dl = createDistributionList("renameDomainSearchAcctCrDl-dl");
        
        
        String domainDN = ((LdapDomain) domain).getDN();
        String searchBase = ((LdapProv) prov).getDIT().domainDNToAccountSearchDN(domainDN);
        
        final List<NamedEntry> entries = Lists.newArrayList();
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                // System.out.println(entry.getName());
                entries.add(entry);
            }
        };
        
        SearchDirectoryOptions options = new SearchDirectoryOptions();
        options.setDomain(domain);
        options.setOnMaster(true);
        options.setFilterString(FilterId.RENAME_DOMAIN, null);
        options.setTypes(ObjectType.accounts, ObjectType.resources, ObjectType.distributionlists);
        prov.searchDirectory(options, visitor);
        assertEquals(3, entries.size());
        
        /*
         // legacy code and ldap trace
        int flags = Provisioning.SD_ACCOUNT_FLAG + Provisioning.SD_CALENDAR_RESOURCE_FLAG + Provisioning.SD_DISTRIBUTION_LIST_FLAG;
        ((LdapProvisioning) prov).searchObjects(null, null, searchBase, flags, visitor, 0);
         * 
         Oct 12 22:10:43 pshao-macbookpro-2 slapd[3065]: conn=1081 op=434 SRCH base="ou=people,dc=com,dc=zimbra,dc=qa,dc=unittest,dc=testldapprovsearchdirectory" scope=2 deref=0 filter="(|(objectClass=zimbraAccount)(objectClass=zimbraDistributionList)(objectClass=zimbraCalendarResource))"
         Oct 12 22:10:43 pshao-macbookpro-2 slapd[3065]: conn=1081 op=434 SEARCH RESULT tag=101 err=0 nentries=3 text=
 
         */
    }

    @Test
    public void searchAliasTarget() throws Exception {
        Account acct = createAccount("searchAliasTarget-acct");
        CalendarResource cr = createCalendarResource("searchAliasTarget-cr");
        DistributionList dl = createDistributionList("searchAliasTarget-dl");
        DynamicGroup dg = createDynamicGroup("searchAliasTarget-dg");
        
        // prepend a digit so the order returned from SearchDirectory is predictable
        prov.addAlias(acct, TestUtil.getAddress("1-acct-alias", domain.getName()));
        prov.addAlias(cr, TestUtil.getAddress("2-cr-alias", domain.getName()));
        prov.addGroupAlias(dl, TestUtil.getAddress("3-dl-alias", domain.getName()));
        prov.addGroupAlias(dg, TestUtil.getAddress("4-dg-alias", domain.getName()));
        
        SearchDirectoryOptions options = new SearchDirectoryOptions(domain);
        options.setTypes(ObjectType.aliases);
        options.setSortOpt(SortOpt.SORT_ASCENDING);
        options.setFilterString(FilterId.UNITTEST, null);
        List<NamedEntry> aliases = prov.searchDirectory(options);
        
        assertEquals(4, aliases.size());
        Alias acctAlias = (Alias) aliases.get(0);
        Alias crAlias = (Alias) aliases.get(1);
        Alias dlAlias = (Alias) aliases.get(2);
        Alias dgAlias = (Alias) aliases.get(3);
        
        NamedEntry acctAliasTarget = prov.searchAliasTarget(acctAlias, true);
        assertEquals(acct.getId(), acctAliasTarget.getId());
        
        NamedEntry crAliasTarget = prov.searchAliasTarget(crAlias, true);
        assertEquals(cr.getId(), crAliasTarget.getId());
        
        NamedEntry dlAliasTarget = prov.searchAliasTarget(dlAlias, true);
        assertEquals(dl.getId(), dlAliasTarget.getId());
        
        NamedEntry dgAliasTarget = prov.searchAliasTarget(dgAlias, true);
        assertEquals(dg.getId(), dgAliasTarget.getId());
        
        deleteAccount(acct);
        deleteAccount(cr);
        deleteGroup(dl);
        deleteGroup(dg);
    }
    
}
