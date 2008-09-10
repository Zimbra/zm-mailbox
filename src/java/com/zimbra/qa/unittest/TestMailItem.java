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

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.TestCase;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.formatter.HeadersOnlyInputStream;

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
            // XXX bburtin: Work around incompatibility between JDBC driver version
            // 5.0.3 and MySQL 5.0.67, where the column name returned for an alias
            // is an empty string.
            // int count = results.getInt("item_count");
            int count = results.getInt(3);
            ZimbraLog.test.debug(
                "Confirming that folder " + folderId + " has " + count + " items of type " + type);
            Folder folder = mbox.getFolderById(null, folderId);
            assertNotNull("Folder not found", folder);

            List<Integer> ids = mbox.listItemIds(null, type, folderId);
            assertEquals("Item count does not match", count, ids.size());
        }
    }
    
    public void testHeadersOnlyInputStream()
    throws Exception {
        // Test no CRLFCRLF.
        String s = "test";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
        HeadersOnlyInputStream headerStream = new HeadersOnlyInputStream(in);
        String read = new String(ByteUtil.getContent(headerStream, s.length()));
        assertEquals(s, read);
        
        // Test CRLFCRLF in the beginning.
        s = "\r\n\r\ntest";
        in = new ByteArrayInputStream(s.getBytes());
        headerStream = new HeadersOnlyInputStream(in);
        read = new String(ByteUtil.getContent(headerStream, s.length()));
        assertEquals(0, read.length());
        
        // Test CRLFCRLF in the middle.
        s = "te\r\n\r\nst";
        in = new ByteArrayInputStream(s.getBytes());
        headerStream = new HeadersOnlyInputStream(in);
        read = new String(ByteUtil.getContent(headerStream, s.length()));
        assertEquals("te", read);
        
        // Test CRLFCRLF in the end.
        s = "test\r\n\r\n";
        in = new ByteArrayInputStream(s.getBytes());
        headerStream = new HeadersOnlyInputStream(in);
        read = new String(ByteUtil.getContent(headerStream, s.length()));
        assertEquals("test", read);
        
        // Test CRLFCR without the last LF.
        s = "te\r\n\rst";
        in = new ByteArrayInputStream(s.getBytes());
        headerStream = new HeadersOnlyInputStream(in);
        read = new String(ByteUtil.getContent(headerStream, s.length()));
        assertEquals(s, read);
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
