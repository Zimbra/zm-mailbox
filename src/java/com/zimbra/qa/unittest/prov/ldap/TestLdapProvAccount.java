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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.google.common.collect.Maps;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.cs.ldap.LdapConstants;

public class TestLdapProvAccount extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName(), null);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return provUtil.createAccount(localPart, domain, attrs);
    }
    
    private CalendarResource createCalendarResource(String localPart) throws Exception {
        return createCalendarResource(localPart, null);
    }
    
    private CalendarResource createCalendarResource(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return provUtil.createCalendarResource(localPart, domain, attrs);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }
    
    private Server createServer(String serverName, Map<String, Object> attrs) 
    throws Exception {
        return provUtil.createServer(serverName, attrs);
    }
    
    private Server createServer(String serverName) 
    throws Exception {
        return createServer(serverName, null);
    }
    
    private void deleteServer(Server server) throws Exception {
        provUtil.deleteServer(server);
    }
    
    private Cos createCos(String cosName, Map<String, Object> attrs) throws Exception {
        return provUtil.createCos(cosName, attrs);
    }
    
    private void deleteCos(Cos cos) throws Exception {
        provUtil.deleteCos(cos);
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
    
    @Test
    public void createAccount() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        deleteAccount(acct);
    }
    
    @Test
    public void createAccountAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
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
        String ADMIN_ACCT_NAME_1 = genAcctNameLocalPart("1");
        String ADMIN_ACCT_NAME_2 = genAcctNameLocalPart("2");
        
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
    
    /*
     * This test does not work with JNDI.  The trailing space in data source name 
     * got stripped after the rename.
     */
    @Test
    public void renameAccount() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        
        String ACCT_NEW_NAME_LOCALPART = 
            Names.makeAccountNameLocalPart(genAcctNameLocalPart("new"));
        
        String ACCT_NEW_NAME = TestUtil.getAddress(
                ACCT_NEW_NAME_LOCALPART,
                domain.getName()).toLowerCase();
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String acctId = acct.getId();
        
        // create some children
        String DATA_SOURCE_NAME_1;
        String DATA_SOURCE_NAME_2;
        String DATA_SOURCE_NAME_3;
        
        DATA_SOURCE_NAME_1 = Names.makeDataSourceName(genDataSourceName("1"));
        DATA_SOURCE_NAME_2 = Names.makeDataSourceName(genDataSourceName("2"));
        DATA_SOURCE_NAME_3 = Names.makeDataSourceName(genDataSourceName("3"));
        
        DataSource ds1 = createDataSource(acct, DATA_SOURCE_NAME_1);
        DataSource ds2 = createDataSource(acct, DATA_SOURCE_NAME_2);
        DataSource ds3 = createDataSource(acct, DATA_SOURCE_NAME_3);
        String DATA_SOURCE_ID_1 = ds1.getId();
        String DATA_SOURCE_ID_2 = ds2.getId();
        String DATA_SOURCE_ID_3 = ds3.getId();
        
        // set zimbraPrefAllowAddressForDelegatedSender
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, acct.getName());
        prov.modifyAttrs(acct, attrs);
        
        prov.renameAccount(acctId, ACCT_NEW_NAME);
        
        prov.flushCache(CacheEntryType.account, null);
        Account renamedAcct = prov.get(AccountBy.name, ACCT_NEW_NAME);
        
        assertEquals(acctId, renamedAcct.getId());
        assertEquals(ACCT_NEW_NAME, renamedAcct.getName());
        assertEquals(ACCT_NEW_NAME_LOCALPART, renamedAcct.getUid());
        
        // make sure children are moved
        assertEquals(DATA_SOURCE_ID_1, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_1).getId());
        assertEquals(DATA_SOURCE_ID_2, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_2).getId());
        assertEquals(DATA_SOURCE_ID_3, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_3).getId());
        
        // make sure zimbraPrefAllowAddressForDelegatedSender is updated
        Set<String> addrsForDelegatedSender = renamedAcct.getMultiAttrSet(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
        assertEquals(1, addrsForDelegatedSender.size());
        assertTrue(addrsForDelegatedSender.contains(ACCT_NEW_NAME));
        
        deleteAccount(renamedAcct);
    }
    
    @Test
    public void renameAccountDomainChanged() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        
        String NEW_DOMAIN_NAME = genDomainSegmentName() + "." + baseDomainName();
        Domain newDomain = provUtil.createDomain(NEW_DOMAIN_NAME);
        String ACCT_NEW_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart("new"));
        String ACCT_NEW_NAME =  
            TestUtil.getAddress(ACCT_NEW_NAME_LOCALPART, NEW_DOMAIN_NAME).toLowerCase();
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        String acctId = acct.getId();
        
        // create some children
        String DATA_SOURCE_NAME_1;
        String DATA_SOURCE_NAME_2;
        String DATA_SOURCE_NAME_3;
        
        DATA_SOURCE_NAME_1 = Names.makeDataSourceName("ds1");
        DATA_SOURCE_NAME_2 = Names.makeDataSourceName("ds2");
        DATA_SOURCE_NAME_3 = Names.makeDataSourceName("ds3");
        
        DataSource ds1 = createDataSource(acct, DATA_SOURCE_NAME_1);
        DataSource ds2 = createDataSource(acct, DATA_SOURCE_NAME_2);
        DataSource ds3 = createDataSource(acct, DATA_SOURCE_NAME_3);
        String DATA_SOURCE_ID_1 = ds1.getId();
        String DATA_SOURCE_ID_2 = ds2.getId();
        String DATA_SOURCE_ID_3 = ds3.getId();
        
        
        // create some aliases
        String ALIAS_NAME_LOCALPART_1 = Names.makeAccountNameLocalPart(genAcctNameLocalPart("alias1")); 
        String ALIAS_NAME_LOCALPART_2 = Names.makeAccountNameLocalPart(genAcctNameLocalPart("alias2"));
        String ALIAS_NAME_1 = TestUtil.getAddress(ALIAS_NAME_LOCALPART_1, domain.getName()).toLowerCase();
        String ALIAS_NAME_2 = TestUtil.getAddress(ALIAS_NAME_LOCALPART_2, domain.getName()).toLowerCase();
        String ALIAS_NEW_NAME_1 = TestUtil.getAddress(ALIAS_NAME_LOCALPART_1, NEW_DOMAIN_NAME).toLowerCase();
        String ALIAS_NEW_NAME_2 = TestUtil.getAddress(ALIAS_NAME_LOCALPART_2, NEW_DOMAIN_NAME).toLowerCase();
        prov.addAlias(acct, ALIAS_NAME_1);
        prov.addAlias(acct, ALIAS_NAME_2);
        
        // set zimbraPrefAllowAddressForDelegatedSender
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, 
                new String[]{acct.getName(), ALIAS_NAME_1, ALIAS_NAME_2});
        prov.modifyAttrs(acct, attrs);
        
        prov.renameAccount(acctId, ACCT_NEW_NAME);
        
        prov.flushCache(CacheEntryType.account, null);
        Account renamedAcct = prov.get(AccountBy.name, ACCT_NEW_NAME);
        
        assertEquals(acctId, renamedAcct.getId());
        assertEquals(ACCT_NEW_NAME, renamedAcct.getName());
        
        // make sure children are moved
        assertEquals(DATA_SOURCE_ID_1, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_1).getId());
        assertEquals(DATA_SOURCE_ID_2, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_2).getId());
        assertEquals(DATA_SOURCE_ID_3, prov.get(renamedAcct, Key.DataSourceBy.name, DATA_SOURCE_NAME_3).getId());
        
        // make sure aliases in the same domain is renamed
        Account acctByAlias1 = prov.get(AccountBy.name, ALIAS_NEW_NAME_1);
        assertEquals(acctId, acctByAlias1.getId());
        Account acctByAlias2 = prov.get(AccountBy.name, ALIAS_NEW_NAME_2);
        assertEquals(acctId, acctByAlias2.getId());
        Set<String> aliases = renamedAcct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertEquals(2, aliases.size());
        assertTrue(aliases.contains(ALIAS_NEW_NAME_1));
        assertTrue(aliases.contains(ALIAS_NEW_NAME_2));
        
        // make sure zimbraPrefAllowAddressForDelegatedSender is updated
        Set<String> addrsForDelegatedSender = 
            renamedAcct.getMultiAttrSet(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
        assertEquals(3, addrsForDelegatedSender.size());
        assertTrue(addrsForDelegatedSender.contains(ACCT_NEW_NAME));
        assertTrue(addrsForDelegatedSender.contains(ALIAS_NEW_NAME_1));
        assertTrue(addrsForDelegatedSender.contains(ALIAS_NEW_NAME_2));
        
        deleteAccount(renamedAcct);
        provUtil.deleteDomain(newDomain);
    }
    
    @Test
    public void renameAccountAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String ACCT_NAME_EXISTS_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart("exists"));
        
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
    public void mailHost() throws Exception {
        Map<String, Object> server1Attrs = Maps.newHashMap();
        server1Attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server1 = createServer(genServerName("1"), server1Attrs);
        
        Map<String, Object> server2Attrs = Maps.newHashMap();
        server2Attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server2 = createServer(genServerName("2"), server2Attrs);
        
        Server server3 = createServer(genServerName("3"));
        
        // specifies a mail host
        Map<String, Object> acct1Attrs = Maps.newHashMap();
        acct1Attrs.put(Provisioning.A_zimbraMailHost, server1.getName());
        Account acct1 = createAccount(genAcctNameLocalPart("1"), acct1Attrs);
        assertEquals(server1.getId(), prov.getServer(acct1).getId());
        
        // specifies a mail host without mailbox server
        Map<String, Object> acct2Attrs = Maps.newHashMap();
        acct2Attrs.put(Provisioning.A_zimbraMailHost, server3.getName());
        boolean caughtException = false;
        try {
            Account acct2 = createAccount(genAcctNameLocalPart("2"), acct2Attrs);
        } catch (ServiceException e) {
            if (ServiceException.INVALID_REQUEST.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        // use a server pool in cos
        Map<String, Object> cos1Attrs = Maps.newHashMap();
        cos1Attrs.put(Provisioning.A_zimbraMailHostPool, server2.getId());
        Cos cos1 = createCos(genCosName("1"), cos1Attrs);
        Map<String, Object> acct3Attrs = Maps.newHashMap();
        acct3Attrs.put(Provisioning.A_zimbraCOSId, cos1.getId());
        Account acct3 = createAccount(genAcctNameLocalPart("3"), acct3Attrs);
        assertEquals(server2.getId(), prov.getServer(acct3).getId());
        
        // use a server pool in cos, but the server pool does not contain a 
        // server with mailbox server, should fallback to the local server
        Map<String, Object> cos2Attrs = Maps.newHashMap();
        cos2Attrs.put(Provisioning.A_zimbraMailHostPool, server3.getId());
        Cos cos2 = createCos(genCosName("3"), cos2Attrs);
        Map<String, Object> acct4Attrs = Maps.newHashMap();
        acct4Attrs.put(Provisioning.A_zimbraCOSId, cos2.getId());
        Account acct4 = createAccount(genAcctNameLocalPart("4"), acct4Attrs);
        assertEquals(prov.getLocalServer().getId(), prov.getServer(acct4).getId());
        
        
        deleteAccount(acct1);
        deleteAccount(acct3);
        deleteAccount(acct4);
        deleteServer(server1);
        deleteServer(server2);
        deleteServer(server3);
        deleteCos(cos1);
        deleteCos(cos2);
    }

}
