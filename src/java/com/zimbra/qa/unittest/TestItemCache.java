/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestItemCache extends TestCase {
    private Mailbox mMbox;
    private Account mAccount;

    @Override
    protected void setUp() throws Exception {
        ZimbraLog.test.debug("TestTags.setUp()");
        super.setUp();

        mAccount = TestUtil.getAccount("user1");
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
    }

    /**
     * Re-gets the same message 10 times and makes sure we don't hit the database.
     */
    public void testCacheHit() throws Exception {
        ZimbraLog.test.debug("testCacheHit");

        List<MailItem> messages = mMbox.getItemList(null, MailItem.Type.MESSAGE);
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
}
