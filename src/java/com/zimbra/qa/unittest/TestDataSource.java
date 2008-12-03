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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZRssDataSource;

public class TestDataSource extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestDataSource.class.getSimpleName();
    
    private String mOriginalAccountPollingInterval;
    private String mOriginalAccountPop3PollingInterval;
    private String mOriginalAccountImapPollingInterval;
    
    private String mOriginalCosPollingInterval;
    private String mOriginalCosPop3PollingInterval;
    private String mOriginalCosImapPollingInterval;
    
    public void setUp()
    throws Exception {
        cleanUp();

        // Remember original polling intervals.
        Account account = TestUtil.getAccount(USER_NAME);
        Cos cos = account.getCOS();
        mOriginalAccountPollingInterval = account.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false);
        if (mOriginalAccountPollingInterval == null) {
            mOriginalAccountPollingInterval = "";
        }
        mOriginalAccountPop3PollingInterval = account.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false);
        if (mOriginalAccountPop3PollingInterval == null) {
            mOriginalAccountPop3PollingInterval = "";
        }
        mOriginalAccountImapPollingInterval = account.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval, false);
        if (mOriginalAccountImapPollingInterval == null) {
            mOriginalAccountImapPollingInterval = "";
        }
        
        mOriginalCosPollingInterval = cos.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, "");
        mOriginalCosPop3PollingInterval = cos.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, "");
        mOriginalCosImapPollingInterval = cos.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval, "");
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
        DataSource ds = prov.createDataSource(account, DataSource.Type.pop3, NAME_PREFIX + " testPollingInterval", attrs);
        
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
    
    public void testMigratePollingInterval()
    throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        Cos cos = account.getCOS();
        
        // Create data source
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(mbox, NAME_PREFIX + " testMigratePollingInterval");
        
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapUtil.LDAP_FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "localhost");
        int port = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort));
        attrs.put(Provisioning.A_zimbraDataSourcePort, Integer.toString(port));
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "user2");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "test123");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, folder.getId());
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, DataSource.ConnectionType.cleartext.toString());
        String dsName = NAME_PREFIX + " testMigratePollingInterval";
        DataSource ds = prov.createDataSource(account, DataSource.Type.pop3, dsName, attrs);

        // Set old polling intervals and unset new ones.
        account.unsetDataSourcePop3PollingInterval();
        account.unsetDataSourceImapPollingInterval();
        cos.unsetDataSourcePop3PollingInterval();
        cos.unsetDataSourceImapPollingInterval();
        account.setDataSourcePollingInterval("1h");
        cos.setDataSourcePollingInterval("2h");
        
        // Trigger the migration.
        ds.getPollingInterval();
        
        // Refresh and verify migrated values.
        account = TestUtil.getAccount(USER_NAME);
        cos = account.getCOS();
        ds = account.getDataSourceByName(dsName);
        
        assertEquals("1h", account.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval));
        assertEquals("1h", account.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval));
        assertEquals("2h", cos.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval));
        assertEquals("2h", cos.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval));
    }
    
    public void testRss()
    throws Exception {
        // Create folder.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String parentId = Integer.toString(Mailbox.ID_FOLDER_USER_ROOT);
        String urlString = "http://feeds.feedburner.com/zimbra";
        ZFolder folder = mbox.createFolder(parentId, NAME_PREFIX + " testRss", null, null, null, urlString);

        // Get the data source that was implicitly created.
        ZRssDataSource ds = getRssDataSource(mbox, folder.getId());
        assertNotNull(ds);
        
        // Test data source.  If the test fails, skip validation so we don't
        // get false positives when the Zimbra RSS feed is down or the test
        // is running on a box that's not connected to the internet.
        String error = mbox.testDataSource(ds);
        if (error != null) {
            ZimbraLog.test.warn("Unable to test RSS data source for %s: %s.", urlString, error);
            return;
        }
        
        // Import data and confirm that the folder is not empty.
        List<ZDataSource> list = new ArrayList<ZDataSource>();
        list.add(ds);
        mbox.importData(list);
        waitForRssData(mbox, folder);
        
        // Delete folder, import data, and make sure that the data source was deleted.
        mbox.deleteFolder(folder.getId());
        mbox.importData(list);
        ds = getRssDataSource(mbox, folder.getId());
        assertNull(ds);
    }
    
    private void waitForRssData(ZMailbox mbox, ZFolder folder)
    throws Exception {
        for (int i = 1; i <= 10; i++) {
            mbox.noOp();
            long folderSize = folder.getSize();
            if (folderSize > 0) {
                return;
            }
            Thread.sleep(500);
        }
        fail("No messages found in folder " + folder.getPath());
    }

    private ZRssDataSource getRssDataSource(ZMailbox mbox, String folderId)
    throws ServiceException {
        for (ZDataSource i : mbox.getAllDataSources()) {
            if (i instanceof ZRssDataSource && ((ZRssDataSource) i).getFolderId().equals(folderId)) {
                return (ZRssDataSource) i;
            }
        }
        return null;
    }
    
    public void tearDown()
    throws Exception {
        // Reset original polling intervals.
        Account account = TestUtil.getAccount(USER_NAME);
        Cos cos = account.getCOS();
        
        account.setDataSourcePollingInterval(mOriginalAccountPollingInterval);
        account.setDataSourcePop3PollingInterval(mOriginalAccountPop3PollingInterval);
        account.setDataSourceImapPollingInterval(mOriginalAccountImapPollingInterval);
        
        cos.setDataSourcePollingInterval(mOriginalCosPollingInterval);
        cos.setDataSourcePop3PollingInterval(mOriginalCosPop3PollingInterval);
        cos.setDataSourceImapPollingInterval(mOriginalCosImapPollingInterval);
        
        cleanUp();
    }
    
    public void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestDataSource.class);
    }
}
