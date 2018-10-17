/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016, 2017 Synacor, Inc.
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

import java.util.List;
import org.junit.Ignore;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class RedirectCopyTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test1@zimbra.com", "secret", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("test2@zimbra.com", "secret", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test3@zimbra.com", "secret", attrs);
        Server server = Provisioning.getInstance().getServer(acct);
        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testCopyRedirect() {
        String filterScript = "require [\"copy\", \"redirect\"];\n"
                + "if header :contains \"Subject\" \"Test\" { redirect :copy \"test3@zimbra.com\"; }";
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name,
                "test2@zimbra.com");
            Account account2 = Provisioning.getInstance().get(Key.AccountBy.name,
                "test3@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(account2);
            account.setMailSieveScript(filterScript);
            String raw = "From: test1@zimbra.com\n" + "To: test2@zimbra.com\n" + "Subject: Test\n"
                + "\n" + "Hello World";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(raw.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);
            Assert.assertEquals("Hello World", notifyMsg.getFragment());
            Assert.assertEquals(2, notifyMsg.getFolderId());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("No exception should be thrown");
        }
    }

    @Test
    public void testPlainRedirect() {
        String filterPlainRedirectScript = "require [\"redirect\"];\n"
            + "if header :contains \"Subject\" \"Test\" { redirect \"test3@zimbra.com\"; }";
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name,
                "test2@zimbra.com");
            Account account2 = Provisioning.getInstance().get(Key.AccountBy.name,
                "test3@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(account2);
            account.setMailSieveScript(filterPlainRedirectScript);
            String raw = "From: test1@zimbra.com\n" + "To: test2@zimbra.com\n" + "Subject: Test\n"
                + "\n" + "Hello World";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(raw.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(0, ids.size());
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message msg = mbox2.getMessageById(null, item);
            Assert.assertEquals("Hello World", msg.getFragment());
            Assert.assertEquals(2, msg.getFolderId());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("No exception should be thrown");
        }
    }

    @Test
    public void testCopyRedirectThenFileInto() {
        String filterScriptPattern1 = "require [\"copy\", \"fileinto\"];\n"
            + "redirect :copy \"test3@zimbra.com\";\n" + "fileinto \"Junk\"; ";
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name,
                "test1@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            account.setMailSieveScript(filterScriptPattern1);
            String rawReal = "From: test1@zimbra.com\n" + "To: test2@zimbra.com\n"
                + "Subject: Test\n" + "\n" + "Hello World";
            RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                mbox, new ParsedMessage(rawReal.getBytes(), false), 0, account.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            // message should not be stored in inbox
            Assert.assertNull(
                mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("No exception should be thrown");
        }
    }

    /*
     * Redirect a message whose body text consists of some non-ascii characters,
     * but it does not have a proper Content-Transfer-Encoding header.
     */
    @Test
    public void testPlainRedirectMimeMsg1() {
        String filterScript = "require [\"copy\", \"fileinto\"];\n"
            + "redirect \"test3@zimbra.com\";\n";
        try {
            Account account2 = Provisioning.getInstance().get(Key.AccountBy.name,
                    "test2@zimbra.com");
            Account account3 = Provisioning.getInstance().get(Key.AccountBy.name,
                    "test3@zimbra.com");
            RuleManager.clearCachedRules(account2);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(account2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(account3);
            account2.setMailSieveScript(filterScript);
            String body = StringUtils.leftPad("", 999, "あ");

            String rawReal = "From: test1@zimbra.com\n" + "To: test2@zimbra.com\n"
                + "Subject: Test\n" + "\n" + body;
            RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox2),
                mbox2, new ParsedMessage(rawReal.getBytes("Shift_JIS"), false), 0, account2.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

            // verify the redirected message
            Integer item = mbox3.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message redirectMsg = mbox3.getMessageById(null, item);
            Assert.assertEquals(body.substring(0, 150), redirectMsg.getFragment().substring(0, 150));
            String[] headers = redirectMsg.getMimeMessage().getHeader("Content-Transfer-Encoding");
            Assert.assertNotNull(headers);
            Assert.assertNotSame(0, headers.length);
            Assert.assertEquals("8bit", headers[0]);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("No exception should be thrown");
        }
    }
}
