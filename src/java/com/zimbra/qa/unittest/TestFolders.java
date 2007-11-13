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

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;

/**
 * @author bburtin
 */
public class TestFolders extends TestCase
{
    private Mailbox mMbox;
    private Account mAccount;

    private static String USER_NAME = "user1";
    private static String NAME_PREFIX = "TestFolders";
    
    /**
     * Creates the message used for tag tests 
     */
    protected void setUp()
    throws Exception {
        super.setUp();

        mAccount = TestUtil.getAccount(USER_NAME);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
        
        // Wipe out folders for this test, in case the last test didn't
        // exit cleanly
        cleanUp();
    }

    /**
     * Confirms that deleting a parent folder also deletes the child.
     */
    public void testDeleteParent()
    throws Exception {
        Folder parent = mMbox.createFolder(null, "/" + NAME_PREFIX + " - parent", (byte) 0, MailItem.TYPE_UNKNOWN);
        int parentId = parent.getId();
        Folder child = mMbox.createFolder(
            null, "NAME_PREFIX" + " - child", parent.getId(), MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mMbox.delete(null, parent.getId(), parent.getType());
        
        // Look up parent by id
        try {
            mMbox.getFolderById(null, parentId);
            fail("Parent folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        assertEquals("Parent folder query returned data.  id=" + parentId, 0, results.size());
        
        // Look up child by id
        try {
            mMbox.getFolderById(null, childId);
            fail("Child folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        assertEquals("Child folder query returned data.  id=" + childId, 0, results.size());
    }

    /**
     * Confirms that emptying a folder removes subfolders only when requested.
     */
    public void testEmptyFolderNonrecursive()
    throws Exception {
        Folder parent = mMbox.createFolder(null, "/" + NAME_PREFIX + " - parent", (byte) 0, MailItem.TYPE_UNKNOWN);
        int parentId = parent.getId();
        Folder child = mMbox.createFolder(
            null, "NAME_PREFIX" + " - child", parent.getId(), MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mMbox.emptyFolder(null, parent.getId(), false);
        
        // Look up parent by id
        mMbox.getFolderById(null, parentId);

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        assertEquals("Parent folder query returned no data.  id=" + parentId, 1, results.size());
        
        // Look up child by id
        mMbox.getFolderById(null, childId);

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        assertEquals("Child folder query returned no data.  id=" + childId, 1, results.size());
    }

    /**
     * Confirms that emptying a folder removes subfolders only when requested.
     */
    public void testEmptyFolderRecursive()
    throws Exception {
        Folder parent = mMbox.createFolder(null, "/" + NAME_PREFIX + " - parent", (byte) 0, MailItem.TYPE_UNKNOWN);
        int parentId = parent.getId();
        Folder child = mMbox.createFolder(
            null, "NAME_PREFIX" + " - child", parent.getId(), MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mMbox.emptyFolder(null, parent.getId(), true);
        
        // Look up parent by id
        mMbox.getFolderById(null, parentId);

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        assertEquals("Parent folder query returned no data.  id=" + parentId, 1, results.size());
        
        // Look up child by id
        try {
            mMbox.getFolderById(null, childId);
            fail("Child folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE mailbox_id = " + mMbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        assertEquals("Child folder query returned data.  id=" + childId, 0, results.size());
    }
    
    /**
     * Creates a hierarchy twenty folders deep.
     */
    public void testManySubfolders()
    throws Exception {
        final int NUM_LEVELS = 20;
        int parentId = Mailbox.ID_FOLDER_INBOX;
        Folder top = null;
        
        for (int i = 1; i <= NUM_LEVELS; i++) {
            Folder folder = mMbox.createFolder(null, NAME_PREFIX + i, parentId, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
            if (i == 1) {
                top = folder;
            }
            parentId = folder.getId();
        }
        
        mMbox.delete(null, top.getId(), top.getType());
    }
    
    /**
     * Deletes a folder that contains messages in a conversation.  Confirms
     * that the conversation size was correctly decremented.
     */
    public void testMarkDeletionTargets()
    throws Exception {
        String name = NAME_PREFIX + " MDT";

        // Create three messages and move two of them into a new folder.
        Message m1 = TestUtil.addMessage(mMbox, 1, name);
        ZimbraLog.test.debug("Created message 1, id=" + m1.getId());
        Message m2 = TestUtil.addMessage(mMbox, 2, "RE: " + name);
        ZimbraLog.test.debug("Created message 2, id=" + m2.getId());
        Message m3 = TestUtil.addMessage(mMbox, 3, "RE: " + name);
        ZimbraLog.test.debug("Created message 3, id=" + m3.getId());
        
        Folder f = mMbox.createFolder(null, name, Mailbox.ID_FOLDER_INBOX, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        mMbox.move(null, m1.getId(), m1.getType(), f.getId());
        mMbox.move(null, m2.getId(), m2.getType(), f.getId());
        
        // Verify conversation size
        Conversation conv = mMbox.getConversationById(null, m1.getConversationId());
        int convId = conv.getId();
        assertEquals("Conversation size before folder delete", 3, conv.getSize());

        // Delete the folder and confirm that the conversation size was decremented
        mMbox.delete(null, f.getId(), f.getType());
        conv = mMbox.getConversationById(null, convId);
        assertEquals("Conversation size after folder delete", 1, conv.getSize());
    }
    
    /**
     * Confirms that deleting a subfolder correctly updates the subfolder hierarchy.
     */
    public void testHierarchy()
    throws Exception {
        Folder f1 = mMbox.createFolder(null, "/f1", (byte) 0, MailItem.TYPE_UNKNOWN);
        Folder f2 = mMbox.createFolder(null, "/f1/f2", (byte) 0, MailItem.TYPE_UNKNOWN);
        mMbox.createFolder(null, "/f1/f2/f3", (byte) 0, MailItem.TYPE_UNKNOWN);
        assertEquals("Hierarchy size before delete", 3, f1.getSubfolderHierarchy().size());
        mMbox.delete(null, f2.getId(), f2.getType());
        List<Folder> hierarchy = f1.getSubfolderHierarchy();
        assertEquals("Hierarchy size after delete", 1, hierarchy.size());
        assertEquals("Folder id", f1.getId(), hierarchy.get(0).getId());
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        
        mAccount = TestUtil.getAccount(USER_NAME);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
        
        // cleanup after testHierarchy()
        try { // delete /f1/f2/f3
            Folder f= mMbox.getFolderByPath(null, "/f1/f2/f3");
            mMbox.delete(null, f.getId(), f.getType());
        } catch (Exception e) {}
        try { // delete /f1/f2
            Folder f= mMbox.getFolderByPath(null, "/f1/f2");
            mMbox.delete(null, f.getId(), f.getType());
        } catch (Exception e) {}
        try { // delete /f1
            Folder f= mMbox.getFolderByPath(null, "/f1");
            mMbox.delete(null, f.getId(), f.getType());
        } catch (Exception e) {}
            
        
    }
    
    protected void tearDown() throws Exception {
        cleanUp();
        super.tearDown();
    }
}
