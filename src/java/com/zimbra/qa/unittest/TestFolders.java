/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.List;

import junit.framework.TestCase;

import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class TestFolders extends TestCase
{
    private static String USER_NAME = "user1";
    private static String NAME_PREFIX = "TestFolders";

    private String mOriginalEmptyFolderBatchSize;
    
    @BeforeMethod
    @Override
    protected void setUp()
    throws Exception {
        cleanUp();
        mOriginalEmptyFolderBatchSize = TestUtil.getServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize);
    }

    /**
     * Confirms that deleting a parent folder also deletes the child.
     */
    @Test(groups = {"Server"})
    public void testDeleteParent()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder parent = mbox.createFolder(null, "/" + NAME_PREFIX + " - parent", (byte) 0, MailItem.TYPE_UNKNOWN);
        int parentId = parent.getId();
        Folder child = mbox.createFolder(
            null, "NAME_PREFIX" + " - child", parent.getId(), MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mbox.delete(null, parent.getId(), parent.getType());
        
        // Look up parent by id
        try {
            mbox.getFolderById(null, parentId);
            fail("Parent folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        assertEquals("Parent folder query returned data.  id=" + parentId, 0, results.size());
        
        // Look up child by id
        try {
            mbox.getFolderById(null, childId);
            fail("Child folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        assertEquals("Child folder query returned data.  id=" + childId, 0, results.size());
    }

    /**
     * Confirms that emptying a folder removes subfolders only when requested.
     */
    @Test(groups = {"Server"})
    public void testEmptyFolderNonrecursive()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder parent = mbox.createFolder(null, "/" + NAME_PREFIX + " - parent", (byte) 0, MailItem.TYPE_UNKNOWN);
        int parentId = parent.getId();
        Folder child = mbox.createFolder(
            null, "NAME_PREFIX" + " - child", parent.getId(), MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mbox.emptyFolder(null, parent.getId(), false);
        
        // Look up parent by id
        mbox.getFolderById(null, parentId);

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        assertEquals("Parent folder query returned no data.  id=" + parentId, 1, results.size());
        
        // Look up child by id
        mbox.getFolderById(null, childId);

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        assertEquals("Child folder query returned no data.  id=" + childId, 1, results.size());
    }

    /**
     * Confirms that emptying a folder removes subfolders only when requested.
     */
    @Test(groups = {"Server"})
    public void testEmptyFolderRecursive()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder parent = mbox.createFolder(null, "/" + NAME_PREFIX + " - parent", (byte) 0, MailItem.TYPE_UNKNOWN);
        int parentId = parent.getId();
        Folder child = mbox.createFolder(
            null, "NAME_PREFIX" + " - child", parent.getId(), MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mbox.emptyFolder(null, parent.getId(), true);
        
        // Look up parent by id
        mbox.getFolderById(null, parentId);

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        assertEquals("Parent folder query returned no data.  id=" + parentId, 1, results.size());
        
        // Look up child by id
        try {
            mbox.getFolderById(null, childId);
            fail("Child folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        assertEquals("Child folder query returned data.  id=" + childId, 0, results.size());
    }
    
    /**
     * Creates a hierarchy twenty folders deep.
     */
    @Test(groups = {"Server"})
    public void testManySubfolders()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        final int NUM_LEVELS = 20;
        int parentId = Mailbox.ID_FOLDER_INBOX;
        Folder top = null;
        
        for (int i = 1; i <= NUM_LEVELS; i++) {
            Folder folder = mbox.createFolder(null, NAME_PREFIX + i, parentId, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
            if (i == 1) {
                top = folder;
            }
            parentId = folder.getId();
        }
        
        mbox.delete(null, top.getId(), top.getType());
    }
    
    /**
     * Deletes a folder that contains messages in a conversation.  Confirms
     * that the conversation size was correctly decremented.
     */
    @Test(groups = {"Server"})
    public void testMarkDeletionTargets()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        String name = NAME_PREFIX + " MDT";

        // Create three messages and move two of them into a new folder.
        Message m1 = TestUtil.addMessage(mbox, name);
        ZimbraLog.test.debug("Created message 1, id=" + m1.getId());
        Message m2 = TestUtil.addMessage(mbox, "RE: " + name);
        ZimbraLog.test.debug("Created message 2, id=" + m2.getId());
        Message m3 = TestUtil.addMessage(mbox, "RE: " + name);
        ZimbraLog.test.debug("Created message 3, id=" + m3.getId());
        
        Folder f = mbox.createFolder(null, name, Mailbox.ID_FOLDER_INBOX, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        mbox.move(null, m1.getId(), m1.getType(), f.getId());
        mbox.move(null, m2.getId(), m2.getType(), f.getId());
        
        // Verify conversation size
        Conversation conv = mbox.getConversationById(null, m1.getConversationId());
        int convId = conv.getId();
        assertEquals("Conversation size before folder delete", 3, conv.getSize());

        // Delete the folder and confirm that the conversation size was decremented
        mbox.delete(null, f.getId(), f.getType());
        conv = mbox.getConversationById(null, convId);
        assertEquals("Conversation size after folder delete", 1, conv.getSize());
    }
    
    /**
     * Confirms that deleting a subfolder correctly updates the subfolder hierarchy.
     */
    @Test(groups = {"Server"})
    public void testHierarchy()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder f1 = mbox.createFolder(null, "/f1", (byte) 0, MailItem.TYPE_UNKNOWN);
        Folder f2 = mbox.createFolder(null, "/f1/f2", (byte) 0, MailItem.TYPE_UNKNOWN);
        mbox.createFolder(null, "/f1/f2/f3", (byte) 0, MailItem.TYPE_UNKNOWN);
        assertEquals("Hierarchy size before delete", 3, f1.getSubfolderHierarchy().size());
        mbox.delete(null, f2.getId(), f2.getType());
        List<Folder> hierarchy = f1.getSubfolderHierarchy();
        assertEquals("Hierarchy size after delete", 1, hierarchy.size());
        assertEquals("Folder id", f1.getId(), hierarchy.get(0).getId());
    }

    @Test
    public void testEmptyLargeFolder()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize, Integer.toString(3));
        
        // Create folders.
        String parentPath = "/" + NAME_PREFIX + "-parent";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder parent = TestUtil.createFolder(mbox, parentPath);
        ZFolder child = TestUtil.createFolder(mbox, parent.getId(), "child");
        
        // Add messages.
        for (int i = 1; i <= 5; i++) {
            TestUtil.addMessage(mbox, NAME_PREFIX + " parent " + i, parent.getId());
            TestUtil.addMessage(mbox, NAME_PREFIX + " child " + i, child.getId());
        }
        mbox.noOp();
        assertEquals(5, parent.getMessageCount());
        assertEquals(5, child.getMessageCount());

        // Empty parent folder without deleting subfolders.
        mbox.emptyFolder(parent.getId(), false);
        mbox.noOp();
        assertEquals(0, parent.getMessageCount());
        assertEquals(5, child.getMessageCount());
        
        // Add more messages to the parent folder.
        for (int i = 6; i <= 10; i++) {
            TestUtil.addMessage(mbox, NAME_PREFIX + " parent " + i, parent.getId());
        }
        mbox.noOp();
        assertEquals(5, parent.getMessageCount());
        assertEquals(5, child.getMessageCount());
        
        // Empty parent folder and delete subfolders.
        String childPath = child.getPath();
        assertNotNull(mbox.getFolderByPath(childPath));
        mbox.emptyFolder(parent.getId(), true);
        mbox.noOp();
        assertEquals(0, parent.getMessageCount());
        assertNull(mbox.getFolderByPath(childPath));
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder f = mbox.getFolderByPath("/f1");
        if (f != null) {
            mbox.deleteFolder(f.getId());
        }
    }
    
    @AfterMethod
    @Override
    protected void tearDown() throws Exception {
        cleanUp();
        TestUtil.setServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize, mOriginalEmptyFolderBatchSize);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestNG testng = TestUtil.newTestNG();
        testng.setExcludedGroups("Server");
        testng.setTestClasses(new Class[] { TestFolders.class });
        testng.run();
    }
}
