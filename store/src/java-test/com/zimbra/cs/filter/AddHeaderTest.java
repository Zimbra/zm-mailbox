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
import java.util.UUID;

import javax.mail.Header;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
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

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class AddHeaderTest {

    private static String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";
    private static String[] sampleBaseMsg2 = {
            "Return-Path: user1@domain1.zimbra.com",
            "Received: from domain1.zimbra.com (LHLO zcs-ubuntu.local) (192.168.44.131)\r\n"
          + " by zcs-ubuntu.local with LMTP; Wed, 7 Dec 2016 15:10:58 +0900 (JST)",
            "Received: from zcs-ubuntu.local (localhost [127.0.0.1])\r\n"
          + " by zcs-ubuntu.local (Postf ix) with ESMTPS id 3D4EA2C2648\r\n"
          + " for <user1@domain1.zimbra.com>; Wed,  7 Dec 2016 15:10:58 +0900 (JST)",
            "Received: from zcs-ubuntu.local (localhost [127.0.0.1])\r\n"
          + " by zcs-ubuntu.local (Postfix) with ESMTPS id 328882C26FC\r\n"
          + " for <user1@domain1.zimbra.com>; Wed,  7 Dec 2016 15:10:58 +0900 (JST)",
            "Received: from zcs-ubuntu.local (localhost [127.0.0.1])\r\n"
          + " by zcs-ubuntu.local (Postfix) with ESMTPS id 2822F2C2648\r\n"
          + " for <user1@domain1.zimbra.com>; Wed,  7 Dec 2016 15:10:58 +0900 (JST)",
            "X-Dummy-Header: ABC",
            "X-Dummy-Header: 123",
            "X-Dummy-Header: abc",
            "X-Dummy-Header: \"\"",
            "X-Dummy-Header: this is sample",
            "X-Dummy-Header: ",
            "X-Dummy-Header: test",
            "X-Dummy-Header: ''",
            "X-Dummy-Header: a1b2c3",
            "Message-ID: <46941357.16.1482318459470.JavaMail.zimbra@dev07>",
            "MIME-Version: 1.0",
            "Content-Transfer-Encoding: 7bit"};

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test@zimbra.com", "secret", attrs);

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

    /*
     * Adding new header
     */
    @Test
    public void testAddHeader() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + "if header :contains \"Subject\" \"example\" {\n"
                    + " addheader \"my-new-header\" \"my-new-header-value\" \r\n"
                    + "  ;\n"
                    + "}";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            int index = 0;
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                index++;
                if ("my-new-header".equals(temp.getName())) {
                    break;
                }
            }
            // the header field is inserted at the beginning of the existing message header.
            Assert.assertEquals(1, index);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Adding new header at last
     */
    @Test
    public void testAddHeaderLast() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "if header :contains \"Subject\" \"example\" {\n"
                    + " addheader :last \"my-new-header\" \"my-new-header-value\" \r\n"
                    + "  ;\n"
                    + "}";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            int index = 0;
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                index++;
                if ("my-new-header".equals(temp.getName())) {
                    break;
                }
            }
            Assert.assertEquals(6, index);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Adding new header with multiline value
     */
    @Test
    public void testAddHeaderWithMultilineValue() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " addheader :last \"X-Test-Header\" \"line1\r\n\tline2\r\n\tline3\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            int index = 0;
            String value = "";
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                index++;
                if ("X-Test-Header".equals(temp.getName())) {
                    value = temp.getValue();
                    break;
                }
            }
            Assert.assertEquals("=?UTF-8?B?bGluZTENCglsaW5lMg0KCWxpbmUz?=", value);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Adding a new header whose value is consisted of ASCII characters and some line breaks
     */
    @Test
    public void testAddHeaderLinebreakVariable() {
        String sampleBaseMsg = "Subject: =?utf-8?B?bGluZSAxCmhlYWRlcjogbGluZTIKDQpsaW5lIDQK?=\n"
                + "from: test2@zimbra.com\n"
                + "to: test@zimbra.com\n";

        String filterScript = "require [\"editheader\", \"variables\"];\n"
                + "if header :matches \"Subject\" \"*\" {\n"
                + " addheader \"my-new-header\" \"${1}\";\n"
                + "}";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String[] headers = message.getMimeMessage().getHeader("my-new-header");
            Assert.assertNotNull(headers);
            Assert.assertNotSame(0, headers.length);
            Assert.assertEquals("=?UTF-8?B?bGluZSAxCmhlYWRlcjogbGluZTIKDQpsaW5lIDQK?=", headers[0]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Adding new header with values from variables
     */
    @Test
    public void testAddHeaderWithVariables() {
        try {
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                    + " set \"nm\" \"X-New-Header\"; \r\n"
                    + " set \"vl\" \"test\"; \r\n"
                    + " addheader :last \"${nm}\" \"${vl}\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Map<String, Object> attrs = Maps.newHashMap();
            attrs = Maps.newHashMap();
            Provisioning.getInstance().getServer(acct1).modify(attrs);
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            boolean match = false;
            String value = "";
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                if ("X-New-Header".equals(temp.getName())) {
                    match = true;
                    value = temp.getValue();
                    break;
                }
            }
            Assert.assertTrue(match);
            Assert.assertEquals("test", value);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Adding a new header with non-ascii value
     */
    @Test
    public void testAddHeaderNonAscii() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + "if header :contains \"Subject\" \"example\" {\n"
                    + " addheader \"my-new-header\" \"追加ヘッダ値\";\n"
                    + "}";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            int index = 0;
            String newHeader = "";
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                index++;
                if ("my-new-header".equals(temp.getName())) {
                    newHeader = temp.getValue();
                    break;
                }
            }
            // the header field is inserted at the beginning of the existing message header.
            Assert.assertEquals(1, index);
            Assert.assertEquals("=?UTF-8?B?6L+95Yqg44OY44OD44OA5YCk?=", newHeader);

        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Adding a new header whose value is assigned by non-ASCII Variables
     */
    @Test
    public void testAddHeaderNonAsciiVariable() {
        String sampleBaseMsg = "Subject: =?utf-8?B?5pel5pys6Kqe44Gu5Lu25ZCN44CC5pel5pys6Kqe?=\n"
                + " =?utf-8?B?44Gu5Lu25ZCN44CC5pel5pys6Kqe44Gu5Lu25ZCN44CC?=\n"
                + "from: test2@zimbra.com\n"
                + "to: test@zimbra.com\n";

        String filterScript = "require [\"editheader\", \"variables\"];\n"
                + "if header :matches \"Subject\" \"*\" {\n"
                + " addheader \"my-new-header\" \"${1}\";\n"
                + "}";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            int index = 0;
            String newHeader = "";
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                index++;
                if ("my-new-header".equals(temp.getName())) {
                    newHeader = temp.getValue();
                    break;
                }
            }
            Assert.assertEquals(1, index);
            Assert.assertEquals("=?UTF-8?B?5pel5pys6Kqe44Gu5Lu25ZCN44CC5pel5pys6Kqe?=\r\n =?UTF-8?B?44Gu5Lu25ZCN44CC5pel5pys6Kqe44Gu5Lu25ZCN44CC?=", newHeader);

        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Add the headers with "X-Dummy-Header: new value"
     * Verify that the order of the header fields does not change.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddHeaderSameHeaderNameMultipleHeaderValues1() {
        StringBuffer triggeringMsg = new StringBuffer();
        for (String line : sampleBaseMsg2) {
            triggeringMsg.append(line).append("\r\n");
        }
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "addheader \"X-Dummy-Header\" \"new value\"; ";
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setSieveEditHeaderEnabled(true);
            account.setAdminSieveScriptBefore(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(triggeringMsg.toString().getBytes(), false), 0,
                    account.getName(), null, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Enumeration e = msg.getMimeMessage().getAllHeaderLines();
            Assert.assertTrue(e.hasMoreElements());

            // The 1st and 2nd line of the headers
            Assert.assertEquals("Return-Path: user1@domain1.zimbra.com", (String) e.nextElement());
            Assert.assertEquals("X-Dummy-Header: new value", (String) e.nextElement());

            // The rest of the headers
            int index = 1;
            while (e.hasMoreElements() && index < sampleBaseMsg2.length) {
                String value = (String) e.nextElement();
                Assert.assertEquals(sampleBaseMsg2[index++], value);
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header without header name which should fail
     */
    @Test
    public void testEmptyHeaderName() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + " addheader \"\" \"my-new-header-value\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                Assert.assertFalse(temp.getValue().equals("my-new-header-value"));
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header with space as header name, which should fail
     */
    @Test
    public void testAddHeaderNameAsSingleSpace() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + " addheader \" \" \"my-new-header-value\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                Assert.assertFalse(temp.getValue().equals("my-new-header-value"));
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header with multiple spaces as header name, which should fail
     */
    @Test
    public void testAddHeaderNameAsMultipleSpace() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + " addheader \"    \" \"my-new-header-value\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setAdminSieveScriptBefore(filterScript);
            acct1.setSieveEditHeaderEnabled(true);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                Assert.assertFalse(temp.getValue().equals("my-new-header-value"));
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header with header name starting with space, which should fail
     */
    @Test
    public void testAddHeaderNameStartingWithSpace() {
        try {
           String filterScript = "require [\"editheader\"];\n"
                    + " addheader \" X-My-Test\" \"my-new-header-value\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setAdminSieveScriptBefore(filterScript);
            acct1.setSieveEditHeaderEnabled(true);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                Assert.assertFalse(temp.getName().equals("X-My-Test"));
                Assert.assertFalse(temp.getValue().equals("my-new-header-value"));
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header with header name with space, which should not be added in mime.
     */
    @Test
    public void testAddHeaderNameWithSpaceFromVariable() {
        try {
           String filterScript = "require [\"editheader\",\"variables\"];\n"
                    + "set \"var1\" \" X-My-Test \";\n"
                    + "addheader \"${var1}\" \"my-new-header-value\" \r\n"
                    + "  ;\n";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setAdminSieveScriptBefore(filterScript);
            acct1.setSieveEditHeaderEnabled(true);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                Assert.assertFalse(temp.getName().equals("X-My-Test"));
                Assert.assertFalse(temp.getName().equals(" X-My-Test "));
                Assert.assertFalse(temp.getValue().equals("my-new-header-value"));
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testAddHeaderBadContentType() {
        String sampleBaseMsg = "Subject: example\n"
                + "Content-Type: text/plain;;\n"
                + "from: test2@zimbra.com\n"
                + "to: test@zimbra.com\n";

        String filterScriptUser = "tag \"tag-user\";";
        String filterAdminBefore = "require [\"editheader\", \"variables\"];\n"
                + "if header :matches \"Subject\" \"*\" {\n"
                + " tag \"tag-${1}1\";\n"
                + " addheader \"my-new-header\" \"${1}\";\n"
                + " tag \"tag-${1}2\";"
                + "}\n";
        String filterAdminAfter = "tag \"tag-admin-after\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            
            RuleManager.clearCachedRules(acct1);
            acct1.unsetAdminSieveScriptBefore();
            acct1.unsetMailSieveScript();
            acct1.unsetAdminSieveScriptAfter();
            acct1.setAdminSieveScriptBefore(filterAdminBefore);
            acct1.setMailSieveScript(filterScriptUser);
            acct1.setAdminSieveScriptAfter(filterAdminAfter);
            acct1.setSieveEditHeaderEnabled(true);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            boolean isAdded = false;
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                if ("my-new-header".equals(temp.getName())) {
                    isAdded = true;
                    break;
                }
            }
            Assert.assertTrue(isAdded);
            String[] tags = mdnMsg.getTags();
            Assert.assertEquals(4, tags.length);
            Assert.assertEquals("tag-example1", tags[0]);
            Assert.assertEquals("tag-example2", tags[1]);
            Assert.assertEquals("tag-user", tags[2]);
            Assert.assertEquals("tag-admin-after", tags[3]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testAddHeaderImmutableHeaders() {
        String sampleBaseMsg = "Subject: example\n"
                + "to: test@zimbra.com\n";

        String filterScriptUser = "require [\"editheader\"];\n"
                + "tag \"tag-example1\";\n"
                + "if exists \"Subject\" {\n"
                + "  addheader \"Content-Type\" \"text/plain\";\n"
                + "  addheader \"MIME-Version\" \"1.0\";\n"
                + "  addheader \"Content-Transfer-Encoding\" \"7bit\";\n"
                + "  addheader \"content-disposition\" \"inline\";\n"
                + "}\n"
                + "tag \"tag-example2\";\n";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.unsetAdminSieveScriptBefore();
            acct1.unsetMailSieveScript();
            acct1.unsetAdminSieveScriptAfter();
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScriptUser);

            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            Assert.assertNull(message.getMimeMessage().getHeader("Content-Type"));
            Assert.assertNull(message.getMimeMessage().getHeader("Content-Disposition"));
            Assert.assertNull(message.getMimeMessage().getHeader("Content-Transfer-Encoding"));
            Assert.assertNull(message.getMimeMessage().getHeader("MIME-Version"));
            String[] tags = message.getTags();
            Assert.assertEquals(2, tags.length);
            Assert.assertEquals("tag-example1", tags[0]);
            Assert.assertEquals("tag-example2", tags[1]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding an immutable header when the ldap value contains whites spaces. Check if spaces are ignored.
     */
    @Test
    public void testAddHeaderImmutableHeadersWithWhiteSpaces() {
        String sampleBaseMsg = "Subject: example\n"
                + "to: test@zimbra.com\n";

        String filterScriptUser = "require [\"editheader\"];\n"
                + "tag \"tag-example1\";\n"
                + "if exists \"Subject\" {\n"
                + "  addheader \"Content-Type\" \"text/plain\";\n"
                + "  addheader \"MIME-Version\" \"1.0\";\n"
                + "  addheader \"Content-Transfer-Encoding\" \"7bit\";\n"
                + "  addheader \"CONTENT-DISPOSITION\" \"inline\";\n"
                + "}\n"
                + "tag \"tag-example2\";\n";
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            // LDAP attribute comma separated list value contains white spaces
            acct1.setSieveImmutableHeaders(
                " Content-Type , Content-Disposition , Content-Transfer-Encoding , MIME-Version");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.unsetAdminSieveScriptBefore();
            acct1.unsetMailSieveScript();
            acct1.unsetAdminSieveScriptAfter();
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScriptUser);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            Assert.assertNull(message.getMimeMessage().getHeader("Content-Type"));
            Assert.assertNull(message.getMimeMessage().getHeader("Content-Disposition"));
            Assert.assertNull(message.getMimeMessage().getHeader("Content-Transfer-Encoding"));
            Assert.assertNull(message.getMimeMessage().getHeader("MIME-Version"));

            String[] tags = message.getTags();
            Assert.assertEquals(2, tags.length);
            Assert.assertEquals("tag-example1", tags[0]);
            Assert.assertEquals("tag-example2", tags[1]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }


    /*
     * Try adding new header in admin script when the SieveEditHeaderEnabled attribute is true
     */
    @Test
    public void addHeaderSieveEditHeaderEnabledTrue() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                + "addheader \"X-New-Header\" \"my-new-header-value\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox1), mbox1,
                new ParsedMessage(sampleBaseMsg.getBytes(), false), 0, acct1.getName(), null,
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e
                .hasMoreElements();) {
                Header temp = e.nextElement();
                if (temp.getName().equals("X-New-Header")) {
                    matchFound = true;
                    Assert.assertEquals("my-new-header-value", temp.getValue());
                }
            }
            Assert.assertTrue(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header in admin script when the SieveEditHeaderEnabled attribute is false
     */
    @Test
    public void addHeaderSieveEditHeaderEnabledFalse() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                + "addheader \"X-New-Header\" \"my-new-header-value\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(false);
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox1), mbox1,
                new ParsedMessage(sampleBaseMsg.getBytes(), false), 0, acct1.getName(), null,
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e
                .hasMoreElements();) {
                Header temp = e.nextElement();
                if (temp.getName().equals("X-New-Header")) {
                    matchFound = true;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try adding new header in user script
     */
    @Test
    public void addHeaderUserSieveScript() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                + "addheader \"X-New-Header\" \"my-new-header-value\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox1), mbox1,
                new ParsedMessage(sampleBaseMsg.getBytes(), false), 0, acct1.getName(), null,
                new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                .getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e
                .hasMoreElements();) {
                Header temp = e.nextElement();
                if (temp.getName().equals("X-New-Header")) {
                    matchFound = true;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }
}
