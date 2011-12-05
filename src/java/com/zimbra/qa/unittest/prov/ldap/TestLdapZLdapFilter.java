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
    
    @Test
    public void hasSubordinates() throws Exception {
        String filter = LegacyLdapFilter.hasSubordinates();
        ZLdapFilter zLdapFilter = filterDactory.hasSubordinates();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.HAS_SUBORDINATES, zLdapFilter);
    }
    
    @Test
    public void createdLaterOrEqual() throws Exception {
        String GENERALIZED_TIME = "20111005190522Z";
        
        String filter = LegacyLdapFilter.createdLaterOrEqual(GENERALIZED_TIME);
        ZLdapFilter zLdapFilter = filterDactory.createdLaterOrEqual(GENERALIZED_TIME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.CREATED_LATEROREQUAL, zLdapFilter);
    }
    
    @Test
    public void anyEntry() throws Exception {
        String filter = LegacyLdapFilter.anyEntry();
        ZLdapFilter zLdapFilter = filterDactory.anyEntry();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ANY_ENTRY, zLdapFilter);
    }
    
    @Test
    public void fromFilterString() throws Exception {
        String FILTER_STR = "(blah=123)";
        ZLdapFilter zLdapFilter = filterDactory.fromFilterString(
                FilterId.AUTO_PROVISION_SEARCH, FILTER_STR);
        assertEquals(FILTER_STR, zLdapFilter.toFilterString());
        verifyStatString(FilterId.AUTO_PROVISION_SEARCH, zLdapFilter);
    }
    
    
    @Test
    public void addrsExist() throws Exception {
        String[] ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr2@test.com"};
        
        String filter = LegacyLdapFilter.addrsExist(ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.addrsExist(ADDRS);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ADDRS_EXIST, zLdapFilter);
    }
    
    @Test
    public void allAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAccounts();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_ACCOUNTS, zLdapFilter);
    }
    
    @Test
    public void allAccountsOnly() throws Exception {
        String filter = LegacyLdapFilter.allAccountsOnly();
        ZLdapFilter zLdapFilter = filterDactory.allAccountsOnly();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_ACCOUNTS_ONLY, zLdapFilter);
    }
    
    @Test
    public void allAdminAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAdminAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAdminAccounts();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_ADMIN_ACCOUNTS, zLdapFilter);
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
        String FOREIFN_PRINCIPAL = "accountByForeignPrincipal";
        
        String filter = LegacyLdapFilter.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNT_BY_FOREIGN_PRINCIPAL, zLdapFilter);
    }
    
    @Test
    public void accountById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.accountById(ID);
        ZLdapFilter zLdapFilter = filterDactory.accountById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNT_BY_ID, zLdapFilter);
    }
    
    @Test
    public void accountByMemberOf() throws Exception {
        String MEMBEROF = "accountByMemberOf";
            
        String filter = LegacyLdapFilter.accountByMemberOf(MEMBEROF);
        ZLdapFilter zLdapFilter = filterDactory.accountByMemberOf(MEMBEROF);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNT_BY_MEMBEROF, zLdapFilter);
    }
    
    @Test
    public void accountByName() throws Exception {
        String NAME = "accountByName";
            
        String filter = LegacyLdapFilter.accountByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.accountByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNT_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void adminAccountByRDN() throws Exception {
        String NAMING_RDN_ATTR = "uid";
        String NAME = "adminAccountByRDN";
        
        String filter = LegacyLdapFilter.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        ZLdapFilter zLdapFilter = filterDactory.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ADMIN_ACCOUNT_BY_RDN, zLdapFilter);
    }
    
    @Test
    public void accountsHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.accountsHomedOnServer(SERVER.getServiceHostname());
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNTS_HOMED_ON_SERVER, zLdapFilter);
    }
    
    @Test
    public void accountsHomedOnServerAccountOnly() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServerAccountsOnly(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.accountsHomedOnServerAccountsOnly(SERVER.getServiceHostname());
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNTS_HOMED_ON_SERVER_ACCOUNTS_ONLY, zLdapFilter);
    }
    
    @Test
    public void homedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.homedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.homedOnServer(SERVER.getServiceHostname());
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.HOMED_ON_SERVER, zLdapFilter);
    }
    
    @Test
    public void accountsOnServerAndCosHasSubordinates() throws Exception {
        Server SERVER = prov.getLocalServer();
        String COS_ID = genUUID();
        
        String filter = LegacyLdapFilter.accountsOnServerOnCosHasSubordinates(SERVER.getServiceHostname(), COS_ID);
        ZLdapFilter zLdapFilter = filterDactory.accountsOnServerAndCosHasSubordinates(SERVER.getServiceHostname(), COS_ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ACCOUNTS_ON_SERVER_AND_COS_HAS_SUBORDINATES, zLdapFilter);
    }
    
    @Test
    public void accountsByExternalGrant() throws Exception {
        String GRANTEE_EMAIL = "accountsSharedWith@test.com";
        
        String legacyFilter = String.format("(&(objectClass=zimbraAccount)(zimbraSharedItem=granteeId:%s*))", GRANTEE_EMAIL);
        
        String filter = LegacyLdapFilter.accountsByExternalGrant(GRANTEE_EMAIL);
        ZLdapFilter zLdapFilter = filterDactory.accountsByExternalGrant(GRANTEE_EMAIL);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.ACCOUNTS_BY_EXTERNAL_GRANT, zLdapFilter);
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
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.ACCOUNTS_BY_GRANTS, zLdapFilter);
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
        assertEquals(legacyFilter, filter);
        
        ZLdapFilter zLdapFilter = filterDactory.CMBSearchAccountsOnly();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(legacyFilter, zFilter);
        verifyStatString(FilterId.CMB_SEARCH_ACCOUNTS_ONLY, zLdapFilter);
        
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
        System.out.println(zLdapFilter.toFilterString());
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
    public void allCalendarResources() throws Exception {
        String filter = LegacyLdapFilter.allCalendarResources();
        ZLdapFilter zLdapFilter = filterDactory.allCalendarResources();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_CALENDAR_RESOURCES, zLdapFilter);
    }
    
    @Test
    public void calendarResourceByForeignPrincipal() throws Exception {
        String FOREIGN_PRINCIPAL = "calendarResourceByForeignPrincipal";
        
        String filter = LegacyLdapFilter.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.CALENDAR_RESOURCE_BY_FOREIGN_PRINCIPAL, zLdapFilter);
    }
    
    @Test
    public void calendarResourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.calendarResourceById(ID);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.CALENDAR_RESOURCE_BY_ID, zLdapFilter);
    }
    
    @Test
    public void calendarResourceByName() throws Exception {
        String NAME = "calendarResourceByName";
        
        String filter = LegacyLdapFilter.calendarResourceByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.calendarResourceByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.CALENDAR_RESOURCE_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void calendarResourcesHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.calendarResourcesHomedOnServer(SERVER.getServiceHostname());
        ZLdapFilter zLdapFilter = filterDactory.calendarResourcesHomedOnServer(SERVER.getServiceHostname());
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.CALENDAR_RESOURCES_HOMED_ON_SERVER, zLdapFilter);
    }

    
    @Test
    public void allCoses() throws Exception {
        String filter = LegacyLdapFilter.allCoses();
        ZLdapFilter zLdapFilter = filterDactory.allCoses();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_COSES, zLdapFilter);
    }
    
    @Test
    public void cosById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.cosById(ID);
        ZLdapFilter zLdapFilter = filterDactory.cosById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.COS_BY_ID, zLdapFilter);
    }
    
    @Test
    public void cosesByMailHostPool() throws Exception {
        String SERVER_ID = genUUID();
        
        String filter = LegacyLdapFilter.cosesByMailHostPool(SERVER_ID);
        ZLdapFilter zLdapFilter = filterDactory.cosesByMailHostPool(SERVER_ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.COSES_BY_MAILHOST_POOL, zLdapFilter);
    }
    
    @Test
    public void allDataSources() throws Exception {
        String filter = LegacyLdapFilter.allDataSources();
        ZLdapFilter zLdapFilter = filterDactory.allDataSources();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_DATA_SOURCES, zLdapFilter);
    }
    
    @Test
    public void dataSourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dataSourceById(ID);
        ZLdapFilter zLdapFilter = filterDactory.dataSourceById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DATA_SOURCE_BY_ID, zLdapFilter);
    }
    
    @Test
    public void dataSourceByName() throws Exception {
        String NAME = "dataSourceByName";
        
        String filter = LegacyLdapFilter.dataSourceByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.dataSourceByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DATA_SOURCE_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void allDistributionLists() throws Exception {
        String filter = LegacyLdapFilter.allDistributionLists();
        ZLdapFilter zLdapFilter = filterDactory.allDistributionLists();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_DISTRIBUTION_LISTS, zLdapFilter);
    }
    
    @Test
    public void distributionListById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.distributionListById(ID);
        ZLdapFilter zLdapFilter = filterDactory.distributionListById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DISTRIBUTION_LIST_BY_ID, zLdapFilter);
    }
    
    @Test
    public void distributionListByName() throws Exception {
        String NAME = "distributionListByName";
        
        String filter = LegacyLdapFilter.distributionListByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.distributionListByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DISTRIBUTION_LIST_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void distributionListsByMemberAddrs() throws Exception {
        String[] MEMBER_ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr3@test.com"};
        
        String filter = LegacyLdapFilter.distributionListsByMemberAddrs(MEMBER_ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.distributionListsByMemberAddrs(MEMBER_ADDRS);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DISTRIBUTION_LISTS_BY_MEMBER_ADDRS, zLdapFilter);
    }
    
    @Test
    public void dynamicGroupById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dynamicGroupById(ID);
        ZLdapFilter zLdapFilter = filterDactory.dynamicGroupById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DYNAMIC_GROUP_BY_ID, zLdapFilter);
    }
    
    @Test
    public void dynamicGroupByName() throws Exception {
        String NAME = "dynamicGroupByName";
        
        String filter = LegacyLdapFilter.dynamicGroupByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.dynamicGroupByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DYNAMIC_GROUP_BY_NAME, zLdapFilter);
    }
    
    
    @Test
    public void groupById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.groupById(ID);
        ZLdapFilter zLdapFilter = filterDactory.groupById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.GROUP_BY_ID, zLdapFilter);
    }
    
    @Test
    public void groupByName() throws Exception {
        String NAME = "groupByName";
        
        String filter = LegacyLdapFilter.groupByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.groupByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.GROUP_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void allDomains() throws Exception {
        String filter = LegacyLdapFilter.allDomains();
        ZLdapFilter zLdapFilter = filterDactory.allDomains();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_DOMAINS, zLdapFilter);
    }
    
    @Test
    public void domainById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.domainById(ID);
        ZLdapFilter zLdapFilter = filterDactory.domainById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_BY_ID, zLdapFilter);
    }
    
    @Test
    public void domainByName() throws Exception {
        String NAME = "domainByName";
        
        String filter = LegacyLdapFilter.domainByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void domainByKrb5Realm() throws Exception {
        String REALM = "domainByKrb5Realm";
        
        String filter = LegacyLdapFilter.domainByKrb5Realm(REALM);
        ZLdapFilter zLdapFilter = filterDactory.domainByKrb5Realm(REALM);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_BY_KRB5_REALM, zLdapFilter);
    }
    
    @Test
    public void domainByVirtualHostame() throws Exception {
        String VIRTUAL_HOST_NAME = "domainByVirtualHostame";
        
        String filter = LegacyLdapFilter.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_BY_VIRTUAL_HOSTNAME, zLdapFilter);
    }
    
    @Test
    public void domainByForeignName() throws Exception {
        String FOREIGN_NAME = "domainByForeignName";
        
        String filter = LegacyLdapFilter.domainByForeignName(FOREIGN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.domainByForeignName(FOREIGN_NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_BY_FOREIGN_NAME, zLdapFilter);
    }
    
    @Test
    public void domainLabel() throws Exception {
        String filter = LegacyLdapFilter.domainLabel();
        ZLdapFilter zLdapFilter = filterDactory.domainLabel();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_LABEL, zLdapFilter);
    }
    
    @Test
    public void domainLockedForEagerAutoProvision() throws Exception {
        String filter = LegacyLdapFilter.domainLockedForEagerAutoProvision();
        ZLdapFilter zLdapFilter = filterDactory.domainLockedForEagerAutoProvision();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.DOMAIN_LOCKED_FOR_AUTO_PROVISION, zLdapFilter);
    }
    
    @Test
    public void globalConfig() throws Exception {
        String filter = LegacyLdapFilter.globalConfig();
        ZLdapFilter zLdapFilter = filterDactory.globalConfig();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.GLOBAL_CONFIG, zLdapFilter);
    }
    
    @Test
    public void allIdentities() throws Exception {
        String filter = LegacyLdapFilter.allIdentities();
        ZLdapFilter zLdapFilter = filterDactory.allIdentities();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_IDENTITIES, zLdapFilter);
    }
    
    @Test
    public void identityByName() throws Exception {
        String NAME = "identityByName";
        
        String filter = LegacyLdapFilter.identityByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.identityByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.IDENTITY_BY_NAME, zLdapFilter);
    }
    
    @Test
    public void allMimeEntries() throws Exception {
        String filter = LegacyLdapFilter.allMimeEntries();
        ZLdapFilter zLdapFilter = filterDactory.allMimeEntries();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_MIME_ENTRIES, zLdapFilter);
    }
    
    @Test
    public void mimeEntryByMimeType() throws Exception {
        String MIME_TYPE = "mimeEntryByMimeType";
        
        String filter = LegacyLdapFilter.mimeEntryByMimeType(MIME_TYPE);
        ZLdapFilter zLdapFilter = filterDactory.mimeEntryByMimeType(MIME_TYPE);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.MIME_ENTRY_BY_MIME_TYPE, zLdapFilter);
    }
    
    @Test
    public void allServers() throws Exception {
        String filter = LegacyLdapFilter.allServers();
        ZLdapFilter zLdapFilter = filterDactory.allServers();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_SERVERS, zLdapFilter);
    }
    
    @Test
    public void serverById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.serverById(ID);
        ZLdapFilter zLdapFilter = filterDactory.serverById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.SERVER_BY_ID, zLdapFilter);
    }
    
    @Test
    public void serverByService() throws Exception {
        String SERVICE = "serverByService";
        
        String filter = LegacyLdapFilter.serverByService(SERVICE);
        ZLdapFilter zLdapFilter = filterDactory.serverByService(SERVICE);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.SERVER_BY_SERVICE, zLdapFilter);
    }
    
    @Test
    public void allSignatures() throws Exception {
        String filter = LegacyLdapFilter.allSignatures();
        ZLdapFilter zLdapFilter = filterDactory.allSignatures();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_SIGNATURES, zLdapFilter);
    }
    
    @Test
    public void signatureById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.signatureById(ID);
        ZLdapFilter zLdapFilter = filterDactory.signatureById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.SIGNATURE_BY_ID, zLdapFilter);
    }
    
    @Test
    public void allXMPPComponents() throws Exception {
        String filter = LegacyLdapFilter.allXMPPComponents();
        ZLdapFilter zLdapFilter = filterDactory.allXMPPComponents();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_XMPP_COMPONENTS, zLdapFilter);
    }
    
    @Test
    public void imComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.imComponentById(ID);
        ZLdapFilter zLdapFilter = filterDactory.imComponentById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.XMPP_COMPONENT_BY_ZIMBRA_XMPP_COMPONENT_ID, zLdapFilter);
    }
    
    @Test
    public void xmppComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.xmppComponentById(ID);
        ZLdapFilter zLdapFilter = filterDactory.xmppComponentById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.XMPP_COMPONENT_BY_ID, zLdapFilter);
    }
    
    @Test
    public void allZimlets() throws Exception {
        String filter = LegacyLdapFilter.allZimlets();
        ZLdapFilter zLdapFilter = filterDactory.allZimlets();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.ALL_ZIMLETS, zLdapFilter);
    }
    
    @Test
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
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.MEMBER_OF, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsByDomain(DOMAIN_NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsOnlyByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsOnlyByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsOnlyByDomain(DOMAIN_NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN, zLdapFilter);
    }
    
    @Test
    public void velodromeAllCalendarResourcesByDomain() throws Exception {
        String DOMAIN_NAME = "test.com";
        
        String filter = LegacyLdapFilter.velodromeAllCalendarResourcesByDomain(DOMAIN_NAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllCalendarResourcesByDomain(DOMAIN_NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN, zLdapFilter);
    }

    @Test
    public void velodromeAllAccountsByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.VELODROME_ALL_ACCOUNTS_BY_DOMAIN_AND_SERVER, zLdapFilter);
    }
    
    @Test
    public void velodromeAllAccountsOnlyByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllAccountsOnlyByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllAccountsOnlyByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.VELODROME_ALL_ACCOUNTS_ONLY_BY_DOMAIN_AND_SERVER, zLdapFilter);
    }
    
    @Test
    public void velodromeAllCalendarResourcesByDomainAndServer() throws Exception {
        String DOMAIN_NAME = "test.com";
        String SERVER_SERVICE_HOSTNAME = "server.com";
        
        String filter = LegacyLdapFilter.velodromeAllCalendarResourcesByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        ZLdapFilter zLdapFilter = filterDactory.velodromeAllCalendarResourcesByDomainAndServer(DOMAIN_NAME, SERVER_SERVICE_HOSTNAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        verifyStatString(FilterId.VELODROME_ALL_CALENDAR_RESOURCES_BY_DOMAIN_AND_SERVER, zLdapFilter);
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

}
