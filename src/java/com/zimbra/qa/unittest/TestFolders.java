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

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.util.ZimbraLog;

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
        String name = NAME_PREFIX + "MDT";

        // Create three messages and move two of them into a new folder.
        Message m1 = TestUtil.insertMessage(mMbox, 1, name);
        ZimbraLog.test.debug("Created message 1, id=" + m1.getId());
        Message m2 = TestUtil.insertMessage(mMbox, 2, "RE: " + name);
        ZimbraLog.test.debug("Created message 2, id=" + m2.getId());
        Message m3 = TestUtil.insertMessage(mMbox, 3, "RE: " + name);
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

    private void cleanUp()
    throws Exception {
        deleteTestData(MailItem.TYPE_MESSAGE);
        deleteTestData(MailItem.TYPE_FOLDER);
    }
    
    protected void tearDown() throws Exception {
        cleanUp();
        super.tearDown();
    }

    private void deleteTestData(byte type)
    throws Exception {
        // Delete folders bottom-up to avoid orphaned folders
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE " +
            (!DebugConfig.disableMailboxGroup ? "mailbox_id = " + mMbox.getId() + " AND " : "") +
            "type = " + type + " AND subject LIKE '%" + NAME_PREFIX + "%' " +
            "ORDER BY id DESC";
        DbResults results = DbUtil.executeQuery(sql);
        while (results.next()) {
            int id = results.getInt(1);
            mMbox.delete(null, id, type);
        }
    }
}
