/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

public class ConversationTest {
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

        // root message in Inbox
        int msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        // two replies in Trash
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_TRASH).setConversationId(-msgId);
        mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject"), dopt, null);
        Message msg3 = mbox.addMessage(null, MailboxTestUtil.generateMessage("Fwd: test subject"), dopt, null);

        // make sure they're all grouped in a single conversation
        int convId = msg3.getConversationId();
        Assert.assertEquals("3 messages in conv", 3, mbox.getConversationById(null, convId).getSize());

        // empty trash and make sure we're down to 1 message in the conversation
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, true);
        Assert.assertEquals("1 message remaining in conv (cache)", 1, mbox.getConversationById(null, convId).getSize());

        // clear the cache and make sure the counts are correct in the DB as well
        mbox.purge(MailItem.Type.CONVERSATION);
        Assert.assertEquals("1 message remaining in conv (DB)", 1, mbox.getConversationById(null, convId).getSize());
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
