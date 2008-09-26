/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.ldap.LdapUtil;

public class TestDataSource extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String DS_NAME = "TestDataSource";
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    public void testPollingInterval()
    throws Exception {
        // Create data source
        Provisioning prov = Provisioning.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapUtil.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "testhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "0");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "testuser");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "testpass");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "1");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, DataSource.ConnectionType.cleartext.toString());
        DataSource ds = prov.createDataSource(account, DataSource.Type.pop3, DS_NAME, attrs);
        
        // Valid polling interval
        assertNotNull("Min not defined", account.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
        long min = account.getTimeInterval(Provisioning.A_zimbraDataSourceMinPollingInterval, 0) / 1000;
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, Long.toString(min));
        prov.modifyDataSource(account, ds.getId(), attrs);
        
        // Invalid polling interval
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, Long.toString(min - 1));
        try {
            prov.modifyDataSource(account, ds.getId(), attrs);
            fail("modifyDataSource() was not supposed to succeed");
        } catch (ServiceException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                e.getMessage().contains("shorter than the allowed minimum"));
        }
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    public void cleanUp()
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        DataSource ds = prov.get(account, DataSourceBy.name, DS_NAME);
        if (ds != null) {
            prov.deleteDataSource(account, ds.getId());
        }
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestDataSource.class);
    }
}
