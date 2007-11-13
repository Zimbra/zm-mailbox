/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import junit.framework.TestCase;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;

/**
 * @author bburtin
 */
public class TestUnread extends TestCase
{
    private Mailbox mMbox;
    private Account mAccount;
    
    private static String USER_NAME = "user1";
    private static String TEST_NAME = "TestUnread";
    
    private static String FOLDER1_NAME = TEST_NAME + " Folder 1";
    private static String FOLDER2_NAME = TEST_NAME + " Folder 2";
    private static String TAG1_NAME = TEST_NAME + " Tag 1";
    private static String TAG2_NAME = TEST_NAME + " Tag 2";
    private static String TAG3_NAME = TEST_NAME + " Tag 3";

    private int mMessage1Id;
    private int mMessage2Id;
    private int mMessage3Id;
    private int mFolder1Id;
    private int mFolder2Id;
    private int mTag1Id;
    private int mTag2Id;
    private int mTag3Id;
    private int mConvId;

    /**
     * Constructor used for instantiating a <code>TestCase</code> that runs a single test.
     * 
     * @param testName the name of the method that will be called when the test is executed
     */
    public TestUnread(String testName) {
        super(testName);
    }
    
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
    protected void setUp()
    throws Exception {
        super.setUp();
        
        mAccount = TestUtil.getAccount(USER_NAME);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
        
        // Clean up data, in case a previous test didn't exit cleanly
        TestUtil.deleteTestData(USER_NAME, TEST_NAME);
        
        Message msg = TestUtil.addMessage(mMbox, 1, TEST_NAME);
        mMessage1Id = msg.getId();
        ZimbraLog.test.debug("Created message 1, id=" + mMessage1Id);
        
        msg = TestUtil.addMessage(mMbox, 2, "RE: " + TEST_NAME);
        mMessage2Id = msg.getId();
        ZimbraLog.test.debug("Created message 2, id=" + mMessage2Id);
        
        msg =  TestUtil.addMessage(mMbox, 3, "RE: " + TEST_NAME);
        mMessage3Id = msg.getId();
        ZimbraLog.test.debug("Created message 3, id=" + mMessage3Id);
        
        mConvId = getMessage1().getConversationId();
        ZimbraLog.test.debug("Created conversation, id=" + mConvId);
        
        Folder folder = mMbox.createFolder(null, FOLDER1_NAME, Mailbox.ID_FOLDER_INBOX,
            MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        mFolder1Id = folder.getId();

        folder = mMbox.createFolder(null, FOLDER2_NAME, mFolder1Id,
            MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
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
        mMbox.alterTag(null, mMessage1Id, getMessage1().getType(), getTag1().getId(), true);
        mMbox.alterTag(null, mMessage1Id, getMessage1().getType(), getTag2().getId(), true);
        mMbox.alterTag(null, mMessage2Id, getMessage2().getType(), getTag2().getId(), true);
    }

    public void testReadMessage1()
    throws Exception {
        ZimbraLog.test.debug("testReadMessage1");
        verifySetUp();
        setUnread(getMessage1(), false);
        verifyMessage1Read();
    }

    public void testReadMessage2()
    throws Exception {
        ZimbraLog.test.debug("testReadMessage2");
        verifySetUp();
        
        setUnread(getMessage2(), false);
        
        assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
        
        verifyAllUnreadFlags();
    }

    public void testReadAllMessages()
    throws Exception {
        ZimbraLog.test.debug("testReadAllMessages");
        verifySetUp();
        
        setUnread(getMessage1(), false);
        setUnread(getMessage2(), false);
        setUnread(getMessage3(), false);
        verifyAllRead();
    }

    public void testReadConversation()
    throws Exception {
        ZimbraLog.test.debug("testReadConversation");
        setUnread(getConv(), false);
        verifyAllRead();
    }
    
    public void testReadFolder1()
    throws Exception {
        ZimbraLog.test.debug("testReadFolder1");
        verifySetUp();
        setUnread(getFolder1(), false);
        verifyMessage1Read();
    }

    public void testReadFolder2()
    throws Exception {
        ZimbraLog.test.debug("testReadFolder2");
        verifySetUp();
        
        setUnread(getFolder2(), false);
        
        assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 0, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 1, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    public void testReadAllFolders()
    throws Exception {
        ZimbraLog.test.debug("testReadAllMessages");
        verifySetUp();
        
        setUnread(getFolder1(), false);
        setUnread(getFolder2(), false);
        verifyAllRead();
    }

    public void testReadTag1()
    throws Exception {
        ZimbraLog.test.debug("testReadTag1");
        verifySetUp();
        setUnread(getTag1(), false);
        verifyMessage1Read();
    }

    public void testReadTag2()
    throws Exception {
        ZimbraLog.test.debug("testReadTag2");
        verifySetUp();
        
        setUnread(getTag2(), false);
        
        assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 1, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
        
        verifyAllUnreadFlags();
    }

    public void testMoveMessage()
    throws Exception {
        ZimbraLog.test.debug("testMoveMessage");
        verifySetUp();
        
        // Move M2 from F2 to F1 
        mMbox.move(null, mMessage2Id, getMessage2().getType(), mFolder1Id);
        assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 2, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
        
        // Mark M2 as read
        setUnread(getMessage2(), false);
        assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
        
        // Move M2 back to F2 and verify that the counts are unchanged
        mMbox.move(null, mMessage2Id, getMessage2().getType(), mFolder2Id);
        assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 1, getFolder2().getUnreadCount());
    }

    public void testMoveConversation()
    throws Exception {
        ZimbraLog.test.debug("testMoveConversation");
        verifySetUp();
        
        // Read M1 and move the whole conversation to F1
        setUnread(getMessage1(), false);
        mMbox.move(null, getConv().getId(), getConv().getType(), mFolder1Id);
        assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 2, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        
        // Move the conversation to F2
        mMbox.move(null, getConv().getId(), getConv().getType(), mFolder2Id);
        assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 2, getFolder2().getUnreadCount());
    }

    public void testTagMessage()
    throws Exception {
        ZimbraLog.test.debug("testTagMessage");
        verifySetUp();
        
        // Add T3 to M3
        mMbox.alterTag(null, mMessage3Id, getMessage3().getType(), getTag3().getId(), true);
        assertEquals("getTag3().getUnreadCount()", 1, getTag3().getUnreadCount());
        
        // Add T3 to M2
        mMbox.alterTag(null, mMessage2Id, getMessage2().getType(), getTag3().getId(), true);
        assertEquals("getTag3().getUnreadCount()", 2, getTag3().getUnreadCount());
        
        // Remove T3 from M3
        mMbox.alterTag(null, mMessage3Id, getMessage3().getType(), getTag3().getId(), false);
        assertEquals("getTag3().getUnreadCount()", 1, getTag3().getUnreadCount());
    }
    
    public void testTagConversation()
    throws Exception {
        ZimbraLog.test.debug("testTagConversation");
        verifySetUp();
        
        // Add T3 to C
        mMbox.alterTag(null, getConv().getId(), getConv().getType(), getTag3().getId(), true);
        assertEquals("getTag3().getUnreadCount()", 3, getTag3().getUnreadCount());
        
        // Remove T3 from C
        mMbox.alterTag(null, getConv().getId(), getConv().getType(), getTag3().getId(), false);
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    /**
     * Moves one read message and two unread messages to the trash.
     */
    public void testMoveToTrash()
    throws Exception {
        ZimbraLog.test.debug("testMoveToTrash");
        verifySetUp();
        
        Folder inbox = mMbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        int unreadCount = inbox.getUnreadCount();
        
        // Move conversation to Inbox
        mMbox.move(null, getConv().getId(), getConv().getType(), Mailbox.ID_FOLDER_INBOX);
        assertEquals("Move conversation to inbox", unreadCount + 3, inbox.getUnreadCount());

        // Read message 2
        setUnread(getMessage2(), false);
        assertEquals("Read message 2", unreadCount + 2, inbox.getUnreadCount());
        
        // Move message 1
        mMbox.move(null, mMessage1Id, getMessage1().getType(), Mailbox.ID_FOLDER_TRASH);
        assertEquals("Move message to trash", unreadCount + 1, inbox.getUnreadCount());
        
        // Move the rest of the conversation
        mMbox.move(null, getConv().getId(), getConv().getType(), Mailbox.ID_FOLDER_TRASH);
        assertEquals("Move conversation to trash", unreadCount, inbox.getUnreadCount());
    }

    /**
     * Makes sure that something comes back when searching for unread items.
     */
    public void testSearch()
    throws Exception {
        ZimbraLog.test.debug("testSearch");
        verifySetUp();
        
        byte[] types = { MailItem.TYPE_MESSAGE };
        ZimbraQueryResults results = mMbox.search(new Mailbox.OperationContext(mMbox), "is:unread", types, MailboxIndex.SortBy.DATE_DESCENDING, 100);
        assertTrue("No search results found", results.hasNext());
        results.doneWithSearchResults();
    }
    
    public void testDeleteConversation()
    throws Exception {
        ZimbraLog.test.debug("testDeleteConversation");
        verifySetUp();
        
        mMbox.delete(null, getConv().getId(), getConv().getType());
        
        assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }
    
    public void testDeleteFolder2()
    throws Exception {
        ZimbraLog.test.debug("testDeleteFolder2");
        verifySetUp();
        
        mMbox.delete(null, mFolder2Id, getFolder2().getType());
        
        assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 1, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }
    
    public void testDeleteFolder1()
    throws Exception {
        ZimbraLog.test.debug("testDeleteFolder1");
        verifySetUp();
        
        mMbox.delete(null, mFolder1Id, getFolder1().getType());
        
        assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
    }

    private void verifyMessage1Read()
    throws Exception {
        assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 2, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 2, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 1, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
        
        verifyAllUnreadFlags();
    }
    
    private void verifyAllUnreadFlags()
    throws Exception {
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
        assertEquals("Found " + numRows + " items with old unread flag set", 0, numRows);
    }

    private void verifyUnreadFlag(MailItem item)
    throws Exception {
        String flagString = item.getFlagString();
        if (item.isUnread()) {
            assertTrue("unread bit test: " + item.getFlagBitmask(), (item.getFlagBitmask() & Flag.BITMASK_UNREAD) > 0);
            assertTrue("unread flag string: " + flagString, flagString.indexOf(Flag.getAbbreviation(Flag.ID_FLAG_UNREAD)) >= 0); 
        } else {
            assertTrue("read bit test: " + item.getFlagBitmask(), (item.getFlagBitmask() & Flag.BITMASK_UNREAD) == 0);
            assertTrue("read flag string: " + flagString, flagString.indexOf(Flag.getAbbreviation(Flag.ID_FLAG_UNREAD)) == -1); 
        }

//        if (item.getType() == MailItem.TYPE_MESSAGE || item.getType() == MailItem.TYPE_INVITE) {
        if (item.getType() == MailItem.TYPE_MESSAGE) {
            DbResults results = DbUtil.executeQuery(
                "SELECT unread " +
                "FROM " + DbMailItem.getMailItemTableName(item) +
                " WHERE mailbox_id = " + item.getMailboxId() +
                " AND id = " + item.getId());
            assertEquals("Verify unread flag in the database", item.isUnread(), results.getBoolean(1));
        }
    }
    
    private void verifyAllRead()
    throws Exception {
        assertEquals("getMessage1().getUnreadCount()", 0, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 0, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 0, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 0, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 0, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 0, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 0, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 0, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());
        
        verifyAllUnreadFlags();
    }
    
    private void setUnread(MailItem item, boolean unread)
    throws Exception {
        mMbox.alterTag(null, item.getId(), item.getType(), Flag.ID_FLAG_UNREAD, unread);
        item = mMbox.getItemById(null, item.getId(), item.getType());
        verifyUnreadFlag(item);
    }
    
    private void verifySetUp()
    throws Exception {
        assertEquals("getMessage1().getUnreadCount()", 1, getMessage1().getUnreadCount());
        assertEquals("getMessage2().getUnreadCount()", 1, getMessage2().getUnreadCount());
        assertEquals("getMessage3().getUnreadCount()", 1, getMessage3().getUnreadCount());
        assertEquals("getConv().getUnreadCount()", 3, getConv().getUnreadCount());
        assertEquals("getFolder1().getUnreadCount()", 1, getFolder1().getUnreadCount());
        assertEquals("getFolder2().getUnreadCount()", 2, getFolder2().getUnreadCount());
        assertEquals("getTag1().getUnreadCount()", 1, getTag1().getUnreadCount());
        assertEquals("getTag2().getUnreadCount()", 2, getTag2().getUnreadCount());
        assertEquals("getTag3().getUnreadCount()", 0, getTag3().getUnreadCount());

        assertEquals("getMessage1().getFolderId()", mFolder1Id, getMessage1().getFolderId());
        assertEquals("getMessage2().getFolderId()", mFolder2Id, getMessage2().getFolderId());
        assertEquals("getMessage3().getFolderId()", mFolder2Id, getMessage3().getFolderId());
        
        assertTrue("getMessage1().isTagged(getTag1())", getMessage1().isTagged(getTag1()));
        assertTrue("getMessage1().isTagged(getTag2())", getMessage1().isTagged(getTag2()));
        assertTrue("getMessage2().isTagged(getTag2())", getMessage2().isTagged(getTag2()));
    }

    protected void tearDown()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, TEST_NAME);
    }
}
