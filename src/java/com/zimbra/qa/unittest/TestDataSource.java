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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZMailbox;

public class TestDataSource extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String DS_NAME = "TestDataSource";
    private static final String TEST_USER_NAME = "testdatasource";
    
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
    
    /**
     * Tests the <tt>lastError</tt> element and <tt>failingSince</tt> attribute
     * for <tt>GetInfoRequest</tt> and <tt>GetDataSourcesRequest</tt>.
     */
    public void testErrorStatus()
    throws Exception {
        Account account = TestUtil.createAccount(TEST_USER_NAME);
        
        // Create data source.
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapUtil.LDAP_TRUE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "localhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort));
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "user2");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, TestUtil.DEFAULT_PASSWORD);
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, Integer.toString(Mailbox.ID_FOLDER_INBOX));
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, DataSource.ConnectionType.cleartext.toString());
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, LdapUtil.LDAP_TRUE);
        DataSource ds = prov.createDataSource(account, DataSource.Type.pop3, DS_NAME, attrs);

        // Make sure error status is not set.
        ZMailbox mbox = TestUtil.getZMailbox(TEST_USER_NAME);
        confirmErrorStatus(mbox, null);
        
        // Invoke data source sync and make sure error status is not set.
        ZDataSource zds = TestUtil.getDataSource(mbox, DS_NAME);
        TestUtil.importDataSource(zds, mbox, null, true);
        confirmErrorStatus(mbox, null);
        
        // Change to an invalid password, make sure error status is set.
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "bogus");
        prov.modifyDataSource(account, ds.getId(), attrs);
        zds = TestUtil.getDataSource(mbox, DS_NAME);
        long startTimestamp = System.currentTimeMillis() - 1000; // timestamp is returned in seconds, not millis
        TestUtil.importDataSource(zds, mbox, null, false);
        confirmErrorStatus(mbox, startTimestamp);
        
        // Fix password, make sure that error status is reset.
        attrs.put(Provisioning.A_zimbraDataSourcePassword, TestUtil.DEFAULT_PASSWORD);
        prov.modifyDataSource(account, ds.getId(), attrs);
        zds = TestUtil.getDataSource(mbox, DS_NAME);
        startTimestamp = System.currentTimeMillis();
        TestUtil.importDataSource(zds, mbox, null, true);
        confirmErrorStatus(mbox, null);
    }
    
    private void confirmErrorStatus(ZMailbox mbox, Long laterThanTimestamp)
    throws Exception {
        // Check GetInfoRequest.
        Element request = new XMLElement(AccountConstants.GET_INFO_REQUEST);
        Element response = mbox.invoke(request);
        Element eDS = response.getElement(AccountConstants.E_DATA_SOURCES);
        Element ePop3 = null;
        for (Element e : eDS.listElements(MailConstants.E_DS_POP3)) {
            if (e.getAttribute(MailConstants.A_NAME).equals(DS_NAME)) {
                ePop3 = e;
            }
        }
        assertNotNull("Could not find data source in response: " + response.prettyPrint(), ePop3);
        confirmErrorStatus(ePop3, laterThanTimestamp);
        
        // Check GetDataSources.
        ePop3 = null;
        request = new XMLElement(MailConstants.GET_DATA_SOURCES_REQUEST);
        response = mbox.invoke(request);
        for (Element e : response.listElements(MailConstants.E_DS_POP3)) {
            if (e.getAttribute(MailConstants.A_NAME).equals(DS_NAME)) {
                ePop3 = e;
            }
        }
        assertNotNull("Could not find data source in response: " + response.prettyPrint(), ePop3);
        confirmErrorStatus(ePop3, laterThanTimestamp);
    }
    
    private void confirmErrorStatus(Element ePop3, Long timestampBeforeSync)
    throws Exception {
        if (timestampBeforeSync != null) {
            assertTrue(ePop3.getElement(MailConstants.E_DS_LAST_ERROR).getText().length() > 0);
            long failingSince = ePop3.getAttributeLong(MailConstants.A_DS_FAILING_SINCE) * 1000;
            long now = System.currentTimeMillis();
            
            assertTrue(failingSince + " is earlier than " + timestampBeforeSync, failingSince >= timestampBeforeSync);
            assertTrue(failingSince + " is later than " + now, failingSince < now);
        } else {
            assertNull("Last error was not reset", ePop3.getOptionalElement(MailConstants.E_DS_LAST_ERROR));
            assertNull("Error timestamp was not reset", ePop3.getAttribute(MailConstants.A_DS_FAILING_SINCE, null));
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
        TestUtil.deleteAccount(TEST_USER_NAME);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestDataSource.class));
    }
}
