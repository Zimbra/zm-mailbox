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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.ldap.LdapConstants;

public class TestLdapProvDataSource {
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        TestLdap.manualInit();
        
        prov = Provisioning.getInstance();
        domain = prov.createDomain(baseDomainName(), new HashMap<String, Object>());
        assertNotNull(domain);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestLdapProvDataSource.class.getName().toLowerCase();
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        String acctName = TestUtil.getAddress(localPart, domain.getName());
        Account acct = prov.get(AccountBy.name, acctName);
        assertNull(acct);
                
        acct = prov.createAccount(acctName, "test123", attrs);
        assertNotNull(acct);
        return acct;
    }
    
    private void deleteAccount(Account acct) throws Exception {
        String acctId = acct.getId();
        prov.deleteAccount(acctId);
        acct = prov.get(AccountBy.id, acctId);
        assertNull(acct);
    }
    
    private void deleteDataSource(Account acct, DataSource dataSource) throws Exception {
        String dataSourceId = dataSource.getId();
        prov.deleteDataSource(acct, dataSourceId);
        dataSource = prov.get(acct, DataSourceBy.id, dataSourceId);
        assertNull(dataSource);
    }
    
    private DataSource createDataSourceRaw(Account acct, String dataSourceName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "123");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        attrs.put(Provisioning.A_zimbraDataSourceHost, "zimbra.com");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "9999");
        return prov.createDataSource(acct, DataSource.Type.pop3, dataSourceName, attrs);
    }
    
    private DataSource createDataSource(Account acct, String dataSourceName) throws Exception {
        DataSource dataSource = prov.get(acct, DataSourceBy.name, dataSourceName);
        assertNull(dataSource);
        
        createDataSourceRaw(acct, dataSourceName);
        
        dataSource = prov.get(acct, DataSourceBy.name, dataSourceName);
        assertNotNull(dataSource);
        
        return dataSource;
    }
    
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    @Test
    public void createDataSource() throws Exception {
        String ACCT_NAME = "createDataSource";
        String DATA_SOURCE_NAME = "createDataSource";
        
        Account acct = createAccount(ACCT_NAME);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        assertEquals(acct.getId(), dataSource.getAccount().getId());
        
        deleteDataSource(acct, dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void createDataSourceAlreadyExists() throws Exception {
        String ACCT_NAME = "createDataSourceAlreadyExists";
        String DATA_SOURCE_NAME = "createDataSourceAlreadyExists";
        
        Account acct = createAccount(ACCT_NAME);
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
        String ACCT_NAME = "modifyDataSource";
        String DATA_SOURCE_NAME = "modifyDataSource";
        
        Account acct = createAccount(ACCT_NAME);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraDataSourceHost;
        String MODIFIED_ATTR_VALUE = "modifyDataSource.com";
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifyDataSource(acct, dataSource.getId(), attrs);
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, DataSourceBy.name, DATA_SOURCE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, dataSource.getAttr(MODIFIED_ATTR_NAME));
        
        deleteDataSource(acct,dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void renameDataSource() throws Exception {
        String ACCT_NAME = "renameDataSource";
        String DATA_SOURCE_NAME = "renameDataSource";
        
        Account acct = createAccount(ACCT_NAME);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        // modifying zimbraDataSourceName will rename the data source and trigger a LDAP moddn
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraDataSourceName;
        String NEW_DATA_SOURCE_NAME = "renameDataSource-new";  
        String MODIFIED_ATTR_VALUE = NEW_DATA_SOURCE_NAME;
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifyDataSource(acct, dataSource.getId(), attrs);
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, DataSourceBy.name, NEW_DATA_SOURCE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, dataSource.getAttr(MODIFIED_ATTR_NAME));
        
        deleteDataSource(acct,dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void getAllDataSources() throws Exception {
        String ACCT_NAME = "getAllDataSources";
        String DATA_SOURCE_NAME_1 = "getAllDataSources-1";
        String DATA_SOURCE_NAME_2 = "getAllDataSources-2";
        String DATA_SOURCE_NAME_3 = "getAllDataSources-3";
        
        Account acct = createAccount(ACCT_NAME);
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
        String ACCT_NAME = "getDataSource";
        String DATA_SOURCE_NAME = "getDataSource";
        
        Account acct = createAccount(ACCT_NAME);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        String dataSourceId = dataSource.getId();
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, DataSourceBy.id, dataSourceId);
        assertNotNull(dataSource);
        
        acct = getFresh(acct);
        dataSource = prov.get(acct, DataSourceBy.name, DATA_SOURCE_NAME);
        assertNotNull(dataSource);
        
        deleteDataSource(acct,dataSource);
        deleteAccount(acct);
    }
}
