/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
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

public class NotifyMailtoTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailSieveNotifyActionRFCCompliant, "TRUE");
        prov.getConfig().modify(attrs);

        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test1@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test3@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    String filterScript1 =
            "require [\"enotify\", \"variables\"];\n"
          + "set \"subject\" \"おしらせ\";\n"
          + "set \"contents\" text:\r\n"
          + "新しいメールが届きました。\n"
          + "You've got a mail.\n"
          + "Chao!\r\n"
          + ".\r\n"
          + ";"
          + "if anyof (true) { \n"
          + "  notify :message \"${subject}\"\n"
          + "    :from \"test1@zimbra.com\"\n"
          + "    :importance \"3\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&From=notifyfrom@example.com&body=${contents}\";"
          + "  keep;\n"
          + "}\n";

    /**
     * Tests the notify action with a typical parameters contains in non-ASCII characters.
     */
    @Test
    public void test() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript1);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, 
                    new ParsedMessage(sampleMsg.getBytes(), false), 0, 
                    acct1.getName(), env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto and to= addresses
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            Assert.assertEquals("新しいメールが届きました。 You've got a mail. Chao!", notifyMsg.getFragment());
            String[] headers = notifyMsg.getMimeMessage().getHeader("Auto-Submitted");
            Assert.assertTrue(headers.length == 1);
            Assert.assertEquals("auto-notified; owner-email=\"test1@zimbra.com\"", headers[0]);

            headers = notifyMsg.getMimeMessage().getHeader("to");
            Assert.assertTrue(headers.length == 1);
            Assert.assertEquals("test2@zimbra.com, test3@zimbra.com", headers[0]);

            headers = notifyMsg.getMimeMessage().getHeader("from");
            Assert.assertFalse(notifyMsg.getSender() == null);
            Assert.assertEquals("test1@zimbra.com", notifyMsg.getSender());

            notifyMsg = mbox3.getMessageById(null, item);
            Assert.assertEquals("おしらせ", notifyMsg.getSubject());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     * Tests that the auto-generated message should NOT generate a notification
     */
    @Test
    public void testLoopDetected() {
        String sampleMsg = "Auto-Submitted: auto-notified; owner-email=\"test2@zimbra.com\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript1);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox (explicit keep)
            Assert.assertEquals(1, ids.size());

            // Notification message should NOT be delivered to mailto and to=
            long size2 = mbox2.getSize();
            Assert.assertEquals(0, size2);
            long size3 = mbox3.getSize();
            Assert.assertEquals(0, size3);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
}
