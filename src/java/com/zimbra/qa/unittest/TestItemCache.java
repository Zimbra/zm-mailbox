/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.stats.ZimbraPerf;

public class TestItemCache extends TestCase
{
    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestItemCache.class.getSimpleName();
    
    private Mailbox mMbox;
    private Account mAccount;
    
    protected void setUp()
    throws Exception {
        cleanUp();
        mAccount = TestUtil.getAccount("user1");
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
    }

    /**
     * Re-gets the same message 10 times and makes sure we don't hit the database.
     */
    public void testCacheHit()
    throws Exception {
        List<MailItem> messages = mMbox.getItemList(null, MailItem.TYPE_MESSAGE);
        assertTrue("No messages found", messages.size() > 0);
        Message msg = (Message) messages.get(0);
        mMbox.getItemById(null, msg.getId(), msg.getType());
        
        int prepareCount = ZimbraPerf.getPrepareCount();
        for (int i = 1; i <= 10; i++) {
            mMbox.getItemById(null, msg.getId(), msg.getType());
        }
        
        prepareCount = ZimbraPerf.getPrepareCount() - prepareCount;
        assertEquals("Detected unexpected SQL statements.",
            0, prepareCount);
    }
    
    /**
     * Gets the specified message from the message cache.
     */
    private class ReaderThread
    extends Thread {
        private String mUserName;
        private String mSubject;
        private boolean mSuccess = false;
        
        public ReaderThread(String userName, String subject) {
            mUserName = userName;
            mSubject = subject;
        }
        
        public boolean wasSuccessful() { 
            return mSuccess;
        }
        
        public void run() {
            try {
                Mailbox mbox = TestUtil.getMailbox(mUserName);
                List<Integer> ids = TestUtil.search(mbox, "in:inbox subject:\"" + mSubject + "\"", MailItem.TYPE_MESSAGE);
                assertEquals(1, ids.size());
                Message msg = mbox.getMessageById(null, ids.get(0));
                msg.getMimeMessage();
                mSuccess = true;
            } catch (Exception e) {
                ZimbraLog.test.error("%s was not able to read message %s", mUserName, mSubject, e);
            }
        }
    }

    /**
     * Simultaneously reads the same message in four mailboxes and confirms that the message
     * cache size is correct (
     * @throws Exception
     */
    public void testMessageCache()
    throws Exception {
        // Deliver a large message.
        StringBuilder buf = new StringBuilder();
        int streamingThreshold = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraMailDiskStreamingThreshold));
        for (int i = 0; i < streamingThreshold; i++) {
            char c = 'a';
            c += (i % 26);
            buf.append(c);
            if (i % 70 == 0) {
                buf.append("\r\n");
            }
        }
        String subject = NAME_PREFIX + " testMessageCache";
        String content = TestUtil.getTestMessage(subject, buf.toString(), USER_NAME, null); 
        String[] recipients = new String[] {
            TestUtil.getAddress("user1"),
            TestUtil.getAddress("user2"),
            TestUtil.getAddress("user3"),
            TestUtil.getAddress("user4")
        };
        TestUtil.addMessageLmtp(recipients, TestUtil.getAddress(USER_NAME), content);
        
        // Start 4 threads that read the message.
        MessageCache.clear();
        ReaderThread[] threads = new ReaderThread[4];
        for (int i = 0; i < 4; i++) {
            threads[i] = new ReaderThread("user" + (i + 1), subject);
        }
        for (int i = 0; i < 4; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 4; i++) {
            threads[i].join();
            assertTrue(threads[i].wasSuccessful());
        }
        
        // Check cache.
        assertEquals(1, MessageCache.getNumMessages());
        assertEquals(MessageCache.STREAMED_MESSAGE_SIZE, MessageCache.getNumBytes());
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        for (int i = 1; i <= 4; i++) {
            TestUtil.deleteTestData("user" + i, NAME_PREFIX);
        }
    }
}
