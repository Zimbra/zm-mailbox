/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import org.junit.Ignore;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.util.TypedIdList;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ConversationTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void delete() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.beginTrackingSync();
        // root message in Inbox
        int msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        // two replies in Trash
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_TRASH).setConversationId(-msgId);
        mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject"), dopt, null);
        Message msg3 = mbox.addMessage(null, MailboxTestUtil.generateMessage("Fwd: test subject"), dopt, null);

        int modSeq = msg3.getModifiedSequence();

        // make sure they're all grouped in a single conversation
        int convId = msg3.getConversationId();
        Assert.assertEquals("3 messages in conv", 3, mbox.getConversationById(null, convId).getSize());

        // empty trash and make sure we're down to 1 message in the conversation
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, true);
        Assert.assertEquals("1 message remaining in conv (cache)", 1, mbox.getConversationById(null, convId).getSize());

        // clear the cache and make sure the counts are correct in the DB as well
        mbox.purge(MailItem.Type.CONVERSATION);
        Assert.assertEquals("1 message remaining in conv (DB)", 1, mbox.getConversationById(null, convId).getSize());

        TypedIdList list = mbox.getTombstones(modSeq);
        List<Integer> tombstoneConvs = list.getIds(MailItem.Type.CONVERSATION);
        Assert.assertNull("No conv tombstone yet", tombstoneConvs);

        mbox.move(null, msgId, MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_TRASH);
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, true);
        list = mbox.getTombstones(modSeq);
        tombstoneConvs = list.getIds(MailItem.Type.CONVERSATION);
        Assert.assertNotNull("conv tombstone exist", tombstoneConvs);
        Assert.assertEquals("conv tombstone size", 1, tombstoneConvs.size());
        Assert.assertEquals("conv tombstone id", (Integer) convId, tombstoneConvs.get(0));
    }

    @Test
    public void expiry() throws Exception {
        Account account = Provisioning.getInstance().get(AccountBy.id, MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // root message in Inbox
        int msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        // two old replies in Trash
        long old = System.currentTimeMillis() - 3 * Constants.MILLIS_PER_MONTH;
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_TRASH).setConversationId(-msgId);
        mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject").setReceivedDate(old), dopt, null);
        Message msg3 = mbox.addMessage(null, MailboxTestUtil.generateMessage("Fwd: test subject").setReceivedDate(old), dopt, null);

        // make sure they're all grouped in a single conversation
        int convId = msg3.getConversationId();
        Assert.assertEquals("3 messages in conv", 3, mbox.getConversationById(null, convId).getSize());

        // purge old messages and make sure we're down to 1 message in the conversation
        account.setMailTrashLifetime("30d");
        account.setMailPurgeUseChangeDateForTrash(false);
        mbox.purgeMessages(null);
        Assert.assertEquals("empty Trash folder", 0, mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH).getSize());
        Assert.assertEquals("1 message remaining in conv (cache)", 1, mbox.getConversationById(null, convId).getSize());

        // clear the cache and make sure the counts are correct in the DB as well
        mbox.purge(MailItem.Type.CONVERSATION);
        Assert.assertEquals("1 message remaining in conv (DB)", 1, mbox.getConversationById(null, convId).getSize());
    }
}
