/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestLdapProvDataSource extends LdapTest {
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
    
    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }
    
    private DataSource createDataSourceRaw(Account acct, String dataSourceName) 
    throws Exception {
        return provUtil.createDataSourceRaw(acct, dataSourceName);
    }
    
    private DataSource createDataSource(Account acct, String dataSourceName) 
    throws Exception {
        return provUtil.createDataSource(acct, dataSourceName);
    }
    
    private void deleteDataSource(Account acct, DataSource dataSource) throws Exception {
        provUtil.deleteDataSource(acct, dataSource);
    }
    
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    @Test
    public void createDataSource() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        assertEquals(acct.getId(), dataSource.getAccount().getId());
        
        deleteDataSource(acct, dataSource);
        deleteAccount(acct);
    }
    
    @Test
    public void createDataSourceAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName());
        
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
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName());
        
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
    public void modifyDataSourceAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName());
        String DATA_SOURCE_NAME_2 = Names.makeDataSourceName(genDataSourceName());

        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        DataSource dataSource2 = createDataSource(acct, DATA_SOURCE_NAME_2);

        Map<String, Object> attrs = new HashMap<String, Object>();
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraDataSourceEmailAddress;
        String MODIFIED_ATTR_VALUE = dataSource.getEmailAddress();
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);

        boolean caughtException = false;
        try {
            prov.modifyDataSource(acct, dataSource2.getId(), attrs);
        } catch (AccountServiceException e) {
            if (AccountServiceException.DATA_SOURCE_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);

        deleteDataSource(acct, dataSource);
        deleteDataSource(acct, dataSource2);
        deleteAccount(acct);
    }

    @Test
    public void renameDataSource() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        DataSource dataSource = createDataSource(acct, DATA_SOURCE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        // modifying zimbraDataSourceName will rename the data source and trigger a LDAP moddn
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraDataSourceName;
        String NEW_DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName("new"));  
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
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME_1 = Names.makeDataSourceName(genDataSourceName("1"));
        String DATA_SOURCE_NAME_2 = Names.makeDataSourceName(genDataSourceName("2"));
        String DATA_SOURCE_NAME_3 = Names.makeDataSourceName(genDataSourceName("3"));
        
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
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String DATA_SOURCE_NAME = Names.makeDataSourceName(genDataSourceName());
        
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
