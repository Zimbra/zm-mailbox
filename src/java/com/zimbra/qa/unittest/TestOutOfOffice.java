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

import junit.framework.TestCase;

import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * @author bburtin
 */
public class TestOutOfOffice extends TestCase
{
    private Connection mConn;
    private Mailbox mMbox;
    
    private static String USER_NAME = "user1";
    private static String RECIPIENT1_ADDRESS = "TestOutOfOffice1@example.zimbra.com";
    private static String RECIPIENT2_ADDRESS = "TestOutOfOffice2@example.zimbra.com";
    

    protected void setUp() throws Exception
    {
        super.setUp();
        
        Account account = TestUtil.getAccount(USER_NAME);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(account);
        mConn = DbPool.getConnection();
        
        cleanUp();
}
    
    public void testRowExists() throws Exception
    {
        long fiveDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 5);

        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT1_ADDRESS, fiveDaysAgo);
        mConn.commit();
        assertFalse("1 day", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 1 * Constants.MILLIS_PER_DAY));
        assertFalse("4 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 4 * Constants.MILLIS_PER_DAY));
        assertFalse("5 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 5 * Constants.MILLIS_PER_DAY));
        assertTrue("6 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 6 * Constants.MILLIS_PER_DAY));
        assertTrue("100 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 100 * Constants.MILLIS_PER_DAY));
    }

    public void testRowDoesntExist() throws Exception
    {
        assertFalse("1 day", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 1 * Constants.MILLIS_PER_DAY));
        assertFalse("5 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 5 * Constants.MILLIS_PER_DAY));
        assertFalse("100 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 100 * Constants.MILLIS_PER_DAY));
    }
    
    public void testPrune() throws Exception
    {
        long fiveDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 5);
        long sixDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 6);

        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT1_ADDRESS, fiveDaysAgo);
        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT2_ADDRESS, sixDaysAgo);
        mConn.commit();
        
        // Prune the entry for 6 days ago
        DbOutOfOffice.prune(mConn, 6 * Constants.MILLIS_PER_DAY);
        mConn.commit();
        
        // Make sure that the later entry is still there and the earlier one is gone 
        assertTrue("recipient1", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 6 * Constants.MILLIS_PER_DAY));
        assertFalse("recipient2", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT2_ADDRESS, 7 * Constants.MILLIS_PER_DAY));
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
        DbPool.quietClose(mConn);
        
        super.tearDown();
    }
    
    private void cleanUp()
    throws Exception {
        DbOutOfOffice.clear(mConn, mMbox.getAccountId());
        mConn.commit();
    }
}
