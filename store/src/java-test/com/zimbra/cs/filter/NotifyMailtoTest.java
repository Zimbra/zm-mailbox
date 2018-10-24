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
import org.junit.Ignore;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.Header;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
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
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class NotifyMailtoTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        MockProvisioning prov = new MockProvisioning();
        Provisioning.setInstance(prov);

        Map<String, Object> attrs = Maps.newHashMap();
        attrs = Maps.newHashMap();

        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraSieveNotifyActionRFCCompliant, "TRUE");
        prov.createAccount("test1@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraSieveNotifyActionRFCCompliant, "TRUE");
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraSieveNotifyActionRFCCompliant, "TRUE");
        prov.createAccount("\"tes\\\\t2\"@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraSieveNotifyActionRFCCompliant, "TRUE");
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
          + ";\n"
          + "if anyof (true) { \n"
          + "  notify :message \"${subject}\"\n"
          + "    :from \"test1@zimbra.com\"\n"
          + "    :importance \"3\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&X-HEADER1=value1&x-header2=value2&x-HeAdEr3=value3&x-hEader4=value4A&x-heAder4=value4B&From=notifyfrom@example.com&body=${contents}\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_NoBodyParameter =
            "require [\"enotify\", \"variables\"];\n"
          + "set \"subject\" \"おしらせ\";\n"
          + "set \"contents\" text:\r\n"
          + "新しいメールが届きました。\n"
          + "You've got a mail.\n"
          + "Chao!\r\n"
          + ".\r\n"
          + ";\n"
          + "# No body= parameter\n"
          + "if anyof (true) { \n"
          + "  notify  :message \"${subject}\"\n"
          + "    :from \"test1@zimbra.com\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_EmptyBodyParameter =
            "require [\"enotify\", \"variables\"];\n"
          + "set \"subject\" \"おしらせ\";\n"
          + "set \"contents\" text:\r\n"
          + "新しいメールが届きました。\n"
          + "You've got a mail.\n"
          + "Chao!\r\n"
          + ".\r\n"
          + ";\n"
          + "# No body= parameter\n"
          + "if anyof (true) { \n"
          + "  notify  :message \"${subject}\"\n"
          + "    :from \"test1@zimbra.com\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&body=\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_NoFrom =
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
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&From=notifyfrom@example.com&body=${contents}\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_InvalidFrom =
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
          + "    :from \"test1...@test2@%*zimbra.com.\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&From=notifyfrom@example.com&body=${contents}\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_NoMessageNoSubjectInURL =
            "require [\"enotify\", \"variables\"];\n"
          + "set \"contents\" text:\r\n"
          + "新しいメールが届きました。\n"
          + "You've got a mail.\n"
          + "Chao!\r\n"
          + ".\r\n"
          + ";\n"
          + "if anyof (true) { \n"
          + "  notify :from \"test1@zimbra.com\"\n"
          + "    :importance \"3\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&From=notifyfrom@example.com&body=${contents}\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_NoMessage =
            "require [\"enotify\", \"variables\"];\n"
          + "set \"subject\" \"じゅげむ　じゅげむ　ごこうのすりきれ　かいじゃりすいぎょの　すいぎょうまつ　うんらいまつ　ふうらいまつ　くうねるところにすむところ　やぶらこうじのぶらこうじ　ぱいぽ　ぱいぽ　ぱいぽのしゅーりんがん　しゅーりんがんのぐーりんだい　ぐーりんだいのぽんぽこぴーの　ぽんぽこなーの　ちょうきゅうめいのちょうすけ\";\n"
          + "set \"contents\" text:\r\n"
          + "新しいメールが届きました。\n"
          + "You've got a mail.\n"
          + "Chao!\r\n"
          + ".\r\n"
          + ";\n"
          + "if anyof (true) { \n"
          + "  notify :from \"test1@zimbra.com\"\n"
          + "    :importance \"3\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority=1&From=notifyfrom@example.com&body=${contents}&Subject=${subject}\";"
          + "  keep;\n"
          + "}\n";

    String filterScript_MsgIDDate =
            "require [\"enotify\"];\n"
          + "notify :message \"NotifyMessage\" "
          + "  :from \"test1@zimbra.com\" "
          + "  \"mailto:test2@zimbra.com?body=notifybody&message-id=dummymessageid&date=dummydate\";\n";

    String filterScriptWithSpaceInHeaderName =
            "require [\"enotify\", \"variables\"];\n"
          + "set \"subject\" \"おしらせ\";\n"
          + "set \"contents\" text:\r\n"
          + "新しいメールが届きました。\n"
          + "You've got a mail.\n"
          + "Chao!\r\n"
          + ".\r\n"
          + ";\n"
          + "if anyof (true) { \n"
          + "  notify :message \"${subject}\"\n"
          + "    :from \"test1@zimbra.com\"\n"
          + "    :importance \"3\"\n"
          + "    \"mailto:test2@zimbra.com?to=test3@zimbra.com&Importance=High&X-Priority =1&From=notifyfrom@example.com&body=${contents}\";"
          + "  keep;\n"
          + "}\n";

    /**
     * Tests 'notify' filter rule:
     *  - Set :message (Subject field), :from (From field) and mechanism (mailto:...)
     *  - Body of the notification message contains non-ascii characters
     *  - Additional header fields are specified via mechanism (maito:) parameter
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

            boolean header1 = false;
            boolean header2 = false;
            boolean header3 = false;
            boolean header4 = false;
            for (Enumeration<Header> e = notifyMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                if ("X-HEADER1".equals(temp.getName())) {
                    header1 = true;
                }
                if ("X-header2".equals(temp.getName())) {
                    header2 = true;
                }
                if ("X-HeAdEr3".equals(temp.getName())) {
                    header3 = true;
                }
                if ("X-hEader4".equals(temp.getName())) {
                    header4 = true;
                }
            }
            Assert.assertTrue(header1);
            Assert.assertTrue(header2);
            Assert.assertTrue(header3);
            Assert.assertTrue(header4);

            notifyMsg = mbox3.getMessageById(null, item);
            Assert.assertEquals("おしらせ", notifyMsg.getSubject());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testPercentEncodingVariable_WithEncodeurl() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n";

        String filterScript_PercentEncodingVariable =
                "require [\"enotify\", \"variables\"];\n"
              + "set :encodeurl \"body_param\" \"Safe body&evil=evilbody\";\n"
              + "notify \"mailto:test2@zimbra.com?body=${body_param}\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

            acct1.setMail("test1@zimbra.com");
            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript_PercentEncodingVariable);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto and to= addresses
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            Assert.assertEquals("Safe body&evil=evilbody", notifyMsg.getFragment());

            String[] headers = notifyMsg.getMimeMessage().getHeader("evil");
            Assert.assertFalse(notifyMsg.getSender() == null);

            RuleManager.clearCachedRules(acct1);

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testEmptyMessageBody() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: test1@zimbra.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<xyz@example.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript_EmptyBodyParameter);
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

            Assert.assertEquals("", notifyMsg.getFragment());
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

    @Test
    public void testNoMessageNoSubjectInURL() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: test1@zimbra.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<xyz@example.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript_NoMessageNoSubjectInURL);
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
            Assert.assertEquals("[acme-users] [fwd] version 1.0 is out", notifyMsg.getSubject());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNoMessage() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: test1@zimbra.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<xyz@example.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript_NoMessage);
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
            Assert.assertEquals("じゅげむ　じゅげむ　ごこうのすりきれ　かいじゃりすいぎょの　すいぎょうまつ　うんらいまつ　ふうらいまつ　くうねるところにすむところ　やぶらこうじのぶらこうじ　ぱいぽ　ぱいぽ　ぱいぽのしゅーりんがん　しゅーりんがんのぐーりんだい　ぐーりんだいのぽんぽこぴーの　ぽんぽこなーの　ちょうきゅうめいのちょうすけ", notifyMsg.getSubject());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testPercentEncodingVariable_WithoutEncodeurl() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n";

        String filterScript_WithoutPercentEncodingVariable =
                "require [\"enotify\", \"variables\"];\n"
              + "set \"body_param\" \"Safe body&evil=evilbody\";\n"
              + "notify \"mailto:test2@zimbra.com?body=${body_param}\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

            acct1.setMail("test1@zimbra.com");
            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript_WithoutPercentEncodingVariable);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto and to= addresses
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            Assert.assertEquals("Safe body", notifyMsg.getFragment());

            String[] headers = notifyMsg.getMimeMessage().getHeader("evil");
            Assert.assertTrue(headers.length == 1);
            Assert.assertEquals("evilbody", headers[0]);

            RuleManager.clearCachedRules(acct1);

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testPercentEncodingVariable_WithoutEncodeurl_BadEncodedString() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n";

        String filterScript_WithoutPercentEncodingVariable =
                "require [\"enotify\", \"variables\"];\n"
              + "set \"body_param\" \"Bad%body&evil=evilbody\";\n"
              + "notify \"mailto:test2@zimbra.com?body=${body_param}\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

            acct1.setMail("test1@zimbra.com");
            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript_WithoutPercentEncodingVariable);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto and to= addresses
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            // Since the message body was originally incorrectly percent-encoded,
            // URLDecoder is failed to decode the message contents. Keep using the
            // original message string.
            Assert.assertEquals("Bad%body", notifyMsg.getFragment());

            String[] headers = notifyMsg.getMimeMessage().getHeader("evil");
            Assert.assertTrue(headers.length == 1);
            Assert.assertEquals("evilbody", headers[0]);

            RuleManager.clearCachedRules(acct1);

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNoMessageBody() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: test1@zimbra.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<xyz@example.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript_NoBodyParameter);
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

            Assert.assertEquals("", notifyMsg.getFragment());
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

    @Test
    public void testFromTag_noFromTag() {
        fromTag(filterScript_NoFrom);
    }

    @Test
    public void testFromTag_invalidFromTag() {
        fromTag(filterScript_InvalidFrom);
    }

    /**
     * == (triggering message) ==
     * LMTP; MAIL FROM: &lt;xyz@example.com&gt;
     * LMTP; RCPT TO: &lt;test1@zimbra.com&gt;
     * LMTP; DATA
     * From: xyz@example.com
     * to: test1@zimbra.com
     *
     * == (generated notification message) ==
     * SMTP; MAIL FROM: &lt;test1@zimbra.com&gt;
     * SMTP; RCPT TO: &lt;test2@zimbra.com&gt;
     * SMTP; DATA
     * From: test1@zimbra.com
     * To: test2@zimbra.com
     * Subject: おしらせ
     *
     * 新しいメールが届きました。
     * You've got a mail.
     * Chao!
     */
    private void fromTag(String script) {
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

            acct1.setMail("test1@zimbra.com");
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<xyz@example.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(script);
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

    /**
     * Verify the "header fields of the notification message
     * that are normally related to an individual new message
     * (such as "Message-ID" and "Date") are generated for the
     * notification message in the normal manner, and MUST NOT
     * be copied from the triggering message" (RFC 5436-2.7)
     */
    @Test
    public void testNotOverridableParams() {
        String sampleMsg = "from: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: test1@zimbra.com\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
            Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            Mailbox mbox3 = MailboxManager.getInstance().getMailboxByAccount(acct3);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<xyz@example.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript_MsgIDDate);
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

            Assert.assertEquals("notifybody", notifyMsg.getFragment());
            String[] headers = notifyMsg.getMimeMessage().getHeader("Message-ID");
            Assert.assertTrue(headers.length == 1);
            Assert.assertNotSame("dummymessageid", headers[0]);

            headers = notifyMsg.getMimeMessage().getHeader("Date");
            Assert.assertTrue(headers.length == 1);
            Assert.assertNotSame("dummydate", headers[0]);
        } catch (Exception e) {
            fail("No exception should be thrown:" + e);
        }
    }

    /**
     * Tests the 'valid_notify_method' test: verify that the given parameters
     * do not have any syntax error.
     */
    @Test
    public void testValidNotifyMethod() {
        // The sample filter script should be matched since the the first argument of the
        // 'valid_notify_method' test is NOT correctly formatted (no parameter after "mailto:").
        String filterScript =
                "require \"enotify\";\n"
              + "require \"tag\";\n"
              + "if not valid_notify_method [\"mailto:\", \n"
              + "         \"http://gw.example.net/notify?test\"] {\n"
              + "  tag \"valid_notify_method\";\n"
              + "}";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setMailSieveScript(filterScript);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage("From: test1@zimbra.com".getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            Assert.assertEquals(1, ids.size());
            Message msg = mbox1.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("valid_notify_method", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     * Tests the notify_method_capability ("online"/"maybe")
     */
    @Test
    public void testNotifyMethodCapability_OnlineMaybe() {
        String filterScript =
                "require [\"enotify\", \"tag\"];\n"
              + "if notify_method_capability\n"
              + "     \"mailto:test2@zimbra.com\"\n"
              + "     \"Online\"\n"
              + "     \"maybe\" { \n"
              + "  tag \"notify_method_capability\";\n"
              + "}";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setMailSieveScript(filterScript);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage("From: test1@zimbra.com".getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // ZCS implements the RFC 5436 so that it returns true when 'notify_method_capability'
            // checkes whether the "Online" status is "maybe".
            Assert.assertEquals(1, ids.size());
            Message msg = mbox1.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("notify_method_capability", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     * Tests the notify_method_capability ("online"/"yes")
     * The mailto method returns true for only "maybe"; return false for other values.
     */
    @Test
    public void testNotifyMethodCapability_OnlineYes() {
        String filterScript =
                "require [\"enotify\", \"tag\"];\n"
              + "if notify_method_capability\n"
              + "     \"mailto:test2@zimbra.com\"\n"
              + "     \"Online\"\n"
              + "     [\"YES\"] { \n"
              + "  tag \"notify_method_capability\";\n"
              + "}";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setMailSieveScript(filterScript);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage("From: test1@zimbra.com".getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // ZCS implements the RFC 5436 so that it returns true when 'notify_method_capability'
            // checks whether the "Online" status is "maybe". Otherwise it returns false.
            Assert.assertEquals(1, ids.size());
            Message msg = mbox1.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     * Tests the notify_method_capability with relational extension
     */
    @Test
    public void testNotifyMethodCapability_Relational() {
        String filterScript =
                "require [\"enotify\", \"tag\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
              + "if notify_method_capability :count \"eq\"\n"
              + "     \"mailto:test2@zimbra.com\"\n"
              + "     \"Online\"\n"
              + "     \"1\" { \n"
              + "  tag \"notify_method_capability_eq_1\";\n"
              + "}";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setMailSieveScript(filterScript);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage("From: test1@zimbra.com".getBytes(), false), 0,
                    acct1.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // ZCS implements the RFC 5436 so that it returns true when 'notify_method_capability'
            // checkes whether the "Online" status is "maybe".
            Assert.assertEquals(1, ids.size());
            Message msg = mbox1.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("notify_method_capability_eq_1", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     * Tests a sieve rule with variable parameters.
     */
    @Test
    public void testNotify_variable() {
        String filterScript =
                "require [\"enotify\", \"tag\", \"variables\", \"envelope\"];\n"
              + "if envelope :matches [\"To\"]     \"*\" {set \"rcptto\"        \"${1}\";}\n"
              + "if envelope :matches [\"From\"]   \"*\" {set \"mailfrom\"      \"${1}\";}\n"
              + "if header   :matches  \"Date\"    \"*\" {set \"dateheader\"    \"${1}\";}\n"
              + "if header   :matches  \"From\"    \"*\" {set \"fromheader\"    \"${1}\";}\n"
              + "if header   :matches  \"Subject\" \"*\" {set \"subjectheader\" \"${1}\";}\n"
              + "if header   :matches  \"X-Header-With-Control-Chars\" \"*\" {set \"xheader\" \"${1}\";}\n"
              + "if anyof(not envelope :is [\"From\"] \"\") {\n"
              + "  set \"subjectparam\" \"Notification\";\n"
              + "  set \"bodyparam\" text:\r\n"
              + "Hello ${rcptto},\n"
              + "A new massage has arrived.\n"
              + "Sent: ${dateheader}\n"
              + "From: ${fromheader}\n"
              + "Subject: ${subjectheader}\r\n"
              + "X-Header-With-Control-Chars: ${xheader}\r\n"
              + ".\r\n"
              + ";\n"
              + "  notify :message \"${subjectparam}\"\n"
              + "         :from \"${rcptto}\"\n"
              + "         \"mailto:test2@zimbra.com?body=${bodyparam}\";\n"
              + "}";

        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Date: Tue, 11 Oct 2016 12:01:37 +0900\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n"
                + "X-Header-With-Control-Chars: =?utf-8?B?dGVzdCBIVAkgVlQLIEVUWAMgQkVMByBCUwggbnVsbAAgYWZ0ZXIgbnVsbA0K?=\n";

        String expectedNotifyMsg = "Hello test1@zimbra.com,\r\n"
                + "A new massage has arrived.\r\n"
                + "Sent: Tue, 11 Oct 2016 12:01:37  0900\r\n"
                + "From: xyz@example.com\r\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\r\n"
                + "X-Header-With-Control-Chars: test HT\t VT\u000b ETX\u0003 BEL\u0007 BS\u0008 null\u0000 after null";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<test2@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript);
            acct1.setMail("test1@zimbra.com");
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto addresses
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            // Verify the subject line of the notification message
            Assert.assertEquals("Notification", notifyMsg.getSubject());

            // Verify the from header of the notification message
            String[] headers = notifyMsg.getMimeMessage().getHeader("from");
            Assert.assertTrue(headers.length == 1);
            Assert.assertEquals("test1@zimbra.com", headers[0]);

            // Verify the message body of the notification message
            MimeMessage mm = notifyMsg.getMimeMessage();
            List<MPartInfo> parts = Mime.getParts(mm);
            Set<MPartInfo> bodies = Mime.getBody(parts, false);
            Assert.assertEquals(1, bodies.size());
            for (MPartInfo body : bodies) {
                Object mimeContent = body.getMimePart().getContent();
                Assert.assertTrue(mimeContent instanceof String);
                String deliveredNotifyMsg = (String) mimeContent;
                Assert.assertEquals(expectedNotifyMsg, deliveredNotifyMsg);
            }
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testBackslashEscapeSequence() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                           + "from: xyz@example.com\n"
                           + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                           + "to: foo@example.com, baz@example.com\n"
                           + "cc: qux@example.com\n";

        String filterScript = "require [\"enotify\"];\n"
                               + "notify :from \"\\\"tes\\\\\\\\t1\\\"@zimbra.com\""
                               + ":message \"sample me\\\\ssa\\\"ge4\" "
                               + "\"mailto:\\\"tes\\\\\\\\t2\\\"@zimbra.com?body=sample_message\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name,
                "\"tes\\\\t2\"@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

            acct1.setMail("test1@zimbra.com");
            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox1),
                mbox1, new ParsedMessage(sampleMsg.getBytes(), false), 0, acct1.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

            Assert.assertEquals(1, ids.size());

            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            Assert.assertEquals("sample me\\ssa\"ge4", notifyMsg.getSubject());
            Assert.assertEquals("<\"tes\\\\t1\"@zimbra.com>", notifyMsg.getSender());

            RuleManager.clearCachedRules(acct1);

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testQuoteEscapeSequence() {
        String sampleMsg = "Auto-Submitted: \"no\"\n"
                           + "from: xyz@example.com\n"
                           + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                           + "to: foo@example.com, baz@example.com\n"
                           + "cc: qux@example.com\n";

        String filterScript = "require [\"enotify\"];\n"
                               + "notify :from \"\\\"tes\\\\\\\"t1\\\"@zimbra.com\""
                               + ":message \"sample me\\\\ssa\\\"ge4\" "
                               + "\"mailto:\\\"tes\\\\\\\\t2\\\"@zimbra.com?body=sample_message\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name,
                "\"tes\\\\t2\"@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

            acct1.setMail("test1@zimbra.com");
            RuleManager.clearCachedRules(acct1);

            acct1.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox1),
                mbox1, new ParsedMessage(sampleMsg.getBytes(), false), 0, acct1.getName(),
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);

            Assert.assertEquals(1, ids.size());

            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            Assert.assertEquals("sample me\\ssa\"ge4", notifyMsg.getSubject());
            Assert.assertEquals("<\"tes\\\"t1\"@zimbra.com>", notifyMsg.getSender());

            RuleManager.clearCachedRules(acct1);

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNotifyMailtoWithSpaceInHeaderName() {
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

            acct1.setMailSieveScript(filterScriptWithSpaceInHeaderName);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto and to= addresses
            Assert.assertTrue(mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX).isEmpty());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     * Tests a sieve rule with mime variable parameters.
     */
    @Test
    public void testNotify_mimeVariables() {
        String filterScript =
                "require [\"enotify\", \"tag\", \"variables\", \"envelope\"];\n"
              + "if envelope :matches [\"To\"]     \"*\" {set \"rcptto\"        \"${1}\";}\n"
              + "if envelope :matches [\"From\"]   \"*\" {set \"mailfrom\"      \"${1}\";}\n"
              + "if anyof(not envelope :is [\"From\"] \"\") {\n"
              + "  set \"subjectparam\" \"Notification\";\n"
              + "  set \"bodyparam\" text:\r\n"
              + "Hello ${rcptto},\n"
              + "A new massage has arrived.\n"
              + "Sent: ${Date}\n"
              + "From: ${From}\n"
              + "Subject: ${Subject}\r\n"
              + ".\r\n"
              + ";\n"
              + "  notify :message \"${subjectparam}\"\n"
              + "         :from \"${rcptto}\"\n"
              + "         \"mailto:test2@zimbra.com?body=${bodyparam}\";\n"
              + "}";

        String sampleMsg = "Auto-Submitted: \"no\"\n"
                + "from: xyz@example.com\n"
                + "Date: Tue, 11 Oct 2016 12:01:37 +0900\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out\n"
                + "to: foo@example.com, baz@example.com\n"
                + "cc: qux@example.com\n";

        String expectedNotifyMsg = "Hello test1@zimbra.com,\n"
                + "A new massage has arrived.\n"
                + "Sent: Tue, 11 Oct 2016 12:01:37  0900\n"
                + "From: xyz@example.com\n"
                + "Subject: [acme-users] [fwd] version 1.0 is out";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<test2@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript);
            acct1.setMail("test1@zimbra.com");
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    acct1.getName(), env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            // The triggered message should be delivered to the target mailbox
            Assert.assertEquals(1, ids.size());

            // Notification message should be delivered to mailto addresses
            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            // Verify the subject line of the notification message
            Assert.assertEquals("Notification", notifyMsg.getSubject());

            // Verify the from header of the notification message
            String[] headers = notifyMsg.getMimeMessage().getHeader("from");
            Assert.assertTrue(headers.length == 1);
            Assert.assertEquals("test1@zimbra.com", headers[0]);

            // Verify the message body of the notification message
            MimeMessage mm = notifyMsg.getMimeMessage();
            List<MPartInfo> parts = Mime.getParts(mm);
            Set<MPartInfo> bodies = Mime.getBody(parts, false);
            Assert.assertEquals(1, bodies.size());
            for (MPartInfo body : bodies) {
                Object mimeContent = body.getMimePart().getContent();
                Assert.assertTrue(mimeContent instanceof String);
                String deliveredNotifyMsg = (String) mimeContent;
                Assert.assertEquals(expectedNotifyMsg, deliveredNotifyMsg);
            }
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
}
