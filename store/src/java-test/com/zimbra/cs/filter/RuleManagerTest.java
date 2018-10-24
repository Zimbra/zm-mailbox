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

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link RuleManager}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class RuleManagerTest {

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
    public void tagAndFileinto() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("require \"fileinto\"; if socialcast { tag \"priority\"; fileinto \"socialcast\"; }");
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: test".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("socialcast", mbox.getFolderById(null, msg.getFolderId()).getName());
        Assert.assertEquals("priority", ArrayUtil.getFirstElement(msg.getTags()));

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("require \"fileinto\"; if socialcast { tag \"priority\"; }\n" +
                "if header :contains [\"Subject\"] [\"Zimbra\"] { fileinto \"zimbra\"; }");
        ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: Zimbra".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals("zimbra", mbox.getFolderById(null, msg.getFolderId()).getName());
        Assert.assertEquals("priority", ArrayUtil.getFirstElement(msg.getTags()));
    }

    @Test
    public void tagAndTag() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if socialcast { tag \"priority\"; tag \"socialcast\"; }");
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: test".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertArrayEquals(new String[] { "priority", "socialcast" }, msg.getTags());

        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if socialcast { tag \"priority\"; }\n" +
                "if header :contains [\"Subject\"] [\"Zimbra\"] { tag \"zimbra\"; }");
        ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox, new ParsedMessage(
                "From: do-not-reply@socialcast.com\nReply-To: share@socialcast.com\nSubject: Zimbra".getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertArrayEquals(new String[] { "priority", "zimbra" }, msg.getTags());
    }

    @Test
    public void testGetRuleRequire() throws Exception {
        String requireLine = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\r\n";
        String rule1 = "# filter1\r\n" + "if anyof (header :contains [\"subject\"] \"test\") {\r\n"
            + "fileinto \"test\";\r\n" + "stop;\r\n" + "}\r\n";
        String rule2 = "# filter2\r\n" + "if anyof (header :contains [\"subject\"] \"test\") {\r\n"
            + "tag \"test\";\r\n" + "stop;\r\n" + "}\r\n";
        String script = requireLine + rule1 + rule2;
        Assert.assertEquals(requireLine, RuleManager.getRuleByName(script, "filter1").getFirst());
        Assert.assertEquals(rule1, RuleManager.getRuleByName(script, "filter1").getSecond());
        Assert.assertEquals(requireLine, RuleManager.getRuleByName(script, "filter2").getFirst());
        Assert.assertEquals(rule2, RuleManager.getRuleByName(script, "filter2").getSecond());
    }
}
