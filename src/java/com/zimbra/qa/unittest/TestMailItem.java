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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.ZimbraLog;

public class TestMailItem extends TestCase {
    
    public void testListItemIds()
    throws Exception {
        ZimbraLog.test.debug("testListItemIds");
        
        Account account = TestUtil.getAccount("user1");
        Mailbox mbox = Mailbox.getMailboxByAccount(account);
        
        // Get item count per folder/type
        String sql = "SELECT folder_id, type, count(*) AS item_count " +
            "FROM " + DbMailItem.getMailItemTableName(mbox.getId()) + " " +
            "GROUP BY folder_id, type";
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

            int ids[] = mbox.listItemIds(null, type, folderId);
            assertEquals("Item count does not match", count, ids.length);
        }
    }
}
