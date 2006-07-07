/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestUnread extends TestCase
{
    private Connection mConn;
    private Mailbox mMbox;
    private Account mAccount;
    
    private static String FOLDER1_NAME = "TestUnread Folder 1";
    private static String FOLDER2_NAME = "TestUnread Folder 2";
    private static String TAG1_NAME = "TestUnread Tag 1";
    private static String TAG2_NAME = "TestUnread Tag 2";
    private static String TAG3_NAME = "TestUnread Tag 3";
    
    private Message mMessage1;
    private Message mMessage2;
    private Message mMessage3;
    private Conversation mConv;
    
    private Folder mFolder1;
    private Folder mFolder2;
    
    private Tag mTag1;
    private Tag mTag2;
    private Tag mTag3;

    /**
     * Constructor used for instantiating a <code>TestCase</code> that runs a single test.
     * 
     * @param testName the name of the method that will be called when the test is executed
     */
    public TestUnread(String testName) {
        super(testName);
    }
    
    /**
     * Sets up the following data set:
     * <ul>
     *   <li>F1 contains M1</li>
     *   <li>F2 contains M2 and M3</li>
     *   <li>T1 is assigned to M1</li>
     *   <li>T2 is assigned to M1 and M2</li>
     * </ul> 
     */
    protected void setUp()
    throws Exception {
        super.setUp();
        
        mAccount = TestUtil.getAccount("user1");
        mMbox = Mailbox.getMailboxByAccount(mAccount);
        
        // Clean up data, in case a previous test didn't exit cleanly
        cleanUp();
        
        mConn = DbPool.getConnection();
        
        mMessage1 = TestUtil.insertMessage(mMbox, 1, "TestUnread");
        ZimbraLog.test.debug("Created message 1, id=" + mMessage1.getId());
        mMessage2 = TestUtil.insertMessage(mMbox, 2, "RE: TestUnread");
        ZimbraLog.test.debug("Created message 2, id=" + mMessage2.getId());
        mMessage3 = TestUtil.insertMessage(mMbox, 3, "RE: TestUnread");
        ZimbraLog.test.debug("Created message 3, id=" + mMessage3.getId());
        mConv = mMbox.getConversationById(null, mMessage1.getConversationId());
        ZimbraLog.test.debug("Created conversation, id=" + mConv.getId());
        
        mFolder1 = mMbox.createFolder(null, FOLDER1_NAME, Mailbox.ID_FOLDER_INBOX, MailItem.TYPE_UNKNOWN, null);
        mFolder2 = mMbox.createFolder(null, FOLDER2_NAME, mFolder1.getId(), MailItem.TYPE_UNKNOWN, null);
        
        mTag1 = mMbox.createTag(null, TAG1_NAME, (byte)0);
        mTag2 = mMbox.createTag(null, TAG2_NAME, (byte)0);
        mTag3 = mMbox.createTag(null, TAG3_NAME, (byte)0);
        
        mMbox.move(null, mMessage1.getId(), mMessage1.getType(), mFolder1.getId());
        refresh();
        mMbox.move(null, mMessage2.getId(), mMessage1.getType(), mFolder2.getId());
        refresh();
        mMbox.move(null, mMessage3.getId(), mMessage1.getType(), mFolder2.getId());
        refresh();
        mMbox.alterTag(null, mMessage1.getId(), mMessage1.getType(), mTag1.getId(), true);
        refresh();
        mMbox.alterTag(null, mMessage1.getId(), mMessage1.getType(), mTag2.getId(), true);
        refresh();
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTag2.getId(), true);
        refresh();
    }

    public void testReadMessage1()
    throws Exception {
        ZimbraLog.test.debug("testReadMessage1");
        verifySetUp();
        setUnread(mMessage1, false);
        verifyMessage1Read();
    }

    public void testReadMessage2()
    throws Exception {
        ZimbraLog.test.debug("testReadMessage2");
        verifySetUp();
        
        setUnread(mMessage2, false);
        
        assertEquals("mMessage1.getUnreadCount()", 1, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 0, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 1, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 2, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 1, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 1, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 1, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 1, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
        
        verifyAllUnreadFlags();
    }

    public void testReadAllMessages()
    throws Exception {
        ZimbraLog.test.debug("testReadAllMessages");
        verifySetUp();
        
        setUnread(mMessage1, false);
        setUnread(mMessage2, false);
        setUnread(mMessage3, false);
        verifyAllRead();
    }

    public void testReadConversation()
    throws Exception {
        ZimbraLog.test.debug("testReadConversation");
        setUnread(mConv, false);
        verifyAllRead();
    }
    
    public void testReadFolder1()
    throws Exception {
        ZimbraLog.test.debug("testReadFolder1");
        verifySetUp();
        setUnread(mFolder1, false);
        verifyMessage1Read();
    }

    public void testReadFolder2()
    throws Exception {
        ZimbraLog.test.debug("testReadFolder2");
        verifySetUp();
        
        setUnread(mFolder2, false);
        
        assertEquals("mMessage1.getUnreadCount()", 1, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 0, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 0, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 1, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 1, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 0, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 1, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 1, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
    }

    public void testReadAllFolders()
    throws Exception {
        ZimbraLog.test.debug("testReadAllMessages");
        verifySetUp();
        
        setUnread(mFolder1, false);
        setUnread(mFolder2, false);
        verifyAllRead();
    }

    public void testReadTag1()
    throws Exception {
        ZimbraLog.test.debug("testReadTag1");
        verifySetUp();
        setUnread(mTag1, false);
        verifyMessage1Read();
    }

    public void testReadTag2()
    throws Exception {
        ZimbraLog.test.debug("testReadTag2");
        verifySetUp();
        
        setUnread(mTag2, false);
        
        assertEquals("mMessage1.getUnreadCount()", 0, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 0, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 1, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 1, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 0, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 1, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 0, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 0, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
        
        verifyAllUnreadFlags();
    }

    public void testMoveMessage()
    throws Exception {
        ZimbraLog.test.debug("testMoveMessage");
        verifySetUp();
        
        // Move M2 from F2 to F1 
        mMbox.move(null, mMessage2.getId(), mMessage2.getType(), mFolder1.getId());
        refresh();
        assertEquals("mMessage2.getUnreadCount()", 1, mMessage2.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 2, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 1, mFolder2.getUnreadCount());
        
        // Mark M2 as read
        setUnread(mMessage2, false);
        refresh();
        assertEquals("mMessage2.getUnreadCount()", 0, mMessage2.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 1, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 1, mFolder2.getUnreadCount());
        
        // Move M2 back to F2 and verify that the counts are unchanged
        mMbox.move(null, mMessage2.getId(), mMessage2.getType(), mFolder2.getId());
        refresh();
        assertEquals("mMessage2.getUnreadCount()", 0, mMessage2.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 1, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 1, mFolder2.getUnreadCount());
    }

    public void testMoveConversation()
    throws Exception {
        ZimbraLog.test.debug("testMoveConversation");
        verifySetUp();
        
        // Read M1 and move the whole conversation to F1
        setUnread(mMessage1, false);
        refresh();
        mMbox.move(null, mConv.getId(), mConv.getType(), mFolder1.getId());
        refresh();
        assertEquals("mMessage1.getUnreadCount()", 0, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 1, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 1, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 2, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 2, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 0, mFolder2.getUnreadCount());
        
        // Move the conversation to F2
        mMbox.move(null, mConv.getId(), mConv.getType(), mFolder2.getId());
        refresh();
        assertEquals("mMessage1.getUnreadCount()", 0, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 1, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 1, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 2, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 0, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 2, mFolder2.getUnreadCount());
    }

    public void testTagMessage()
    throws Exception {
        ZimbraLog.test.debug("testTagMessage");
        verifySetUp();
        
        // Add T3 to M3
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTag3.getId(), true);
        refresh();
        assertEquals("mTag3.getUnreadCount()", 1, mTag3.getUnreadCount());
        
        // Add T3 to M2
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTag3.getId(), true);
        refresh();
        assertEquals("mTag3.getUnreadCount()", 2, mTag3.getUnreadCount());
        
        // Remove T3 from M3
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTag3.getId(), false);
        refresh();
        assertEquals("mTag3.getUnreadCount()", 1, mTag3.getUnreadCount());
    }
    
    public void testTagConversation()
    throws Exception {
        ZimbraLog.test.debug("testTagConversation");
        verifySetUp();
        
        // Add T3 to C
        mMbox.alterTag(null, mConv.getId(), mConv.getType(), mTag3.getId(), true);
        refresh();
        assertEquals("mTag3.getUnreadCount()", 3, mTag3.getUnreadCount());
        
        // Remove T3 from C
        mMbox.alterTag(null, mConv.getId(), mConv.getType(), mTag3.getId(), false);
        refresh();
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
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
        mMbox.move(null, mConv.getId(), mConv.getType(), Mailbox.ID_FOLDER_INBOX);
        refresh();
        assertEquals("Move conversation to inbox", unreadCount + 3, inbox.getUnreadCount());

        // Read message 2
        setUnread(mMessage2, false);
        refresh();
        assertEquals("Read message 2", unreadCount + 2, inbox.getUnreadCount());
        
        // Move message 1
        mMbox.move(null, mMessage1.getId(), mMessage1.getType(), Mailbox.ID_FOLDER_TRASH);
        refresh();
        assertEquals("Move message to trash", unreadCount + 1, inbox.getUnreadCount());
        
        // Move the rest of the conversation
        mMbox.move(null, mConv.getId(), mConv.getType(), Mailbox.ID_FOLDER_TRASH);
        refresh();
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
        
        mMbox.delete(null, mConv.getId(), mConv.getType());
        mMessage1 = null;
        mMessage2 = null;
        mMessage3 = null;
        refresh();
        
        assertEquals("mFolder1.getUnreadCount()", 0, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 0, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 0, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 0, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
    }
    
    public void testDeleteFolder2()
    throws Exception {
        ZimbraLog.test.debug("testDeleteFolder2");
        verifySetUp();
        
        mMbox.delete(null, mFolder2.getId(), mFolder2.getType());
        mFolder2 = null;
        mMessage2 = null;
        mMessage3 = null;
        refresh();
        
        assertEquals("mMessage1.getUnreadCount()", 1, mMessage1.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 1, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 1, mFolder1.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 1, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 1, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
    }
    
    public void testDeleteFolder1()
    throws Exception {
        ZimbraLog.test.debug("testDeleteFolder1");
        verifySetUp();
        
        mMbox.delete(null, mFolder1.getId(), mFolder1.getType());
        mFolder1 = null;
        mFolder2 = null;
        mMessage1 = null;
        mMessage2 = null;
        mMessage3 = null;
        refresh();
        
        assertEquals("mTag1.getUnreadCount()", 0, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 0, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
    }
    
    private void verifyMessage1Read()
    throws Exception {
        assertEquals("mMessage1.getUnreadCount()", 0, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 1, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 1, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 2, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 0, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 2, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 0, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 1, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
        
        verifyAllUnreadFlags();
    }
    
    private void verifyAllUnreadFlags()
    throws Exception {
        verifyUnreadFlag(mMessage1);
        verifyUnreadFlag(mMessage2);
        verifyUnreadFlag(mMessage3);
        verifyUnreadFlag(mConv);
        verifyUnreadFlag(mFolder1);
        verifyUnreadFlag(mFolder2);
        verifyUnreadFlag(mTag1);
        verifyUnreadFlag(mTag2);
        
        // Make sure we aren't still setting the old unread flag
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE flags & " + Flag.FLAG_UNREAD + " > 0");
        int numRows = results.getInt(1);
        assertEquals("Found " + numRows + " items with old unread flag set", 0, numRows);
    }

    private void verifyUnreadFlag(MailItem item)
    throws Exception {
        String flagString = item.getFlagString();
        if (item.isUnread()) {
            assertTrue("unread bit test: " + item.getFlagBitmask(), (item.getFlagBitmask() & Flag.FLAG_UNREAD) > 0);
            assertTrue("unread flag string: " + flagString, flagString.indexOf(Flag.getAbbreviation(Flag.ID_FLAG_UNREAD)) >= 0); 
        } else {
            assertTrue("read bit test: " + item.getFlagBitmask(), (item.getFlagBitmask() & Flag.FLAG_UNREAD) == 0);
            assertTrue("read flag string: " + flagString, flagString.indexOf(Flag.getAbbreviation(Flag.ID_FLAG_UNREAD)) == -1); 
        }

//        if (item.getType() == MailItem.TYPE_MESSAGE || item.getType() == MailItem.TYPE_INVITE) {
        if (item.getType() == MailItem.TYPE_MESSAGE) {
            DbResults results = DbUtil.executeQuery(
                "SELECT unread " +
                "FROM " + DbMailItem.getMailItemTableName(item) +
                " WHERE id = " + item.getId());
            assertEquals("Verify unread flag in the database", item.isUnread(), results.getBoolean(1));
        }
    }
    
    private void verifyAllRead()
    throws Exception {
        assertEquals("mMessage1.getUnreadCount()", 0, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 0, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 0, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 0, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 0, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 0, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 0, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 0, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());
        
        verifyAllUnreadFlags();
    }
    
    private void setUnread(MailItem item, boolean unread)
    throws Exception {
        mMbox.alterTag(null, item.getId(), item.getType(), Flag.ID_FLAG_UNREAD, unread);
        refresh();
        item = mMbox.getItemById(null, item.getId(), item.getType());
        verifyUnreadFlag(item);
    }
    
    private void verifySetUp()
    throws Exception {
        assertEquals("mMessage1.getUnreadCount()", 1, mMessage1.getUnreadCount());
        assertEquals("mMessage2.getUnreadCount()", 1, mMessage2.getUnreadCount());
        assertEquals("mMessage3.getUnreadCount()", 1, mMessage3.getUnreadCount());
        assertEquals("mConv.getUnreadCount()", 3, mConv.getUnreadCount());
        assertEquals("mFolder1.getUnreadCount()", 1, mFolder1.getUnreadCount());
        assertEquals("mFolder2.getUnreadCount()", 2, mFolder2.getUnreadCount());
        assertEquals("mTag1.getUnreadCount()", 1, mTag1.getUnreadCount());
        assertEquals("mTag2.getUnreadCount()", 2, mTag2.getUnreadCount());
        assertEquals("mTag3.getUnreadCount()", 0, mTag3.getUnreadCount());

        assertEquals("mMessage1.getFolderId()", mFolder1.getId(), mMessage1.getFolderId());
        assertEquals("mMessage2.getFolderId()", mFolder2.getId(), mMessage2.getFolderId());
        assertEquals("mMessage3.getFolderId()", mFolder2.getId(), mMessage3.getFolderId());
        
        assertTrue("mMessage1.isTagged(mTag1)", mMessage1.isTagged(mTag1));
        assertTrue("mMessage1.isTagged(mTag2)", mMessage1.isTagged(mTag2));
        assertTrue("mMessage2.isTagged(mTag2)", mMessage2.isTagged(mTag2));
    }
    
    private void refresh()
    throws Exception {
        if (mMessage1 != null) {
            mMessage1 = mMbox.getMessageById(null, mMessage1.getId());
            mConv = mMbox.getConversationById(null, mMessage1.getConversationId());
        }
        if (mMessage2 != null) {
            mMessage2 = mMbox.getMessageById(null, mMessage2.getId());
        }
        if (mMessage3 != null) {
            mMessage3 = mMbox.getMessageById(null, mMessage3.getId());
        }
        if (mFolder1 != null) {
            mFolder1 = mMbox.getFolderById(null, mFolder1.getId());
        }
        if (mFolder2 != null) {
            mFolder2 = mMbox.getFolderById(null, mFolder2.getId());
        }
        if (mTag1 != null) {
            mTag1 = mMbox.getTagById(null, mTag1.getId());
        }
        if (mTag2 != null) {
            mTag2 = mMbox.getTagById(null, mTag2.getId());
        }
        if (mTag3 != null) {
            mTag3 = mMbox.getTagById(null, mTag3.getId());
        }
    }
    
    protected void tearDown()
    throws Exception {
        cleanUp();
        super.tearDown();
    }

    /**
     * Deletes messages, folders and tags whose name or subject contains
     * "TestUnread".
     */
    private void cleanUp()
    throws Exception {
        // Iterate the item list to find things we should clean up.  We do
        // this because Mailbox.getFolderByName() will throw a
        // ServiceException if the folder doesn't exist.
        
        // Delete folder 1.  This will automatically delete folder 2 and the messages
        // in the folders.
        List l = mMbox.getItemList(null, MailItem.TYPE_FOLDER);
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Folder folder = (Folder) i.next();
            if (folder.getName().equals(FOLDER1_NAME)) {
                // Folder 2 is a subfolder and will get deleted automatically
                mMbox.delete(null, folder.getId(), folder.getType());
            }
        }

        // Delete tags
        l = mMbox.getItemList(null, MailItem.TYPE_TAG);
        i = l.iterator();
        while (i.hasNext()) {
            Tag tag = (Tag) i.next();
            if (tag.getName().indexOf("TestUnread") >= 0) {
                mMbox.delete(null, tag.getId(), tag.getType());
            }
        }
        
        DbPool.quietClose(mConn);
    }
}
