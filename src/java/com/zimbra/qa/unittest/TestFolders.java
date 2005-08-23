/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestFolders extends TestCase
{
    private Mailbox mMbox;
    private Account mAccount;
    private Connection mConn;
    
    private static String FOLDER_PREFIX = "TestFolders";
    
    /**
     * Creates the message used for tag tests 
     */
    protected void setUp()
    throws Exception {
        ZimbraLog.test.debug("TestFolders.setUp()");
        super.setUp();

        mAccount = TestUtil.getAccount("user1");
        mMbox = Mailbox.getMailboxByAccount(mAccount);
        mConn = DbPool.getConnection();
        
        // Wipe out folders for this test, in case the last test didn't
        // exit cleanly
        deleteFolders();
    }

    public void testManySubfolders()
    throws Exception {
        ZimbraLog.test.debug("testManySubfolders");
        final int NUM_LEVELS = 20;
        int parentId = Mailbox.ID_FOLDER_INBOX;
        Folder top = null;
        
        for (int i = 1; i <= NUM_LEVELS; i++) {
            Folder folder = mMbox.createFolder(null, FOLDER_PREFIX + i, parentId);
            if (i == 1) {
                top = folder;
            }
            parentId = folder.getId();
        }
        
        mMbox.delete(null, top.getId(), top.getType());
    }
    
    protected void tearDown() throws Exception {
        ZimbraLog.test.debug("TestFolders.tearDown()");

        deleteFolders();
        
        DbPool.quietClose(mConn);
        super.tearDown();
    }

    private void deleteFolders()
    throws Exception {
        // Delete folders bottom-up to avoid orphaned folders
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mMbox) +
            " WHERE subject LIKE '" + FOLDER_PREFIX + "%' " +
            "ORDER BY id DESC";
        DbResults results = DbUtil.executeQuery(sql);
        while (results.next()) {
            int id = results.getInt(1);
            mMbox.delete(null, id, MailItem.TYPE_FOLDER);
        }
    }
}
