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
package com.zimbra.qa.unittest.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZDataSource;
import com.zimbra.client.ZFilterRules;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZPop3DataSource;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.admin.type.DataSourceType;


public class TestPop3ImportServer {

    @Rule
    public TestName testInfo = new TestName();

    private static final String NAME_PREFIX = TestPop3ImportServer.class.getSimpleName();
    private static final String DATA_SOURCE_NAME = NAME_PREFIX;
    private static final String TEMP_USER_NAME = NAME_PREFIX + "Temp";
    private static String USER_NAME;
    private static String PREFIX;
    private Account mAccount;
    private Provisioning mProv;

    private ZFilterRules mOriginalRules;
    private boolean mIsServerSideTest;

    @Before
    public void setUp() throws Exception {
        PREFIX = String.format("%s-%s-", this.getClass().getName(), testInfo.getMethodName()).toLowerCase();
        USER_NAME = String.format("%s-%s", PREFIX, "user1");
        mProv = Provisioning.getInstance();
        mIsServerSideTest = false;
        tearDown();
        mAccount = TestUtil.createAccount(USER_NAME);
        createDataSource();
        mOriginalRules = TestUtil.getZMailbox(USER_NAME).getIncomingFilterRules();
    }

    /*
     * Tests the UID persistence methods in {@link DbPop3Message}.
     */
    @Test
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
    @Test
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
    @Test
    public void testDeleteMailbox() throws Exception {
        mIsServerSideTest = true;

        // Create temp account and mailbox
        Account account = mProv.createAccount(TestUtil.getAddress(TEMP_USER_NAME), "test123", null);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // Store bogus POP3 message row and delete mailbox
        DbPop3Message.storeUid(mbox, "TestPop3Import", "uid1", Mailbox.ID_FOLDER_INBOX);
        mbox.deleteMailbox();
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

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(TEMP_USER_NAME);

    }

    private void createDataSource() throws Exception {
        int port = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraPop3BindPort));
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, ProvisioningConstants.FALSE);
        attrs.put(Provisioning.A_zimbraDataSourceHost, "localhost");
        attrs.put(Provisioning.A_zimbraDataSourcePort, Integer.toString(port));
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "user1");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "test123");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, Integer.toString(Mailbox.ID_FOLDER_INBOX));
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "cleartext");
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, ProvisioningConstants.FALSE);
        mProv.createDataSource(mAccount, DataSourceType.pop3, DATA_SOURCE_NAME, attrs);
    }

    private DataSource getDataSource() throws Exception {
        return mProv.get(mAccount, Key.DataSourceBy.name, DATA_SOURCE_NAME);
    }

}
