package com.zimbra.qa.unittest;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.index.LiquidHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.LiquidPerf;
import com.zimbra.cs.util.LiquidLog;

/**
 * @author bburtin
 */
public class TestTags extends TestCase
{
    private Connection mConn;
    private Mailbox mMbox;
    private Account mAccount;
    
    private static String TAG_PREFIX = "TestTags";
    
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
        LiquidLog.test.debug("TestTags.setUp()");
        super.setUp();

        mAccount = TestUtil.getAccount("user1");
        mMbox = Mailbox.getMailboxByAccount(mAccount);
        mConn = DbPool.getConnection();
        
        // Clean up, in case the last test didn't exit cleanly
        cleanUp();
        
        mMessage1 = TestUtil.insertMessage(mMbox, 1, "Test tags");
        mMessage2 = TestUtil.insertMessage(mMbox, 2, "Test tags");
        mMessage3 = TestUtil.insertMessage(mMbox, 3, "Test tags");
        mMessage4 = TestUtil.insertMessage(mMbox, 4, "Test tags");
        
        mConv = mMbox.getConversationById(mMessage1.getConversationId());
        refresh();
    }

    public void testManyTags()
    throws Exception {
        LiquidLog.test.debug("testManyTags()");
        
        int numPrepares = LiquidPerf.getPrepareCount();
        
        // Create the maximum number of tags, based on the number that already exist
        // in the mailbox
        int numTags = MailItem.MAX_TAG_COUNT - mMbox.getTagList().size();
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
        
        numPrepares = LiquidPerf.getPrepareCount() - numPrepares;
        LiquidLog.test.debug("testManyTags generated " + numPrepares + " SQL statements.");
    }
    
    public void testTagSearch()
    throws Exception {
        LiquidLog.test.debug("testTagSearch()");

        // Create tags
        mTags = new Tag[3];
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
        
        // tag:TestTags1 tag:TestTags2 -> (M1,M2,M3)
        ids = search("tag:" + mTags[0].getName() + " tag:" + mTags[1].getName(), MailItem.TYPE_MESSAGE);
        assertEquals("2: result size", 3, ids.size());
        assertTrue("2: no message 1", ids.contains(new Integer(mMessage1.getId())));
        assertTrue("2: no message 2", ids.contains(new Integer(mMessage2.getId())));
        assertTrue("2: no message 3", ids.contains(new Integer(mMessage3.getId())));
        
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
    }
    
    private Set search(String query, byte type)
    throws Exception {
        LiquidLog.test.debug("Running search: '" + query + "', type=" + type);
        byte[] types = new byte[1];
        types[0] = type;

        Set ids = new HashSet();
        ZimbraQueryResults r = mMbox.search(query, types, MailboxIndex.SEARCH_ORDER_DATE_DESC);
        while (r.hasNext()) {
            LiquidHit hit = r.getNext();
            ids.add(new Integer(hit.getItemId()));
        }
        return ids;
    }
    
    private void refresh()
    throws Exception {
        if (mMessage1 != null) {
            mMessage1 = mMbox.getMessageById(mMessage1.getId());
        }
        if (mMessage2 != null) {
            mMessage2 = mMbox.getMessageById(mMessage2.getId());
        }
        if (mMessage3 != null) {
            mMessage3 = mMbox.getMessageById(mMessage3.getId());
        }
        if (mMessage4 != null) {
            mMessage4 = mMbox.getMessageById(mMessage4.getId());
        }
        if (mConv != null) {
            mConv = mMbox.getConversationById(mConv.getId());
        }
        for (int i = 0; i < mTags.length; i++) {
            mTags[i] = mMbox.getTagById(mTags[i].getId());
        }
    }
    
    protected void tearDown() throws Exception {
        LiquidLog.test.debug("TestTags.tearDown()");

        cleanUp();
        
        DbPool.quietClose(mConn);
        super.tearDown();
    }

    private void cleanUp()
    throws Exception {
        if (mMessage1 != null) {
            mMbox.delete(null, mMessage1.getId(), MailItem.TYPE_MESSAGE);
        }
        if (mMessage2 != null) {
            mMbox.delete(null, mMessage2.getId(), MailItem.TYPE_MESSAGE);
        }
        if (mMessage3 != null) {
            mMbox.delete(null, mMessage3.getId(), MailItem.TYPE_MESSAGE);
        }
        if (mMessage4 != null) {
            mMbox.delete(null, mMessage4.getId(), MailItem.TYPE_MESSAGE);
        }

        List tagList = mMbox.getTagList();
        if (tagList == null) {
            return;
        }
        Iterator i = tagList.iterator();
        while (i.hasNext()) {
            Tag tag = (Tag)i.next();
            if (tag.getName().startsWith(TAG_PREFIX)) {
                mMbox.delete(null, tag.getId(), tag.getType());
            }
        }
    }
    
}
