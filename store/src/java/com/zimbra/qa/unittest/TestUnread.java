/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;
import java.util.EnumSet;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
/**
 * @author bburtin
 */
public class TestUnread {
    @Rule
    public TestName name = new TestName();

    private Mailbox mMbox;
    private Account mAccount;

    private static String TEST_NAME = "TestUnread";
    private static String USER_NAME = TEST_NAME.toLowerCase() + "user1";

    private static String FOLDER1_NAME =  "Folder 1";
    private static String FOLDER2_NAME = "Folder 2";
    private static String TAG1_NAME = "Tag 1";
    private static String TAG2_NAME = "Tag 2";
    private static String TAG3_NAME = "Tag 3";

    private int mMessage1Id;
    private int mMessage2Id;
    private int mMessage3Id;
    private int mFolder1Id;
    private int mFolder2Id;
    private int mTag1Id;
    private int mTag2Id;
    private int mTag3Id;
    private int mConvId;

    private Message getMessage1() throws Exception { return mMbox.getMessageById(null, mMessage1Id); }
    private Message getMessage2() throws Exception { return mMbox.getMessageById(null, mMessage2Id); }
    private Message getMessage3() throws Exception { return mMbox.getMessageById(null, mMessage3Id); }
    private Conversation getConv() throws Exception { return mMbox.getConversationById(null, mConvId); }
    private Folder getFolder1() throws Exception { return mMbox.getFolderById(null, mFolder1Id); }
    private Folder getFolder2() throws Exception { return mMbox.getFolderById(null, mFolder2Id); }
    private Tag getTag1() throws Exception { return mMbox.getTagById(null, mTag1Id); }
    private Tag getTag2() throws Exception { return mMbox.getTagById(null, mTag2Id); }
    private Tag getTag3() throws Exception { return mMbox.getTagById(null, mTag3Id); }

    /**
     * Sets up the following data set:
     * <ul>
     *   <li>F1 contains F2 and M1</li>
     *   <li>F2 contains M2 and M3</li>
     *   <li>T1 is assigned to M1</li>
     *   <li>T2 is assigned to M1 and M2</li>
     * </ul>
     */
    @Before
    public void setUp() throws Exception {

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, localServer.getServiceHostname());

        mAccount = TestUtil.createAccount(USER_NAME, attrs);
        Assert.assertNotNull(String.format("Unable to create account for user '%s'", USER_NAME), mAccount);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);

        Message msg = TestUtil.addMessage(mMbox, TEST_NAME);
        mMessage1Id = msg.getId();
        ZimbraLog.test.debug("Created message 1, id=" + mMessage1Id);

        msg = TestUtil.addMessage(mMbox, "RE: " + TEST_NAME);
        mMessage2Id = msg.getId();
        ZimbraLog.test.debug("Created message 2, id=" + mMessage2Id);

        msg =  TestUtil.addMessage(mMbox, "RE: " + TEST_NAME);
        mMessage3Id = msg.getId();
        ZimbraLog.test.debug("Created message 3, id=" + mMessage3Id);

        mConvId = getMessage1().getConversationId();
        ZimbraLog.test.debug("Created conversation, id=" + mConvId);

        Folder folder = mMbox.createFolder(null, FOLDER1_NAME, Mailbox.ID_FOLDER_INBOX, new Folder.FolderOptions());
        mFolder1Id = folder.getId();

        folder = mMbox.createFolder(null, FOLDER2_NAME, mFolder1Id, new Folder.FolderOptions());
        mFolder2Id = folder.getId();

        Tag tag = mMbox.createTag(null, TAG1_NAME, (byte)0);
        mTag1Id = tag.getId();

        tag = mMbox.createTag(null, TAG2_NAME, (byte)0);
        mTag2Id = tag.getId();

        tag = mMbox.createTag(null, TAG3_NAME, (byte)0);
        mTag3Id = tag.getId();

        mMbox.move(null, mMessage1Id, getMessage1().getType(), mFolder1Id);
        mMbox.move(null, mMessage2Id, getMessage1().getType(), mFolder2Id);
        mMbox.move(null, mMessage3Id, getMessage1().getType(), mFolder2Id);
        mMbox.alterTag(null, mMessage1Id, getMessage1().getType(), getTag1().getName(), true, null);
        mMbox.alterTag(null, mMessage1Id, getMessage1().getType(), getTag2().getName(), true, null);
        mMbox.alterTag(null, mMessage2Id, getMessage2().getType(), getTag2().getName(), true, null);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }

    @Test
    public void testReadMessage1()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();
        setUnread(getMessage1(), false);
        verifyMessage1Read();
    }

    @Test
    public void testReadMessage2()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        setUnread(getMessage2(), false);

        Assert.assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());

        verifyAllUnreadFlags();
    }

    @Test
    public void testReadAllMessages()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        setUnread(getMessage1(), false);
        setUnread(getMessage2(), false);
        setUnread(getMessage3(), false);
        verifyAllRead();
    }

    @Test
    public void testReadConversation()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        setUnread(getConv(), false);
        verifyAllRead();
    }

    @Test
    public void testReadFolder1()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();
        setUnread(getFolder1(), false);
        verifyMessage1Read();
    }

    @Test
    public void testReadFolder2()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        setUnread(getFolder2(), false);

        Assert.assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 0, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 1, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    @Test
    public void testReadAllFolders()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        setUnread(getFolder1(), false);
        setUnread(getFolder2(), false);
        verifyAllRead();
    }

    @Test
    public void testReadTag1()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();
        setUnread(getTag1(), false);
        verifyMessage1Read();
    }

    @Test
    public void testReadTag2()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        setUnread(getTag2(), false);

        Assert.assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 1, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());

        verifyAllUnreadFlags();
    }

    @Test
    public void testMoveMessage()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        // Move M2 from F2 to F1
        mMbox.move(null, mMessage2Id, getMessage2().getType(), mFolder1Id);
        Assert.assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 2, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());

        // Mark M2 as read
        setUnread(getMessage2(), false);
        Assert.assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());

        // Move M2 back to F2 and verify that the counts are unchanged
        mMbox.move(null, mMessage2Id, getMessage2().getType(), mFolder2Id);
        Assert.assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
    }

    @Test
    public void testMoveConversation()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        // Read M1 and move the whole conversation to F1
        setUnread(getMessage1(), false);
        mMbox.move(null, getConv().getId(), getConv().getType(), mFolder1Id);
        Assert.assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 2, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());

        // Move the conversation to F2
        mMbox.move(null, getConv().getId(), getConv().getType(), mFolder2Id);
        Assert.assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 2, getFolder2().getUnreadCount());
    }

    @Test
    public void testTagMessage()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        // Add T3 to M3
        mMbox.alterTag(null, mMessage3Id, getMessage3().getType(), getTag3().getName(), true, null);
        Assert.assertEquals("getTag3().getUnreadCount()", 1, getTag3().getUnreadCount());

        // Add T3 to M2
        mMbox.alterTag(null, mMessage2Id, getMessage2().getType(), getTag3().getName(), true, null);
        Assert.assertEquals("getTag3().getUnreadCount()", 2, getTag3().getUnreadCount());

        // Remove T3 from M3
        mMbox.alterTag(null, mMessage3Id, getMessage3().getType(), getTag3().getName(), false, null);
        Assert.assertEquals("getTag3().getUnreadCount()", 1, getTag3().getUnreadCount());
    }

    @Test
    public void testTagConversation()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        // Add T3 to C
        mMbox.alterTag(null, getConv().getId(), getConv().getType(), getTag3().getName(), true, null);
        Assert.assertEquals("getTag3().getUnreadCount()", 3, getTag3().getUnreadCount());

        // Remove T3 from C
        mMbox.alterTag(null, getConv().getId(), getConv().getType(), getTag3().getName(), false, null);
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    /**
     * Moves one read message and two unread messages to the trash.
     */
    @Test
    public void testMoveToTrash()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());

        verifySetUp();

        Folder inbox = mMbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        int unreadCount = inbox.getUnreadCount();

        // Move conversation to Inbox
        mMbox.move(null, getConv().getId(), getConv().getType(), Mailbox.ID_FOLDER_INBOX);
        Assert.assertEquals("Move conversation to inbox", unreadCount + 3, inbox.getUnreadCount());

        // Read message 2
        setUnread(getMessage2(), false);
        Assert.assertEquals("Read message 2", unreadCount + 2, inbox.getUnreadCount());

        // Move message 1
        mMbox.move(null, mMessage1Id, getMessage1().getType(), Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals("Move message to trash", unreadCount + 1, inbox.getUnreadCount());

        // Move the rest of the conversation
        mMbox.move(null, getConv().getId(), getConv().getType(), Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals("Move conversation to trash", unreadCount, inbox.getUnreadCount());
    }

    /**
     * Makes sure that something comes back when searching for unread items.
     */
    @Test
    public void testSearch() throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());
        verifySetUp();

        ZimbraQueryResults results = mMbox.index.search(new OperationContext(mMbox), "is:unread",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.DATE_DESC, 100);
        Assert.assertTrue("No search results found", results.hasNext());
        results.close();
    }

    @Test
    public void testDeleteConversation()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());

        verifySetUp();

        mMbox.delete(null, getConv().getId(), getConv().getType());

        Assert.assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    @Test
    public void testDeleteFolder2()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());

        verifySetUp();

        mMbox.delete(null, mFolder2Id, getFolder2().getType());

        Assert.assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 1, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    @Test
    public void testDeleteFolder1()
    throws Exception {
        ZimbraLog.test.debug("Starting Test %s", name.getMethodName());

        verifySetUp();

        mMbox.delete(null, mFolder1Id, getFolder1().getType());

        Assert.assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    private void verifyMessage1Read()
    throws Exception {
        Assert.assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 2, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());

        verifyAllUnreadFlags();
    }

    private void verifyAllUnreadFlags() throws Exception {
        verifyUnreadFlag(getMessage1());
        verifyUnreadFlag(getMessage2());
        verifyUnreadFlag(getMessage3());
        verifyUnreadFlag(getConv());
        verifyUnreadFlag(getFolder1());
        verifyUnreadFlag(getFolder2());
        verifyUnreadFlag(getTag1());
        verifyUnreadFlag(getTag2());

        // Make sure we aren't still setting the old unread flag
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() +
            " AND flags & " + Flag.BITMASK_UNREAD + " > 0");
        int numRows = results.getInt(1);
        Assert.assertEquals("Found " + numRows + " items with old unread flag set", 0, numRows);
    }

    private void verifyUnreadFlag(MailItem item) throws Exception {
        String flagString = item.getFlagString();
        if (item.isUnread()) {
            Assert.assertTrue("unread bit test: " + item.getFlagBitmask(), (item.getFlagBitmask() & Flag.BITMASK_UNREAD) > 0);
            Assert.assertTrue("unread flag string: " + flagString, flagString.indexOf(Flag.toChar(Flag.ID_UNREAD)) >= 0);
        } else {
            Assert.assertTrue("read bit test: " + item.getFlagBitmask(), (item.getFlagBitmask() & Flag.BITMASK_UNREAD) == 0);
            Assert.assertTrue("read flag string: " + flagString, flagString.indexOf(Flag.toChar(Flag.ID_UNREAD)) == -1);
        }

//        if (item.getType() == MailItem.TYPE_MESSAGE || item.getType() == MailItem.TYPE_INVITE) {
        if (item.getType() == MailItem.Type.MESSAGE) {
            DbResults results = DbUtil.executeQuery(
                "SELECT unread " +
                "FROM " + DbMailItem.getMailItemTableName(item) +
                " WHERE mailbox_id = " + item.getMailboxId() +
                " AND id = " + item.getId());
            Assert.assertEquals("Verify unread flag in the database", item.isUnread(), results.getBoolean(1));
        }
    }

    private void verifyAllRead()
    throws Exception {
        Assert.assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 0, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 0, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());

        verifyAllUnreadFlags();
    }

    private void setUnread(MailItem item, boolean unread) throws Exception {
        mMbox.alterTag(null, item.getId(), item.getType(), Flag.FlagInfo.UNREAD, unread, null);
        item = mMbox.getItemById(null, item.getId(), item.getType());
        verifyUnreadFlag(item);
    }

    private void verifySetUp()
    throws Exception {
        Assert.assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        Assert.assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        Assert.assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        Assert.assertEquals("getConv().getUnreadCount()", 3, getConv().getUnreadCount());
        Assert.assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        Assert.assertEquals("getFolder2().getUnreadCount()", 2, getFolder2().getUnreadCount());
        Assert.assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        Assert.assertEquals("getTag2().getUnreadCount()", 2, getTag2().getUnreadCount());
        Assert.assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());

        Assert.assertEquals("getMessage1().getFolderId()", mFolder1Id, getMessage1().getFolderId());
        Assert.assertEquals("getMessage2().getFolderId()", mFolder2Id, getMessage2().getFolderId());
        Assert.assertEquals("getMessage3().getFolderId()", mFolder2Id, getMessage3().getFolderId());

        Assert.assertTrue("getMessage1().isTagged(getTag1())", getMessage1().isTagged(getTag1()));
        Assert.assertTrue("getMessage1().isTagged(getTag2())", getMessage1().isTagged(getTag2()));
        Assert.assertTrue("getMessage2().isTagged(getTag2())", getMessage2().isTagged(getTag2()));
    }
}

