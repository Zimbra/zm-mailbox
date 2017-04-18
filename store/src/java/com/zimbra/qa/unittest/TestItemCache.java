/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * @author bburtin
 */
public class TestItemCache {

    @Rule
    public TestName testInfo = new TestName();

    private Mailbox mMbox;
    private Account mAccount;
    private static String USER_NAME;
    private String PREFIX;

    @Before
    public void setUp() throws Exception {
        PREFIX = String.format("%s-%s-", this.getClass().getName(), testInfo.getMethodName()).toLowerCase();
        USER_NAME = String.format("%s-%s", PREFIX, "user1");
        tearDown();
        mAccount = TestUtil.createAccount(USER_NAME);
        mMbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccount(USER_NAME);
    }

    /**
     * Re-gets the same message 10 times and makes sure we don't hit the database.
     */
    @Test
    public void cacheHit() throws Exception {
        ZimbraLog.test.debug("Starting %s", testInfo.getMethodName());

        TestUtil.addMessage(mMbox, String.format("%s-%s", PREFIX, "missive in inbox"));
        TestUtil.addMessage(mMbox, String.format("%s-%s", PREFIX, "2nd missive in inbox"));
        List<MailItem> messages = mMbox.getItemList(null, MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);
        assertNotNull("List returned by mMbox.getItemList", messages);
        assertEquals("Expected number of messages in the inbox", 2, messages.size());
        Message msg = (Message) messages.get(0);
        mMbox.getItemById(null, msg.getId(), msg.getType());

        int prepareCount = ZimbraPerf.getPrepareCount();
        for (int i = 1; i <= 10; i++) {
            mMbox.getItemById(null, msg.getId(), msg.getType());
        }

        prepareCount = ZimbraPerf.getPrepareCount() - prepareCount;
        assertEquals("Detected unexpected SQL statements.", 0, prepareCount);
    }
}
