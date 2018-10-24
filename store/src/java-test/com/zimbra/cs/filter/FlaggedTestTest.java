/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import java.util.HashMap;
import org.junit.Ignore;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.jsieve.FlaggedTest;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link FlaggedTest}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class FlaggedTestTest {

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
    public void incoming() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if me :in \"To\" { flag \"priority\"; }\n" +
                "if flagged \"priority\" { stop; }\n" +
                "if header :contains \"Subject\" \"test\" { fileinto \"test\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage("From: sender@zimbra.com\nTo: test@zimbra.com\nSubject: test".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertTrue(msg.isTagged(Flag.FlagInfo.PRIORITY));
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void existing() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        OperationContext octx = new OperationContext(mbox);
        Message msg = mbox.addMessage(octx,
                new ParsedMessage("From: sender@zimbra.com\nTo: test@zimbra.com\nSubject: test".getBytes(), false),
                new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_PRIORITY),
                new DeliveryContext());

        boolean filtered = RuleManager.applyRulesToExistingMessage(new OperationContext(mbox), mbox, msg.getId(),
                RuleManager.parse("if flagged \"priority\" { stop; }\n" +
                        "if header :contains \"Subject\" \"test\" { fileinto \"test\"; }"));
        Assert.assertEquals(false, filtered);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(octx, msg.getId()).getFolderId());
    }

}
