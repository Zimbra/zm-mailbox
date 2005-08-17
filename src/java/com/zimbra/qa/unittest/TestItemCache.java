package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.util.LiquidPerf;
import com.zimbra.cs.util.LiquidLog;

/**
 * @author bburtin
 */
public class TestItemCache extends TestCase
{
    private Mailbox mMbox;
    private Account mAccount;
    
    protected void setUp()
    throws Exception {
        LiquidLog.test.debug("TestTags.setUp()");
        super.setUp();

        mAccount = TestUtil.getAccount("user1");
        mMbox = Mailbox.getMailboxByAccount(mAccount);
    }

    /**
     * Re-gets the same message 10 times and makes sure we don't hit the database.
     */
    public void testCacheHit()
    throws Exception {
        LiquidLog.test.debug("testCacheHit");

        List messages = mMbox.getItemList(MailItem.TYPE_MESSAGE);
        assertTrue("No messages found", messages.size() > 0);
        Message msg = (Message) messages.get(0);
        
        int prepareCount = LiquidPerf.getPrepareCount();
        for (int i = 1; i <= 10; i++) {
            mMbox.getItemById(msg.getId(), msg.getType());
        }
        
        prepareCount = LiquidPerf.getPrepareCount() - prepareCount;
        assertEquals("testRefresh() generated " + prepareCount + " SQL statements.",
            0, prepareCount);
    }
}
