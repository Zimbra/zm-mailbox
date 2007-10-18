/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestTags extends TestCase
{
    private Connection mConn;
    private Mailbox mMbox;
    private Account mAccount;
    
    private static String TAG_PREFIX = "TestTags";
    private static String MSG_SUBJECT = "Test tags";
    
    private Message mMessage1;
    private Message mMessage2;
    private Message mMessage3;
    private Message mMessage4;
    private Conversation mConv;
    private Tag[] mTags = new Tag[0];
    
    /**
     * Creates the message used for tag tests 
     */
    protected void setUp()
    throws Exception {
        ZimbraLog.test.debug("TestTags.setUp()");
        super.setUp();

        mAccount = TestUtil.getAccount("user1");
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
        mConn = DbPool.getConnection();
        
        // Clean up, in case the last test didn't exit cleanly
        cleanUp();
        
        mMessage1 = TestUtil.insertMessage(mMbox, 1, MSG_SUBJECT);
        mMessage2 = TestUtil.insertMessage(mMbox, 2, MSG_SUBJECT);
        mMessage3 = TestUtil.insertMessage(mMbox, 3, MSG_SUBJECT);
        mMessage4 = TestUtil.insertMessage(mMbox, 4, MSG_SUBJECT);
        
        mConv = mMbox.getConversationById(null, mMessage1.getConversationId());
        refresh();
    }

    public void testManyTags()
    throws Exception {
        // xxx bburtin: Don't run this test as part of the regular test suite,
        // since it takes almost 20 seconds to run.
        boolean runTest = false;
        if (!runTest) {
            return;
        }
        
        int numPrepares = ZimbraPerf.getPrepareCount();
        
        // Create the maximum number of tags, based on the number that already exist
        // in the mailbox
        int numTags = MailItem.MAX_TAG_COUNT - mMbox.getTagList(null).size();
        assertTrue("Can't create any new tags", numTags != 0);
        
        // Create tags
        mTags = new Tag[numTags];
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.createTag(null, TAG_PREFIX + (i + 1), (byte)0);
        }
        refresh();
        
        // Assign each tag to M1
        for (int i = 0; i < mTags.length; i++) {
            mMbox.alterTag(null, mMessage1.getId(), mMessage1.getType(), mTags[i].getId(), true);
            refresh();
        }
        
        numPrepares = ZimbraPerf.getPrepareCount() - numPrepares;
        ZimbraLog.test.debug("testManyTags generated " + numPrepares + " SQL statements.");
    }
    
    public void testTagSearch()
    throws Exception {
        // Create tags
        mTags = new Tag[4];
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.createTag(null, TAG_PREFIX + (i + 1), (byte)0);
        }
        refresh();

        // First assign T1 to the entire conversation, then remove it from M2-M4
        mMbox.alterTag(null, mConv.getId(), mConv.getType(), mTags[0].getId(), true);
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[0].getId(), false);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[0].getId(), false);
        mMbox.alterTag(null, mMessage4.getId(), mMessage4.getType(), mTags[0].getId(), false);
        
        // Assign tags:
        //   M1: T1
        //   M2: T2
        //   M3: T2, T3
        //   M4: no tags
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), mTags[1].getId(), true);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[1].getId(), true);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), mTags[2].getId(), true);
        refresh();
        
        // tag:TestTags1 -> (M1)
        Set ids = search("tag:" + mTags[0].getName(), MailItem.TYPE_MESSAGE);
        assertEquals("1: result size", 1, ids.size());
        assertTrue("1: no message 1", ids.contains(new Integer(mMessage1.getId())));
        
        // tag:TestTags1 or tag:TestTags2 -> (M1,M2,M3)
        ids = search("tag:" + mTags[0].getName() + " or tag:" + mTags[1].getName(), MailItem.TYPE_MESSAGE);
        assertEquals("2a: result size", 3, ids.size());
        assertTrue("2a: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("2a: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2a: no message 3", ids.contains(new Integer(mMessage3.getId())));
        
        // tag:TestTags2 tag:TestTags3 -> (M3)
        ids = search("tag:" + mTags[1].getName() + " tag:" + mTags[2].getName(), MailItem.TYPE_MESSAGE);
        assertEquals("2b: result size", 1, ids.size());
        assertTrue("2b: no message 3", ids.contains(new Integer(mMessage3.getId())));
        
        // not tag:TestTags1 -> (M2,M3,M4,...)
        ids = search("not tag:" + mTags[0].getName(), MailItem.TYPE_MESSAGE);
        assertFalse("3: message 1 found", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("3: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("3: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("3: no message 4", ids.contains(new Integer(mMessage4.getId())));
        
        // not tag:TestTags2 not tag:TestTags3 -> (M1,M4,...)
        ids = search("not tag:" + mTags[1].getName() + " not tag:" + mTags[2].getName(), MailItem.TYPE_MESSAGE);
        assertTrue("4: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("4: contains message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("4: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("4: no message 4", ids.contains(new Integer(mMessage4.getId())));
        
        // tag:TestTags2 not tag:TestTags3 -> (M2)
        ids = search("tag:" + mTags[1].getName() + " not tag:" + mTags[2].getName(), MailItem.TYPE_MESSAGE);
        assertFalse("5: message 1 found", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("5: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("5: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("5: contains message 4", ids.contains(new Integer(mMessage4.getId())));
        
        // tag:TestTags4 -> ()
        ids = search("tag:" + mTags[3].getName(), MailItem.TYPE_MESSAGE);
        assertEquals("6: search should have returned no results", 0, ids.size());
    }
    
    public void testFlagSearch()
    throws Exception {
        // Look up flags
        Flag replied = mMbox.getFlagById(Flag.ID_FLAG_REPLIED);
        Flag flagged = mMbox.getFlagById(Flag.ID_FLAG_FLAGGED);
        Flag forwarded = mMbox.getFlagById(Flag.ID_FLAG_FORWARDED);

        // First assign T1 to the entire conversation, then remove it from M2-M4
        mMbox.alterTag(null, mConv.getId(), mConv.getType(), replied.getId(), true);
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), replied.getId(), false);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), replied.getId(), false);
        mMbox.alterTag(null, mMessage4.getId(), mMessage4.getType(), replied.getId(), false);
        
        // Assign tags:
        //   M1: replied
        //   M2: flagged
        //   M3: flagged, forwarded
        //   M4: no flags
        mMbox.alterTag(null, mMessage2.getId(), mMessage2.getType(), flagged.getId(), true);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), flagged.getId(), true);
        mMbox.alterTag(null, mMessage3.getId(), mMessage3.getType(), forwarded.getId(), true);
        refresh();
        
        // is:replied -> (M1,...)
        Set ids = search("is:replied", MailItem.TYPE_MESSAGE);
        assertTrue("1: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("1: message 2 found", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("1: message 3 found", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("1: message 4 found", ids.contains(new Integer(mMessage4.getId())));
        
        // is:flagged is:forwarded -> (M3,...)
        ids = search("is:flagged is:forwarded", MailItem.TYPE_MESSAGE);
        assertFalse("2a: message 1 found", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("2a: message 2 found", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2a: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("2a: message 4 found", ids.contains(new Integer(mMessage4.getId())));
        
        // is:replied or is:flagged -> (M2,M3,...)
        ids = search("is:replied or is:flagged", MailItem.TYPE_MESSAGE);
        assertTrue("2b: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("2b: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2b: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("2b: message 4 found", ids.contains(new Integer(mMessage4.getId())));
        
        
        // not is:replied -> (M2,M3,M4,...)
        ids = search("not is:replied", MailItem.TYPE_MESSAGE);
        assertFalse("3: contains message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("3: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("3: no message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("3: no message 4", ids.contains(new Integer(mMessage4.getId())));
        
        // not is:flagged not is:forwarded -> (M1,M4,...)
        ids = search("not is:flagged not is:forwarded", MailItem.TYPE_MESSAGE);
        assertTrue("4: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertFalse("4: contains message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("4: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertTrue("4: no message 4", ids.contains(new Integer(mMessage4.getId())));
        
        // is:flagged not is:forwarded -> (M2)
        ids = search("is:flagged not is:forwarded", MailItem.TYPE_MESSAGE);
        assertFalse("5: contains message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("5: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertFalse("5: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        assertFalse("5: contains message 4", ids.contains(new Integer(mMessage4.getId())));
        
        // tag:\Deleted -> ()
        // Cannot search for tag:\\Deleted with the old-style Mailbox.search(String query...) API
        // need to update test code to use the new API Mailbox.search(SearchParams...) API
        // and specify SearchParams.setIncludeTagDeleted(true) on the params
        //
        //        ids = search("tag:\\Deleted", MailItem.TYPE_MESSAGE);
        //        assertFalse("6: contains message 1", ids.contains(new Integer(mMessage1.getId())));
        //        assertFalse("6: contains message 2", ids.contains(new Integer(mMessage2.getId())));
        //        assertFalse("6: contains message 3", ids.contains(new Integer(mMessage3.getId())));
        //        assertFalse("6: contains message 4", ids.contains(new Integer(mMessage4.getId())));
    }

    public void testSearchUnreadAsTag()
    throws Exception {
        boolean unseenSearchSucceeded = false;
        try {
            search("tag:\\Unseen", MailItem.TYPE_MESSAGE);
            unseenSearchSucceeded = true;
        } catch (ServiceException e) {
            assertEquals("Unexpected exception type", MailServiceException.NO_SUCH_TAG, e.getCode());
        }
        assertFalse("tag:\\Unseen search should not have succeeded", unseenSearchSucceeded);
        
        Set isUnreadIds = search("is:unread", MailItem.TYPE_MESSAGE);
        Set tagUnreadIds = search("tag:\\Unread", MailItem.TYPE_MESSAGE);
        if (!(isUnreadIds.containsAll(tagUnreadIds))) {
            fail("Mismatch in search results.  is:unread returned (" +
                StringUtil.join(",", isUnreadIds) + "), tag:\\Unread returned (" +
                StringUtil.join(",", tagUnreadIds) + ")");
        }
    }
    
    private Set<Integer> search(String query, byte type)
    throws Exception {
        List<Integer> ids = TestUtil.search(mMbox, query, type);
        return new HashSet<Integer>(ids);
    }
    
    private void refresh()
    throws Exception {
        if (mMessage1 != null) {
            mMessage1 = mMbox.getMessageById(null, mMessage1.getId());
        }
        if (mMessage2 != null) {
            mMessage2 = mMbox.getMessageById(null, mMessage2.getId());
        }
        if (mMessage3 != null) {
            mMessage3 = mMbox.getMessageById(null, mMessage3.getId());
        }
        if (mMessage4 != null) {
            mMessage4 = mMbox.getMessageById(null, mMessage4.getId());
        }
        if (mConv != null) {
            mConv = mMbox.getConversationById(null, mConv.getId());
        }
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.getTagById(null, mTags[i].getId());
        }
    }
    
    protected void tearDown() throws Exception {
        ZimbraLog.test.debug("TestTags.tearDown()");

        cleanUp();
        
        DbPool.quietClose(mConn);
        super.tearDown();
    }

    private void cleanUp()
    throws Exception {
        Set messageIds = search("subject:\"Test tags\"", MailItem.TYPE_MESSAGE);
        Iterator i = messageIds.iterator();
        while (i.hasNext()) {
            int id = ((Integer) i.next()).intValue();
            mMbox.delete(null, id, MailItem.TYPE_MESSAGE);
        }

        List tagList = mMbox.getTagList(null);
        if (tagList == null) {
            return;
        }
        
        i = tagList.iterator();
        while (i.hasNext()) {
            Tag tag = (Tag)i.next();
            if (tag.getName().startsWith(TAG_PREFIX)) {
                mMbox.delete(null, tag.getId(), tag.getType());
            }
        }
    }
    
}
