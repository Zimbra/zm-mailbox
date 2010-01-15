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

import java.util.Date;

import junit.framework.TestCase;

import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;

/**
 * Tests out-of-office notification.  All tests must be run inside the server, because
 * the cleanup code needs to delete notification rows from the database table.
 */
public class TestOutOfOffice
extends TestCase {
    
    private Connection mConn;
    private Mailbox mMbox;
    private String mOriginalFromAddress;
    private String mOriginalFromDisplay;
    private String mOriginalReplyEnabled;
    private String mOriginalFromDate;
    private String mOriginalUntilDate;
    private String mOriginalAllowAnyFrom;
    private String mOriginalReplyToAddress;
    private String mOriginalReplyToDisplay;
    
    private static String NAME_PREFIX = TestOutOfOffice.class.getSimpleName();
    private static String RECIPIENT_NAME = "user1";
    private static String SENDER_NAME = "user2";
    private static String RECIPIENT1_ADDRESS = "TestOutOfOffice1@example.zimbra.com";
    private static String RECIPIENT2_ADDRESS = "TestOutOfOffice2@example.zimbra.com";
    
    protected void setUp() throws Exception {
        super.setUp();
        
        mMbox = TestUtil.getMailbox(RECIPIENT_NAME);
        mConn = DbPool.getConnection();
        
        Account recipient = TestUtil.getAccount(RECIPIENT_NAME);
        mOriginalFromAddress = recipient.getPrefFromAddress();
        mOriginalFromDisplay = recipient.getPrefFromDisplay();
        mOriginalAllowAnyFrom = TestUtil.getAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraAllowAnyFromAddress);
        mOriginalReplyEnabled =
            TestUtil.getAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled);
        mOriginalFromDate = recipient.getPrefOutOfOfficeFromDateAsString();
        mOriginalUntilDate = recipient.getPrefOutOfOfficeUntilDateAsString();
        mOriginalReplyToAddress = recipient.getPrefReplyToAddress();
        mOriginalReplyToDisplay = recipient.getPrefReplyToDisplay();
        
        cleanUp();
}
    
    public void testRowExists() throws Exception {
        long fiveDaysAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 5);

        DbOutOfOffice.setSentTime(mConn, mMbox, RECIPIENT1_ADDRESS, fiveDaysAgo);
        mConn.commit();
        assertFalse("1 day", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 1 * Constants.MILLIS_PER_DAY));
        assertFalse("4 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 4 * Constants.MILLIS_PER_DAY));
        assertFalse("5 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 5 * Constants.MILLIS_PER_DAY));
        assertTrue("6 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 6 * Constants.MILLIS_PER_DAY));
        assertTrue("100 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 100 * Constants.MILLIS_PER_DAY));
    }

    public void testRowDoesntExist() throws Exception {
        assertFalse("1 day", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 1 * Constants.MILLIS_PER_DAY));
        assertFalse("5 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 5 * Constants.MILLIS_PER_DAY));
        assertFalse("100 days", DbOutOfOffice.alreadySent(mConn, mMbox, RECIPIENT1_ADDRESS, 100 * Constants.MILLIS_PER_DAY));
    }
    
    public void testPrune() throws Exception {
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

    /**
     * Confirms that out-of-office notifications use the user's address preference
     * (bug 40869).
     */
    public void testPrefFromAddress()
    throws Exception {
        String newFromAddress = TestUtil.getAddress("testPrefFromAddress");
        String newFromDisplay = NAME_PREFIX + " testPrefFromAddress";
        String newReplyToAddress = TestUtil.getAddress("testReplyToAddress");
        String newReplyToDisplay = NAME_PREFIX + " testReplyToAddress";

        // Set custom from address and turn on out-of-office.
        Account recipient = TestUtil.getAccount(RECIPIENT_NAME);
        recipient.setPrefFromAddress(newFromAddress);
        recipient.setPrefFromDisplay(newFromDisplay);
        long now = System.currentTimeMillis();
        recipient.setPrefOutOfOfficeFromDate(new Date(now));
        recipient.setPrefOutOfOfficeUntilDate(new Date(now + Constants.MILLIS_PER_DAY));
        recipient.setPrefOutOfOfficeReplyEnabled(true);
        recipient.setPrefReplyToAddress(newReplyToAddress);
        recipient.setPrefReplyToDisplay(newReplyToDisplay);
        
        // Test with zimbraAllowAnyFromAddress = FALSE.
        recipient.setAllowAnyFromAddress(false);
        String subject = NAME_PREFIX + " testPrefFromAddress 1";
        ZMailbox senderMbox = TestUtil.getZMailbox(SENDER_NAME);
        TestUtil.sendMessage(senderMbox, RECIPIENT_NAME, subject);
        ZMessage reply = TestUtil.waitForMessage(senderMbox, "in:inbox subject:\"" + subject + "\"");
        
        // Validate addresses.
        ZEmailAddress fromAddress = getAddress(reply, ZEmailAddress.EMAIL_TYPE_FROM);
        assertEquals(recipient.getName(), fromAddress.getAddress());
        assertEquals(recipient.getDisplayName(), fromAddress.getPersonal());
        
        ZEmailAddress replyToAddress = getAddress(reply, ZEmailAddress.EMAIL_TYPE_REPLY_TO);
        assertEquals(newReplyToAddress, replyToAddress.getAddress());
        assertEquals(newReplyToDisplay, replyToAddress.getPersonal());
        
        DbOutOfOffice.clear(mConn, mMbox);
        mConn.commit();
        
        // Test with zimbraAllowAnyFromAddress = TRUE.
        recipient.setAllowAnyFromAddress(true);
        subject = NAME_PREFIX + " testPrefFromAddress 2";
        TestUtil.sendMessage(senderMbox, RECIPIENT_NAME, subject);
        reply = TestUtil.waitForMessage(senderMbox, "in:inbox subject:\"" + subject + "\"");
        ZimbraLog.test.info("Second reply:\n" + TestUtil.getContent(senderMbox, reply.getId()));
        
        // Validate addresses.
        fromAddress = getAddress(reply, ZEmailAddress.EMAIL_TYPE_FROM);
        assertEquals(newFromAddress, fromAddress.getAddress());
        assertEquals(newFromDisplay, fromAddress.getPersonal());
        
        replyToAddress = getAddress(reply, ZEmailAddress.EMAIL_TYPE_REPLY_TO);
        assertEquals(newReplyToAddress, replyToAddress.getAddress());
        assertEquals(newReplyToDisplay, replyToAddress.getPersonal());
    }
    
    private ZEmailAddress getAddress(ZMessage msg, String type) {
        for (ZEmailAddress address : msg.getEmailAddresses()) {
            if (address.getType().equals(type)) {
                return address;
            }
        }
        fail("Could not find address of type " + type + " in message: " + msg.getSubject());
        return null;
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
        DbPool.quietClose(mConn);
        
        Account recipient = TestUtil.getAccount(RECIPIENT_NAME);
        recipient.setPrefFromAddress(mOriginalFromAddress);
        recipient.setPrefFromDisplay(mOriginalFromDisplay);
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraAllowAnyFromAddress, mOriginalAllowAnyFrom);
        
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, mOriginalReplyEnabled);
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraPrefOutOfOfficeFromDate, mOriginalFromDate);
        TestUtil.setAccountAttr(RECIPIENT_NAME, Provisioning.A_zimbraPrefOutOfOfficeUntilDate, mOriginalUntilDate);
        recipient.setPrefReplyToAddress(mOriginalReplyToAddress);
        recipient.setPrefReplyToDisplay(mOriginalReplyToDisplay);
        
        super.tearDown();
    }
    
    private void cleanUp()
    throws Exception {
        DbOutOfOffice.clear(mConn, mMbox);
        mConn.commit();
        TestUtil.deleteTestData(SENDER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(RECIPIENT_NAME, NAME_PREFIX);
    }
}
