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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DataSourceBy;
import com.zimbra.common.datasource.DataSourceType;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.ldap.LdapConstants;

public class TestLdapProvDataSource extends TestLdap {
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
        return TestLdapProvDataSource.class.getName().toLowerCase();
    }
    
    private static DataSource createDataSourceRaw(Provisioning prov,Account acct, 
            String dataSourceName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "123");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        attrs.put(Provisioning.A_zimbraDataSourceHost, "zimbra.com");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        return prov.createDataSource(acct, DataSourceType.pop3, dataSourceName, attrs);
    }
    
    static DataSource createDataSource(Provisioning prov, Account acct, 
            String dataSourceName) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        DataSource dataSource = prov.get(acct, Key.DataSourceBy.name, dataSourceName);
        assertNull(dataSource);
        
        dataSource = createDataSourceRaw(prov, acct, dataSourceName);
        assertNotNull(dataSource);
        
        prov.flushCache(CacheEntryType.account, null);
        dataSource = prov.get(acct, Key.DataSourceBy.name, dataSourceName);
        assertNotNull(dataSource);
        assertEquals(dataSourceName, dataSource.getName());
        
        return dataSource;
    }

    static void deleteDataSource(Provisioning prov, Account acct, DataSource dataSource) 
    throws Exception {
        String dataSourceId = dataSource.getId();
        prov.deleteDataSource(acct, dataSourceId);
        prov.flushCache(CacheEntryType.account, null);
        dataSource = prov.get(acct, Key.DataSourceBy.id, dataSourceId);
        assertNull(dataSource);
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        return TestLdapProvAccount.createAccount(prov, localPart, domain, attrs);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        TestLdapProvAccount.deleteAccount(prov, acct);
    }
    
    private DataSource createDataSourceRaw(Account acct, String dataSourceName) throws Exception {
        return createDataSourceRaw(prov, acct, dataSourceName);
    }
    
    private DataSource createDataSource(Account acct, String dataSourceName) throws Exception {
        return createDataSource(prov, acct, dataSourceName);
    }
    
    private void deleteDataSource(Account acct, DataSource dataSource) throws Exception {
        deleteDataSource(prov, acct, dataSource);
    }
    
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    @Test
    public void createDataSource() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("createDataSource");
        String DATA_SOURCE_NAME = TestLdap.makeDataSourceName("createDataSource");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        assertEquals(acct.getId(), dataSource.getAccount().getId());
        
        deleteDataSource(acct, dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void createDataSourceAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("createDataSourceAlreadyExists");
        String DATA_SOURCE_NAME = TestLdap.makeDataSourceName("createDataSourceAlreadyExists");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        boolean caughtException = false;
        try {
            createDataSourceRaw(acct, DATA_SOURCE_NAME);
        } catch (AccountServiceException e) {
            if (AccountServiceException.DATA_SOURCE_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteDataSource(acct, dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void modifyDataSource() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("modifyDataSource");
        String DATA_SOURCE_NAME = TestLdap.makeDataSourceName("modifyDataSource");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraDataSourceHost;
        String MODIFIED_ATTR_VALUE = "modifyDataSource.com";
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifyDataSource(acct, dataSource.getId(), attrs);
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, Key.DataSourceBy.name, DATA_SOURCE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, dataSource.getAttr(MODIFIED_ATTR_NAME));
        
        deleteDataSource(acct,dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void renameDataSource() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("renameDataSource");
        String DATA_SOURCE_NAME = TestLdap.makeDataSourceName("renameDataSource");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        // modifying zimbraDataSourceName will rename the data source and trigger a LDAP moddn
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraDataSourceName;
        String NEW_DATA_SOURCE_NAME = TestLdap.makeDataSourceName("renameDataSource-new");  
        String MODIFIED_ATTR_VALUE = NEW_DATA_SOURCE_NAME;
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifyDataSource(acct, dataSource.getId(), attrs);
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, Key.DataSourceBy.name, NEW_DATA_SOURCE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, dataSource.getAttr(MODIFIED_ATTR_NAME));
        
        deleteDataSource(acct,dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void getAllDataSources() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("getAllDataSources");
        String DATA_SOURCE_NAME_1 = TestLdap.makeDataSourceName("getAllDataSources-1");
        String DATA_SOURCE_NAME_2 = TestLdap.makeDataSourceName("getAllDataSources-2");
        String DATA_SOURCE_NAME_3 = TestLdap.makeDataSourceName("getAllDataSources-3");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource1 = createDataSource(acct, DATA_SOURCE_NAME_1);
        DataSource dataSource2 = createDataSource(acct, DATA_SOURCE_NAME_2);
        DataSource dataSource3 = createDataSource(acct, DATA_SOURCE_NAME_3);
        
        acct = getFresh(acct);
        List<DataSource> allDataSources = prov.getAllDataSources(acct);
        assertEquals(3, allDataSources.size());
        
        Set<String> allDataSourceIds = new HashSet<String>();
        for (DataSource dataSource : allDataSources) {
            allDataSourceIds.add(dataSource.getId());
        }
        
        assertTrue(allDataSourceIds.contains(dataSource1.getId()));
        assertTrue(allDataSourceIds.contains(dataSource2.getId()));
        assertTrue(allDataSourceIds.contains(dataSource3.getId()));
        
        deleteDataSource(acct,dataSource1);
        deleteDataSource(acct,dataSource2);
        deleteDataSource(acct,dataSource3);
        deleteAccount(acct);
    }
    
    @Test
    public void getDataSource() throws Exception {
        String ACCT_NAME_LOCALPART = TestLdap.makeAccountNameLocalPart("getDataSource");
        String DATA_SOURCE_NAME = TestLdap.makeDataSourceName("getDataSource");
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        String dataSourceId = dataSource.getId();
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, Key.DataSourceBy.id, dataSourceId);
        assertNotNull(dataSource);
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, Key.DataSourceBy.name, DATA_SOURCE_NAME);
        assertNotNull(dataSource);
        
        deleteDataSource(acct,dataSource);
        deleteAccount(acct);
    }
}
