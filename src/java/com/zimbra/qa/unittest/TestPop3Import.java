/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.MailDateFormat;

import junit.framework.TestCase;

import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZFilterAction;
import com.zimbra.cs.zclient.ZFilterCondition;
import com.zimbra.cs.zclient.ZFilterRule;
import com.zimbra.cs.zclient.ZFilterRules;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZPop3DataSource;
import com.zimbra.cs.zclient.ZFilterAction.ZFileIntoAction;
import com.zimbra.cs.zclient.ZFilterCondition.HeaderOp;
import com.zimbra.cs.zclient.ZFilterCondition.ZHeaderCondition;


public class TestPop3Import extends TestCase {
    private static final String USER_NAME = "user1";
    private static final String USER2_NAME = "user2";
    private static final String NAME_PREFIX = TestPop3Import.class.getSimpleName();
    private static final String DATA_SOURCE_NAME = NAME_PREFIX;
    private static final String TEMP_USER_NAME = NAME_PREFIX + "Temp";
    
    private ZFilterRules mOriginalRules;
    private boolean mIsServerSideTest;

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        mIsServerSideTest = false;
        cleanUp();
        createDataSource();
        mOriginalRules = TestUtil.getZMailbox(USER_NAME).getIncomingFilterRules();
    }

    /*
     * Tests the UID persistence methods in {@link DbPop3Message}.
     */
    @Test(groups = {"Server"})
    public void testUidPersistence() throws Exception {
        mIsServerSideTest = true;
        DataSource ds = getDataSource();
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);

        // Create set of id's.  Id's are the same characters in different
        // case, to test case-sensitivity (bug 15090)
        String uid1 = "myUID";
        String uid2 = "myUid";
        Set<String> uids = new HashSet<String>();
        uids.add(uid1);
        uids.add(uid2);
        
        // Make sure no id's match
        Set<String> matchingUids = DbPop3Message.getMatchingUids(mbox, ds, uids);
        assertEquals("Test 1: set size", 0, matchingUids.size());
        
        // Store uid1 and make sure it matches
        DbPop3Message.storeUid(mbox, ds.getId(), uid1, Mailbox.ID_FOLDER_INBOX);
        matchingUids = DbPop3Message.getMatchingUids(mbox, ds, uids);
        assertEquals("Test 2: set size", 1, matchingUids.size());
        assertTrue("Test 2: did not find uid1", matchingUids.contains(uid1));
        assertFalse("Test 2: found uid2", matchingUids.contains(uid2));
        
        // Store uid2 and make sure it matches
        DbPop3Message.storeUid(mbox, ds.getId(), uid2, Mailbox.ID_FOLDER_TRASH);
        matchingUids = DbPop3Message.getMatchingUids(mbox, ds, uids);
        assertEquals("Test 3: set size", 2, matchingUids.size());
        assertTrue("Test 3: did not find uid1", matchingUids.contains(uid1));
        assertTrue("Test 3: did not find uid2", matchingUids.contains(uid2));
        
        // Test delete
        DbPop3Message.deleteUids(mbox, ds.getId());
        matchingUids = DbPop3Message.getMatchingUids(mbox, ds, uids);
        assertEquals("Test 3: set size", 0, matchingUids.size());
    }
    
    /*
     * Confirms that the UID database gets cleared when the host name, account
     * name or leave on server flag are changed. 
     */
    @Test(groups = {"Server"})
    public void testModifyDataSource() throws Exception {
        mIsServerSideTest = true;

        // Test modifying host
        ZPop3DataSource zds = getZDataSource();
        zds.setHost(zds.getHost() + "2");
        modifyAndCheck(zds, false);
        
        // Test modifying username
        zds = getZDataSource();
        zds.setUsername(zds.getUsername() + "2");
        modifyAndCheck(zds, false);
        
        // Test modifying leave on server
        zds = getZDataSource();
        ZimbraLog.test.info("Old leaveOnServer value: " + zds.leaveOnServer());
        zds.setLeaveOnServer(!zds.leaveOnServer());
        modifyAndCheck(zds, true);
    }
    
    /*
     * Confirms that POP3 data is deleted when the mailbox is deleted (bug 14574).  Any leftover POP3
     * data will cause a foreign key violation.
     */
    @Test(groups = {"Server"})
    public void testDeleteMailbox() throws Exception {
        mIsServerSideTest = true;

        // Create temp account and mailbox
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.createAccount(TestUtil.getAddress(TEMP_USER_NAME), "test123", null);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        
        // Store bogus POP3 message row and delete mailbox
        DbPop3Message.storeUid(mbox, "TestPop3Import", "uid1", Mailbox.ID_FOLDER_INBOX);
        IMPersona.deleteIMPersona(account.getName());
        mbox.deleteMailbox();
    }
    
    /**
     * Tests import of a message with a date in the future (bug 17031).
     */
    @Test
    public void testBogusDate() throws Exception {
        // Create remote account
        Provisioning.getInstance().createAccount(TestUtil.getAddress(TEMP_USER_NAME), "test123", null);
        
        // Add message with bogus date to remote mailbox
        MailDateFormat format = new MailDateFormat();
        Date date = format.parse("Thu, 31  Aug 2039 10:29:46 +0800");
        String message = TestUtil.getTestMessage(NAME_PREFIX + " testBogusDate", null, null, date);
        ZMailbox remoteMbox = TestUtil.getZMailbox(TEMP_USER_NAME);
        String folderId = Integer.toString(Mailbox.ID_FOLDER_INBOX);
        remoteMbox.addMessage(folderId, null, null, 0, message, true);
        
        // Update the data source, import data
        ZMailbox localMbox = TestUtil.getZMailbox(USER_NAME);
        ZPop3DataSource ds = getZDataSource();
        ds.setUsername(TEMP_USER_NAME);
        ds.setEnabled(true);
        localMbox.modifyDataSource(ds);
        
        // Import data and make sure the message was imported
        List<ZMessage> messages = TestUtil.search(localMbox, "in:inbox " + NAME_PREFIX);
        assertEquals("Found unexpected message in local inbox", 0, messages.size());
        TestUtil.importDataSource(ds, localMbox, remoteMbox);
        messages = TestUtil.search(localMbox, "in:inbox " + NAME_PREFIX);
        assertEquals("Imported message not found", 1, messages.size());
    }
    
    /**
     * Tests {@link ZMailbox#testDataSource}.
     */
    @Test
    public void testTestDataSource() throws Exception {
        ZMailbox localMbox = TestUtil.getZMailbox(USER_NAME);
        ZPop3DataSource ds = getZDataSource();
        ds.setUsername(USER2_NAME);
        localMbox.modifyDataSource(ds);
        assertNull(localMbox.testDataSource(ds));
    }
    
    /**
     * Confirms that messages pulled from a POP3 account are affected by
     * mail filtering (bug 13821).
     */
    @Test
    public void testFiltering()
    throws Exception {
        String folderPath = "/" + NAME_PREFIX + "-testFiltering";
        String filteredPath = "/" + NAME_PREFIX + "-testFiltering-filtered";
        
        // Create remote account
        Provisioning.getInstance().createAccount(TestUtil.getAddress(TEMP_USER_NAME), "test123", null);
        
        // Add message to remote mailbox
        ZMailbox remoteMbox = TestUtil.getZMailbox(TEMP_USER_NAME);
        TestUtil.addMessage(remoteMbox, NAME_PREFIX + " testFiltering");

        // Create local folders
        ZMailbox localMbox = TestUtil.getZMailbox(USER_NAME);
        localMbox.getFolderByPath("/Inbox");
        ZFolder dsFolder = TestUtil.createFolder(localMbox, folderPath);
        TestUtil.createFolder(localMbox, filteredPath);
        
        // Create filter rule that files to the local folder
        List<ZFilterRule> rules = new ArrayList<ZFilterRule>();
        List<ZFilterCondition> conditions = new ArrayList<ZFilterCondition>();
        List<ZFilterAction> actions = new ArrayList<ZFilterAction>();
        conditions.add(new ZHeaderCondition("subject", HeaderOp.CONTAINS, "testFiltering"));
        actions.add(new ZFileIntoAction(filteredPath));
        rules.add(new ZFilterRule("testFiltering", true, false, conditions, actions));
        localMbox.saveIncomingFilterRules(new ZFilterRules(rules));
        
        // Set up data source and run import
        ZPop3DataSource ds = getZDataSource();
        ds.setUsername(TEMP_USER_NAME);
        ds.setFolderId(dsFolder.getId());
        ds.setEnabled(true);
        localMbox.modifyDataSource(ds);
        
        // Import data and make sure the message was filed to the folder
        TestUtil.importDataSource(ds, localMbox, remoteMbox);
        List<ZMessage> messages = TestUtil.search(localMbox, "in:" + folderPath);
        assertEquals("Found unexpected messages in " + folderPath, 0, messages.size());
        messages = TestUtil.search(localMbox, "in:" + filteredPath);
        assertEquals("Message not found in " + filteredPath, 1, messages.size());
    }
    
    /*
     * Modifies the data source and confirms whether UID data was deleted.
     */
    private void modifyAndCheck(ZPop3DataSource zds, boolean shouldDeleteData)
        throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        DataSource ds = getDataSource();
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);

        // Reinitialize persisted UID's
        DbPop3Message.deleteUids(mbox, ds.getId());
        DbPop3Message.storeUid(mbox, ds.getId(), "1", Mailbox.ID_FOLDER_INBOX);

        // Modify data source and make sure the existing
        // UID's were deleted
        zmbox.modifyDataSource(zds);
        
        Set<String> uids = new HashSet<String>();
        uids.add("1");
        Set<String> matchingUids = DbPop3Message.getMatchingUids(mbox, ds, uids);
        int expected = shouldDeleteData ? 0 : 1;
        assertEquals("matching UID's: " + StringUtil.join(",", matchingUids), expected, matchingUids.size());
    }

    private ZPop3DataSource getZDataSource() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZDataSource> dataSources = mbox.getAllDataSources();
        for (ZDataSource ds : dataSources) {
            if (ds.getName().equals(DATA_SOURCE_NAME)) {
                return (ZPop3DataSource) ds;
            }
        }
        fail("Could not find data source " + DATA_SOURCE_NAME);
        return null;
    }
    
    @Override
    @AfterMethod
    public void tearDown() throws Exception {
        cleanUp();
        TestUtil.getZMailbox(USER_NAME).saveIncomingFilterRules(mOriginalRules);
    }
    
    private void createDataSource() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        int port = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort));
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, Provisioning.FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "localhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, Integer.toString(port));
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "user1");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "test123");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, Integer.toString(Mailbox.ID_FOLDER_INBOX));
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "cleartext");
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, Provisioning.FALSE);
        prov.createDataSource(account, DataSource.Type.pop3, DATA_SOURCE_NAME, attrs);
    }
    
    private DataSource getDataSource() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        return prov.get(account, DataSourceBy.name, DATA_SOURCE_NAME);
    }
    
    private void cleanUp() throws Exception {
        // Delete data source
        Provisioning prov = Provisioning.getInstance();
        DataSource ds = getDataSource();
        if (ds != null) {
            Account account = TestUtil.getAccount(USER_NAME);
            if (mIsServerSideTest) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
                DbPop3Message.deleteUids(mbox, ds.getId());
            }
            prov.deleteDataSource(account, ds.getId());
        }
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteAccount(TEMP_USER_NAME);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestNG testng = TestUtil.newTestNG();
        testng.setExcludedGroups("Server");
        testng.setTestClasses(new Class[] { TestPop3Import.class });
        testng.run();
    }
}
