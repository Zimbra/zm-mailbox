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

import org.junit.*;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

import static org.junit.Assert.*;

public class TestLdapZLdapFilter extends TestLdap {
    
    private static Provisioning prov;
    private static ZLdapFilterFactory filterDactory;
    
    @BeforeClass
    public static void init() throws Exception {
        prov = Provisioning.getInstance();
        filterDactory = ZLdapFilterFactory.getInstance();
    }
    
    private String genUUID() {
        return LdapUtilCommon.generateUUID();
    }
    
    @Test
    public void hasSubordinates() throws Exception {
        String filter = LegacyLdapFilter.hasSubordinates();
        ZLdapFilter zLdapFilter = filterDactory.hasSubordinates();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.HAS_SUBORDINATES.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void anyEntry() throws Exception {
        String filter = LegacyLdapFilter.anyEntry();
        ZLdapFilter zLdapFilter = filterDactory.anyEntry();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ANY_ENTRY.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void fromFilterString() throws Exception {
        String FILTER_STR = "(blah=123)";
        ZLdapFilter zLdapFilter = filterDactory.fromFilterString(
                FilterId.AUTO_PROVISION_GET_EXTERNAL_ATTRS, FILTER_STR);
        assertEquals(FILTER_STR, zLdapFilter.toFilterString());
        assertEquals(FilterId.AUTO_PROVISION_GET_EXTERNAL_ATTRS.getStatString(), zLdapFilter.getStatString());
    }
    
    
    @Test
    public void addrsExist() throws Exception {
        String[] ADDRS = new String[]{"addr1@test.com", "addr2@test.com", "addr2@test.com"};
        
        String filter = LegacyLdapFilter.addrsExist(ADDRS);
        ZLdapFilter zLdapFilter = filterDactory.addrsExist(ADDRS);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ADDRS_EXIST.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void allAccounts() throws Exception {
        String filter = LegacyLdapFilter.allAccounts();
        ZLdapFilter zLdapFilter = filterDactory.allAccounts();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ALL_ACCOUNTS.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void allNonSystemAccounts() throws Exception {
        String filter = LegacyLdapFilter.allNonSystemAccounts();
        // (&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource))(!(zimbraIsSystemResource=TRUE)))
        
        ZLdapFilter zLdapFilter = filterDactory.allNonSystemAccounts();
        String zFilter = zLdapFilter.toFilterString();
        // (&(&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource)))(!(zimbraIsSystemResource=TRUE)))
        
        // assertEquals(filter, zFilter);  the diff is OK
        assertEquals(FilterId.ALL_NON_SYSTEM_ACCOUNTS.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void accountByForeignPrincipal() throws Exception {
        String FOREIFN_PRINCIPAL = "accountByForeignPrincipal";
        
        String filter = LegacyLdapFilter.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        ZLdapFilter zLdapFilter = filterDactory.accountByForeignPrincipal(FOREIFN_PRINCIPAL);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ACCOUNT_BY_FOREIGN_PRINCIPAL.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void accountById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.accountById(ID);
        ZLdapFilter zLdapFilter = filterDactory.accountById(ID);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ACCOUNT_BY_ID.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void accountByMemberOf() throws Exception {
        String MEMBEROF = "accountByMemberOf";
            
        String filter = LegacyLdapFilter.accountByMemberOf(MEMBEROF);
        ZLdapFilter zLdapFilter = filterDactory.accountByMemberOf(MEMBEROF);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ACCOUNT_BY_MEMBEROF.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void accountByName() throws Exception {
        String NAME = "accountByName";
            
        String filter = LegacyLdapFilter.accountByName(NAME);
        ZLdapFilter zLdapFilter = filterDactory.accountByName(NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ACCOUNT_BY_NAME.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void adminAccountByRDN() throws Exception {
        String NAMING_RDN_ATTR = "uid";
        String NAME = "adminAccountByRDN";
        
        String filter = LegacyLdapFilter.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        ZLdapFilter zLdapFilter = filterDactory.adminAccountByRDN(NAMING_RDN_ATTR, NAME);
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ADMIN_ACCOUNT_BY_RDN.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void adminAccountByAdminFlag() throws Exception {
        String filter = LegacyLdapFilter.adminAccountByAdminFlag();
        ZLdapFilter zLdapFilter = filterDactory.adminAccountByAdminFlag();
        String zFilter = zLdapFilter.toFilterString();
        assertEquals(filter, zFilter);
        assertEquals(FilterId.ADMIN_ACCOUNT_BY_ADMIN_FLAG.getStatString(), zLdapFilter.getStatString());
    }
    
    @Test
    public void accountsHomedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.accountsHomedOnServer(SERVER);
        String zFilter = filterDactory.accountsHomedOnServer(SERVER).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void homedOnServer() throws Exception {
        Server SERVER = prov.getLocalServer();
        
        String filter = LegacyLdapFilter.homedOnServer(SERVER);
        String zFilter = filterDactory.homedOnServer(SERVER).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void homedOnServerByServerName() throws Exception {
        String SERVER_NAME = "homedOnServerByServerName";
        
        String filter = LegacyLdapFilter.homedOnServer(SERVER_NAME);
        String zFilter = filterDactory.homedOnServer(SERVER_NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void accountsOnServerOnCosHasSubordinates() throws Exception {
        Server SERVER = prov.getLocalServer();
        String COS_ID = genUUID();
        
        String filter = LegacyLdapFilter.accountsOnServerOnCosHasSubordinates(SERVER, COS_ID);
        ZLdapFilter zLapFilter = filterDactory.accountsOnServerOnCosHasSubordinates(SERVER, COS_ID);
        String zFilter = zLapFilter.toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allCalendarResources() throws Exception {
        String filter = LegacyLdapFilter.allCalendarResources();
        String zFilter = filterDactory.allCalendarResources().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void calendarResourceByForeignPrincipal() throws Exception {
        String FOREIGN_PRINCIPAL = "calendarResourceByForeignPrincipal";
        
        String filter = LegacyLdapFilter.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL);
        String zFilter = filterDactory.calendarResourceByForeignPrincipal(FOREIGN_PRINCIPAL).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void calendarResourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.calendarResourceById(ID);
        String zFilter = filterDactory.calendarResourceById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void calendarResourceByName() throws Exception {
        String NAME = "calendarResourceByName";
        
        String filter = LegacyLdapFilter.calendarResourceByName(NAME);
        String zFilter = filterDactory.calendarResourceByName(NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allCoses() throws Exception {
        String filter = LegacyLdapFilter.allCoses();
        String zFilter = filterDactory.allCoses().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void cosById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.cosById(ID);
        String zFilter = filterDactory.cosById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void cosesByMailHostPool() throws Exception {
        String SERVER_NAME = "cosesByMailHostPool";
        
        String filter = LegacyLdapFilter.cosesByMailHostPool(SERVER_NAME);
        String zFilter = filterDactory.cosesByMailHostPool(SERVER_NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allDataSources() throws Exception {
        String filter = LegacyLdapFilter.allDataSources();
        String zFilter = filterDactory.allDataSources().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void dataSourceById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.dataSourceById(ID);
        String zFilter = filterDactory.dataSourceById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void dataSourceByName() throws Exception {
        String NAME = "dataSourceByName";
        
        String filter = LegacyLdapFilter.dataSourceByName(NAME);
        String zFilter = filterDactory.dataSourceByName(NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allDistributionLists() throws Exception {
        String filter = LegacyLdapFilter.allDistributionLists();
        String zFilter = filterDactory.allDistributionLists().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void distributionListById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.distributionListById(ID);
        String zFilter = filterDactory.distributionListById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void distributionListByName() throws Exception {
        String NAME = "distributionListByName";
        
        String filter = LegacyLdapFilter.distributionListByName(NAME);
        String zFilter = filterDactory.distributionListByName(NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allDomains() throws Exception {
        String filter = LegacyLdapFilter.allDomains();
        String zFilter = filterDactory.allDomains().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void domainById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.domainById(ID);
        String zFilter = filterDactory.domainById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void domainByName() throws Exception {
        String NAME = "domainByName";
        
        String filter = LegacyLdapFilter.domainByName(NAME);
        String zFilter = filterDactory.domainByName(NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void domainByKrb5Realm() throws Exception {
        String REALM = "domainByKrb5Realm";
        
        String filter = LegacyLdapFilter.domainByKrb5Realm(REALM);
        String zFilter = filterDactory.domainByKrb5Realm(REALM).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void domainByVirtualHostame() throws Exception {
        String VIRTUAL_HOST_NAME = "domainByVirtualHostame";
        
        String filter = LegacyLdapFilter.domainByVirtualHostame(VIRTUAL_HOST_NAME);
        String zFilter = filterDactory.domainByVirtualHostame(VIRTUAL_HOST_NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void domainByForeignName() throws Exception {
        String FOREIGN_NAME = "domainByForeignName";
        
        String filter = LegacyLdapFilter.domainByForeignName(FOREIGN_NAME);
        String zFilter = filterDactory.domainByForeignName(FOREIGN_NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void domainLabel() throws Exception {
        String filter = LegacyLdapFilter.domainLabel();
        String zFilter = filterDactory.domainLabel().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allIdentities() throws Exception {
        String filter = LegacyLdapFilter.allIdentities();
        String zFilter = filterDactory.allIdentities().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void identityByName() throws Exception {
        String NAME = "identityByName";
        
        String filter = LegacyLdapFilter.identityByName(NAME);
        String zFilter = filterDactory.identityByName(NAME).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allMimeEntries() throws Exception {
        String filter = LegacyLdapFilter.allMimeEntries();
        String zFilter = filterDactory.allMimeEntries().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void mimeEntryByMimeType() throws Exception {
        String MIME_TYPE = "mimeEntryByMimeType";
        
        String filter = LegacyLdapFilter.mimeEntryByMimeType(MIME_TYPE);
        String zFilter = filterDactory.mimeEntryByMimeType(MIME_TYPE).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allServers() throws Exception {
        String filter = LegacyLdapFilter.allServers();
        String zFilter = filterDactory.allServers().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void serverById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.serverById(ID);
        String zFilter = filterDactory.serverById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void serverByService() throws Exception {
        String SERVICE = "serverByService";
        
        String filter = LegacyLdapFilter.serverByService(SERVICE);
        String zFilter = filterDactory.serverByService(SERVICE).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allSignatures() throws Exception {
        String filter = LegacyLdapFilter.allSignatures();
        String zFilter = filterDactory.allSignatures().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void signatureById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.signatureById(ID);
        String zFilter = filterDactory.signatureById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allXMPPComponents() throws Exception {
        String filter = LegacyLdapFilter.allXMPPComponents();
        String zFilter = filterDactory.allXMPPComponents().toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void imComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.imComponentById(ID);
        String zFilter = filterDactory.imComponentById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void xmppComponentById() throws Exception {
        String ID = genUUID();
        
        String filter = LegacyLdapFilter.xmppComponentById(ID);
        String zFilter = filterDactory.xmppComponentById(ID).toFilterString();
        assertEquals(filter, zFilter);
    }
    
    @Test
    public void allZimlets() throws Exception {
        String filter = LegacyLdapFilter.allZimlets();
        String zFilter = filterDactory.allZimlets().toFilterString();
        assertEquals(filter, zFilter);
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
    
}
