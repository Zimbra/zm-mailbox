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
package com.zimbra.qa.unittest.prov.ldap;

import java.util.List;

import org.junit.*;

import com.google.common.collect.Lists;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.qa.QA.Bug;

import static org.junit.Assert.*;

public class TestLdapZLdapFilter extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static ZLdapFilterFactory filterDactory;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        filterDactory = ZLdapFilterFactory.getInstance();
    }
    
    private String genUUID() {
        return LdapUtil.generateUUID();
    }
    
    private void verifyStatString(FilterId filterId, ZLdapFilter zLdapFilter) throws Exception {
        assertEquals(filterId.getStatString(), zLdapFilter.getStatString());
    }
    
    private void verify(FilterId filterId, String expected, ZLdapFilter actual) 
    throws Exception {
        String filter = actual.toFilterString();
        assertEquals(expected, filter);
        verifyStatString(filterId, actual);
    }
    
    @Test
    public void hasSubordinates() throws Exception {
        String filter = LegacyLdapFilter.hasSubordinates();
        ZLdapFilter zLdapFilter = filterDactory.hasSubordinates();
        verify(FilterId.HAS_SUBORDINATES, filter, zLdapFilter);
    }
    
    @Test
    public void createdLaterOrEqual() throws Exception {
        String GENERALIZED_TIME = "20111005190522Z";
        
        String filter = LegacyLdapFilter.createdLaterOrEqual(GENERALIZED_TIME);
        ZLdapFilter zLdapFilter = filterDactory.createdLaterOrEqual(GENERALIZED_TIME);
        verify(FilterId.CREATED_LATEROREQUAL, filter, zLdapFilter);
    }
    
    @Test
    public void anyEntry() throws Exception {
        String filter = LegacyLdapFilter.anyEntry();
        ZLdapFilter zLdapFilter = filterDactory.anyEntry();
        verify(FilterId.ANY_ENTRY, filter, zLdapFilter);
    }
    
    @Test
    public void fromFilterString() throws Exception {
        String FILTER_STR = "(blah=123)";
        ZLdapFilter zLdapFilter = filterDactory.fromFilterString(
                FilterId.AUTO_PROVISION_SEARCH, FILTER_STR);
        verify(FilterId.AUTO_PROVISION_SEARCH, FILTER_STR, zLdapFilter);
    }
    
    @Test
    public void presenceFilter() throws Exception {
        String ATTR = "foo";
        
        String filter = LegacyLdapFilter.presenceFilter(ATTR);
        ZLdapFilter zLdapFilter = filterDactory.presenceFilter(FilterId.UNITTEST, ATTR);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void equalityFilter() throws Exception {
        String ATTR = "foo";
        String VALUE = "bar";
        
        String filter = LegacyLdapFilter.equalityFilter(ATTR, VALUE);
        ZLdapFilter zLdapFilter = filterDactory.equalityFilter(FilterId.UNITTEST, ATTR, VALUE);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void greaterOrEqualFilter() throws Exception {
        String ATTR = "foo";
        String VALUE = "bar";
        
        String filter = LegacyLdapFilter.greaterOrEqualFilter(ATTR, VALUE);
        ZLdapFilter zLdapFilter = filterDactory.greaterOrEqualFilter(FilterId.UNITTEST, ATTR, VALUE);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void lessOrEqualFilter() throws Exception {
        String ATTR = "foo";
        String VALUE = "bar";
        
        String filter = LegacyLdapFilter.lessOrEqualFilter(ATTR, VALUE);
        ZLdapFilter zLdapFilter = filterDactory.lessOrEqualFilter(FilterId.UNITTEST, ATTR, VALUE);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void startsWithFilter() throws Exception {
        String ATTR = "foo";
        String VALUE = "bar";
        
        String filter = LegacyLdapFilter.startsWithFilter(ATTR, VALUE);
        ZLdapFilter zLdapFilter = filterDactory.startsWithFilter(FilterId.UNITTEST, ATTR, VALUE);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void endsWithFilter() throws Exception {
        String ATTR = "foo";
        String VALUE = "bar";
        
        String filter = LegacyLdapFilter.endsWithFilter(ATTR, VALUE);
        ZLdapFilter zLdapFilter = filterDactory.endsWithFilter(FilterId.UNITTEST, ATTR, VALUE);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void substringFilter() throws Exception {
        String ATTR = "foo";
        String VALUE = "bar";
        
        String filter = LegacyLdapFilter.substringFilter(ATTR, VALUE);
        ZLdapFilter zLdapFilter = filterDactory.substringFilter(FilterId.UNITTEST, ATTR, VALUE);
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
    
    @Test
    public void andWith() throws Exception {
        String FILTER1 = "(foo=1)";
        String FILTER2 = "(bar=2)";
        
        String filter = LegacyLdapFilter.andWith(FILTER1, FILTER2);
        ZLdapFilter zLdapFilter = filterDactory.andWith(
                /* use ADMIN_SEARCH instead of UNITTEST to distinguish with filter id for FILER2,
                 * filter id of the first filter should be used in the result filter
                 */
                filterDactory.fromFilterString(FilterId.ADMIN_SEARCH, FILTER1), 
                filterDactory.fromFilterString(FilterId.UNITTEST, FILTER2));
        verify(FilterId.ADMIN_SEARCH, filter, zLdapFilter);
    }
    
    @Test
    public void negate() throws Exception {
        String FILTER = "(foo=bar)";
        
        String filter = LegacyLdapFilter.negate(FILTER);
        ZLdapFilter zLdapFilter = filterDactory.negate(
                filterDactory.fromFilterString(FilterId.UNITTEST, FILTER));
        verify(FilterId.UNITTEST, filter, zLdapFilter);
    }
  
    @Test
    public void addrsExist() throws Exception {
        String[] ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr2@test.com"};
        
        String filter = LegacyLdapFilter.addrsExist(ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.addrsExist(ADDRS);
        verify(FilterId.ADDRS_EXIST, filter, zLdapFilter);
    }
    
    @Test
    public void allAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAccounts();
        verify(FilterId.ALL_ACCOUNTS, filter, zLdapFilter);
    }
    
    @Test
    public void allAccountsOnly() throws Exception {
        String filter = LegacyLdapFilter.allAccountsOnly();
        ZLdapFilter zLdapFilter = filterDactory.allAccountsOnly();
        verify(FilterId.ALL_ACCOUNTS_ONLY, filter, zLdapFilter);
    }
    
    @Test
    public void allAdminAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAdminAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAdminAccounts();
        verify(FilterId.ALL_ADMIN_ACCOUNTS, filter, zLdapFilter);
    }
    
    @Test
    public void allNonSystemAccounts() throws Exception {
        String filter = LegacyLdapFilter.allNonSystemAccounts();
        // (&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource))(!(zimbraIsSystemResource=TRUE)))
        
        ZLdapFilter zLdapFilter = filterDactory.allNonSystemAccounts();
        String zFilter = zLdapFilter.toFilterString();
        // (&(&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource)))(!(zimbraIsSystemResource=TRUE)))
        
        // assertEquals(filter, zFilter);  the diff is OK
        verifyStatString(FilterId.ALL_NON_SYSTEM_ACCOUNTS, zLdapFilter);
    }
    
    @Test
    public void accountByForeignPrincipal() throws Exception {
        String FOREIFN_PRINCIPAL = getTestName();
        
        String filter = LegacyLdapFilter.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        verify(FilterId.ACCOUNT_BY_FOREIGN_PRINCIPAL, filter, zLdapFilter);
    }
    
    @Test
    public void accountById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.accountById(ID);
        ZLdapFilter zLdapFilter = filterDactory.accountById(ID);
        verify(FilterId.ACCOUNT_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void accountByMemberOf() throws Exception {
        String MEMBEROF = getTestName();
            
        String filter = LegacyLdapFilter.accountByMemberOf(MEMBEROF);
        ZLdapFilter zLdapFilter = filterDactory.accountByMemberOf(MEMBEROF);
        verify(FilterId.ACCOUNT_BY_MEMBEROF, filter, zLdapFilter);
    }
    
    @Test
    public void accountByName() throws Exception {
        String NAME = getTestName();
            
        String filter = LegacyLdapFilter.accountByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.accountByName(NAME);
        verify(FilterId.ACCOUNT_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void adminAccountByRDN() throws Exception {
        String NAMING_RDN_ATTR = "uid";
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        ZLdapFilter zLdapFilter = filterDactory.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        verify(FilterId.ADMIN_ACCOUNT_BY_RDN, filter, zLdapFilter);
    }
    
    @Test
    public void accountsHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.accountsHomedOnServer(SERVER.getServiceHostname());
        verify(FilterId.ACCOUNTS_HOMED_ON_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void accountsHomedOnServerAccountOnly() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServerAccountsOnly(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.accountsHomedOnServerAccountsOnly(SERVER.getServiceHostname());
        verify(FilterId.ACCOUNTS_HOMED_ON_SERVER_ACCOUNTS_ONLY, filter, zLdapFilter);
    }
    
    @Test
    public void homedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.homedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.homedOnServer(SERVER.getServiceHostname());
        verify(FilterId.HOMED_ON_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void accountsOnServerAndCosHasSubordinates() throws Exception {
        Server SERVER = prov.getLocalServer();
        String COS_ID = genUUID();
        
        String filter = LegacyLdapFilter.accountsOnServerOnCosHasSubordinates(SERVER.getServiceHostname(), COS_ID);
        ZLdapFilter zLdapFilter = filterDactory.accountsOnServerAndCosHasSubordinates(SERVER.getServiceHostname(), COS_ID);
        verify(FilterId.ACCOUNTS_ON_SERVER_AND_COS_HAS_SUBORDINATES, filter, zLdapFilter);
    }
    
    @Test
    public void accountsByExternalGrant() throws Exception {
        String GRANTEE_EMAIL = "accountsSharedWith@test.com";
        
        String legacyFilter = String.format("(&(objectClass=zimbraAccount)(zimbraSharedItem=granteeId:%s*))", GRANTEE_EMAIL);
        
        String filter = LegacyLdapFilter.accountsByExternalGrant(GRANTEE_EMAIL);
        ZLdapFilter zLdapFilter = filterDactory.accountsByExternalGrant(GRANTEE_EMAIL);
        
        assertEquals(legacyFilter, filter);
        verify(FilterId.ACCOUNTS_BY_EXTERNAL_GRANT, filter, zLdapFilter);
    }
    
    @Test
    public void accountsByGrants() throws Exception {
        List<String> GRANTEE_IDS = Lists.newArrayList("GRANTEE-ID-1", "GRANTEE-ID-2", "...");
        boolean includePublicShares = true;
        boolean includeAllAuthedShares = true;
        
        // legacy code
        StringBuilder searchQuery = new StringBuilder().append("(&(objectClass=zimbraAccount)(|");
        for (String id : GRANTEE_IDS) {
            searchQuery.append(String.format("(zimbraSharedItem=granteeId:%s*)", id));
        }
        if (includePublicShares) {
            searchQuery.append("(zimbraSharedItem=*granteeType:pub*)");
        }
        if (includeAllAuthedShares) {
            searchQuery.append("(zimbraSharedItem=*granteeType:all*)");
        }
        searchQuery.append("))");
        
        String legacyFilter = searchQuery.toString();
        
        String filter = LegacyLdapFilter.accountsByGrants(GRANTEE_IDS, includePublicShares, includeAllAuthedShares);
        ZLdapFilter zLdapFilter = filterDactory.accountsByGrants(GRANTEE_IDS, includePublicShares, includeAllAuthedShares);
        
        assertEquals(legacyFilter, filter);
        verify(FilterId.ACCOUNTS_BY_GRANTS, filter, zLdapFilter);
    }
    
    @Test
    public void CMBSearchAccountsOnly() throws Exception {
        
        /*
        orig filter before refactoring
        String legacyFilter = 
            "(&(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))";
        */
        
        // moved objectClass to the front
        String legacyFilter = 
            "(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))";

        String filter = LegacyLdapFilter.CMBSearchAccountsOnly();
        
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchAccountsOnly();

        assertEquals(legacyFilter, filter);
        verify(FilterId.CMB_SEARCH_ACCOUNTS_ONLY, filter, zLdapFilter);
    }
    
    @Test
    public void CMBSearchAccountsOnlyWithArchive() throws Exception {
        /*
        orig filter before refactoring
        String legacyFilter = 
            "(&(&(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))";
        */
        
        // moved objectClass to the front
        String legacyFilter = 
            "(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(&(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))))";
        
        String filter = LegacyLdapFilter.CMBSearchAccountsOnlyWithArchive();
        assertEquals(legacyFilter, filter);
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchAccountsOnlyWithArchive();
        String zFilter = zLdapFilter.toFilterString();
        
        // This assertion fails because we optimized it in the new code
        // it is now:
        // (&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(zimbraArchiveAccount=*)(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))
        // System.out.println(zLdapFilter.toFilterString());
        // assertEquals(filter, zFilter);
        // assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.CMB_SEARCH_ACCOUNTS_ONLY_WITH_ARCHIVE, zLdapFilter);
    }
    
    @Test
    public void CMBSearchNonSystemResourceAccountsOnly() throws Exception {
        /*
        orig filter before refactoring
        String legacyFilter = 
            "(&(&(!(zimbraIsSystemResource=*))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource))))";
         */
        
        String legacyFilter = 
            "(&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(&(!(zimbraIsSystemResource=*))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE))))";

        String filter = LegacyLdapFilter.CMBSearchNonSystemResourceAccountsOnly();
        assertEquals(legacyFilter, filter);
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchNonSystemResourceAccountsOnly();
        String zFilter = zLdapFilter.toFilterString();
        
        // This assertion fails because we optimized it in the new code
        // it is now:
        // (&(&(objectClass=zimbraAccount)(!(objectClass=zimbraCalendarResource)))(!(zimbraIsSystemResource=TRUE))(|(!(zimbraExcludeFromCMBSearch=*))(zimbraExcludeFromCMBSearch=FALSE)))
        // System.out.println(zLdapFilter.toFilterString());
        // assertEquals(filter, zFilter);
        // assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.CMB_SEARCH_NON_SYSTEM_RESOURCE_ACCOUNTS_ONLY, zLdapFilter);
    }
    
    @Test
    public void allAliases() throws Exception {
        String filter = LegacyLdapFilter.allAliases();
        ZLdapFilter zLdapFilter = filterDactory.allAliases();
        verify(FilterId.ALL_ALIASES, filter, zLdapFilter);
    }
    
    @Test
    public void allCalendarResources() throws Exception {
        String filter = LegacyLdapFilter.allCalendarResources();
        ZLdapFilter zLdapFilter = filterDactory.allCalendarResources();
        verify(FilterId.ALL_CALENDAR_RESOURCES, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourceByForeignPrincipal() throws Exception {
        String FOREIGN_PRINCIPAL = getTestName();
        
        String filter = LegacyLdapFilter.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        verify(FilterId.CALENDAR_RESOURCE_BY_FOREIGN_PRINCIPAL, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.calendarResourceById(ID);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceById(ID);
        verify(FilterId.CALENDAR_RESOURCE_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourceByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.calendarResourceByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceByName(NAME);
        verify(FilterId.CALENDAR_RESOURCE_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void calendarResourcesHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.calendarResourcesHomedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.calendarResourcesHomedOnServer(SERVER.getServiceHostname());
        verify(FilterId.CALENDAR_RESOURCES_HOMED_ON_SERVER, filter, zLdapFilter);
    }

    
    @Test
    public void allCoses() throws Exception {
        String filter = LegacyLdapFilter.allCoses();
        ZLdapFilter zLdapFilter = filterDactory.allCoses();
        verify(FilterId.ALL_COSES, filter, zLdapFilter);
    }
    
    @Test
    public void cosById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.cosById(ID);
        ZLdapFilter zLdapFilter = filterDactory.cosById(ID);
        verify(FilterId.COS_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void cosesByMailHostPool() throws Exception {
        String SERVER_ID = genUUID();
        
        String filter = LegacyLdapFilter.cosesByMailHostPool(SERVER_ID);
        ZLdapFilter zLdapFilter = filterDactory.cosesByMailHostPool(SERVER_ID);
        verify(FilterId.COSES_BY_MAILHOST_POOL, filter, zLdapFilter);
    }
    
    @Test
    public void allDataSources() throws Exception {
        String filter = LegacyLdapFilter.allDataSources();
        ZLdapFilter zLdapFilter = filterDactory.allDataSources();
        verify(FilterId.ALL_DATA_SOURCES, filter, zLdapFilter);
    }
    
    @Test
    public void dataSourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dataSourceById(ID);
        ZLdapFilter zLdapFilter = filterDactory.dataSourceById(ID);
        verify(FilterId.DATA_SOURCE_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void dataSourceByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.dataSourceByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.dataSourceByName(NAME);
        verify(FilterId.DATA_SOURCE_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void allDistributionLists() throws Exception {
        String filter = LegacyLdapFilter.allDistributionLists();
        ZLdapFilter zLdapFilter = filterDactory.allDistributionLists();
        verify(FilterId.ALL_DISTRIBUTION_LISTS, filter, zLdapFilter);
    }
    
    @Test
    public void distributionListById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.distributionListById(ID);
        ZLdapFilter zLdapFilter = filterDactory.distributionListById(ID);
        verify(FilterId.DISTRIBUTION_LIST_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void distributionListByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.distributionListByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.distributionListByName(NAME);
        verify(FilterId.DISTRIBUTION_LIST_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void distributionListsByMemberAddrs() throws Exception {
        String[] MEMBER_ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr3@test.com"};
        
        String filter = LegacyLdapFilter.distributionListsByMemberAddrs(MEMBER_ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.distributionListsByMemberAddrs(MEMBER_ADDRS);
        verify(FilterId.DISTRIBUTION_LISTS_BY_MEMBER_ADDRS, filter, zLdapFilter);
    }
    
    @Test
    public void dynamicGroupById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dynamicGroupById(ID);
        ZLdapFilter zLdapFilter = filterDactory.dynamicGroupById(ID);
        verify(FilterId.DYNAMIC_GROUP_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void dynamicGroupByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.dynamicGroupByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.dynamicGroupByName(NAME);
        verify(FilterId.DYNAMIC_GROUP_BY_NAME, filter, zLdapFilter);
    }
    
    
    @Test
    public void groupById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.groupById(ID);
        ZLdapFilter zLdapFilter = filterDactory.groupById(ID);
        verify(FilterId.GROUP_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void groupByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.groupByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.groupByName(NAME);
        verify(FilterId.GROUP_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void allDomains() throws Exception {
        String filter = LegacyLdapFilter.allDomains();
        ZLdapFilter zLdapFilter = filterDactory.allDomains();
        verify(FilterId.ALL_DOMAINS, filter, zLdapFilter);
    }
    
    @Test
    public void domainById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.domainById(ID);
        ZLdapFilter zLdapFilter = filterDactory.domainById(ID);
        verify(FilterId.DOMAIN_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void domainByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.domainByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByName(NAME);
        verify(FilterId.DOMAIN_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void domainByKrb5Realm() throws Exception {
        String REALM = getTestName();
        
        String filter = LegacyLdapFilter.domainByKrb5Realm(REALM);
        ZLdapFilter zLdapFilter = filterDactory.domainByKrb5Realm(REALM);
        verify(FilterId.DOMAIN_BY_KRB5_REALM, filter, zLdapFilter);
    }
    
    @Test
    public void domainByVirtualHostame() throws Exception {
        String VIRTUAL_HOST_NAME = getTestName();
        
        String filter = LegacyLdapFilter.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        verify(FilterId.DOMAIN_BY_VIRTUAL_HOSTNAME, filter, zLdapFilter);
    }
    
    @Test
    public void domainByForeignName() throws Exception {
        String FOREIGN_NAME = getTestName();
        
        String filter = LegacyLdapFilter.domainByForeignName(FOREIGN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByForeignName(FOREIGN_NAME);
        verify(FilterId.DOMAIN_BY_FOREIGN_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void domainLabel() throws Exception {
        String filter = LegacyLdapFilter.domainLabel();
        ZLdapFilter zLdapFilter = filterDactory.domainLabel();
        verify(FilterId.DOMAIN_LABEL, filter, zLdapFilter);
    }
    
    @Test
    public void domainLockedForEagerAutoProvision() throws Exception {
        String filter = LegacyLdapFilter.domainLockedForEagerAutoProvision();
        ZLdapFilter zLdapFilter = filterDactory.domainLockedForEagerAutoProvision();
        verify(FilterId.DOMAIN_LOCKED_FOR_AUTO_PROVISION, filter, zLdapFilter);
    }
    
    @Test
    public void globalConfig() throws Exception {
        String filter = LegacyLdapFilter.globalConfig();
        ZLdapFilter zLdapFilter = filterDactory.globalConfig();
        verify(FilterId.GLOBAL_CONFIG, filter, zLdapFilter);
    }
    
    @Test
    public void allIdentities() throws Exception {
        String filter = LegacyLdapFilter.allIdentities();
        ZLdapFilter zLdapFilter = filterDactory.allIdentities();
        verify(FilterId.ALL_IDENTITIES, filter, zLdapFilter);
    }
    
    @Test
    public void identityByName() throws Exception {
        String NAME = getTestName();
        
        String filter = LegacyLdapFilter.identityByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.identityByName(NAME);
        verify(FilterId.IDENTITY_BY_NAME, filter, zLdapFilter);
    }
    
    @Test
    public void allMimeEntries() throws Exception {
        String filter = LegacyLdapFilter.allMimeEntries();
        ZLdapFilter zLdapFilter = filterDactory.allMimeEntries();
        verify(FilterId.ALL_MIME_ENTRIES, filter, zLdapFilter);
    }
    
    @Test
    public void mimeEntryByMimeType() throws Exception {
        String MIME_TYPE = getTestName();
        
        String filter = LegacyLdapFilter.mimeEntryByMimeType(MIME_TYPE);
        ZLdapFilter zLdapFilter = filterDactory.mimeEntryByMimeType(MIME_TYPE);
        verify(FilterId.MIME_ENTRY_BY_MIME_TYPE, filter, zLdapFilter);
    }
    
    @Test
    public void allServers() throws Exception {
        String filter = LegacyLdapFilter.allServers();
        ZLdapFilter zLdapFilter = filterDactory.allServers();
        verify(FilterId.ALL_SERVERS, filter, zLdapFilter);
    }
    
    @Test
    public void serverById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.serverById(ID);
        ZLdapFilter zLdapFilter = filterDactory.serverById(ID);
        verify(FilterId.SERVER_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void serverByService() throws Exception {
        String SERVICE = getTestName();
        
        String filter = LegacyLdapFilter.serverByService(SERVICE);
        ZLdapFilter zLdapFilter = filterDactory.serverByService(SERVICE);
        verify(FilterId.SERVER_BY_SERVICE, filter, zLdapFilter);
    }
    
    @Test
    public void allSignatures() throws Exception {
        String filter = LegacyLdapFilter.allSignatures();
        ZLdapFilter zLdapFilter = filterDactory.allSignatures();
        verify(FilterId.ALL_SIGNATURES, filter, zLdapFilter);
    }
    
    @Test
    public void signatureById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.signatureById(ID);
        ZLdapFilter zLdapFilter = filterDactory.signatureById(ID);
        verify(FilterId.SIGNATURE_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void allXMPPComponents() throws Exception {
        String filter = LegacyLdapFilter.allXMPPComponents();
        ZLdapFilter zLdapFilter = filterDactory.allXMPPComponents();
        verify(FilterId.ALL_XMPP_COMPONENTS, filter, zLdapFilter);
    }
    
    @Test
    public void imComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.imComponentById(ID);
        ZLdapFilter zLdapFilter = filterDactory.imComponentById(ID);
        verify(FilterId.XMPP_COMPONENT_BY_ZIMBRA_XMPP_COMPONENT_ID, filter, zLdapFilter);
    }
    
    @Test
    public void xmppComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.xmppComponentById(ID);
        ZLdapFilter zLdapFilter = filterDactory.xmppComponentById(ID);
        verify(FilterId.XMPP_COMPONENT_BY_ID, filter, zLdapFilter);
    }
    
    @Test
    public void allZimlets() throws Exception {
        String filter = LegacyLdapFilter.allZimlets();
        ZLdapFilter zLdapFilter = filterDactory.allZimlets();
        verify(FilterId.ALL_ZIMLETS, filter, zLdapFilter);
    }
    
    @Test
    @Bug(bug=64260)
    public void bug64260() throws Exception {
        String badStringFilter = "ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*(EMC)))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))";
        
        ZLdapFilter filter;
        
        boolean caughtException = false;
        
        try {
            filter = filterDactory.fromFilterString(FilterId.UNITTEST, badStringFilter);
        } catch (LdapException e) {
            // e.printStackTrace();
            if (LdapException.INVALID_SEARCH_FILTER.equals(e.getCode())) {
                caughtException = true;
            }
        }
        
        assertTrue(caughtException);
        
        String goodStringFilter = "(&(|(displayName=*)(cn=*)(sn=*)(givenName=*)(mail=*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*\\28EMC\\29))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))";
        // String goodStringFilter = "(displayName=*\\28EMC\\29)";
        filter = filterDactory.fromFilterString(FilterId.UNITTEST, goodStringFilter);
        // System.out.println(filter.toFilterString());
        
        /*
DEV:
zmprov mcf -zimbraGalLdapFilterDef 'zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource)))'
zmprov mcf +zimbraGalLdapFilterDef 'zimbraAccountSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(zimbraPhoneticFirstName=*%s*)(zimbraPhoneticLastName=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(objectclass=zimbraCalendarResource))(!(displayName=*\28EMC\29)))'


DF:
zmprov mds galsync@zimbra.com VMware -zimbraGalSyncLdapFilter '(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*EMC))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'
zmprov mds galsync@zimbra.com VMware +zimbraGalSyncLdapFilter '(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*\28EMC\29))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'

zmprov mcf -zimbraGalLdapFilterDef 'ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*EMC))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'
zmprov mcf +zimbraGalLdapFilterDef 'ad:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(givenName=*%s*)(mail=*%s*))(!(msExchHideFromAddressLists=TRUE))(!(displayName=*\28EMC\29))(mailnickname=*)(|(&(objectCategory=person)(objectClass=user)(!(homeMDB=*))(!(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=user)(|(homeMDB=*)(msExchHomeServerName=*)))(&(objectCategory=person)(objectClass=contact))(objectCategory=group)(objectCategory=publicFolder)(objectCategory=msExchDynamicDistributionList)))'
         */
    }
    
    @Test
    public void autoProvisionSearchCreatedLaterThan() throws Exception {
        String FILTER = "(foo=bar)";
        String GENERALIZED_TIME = "20111005190522Z";
        
        String filter = "(&" + FILTER + "(createTimestamp>=" + GENERALIZED_TIME + "))";
        
        ZLdapFilter createdLaterThanFilter = filterDactory.createdLaterOrEqual(GENERALIZED_TIME);
        String filter2 = "(&" + FILTER + createdLaterThanFilter.toFilterString() + ")";
        
        assertEquals(filter, filter2);
        
        ZLdapFilter zLdapFilter = filterDactory.fromFilterString(
                FilterId.AUTO_PROVISION_SEARCH_CREATED_LATERTHAN, filter2);
        verifyStatString(FilterId.AUTO_PROVISION_SEARCH_CREATED_LATERTHAN, zLdapFilter);
    }
    
    @Test
    public void memberOf() throws Exception {
        String DN = "dc=com";
        
        String filter = LegacyLdapFilter.memberOf(DN);
        ZLdapFilter zLdapFilter = filterDactory.memberOf(DN);
        verify(FilterId.MEMBER_OF, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsByDomain(DOMAIN_NAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsOnlyByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsOnlyByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsOnlyByDomain(DOMAIN_NAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllCalendarResourcesByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllCalendarResourcesByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllCalendarResourcesByDomain(DOMAIN_NAME);
        verify(FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN, filter, zLdapFilter);
    }

    @Test
    public void velodromeAllAccountsByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN_AND_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsOnlyByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsOnlyByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsOnlyByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        verify(FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN_AND_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void velodromeAllCalendarResourcesByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllCalendarResourcesByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllCalendarResourcesByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        verify(FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN_AND_SERVER, filter, zLdapFilter);
    }
    
    @Test
    public void dnSubtreeMatch() throws Exception {
        String DN1 = "ou=people,dc=test,dc=com";
        String DN2 = "cn=groups,dc=test,dc=com";
        
        String filter = LegacyLdapFilter.dnSubtreeMatch(DN1, DN2);
        ZLdapFilter zLdapFilter = filterDactory.dnSubtreeMatch(DN1, DN2);
        verify(FilterId.DN_SUBTREE_MATCH, filter, zLdapFilter);
    }
    
    @Test
    public void toIDNFilter() throws Exception {
        assertEquals("(!(zimbraDomainName=*\\e4\\b8\\ad\\e6\\96\\87*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(zimbraDomainName=*\u4e2d\u6587*))"));
        
        assertEquals("(objectClass=*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(objectClass=*)"));
        
        assertEquals("(!(objectClass=\\2a\\2a))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(objectClass=**))"));
        
        assertEquals("(!(objectClass=*abc))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(objectClass=*abc))"));
        
        assertEquals("(!(objectClass=abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(!(objectClass=abc*))"));
        
        assertEquals("(!(objectClass=*abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*abc*)"));

        assertEquals("(|(zimbraMailDeliveryAddress=*@test.xn--fiq228c.com)(zimbraMailAlias=*@test.xn--fiq228c.com))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(|(zimbraMailDeliveryAddress=*@test.\u4e2d\u6587.com)(zimbraMailAlias=*@test.\u4e2d\u6587.com))"));

        
        /*
         * legacy JNDI results
         */
        /*
        assertEquals("!(zimbraDomainName=*\u4e2d\u6587*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(zimbraDomainName=*\u4e2d\u6587*)"));
        
        assertEquals("(!(objectClass=*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*)"));
        
        assertEquals("(!(objectClass=**))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=**)"));

        assertEquals("(!(objectClass=*abc))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*abc)"));
        
        assertEquals("(!(objectClass=abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=abc*)"));
        
        assertEquals("(!(objectClass=*abc*))", 
                LdapEntrySearchFilter.toLdapIDNFilter("!(objectClass=*abc*)"));
        
        assertEquals("(|(zimbraMailDeliveryAddress=*@test.xn--fiq228c.com)(zimbraMailAlias=*@test.xn--fiq228c.com))", 
                LdapEntrySearchFilter.toLdapIDNFilter("(|(zimbraMailDeliveryAddress=*@test.\u4e2d\u6587.com)(zimbraMailAlias=*@test.\u4e2d\u6587.com))"));
        */
    }
    
    @Test
    @Bug(bug=68964)
    public void toIDNFilterTrailingDot() throws Exception {
        assertEquals("(zimbraMailDeliveryAddress=.)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=.)"));

        assertEquals("(zimbraMailDeliveryAddress=...)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=...)"));
        
        assertEquals("(zimbraMailDeliveryAddress=.a.)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=.a.)"));
        
        assertEquals("(zimbraMailDeliveryAddress=a.b.)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=a.b.)"));
        
        assertEquals("(zimbraMailDeliveryAddress=*.*)", 
                LdapEntrySearchFilter.toLdapIDNFilter("(zimbraMailDeliveryAddress=*.*)"));
    }

}
