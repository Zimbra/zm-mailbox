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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.common.util.ZimbraLog;

public class TestMailItem extends TestCase {
    
    private static final String USER_NAME = "user1";
    private static final int TEST_CONTACT_ID = 9999;
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    public void testListItemIds()
    throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        
        // Get item count per folder/type
        String sql = "SELECT folder_id, type, count(*) AS item_count " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() +
            " GROUP BY folder_id, type";
        DbResults results = DbUtil.executeQuery(sql);
        assertTrue("No results returned", results.size() > 0);
        
        // Confirm that listItemIds() returns the right count for each folder/type
        while (results.next()) {
            int folderId = results.getInt("folder_id");
            byte type = (byte) results.getInt("type");
            int count = results.getInt("item_count");
            ZimbraLog.test.debug(
                "Confirming that folder " + folderId + " has " + count + " items of type " + type);
            Folder folder = mbox.getFolderById(null, folderId);
            assertNotNull("Folder not found", folder);

            List<Integer> ids = mbox.listItemIds(null, type, folderId);
            assertEquals("Item count does not match", count, ids.size());
        }
    }
    
    /**
     * Confirms that {@link MailItem#getBlob()} works when the blob digest
     * contains an empty string.
     */
    public void testGetBlob()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        List<Integer> ids = TestUtil.search(mbox, "contact:Chin", MailItem.TYPE_CONTACT);
        assertEquals("Unexpected number of contacts", 1, ids.size());
        
        String sql = "INSERT INTO " + DbMailItem.getMailItemTableName(mbox) +
            "  (mailbox_id, id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, " +
            "  blob_digest, unread, flags, tags, sender, subject, name, metadata, mod_metadata, " +
            "  change_date, mod_content) " +
            "SELECT mailbox_id, " + TEST_CONTACT_ID + ", type, parent_id, folder_id, " +
              TEST_CONTACT_ID + ", imap_id, date, size, volume_id, " +
            "  '', unread, flags, tags, 'TestMailItem.testGetBlob', subject, name, metadata, mod_metadata, " +
            "  change_date, mod_content " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE id = " + ids.get(0);
        int numRows = DbUtil.executeUpdate(sql);
        assertEquals("Unexpected number of rows updated", 1, numRows);
        
        Contact testContact = mbox.getContactById(null, TEST_CONTACT_ID);
        String blobDigest = testContact.getDigest();
        assertNull("Blob digest should have been null: " + blobDigest, blobDigest);
        MailboxBlob blob = testContact.getBlob(); 
        assertNull("Blob should have been null: " + blob, blob);
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        mbox.delete(null, TEST_CONTACT_ID, MailItem.TYPE_CONTACT);
    }
}
