package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.db.DbMailItem;
import com.liquidsys.coco.db.DbResults;
import com.liquidsys.coco.db.DbUtil;
import com.liquidsys.coco.mailbox.Folder;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.util.LiquidLog;

public class TestMailItem extends TestCase {
    
    public void testListItemIds()
    throws Exception {
        LiquidLog.test.debug("testListItemIds");
        
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
            LiquidLog.test.debug(
                "Confirming that folder " + folderId + " has " + count + " items of type " + type);
            Folder folder = mbox.getFolderById(folderId);
            assertNotNull("Folder not found", folder);

            int ids[] = mbox.listItemIds(type, folderId);
            assertEquals("Item count does not match", count, ids.length);
        }
    }
}
