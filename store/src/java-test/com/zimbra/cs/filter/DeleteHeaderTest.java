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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
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

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class DeleteHeaderTest {

    private static String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "X-Test-Header: test1\n"
            + "X-Test-Header: test2\n"
            + "X-Test-Header: test3\n"
            + "X-Test-Header-non-ascii: =?utf-8?B?5pel5pys6Kqe44Gu5Lu25ZCN?=\n"
            + "X-Header-With-Control-Chars: =?utf-8?B?dGVzdCBIVAkgVlQLIEVUWAMgQkVMByBCUwggbnVsbAAgYWZ0ZXIgbnVsbA0K?=\n"
            + "X-Numeric-Header: 2\n"
            + "X-Numeric-Header: 3\n"
            + "X-Numeric-Header: 4\n"
            + "X-Dummy-Header: ABC\n"
            + "X-Dummy-Header: 123\n"
            + "X-Dummy-Header: abc\n"
            + "X-Dummy-Header: \"\"\n"
            + "X-Dummy-Header: xyz\n"
            + "X-Dummy-Header: \n"
            + "X-Dummy-Header: test\n"
            + "X-Dummy-Header: ''\n"
            + "X-Dummy-Header: a1b2c3\n"
            + "from: test2@zimbra.com\n"
            + "Subject: example\n"
            + "to: test@zimbra.com\n";

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

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test4@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());

    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    /*
     * Delete all X-Test-Header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderXTestHeaderAll() {
        String[] expected = {"Received: from edge01e.zimbra.com ([127.0.0.1])\r\n"
                + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\r\n"
                + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)",
                "Received: from localhost (localhost [127.0.0.1])\r\n"
                + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\r\n"
                + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)",
                "X-Test-Header-non-ascii: =?utf-8?B?5pel5pys6Kqe44Gu5Lu25ZCN?=",
                "X-Header-With-Control-Chars: =?utf-8?B?dGVzdCBIVAkgVlQLIEVUWAMgQkVMByBCUwggbnVsbAAgYWZ0ZXIgbnVsbA0K?=",
                "X-Numeric-Header: 2",
                "X-Numeric-Header: 3",
                "X-Numeric-Header: 4",
                "X-Dummy-Header: ABC",
                "X-Dummy-Header: 123",
                "X-Dummy-Header: abc",
                "X-Dummy-Header: \"\"",
                "X-Dummy-Header: xyz",
                "X-Dummy-Header: ",
                "X-Dummy-Header: test",
                "X-Dummy-Header: ''",
                "X-Dummy-Header: a1b2c3",
                "from: test2@zimbra.com",
                "Subject: example",
                "to: test@zimbra.com",
                "Content-Transfer-Encoding: 7bit",
                "MIME-Version: 1.0",
                "Message-ID:"};

        try {
           String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader \"X-Test-Header\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean headerDeleted = true;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header temp = enumeration.nextElement();
                if ("X-Test-Header".equals(temp.getName())) {
                    headerDeleted = false;
                    break;
                }
            }
            Assert.assertTrue(headerDeleted);

            // Verify the order of the message header
            MimeMessage mm = message.getMimeMessage();
            List<MPartInfo> parts = Mime.getParts(mm);
            Set<MPartInfo> bodies = Mime.getBody(parts, false);
            Assert.assertEquals(1, bodies.size());
            for (MPartInfo body : bodies) {
                Enumeration e = body.getMimePart().getAllHeaderLines();
                int i = 0;
                while (e.hasMoreElements()) {
                    String header = (String) e.nextElement();
                    if (header.startsWith("Message-ID:")) {
                        header = "Message-ID:";
                    }
                    Assert.assertEquals(expected[i++], header);
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header at index
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderAtIndex() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :index 2 \"X-Test-Header\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName()) && "test2".equals(header.getValue())) {
                    matchFound = true;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete last header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteLastHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :last :index 1 \"X-Test-Header\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 0;
            String headerValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    indexMatch++;
                    headerValue = header.getValue();
                }
            }
            Assert.assertEquals(indexMatch, 2);
            Assert.assertEquals("test2", headerValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header value of 2nd from bottom
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteSecondFromBottomHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :index 2 :last \"X-Test-Header\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 0;
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    indexMatch++;
                    if ("test2".equals(header.getValue())) {
                        matchFound = true;
                    }
                }
            }
            Assert.assertEquals(indexMatch, 2);
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header using is match-type :is
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderUsingIs() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :is \"X-Test-Header\" \"test2\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName()) && "test2".equals(header.getValue())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header using matches match-type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderUsingMatches() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :matches \"X-Test-Header\" \"test*\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header using contains match-type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderUsingContains() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :contains \"X-Test-Header\" \"test2\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName()) && "test2".equals(header.getValue())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header with numeric comparator :value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderWithNumericComparisionUsingValue() {
        try {
            String filterScript = "require [\"editheader\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                    + " deleteheader :value \"lt\" :comparator \"i;ascii-numeric\" \"X-Numeric-Header\" \"3\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric-Header".equals(header.getName()) && Integer.valueOf(header.getValue()) < 3) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header with numeric comparator :count
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderWithNumericComparisionUsingCount() {
        try {
            String filterScript = "require [\"editheader\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                    + " deleteheader :count \"ge\" :comparator \"i;ascii-numeric\" \"X-Numeric-Header\" \"3\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric-Header".equals(header.getName())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header with index, last and match-type
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderWithIndexLastAndMatchType() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :index 2 :last :contains \"X-Test-Header\" \"2\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName()) && "test2".equals(header.getValue())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header than add header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderThanAddHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :is \"X-Test-Header\" \"test2\" \r\n"
                    + "  ;\n"
                    + " addheader \"X-Test-Header\" \"test5\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            boolean newAdded = false;
            int matchCount = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())){
                    matchCount++;
                    if ("test2".equals(header.getValue())) {
                        matchFound = true;
                    } else if ("test5".equals(header.getValue())) {
                        newAdded = true;
                    }
                }
            }
            Assert.assertFalse(matchFound);
            Assert.assertTrue(newAdded);
            Assert.assertEquals(matchCount, 3);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Add header than delete header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddHeaderThanDeleteHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " addheader :last \"X-Test-Header\" \"test5\" \r\n"
                    + "  ;\n"
                    + " deleteheader :is \"X-Test-Header\" \"test2\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            boolean newAdded = false;
            int matchCount = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())){
                    matchCount++;
                    if ("test2".equals(header.getValue())) {
                        matchFound = true;
                    }
                    if ("test5".equals(header.getValue())) {
                        newAdded = true;
                    }
                }
            }
            Assert.assertFalse(matchFound);
            Assert.assertTrue(newAdded);
            Assert.assertEquals(matchCount, 3);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Add header than delete header than again add header
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddHeaderThanDeleteHeaderThanAddHeader() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " addheader :last \"X-Test-Header\" \"test5\" ;\n"
                    + " deleteheader :contains \"X-Test-Header\" \"2\" ;\n"
                    + " addheader :last \"X-Test-Header\" \"test6\" ;\n";
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
            boolean matchFound = false;
            boolean firstAdded = false;
            boolean secondAdded = false;
            int matchCount = 0;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())){
                    matchCount++;
                    if ("test2".equals(header.getValue())) {
                        matchFound = true;
                    }
                    if ("test5".equals(header.getValue())) {
                        firstAdded = true;
                    }
                    if ("test6".equals(header.getValue())) {
                        secondAdded = true;
                    }
                }
            }
            Assert.assertFalse(matchFound);
            Assert.assertTrue(firstAdded);
            Assert.assertTrue(secondAdded);
            Assert.assertEquals(matchCount, 4);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header using matches match-type whose key is non-ASCII
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderUsingMatchesNonASCII() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "deleteheader :comparator \"i;ascii-casemap\" :is  \"X-Test-Header-non-ascii\" \"日本語の件名\";\n";
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
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header-non-ascii".equals(header.getName())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header using matches wild-card
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderUsingMatchesWildcardNonASCII() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "deleteheader :matches \"X-Test-Header-non-ascii\" \"*\";\n";
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
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header-non-ascii".equals(header.getName())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Verify the Match Variables assigned by deleteheader's wild-card match
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMatchVariables() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "deleteheader :matches \"X-Dummy-Header\" \"*\";";
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

            String expectedTags[] = {
                    "tag1-ABC",
                    "tag2-123",
                    "tag3-abc",
                    "tag4-\"\"",
                    "tag5-xyz",
                    "tag6-",
                    "tag7-tes",
                    "tag8-\'\'",
                    "tag9-a1b2c3"};
            String resultTags[] = message.getTags();
            for (String resultTag : resultTags) {
                String expectedTag = null;
                for (String testTag : expectedTags) {
                    if (testTag.equalsIgnoreCase(resultTag)) {
                        expectedTag = testTag;
                        break;
                    }
                }
                Assert.assertEquals(expectedTag, resultTag);
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header value of 2nd from bottom
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderNinthFromBottom() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader :index 9 :last \"X-Dummy-Header\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Dummy-Header".equals(header.getName())) {
                    Assert.assertEquals("123", header.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header with casemap comparator with :value
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderWithValueComparisionForCasemapComparator() {
        try {
            String filterScript = "require [\"editheader\", \"relational\"];\n"
                    + " deleteheader :value \"lt\" :comparator \"i;ascii-casemap\" \"X-Numeric-Header\" \"3\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric-Header".equals(header.getName()) && Integer.valueOf(header.getValue()) < 3) {
                        matchFound = true;
                        break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header without match-type.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderWithOutMatchType() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader \"X-Numeric-Header\" \"3\";";
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
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric-Header".equals(header.getName()) && Integer.valueOf(header.getValue()) == 3) {
                        matchFound = true;
                        break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * delete header when value-patterns specified is empty should return false
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteNoValuePattern() {
        try {
            String filterScript = "require [\"editheader\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                    + " deleteheader :count \"gt\" :comparator \"i;ascii-numeric\" \"Subject\" \"\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage()
                .getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("Subject".equals(header.getName())) {
                    matchFound = true;
                }
            }
            Assert.assertTrue(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try to delete header with header name starting with space, which should fail
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderWithHeaderNameStartingWithSpace() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + " deleteheader \" X-Test-Header\" \r\n"
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
            Message message = mbox1.getMessageById(null, itemId);
            int indexMatch = 0;
            String headerValue = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Test-Header".equals(header.getName())) {
                    indexMatch++;
                    headerValue = header.getValue();
                }
            }
            Assert.assertEquals(indexMatch, 3);
            Assert.assertEquals("test3", headerValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Parameter of the Negative number causes an exception and filter execution
     * will be failed --> message is delivered to the Inbox without deleting the
     * header "X-Numeric-Header"
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderCountNegative() {
        try {
            String filterScript = "require [\"editheader\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                    + "deleteheader :count \"le\" :comparator \"i;ascii-numeric\" \"X-Numeric-Header\" \"-1\";\n";
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
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage()
                    .getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Numeric-Header".equals(header.getName())) {
                    matchFound = true;
                }
            }
            Assert.assertTrue(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteHeaderBadContentType() {
        String sampleBaseMsg = "Subject: example\n"
                + "Content-Type: text/plain;;\n"
                + "from: test2@zimbra.com\n"
                + "to: test@zimbra.com\n"
                + "X-Test-Header: test1";

        String filterScriptUser = "tag \"tag-user\";";
        String filterAdminBefore = "require [\"editheader\"];\n"
                + "tag \"tag-user1\";\n"
                + "deleteheader \"X-Test-Header\";\n"
                + "tag \"tag-user2\";\n";
        String filterAdminAfter = "tag \"tag-admin-after\";";

        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterAdminBefore);
            acct1.setMailSieveScript(filterScriptUser);
            acct1.setAdminSieveScriptAfter(filterAdminAfter);

            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox1.getMessageById(null, itemId);
            boolean isDeleted = true;
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                if ("X-Test-Header".equals(temp.getName())) {
                    isDeleted = false;
                }
            }
            Assert.assertTrue(isDeleted);
            String[] tags = mdnMsg.getTags();
            Assert.assertEquals(4, tags.length);
            Assert.assertEquals("tag-user1", tags[0]);
            Assert.assertEquals("tag-user2", tags[1]);
            Assert.assertEquals("tag-user", tags[2]);
            Assert.assertEquals("tag-admin-after", tags[3]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderAsciiNumbericIsComparator() {
        try {
            String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
                + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
                + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "Received: from localhost (localhost [127.0.0.1])\n"
                + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
                + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n" + "Subject: 1\n"
                + "to: test@zimbra.com\n";
            String filterScript = "require [\"editheader\", \"comparator-i;ascii-numeric\"];\n"
                + "deleteheader :is :comparator \"i;ascii-numeric\" \"Subject\" \"1\";\n";
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
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage()
                .getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                System.out.println(header.getName() + " - " + header.getValue());
                if ("Subject".equals(header.getName())) {
                    matchFound = true;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try deleting an immutable header from a sieve script
     */
    @Test
    public void testDeleteHeaderImmutableHeaders() {
        String sampleBaseMsg = "Subject: example\n"
                + "to: test@zimbra.com\n"
                + "Content-Type: text/plain; charset=\"ISO-2022-JP\"\n"
                + "MIME-Version: 1.0\n"
                + "Content-Transfer-Encoding: 7bit\n"
                + "Content-Disposition: inline\n"
                + "Auto-Submitted: auto-generated\n";
        String filterScript = "require [\"editheader\"];\n"
                + "tag \"tag-example1\";\n"
                + "if exists \"Subject\" {\n"
                + "  deleteheader \"Content-Type\" \"text/plain\";\n"
                + "  deleteheader \"MIME-Version\" \"1.0\";\n"
                + "  deleteheader \"Content-Transfer-Encoding\" \"7bit\";\n"
                + "  deleteheader \"Content-Disposition\" \"inline\";\n"
                + "  deleteheader \"auto-submitted\" \"auto-generated\";\n"
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
            acct1.setAdminSieveScriptBefore(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            Assert.assertNotNull(message.getMimeMessage().getHeader("Content-Type"));
            Assert.assertNotNull(message.getMimeMessage().getHeader("Content-Disposition"));
            Assert.assertNotNull(message.getMimeMessage().getHeader("Content-Transfer-Encoding"));
            Assert.assertNotNull(message.getMimeMessage().getHeader("MIME-Version"));
            Assert.assertNotNull(message.getMimeMessage().getHeader("Auto-Submitted"));
            String[] tags = message.getTags();
            Assert.assertEquals(2, tags.length);
            Assert.assertEquals("tag-example1", tags[0]);
            Assert.assertEquals("tag-example2", tags[1]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try deleting a header in admin script when the SieveEditHeaderEnabled attribute is true
     */
    @Test
    public void deleteHeaderSieveEditHeaderEnabledTrue() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                + "deleteheader \"Subject\";";
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
                if (temp.getName().equals("Subject")) {
                    matchFound = true;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try deleting a header in admin script when the SieveEditHeaderEnabled attribute is false
     */
    @Test
    public void deleteHeaderSieveEditHeaderEnabledFalse() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                + "deleteheader \"Subject\";";
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
                if (temp.getName().equals("Subject")) {
                    matchFound = true;
                }
            }
            Assert.assertTrue(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Try deleting a header in user script
     */
    @Test
    public void deleteHeaderUserSieveScript() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                + "deleteheader \"Subject\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4@zimbra.com");
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
                if (temp.getName().equals("Subject")) {
                    matchFound = true;
                }
            }
            Assert.assertTrue(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /*
     * Delete header using matches match-type (X-Header-With-Control-Chars)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteHeaderUsingWildcardMatchesToControlChars() {
        try {
            String filterScript = "require [\"editheader\"];\n"
                    + "deleteheader :matches \"X-Header-With-Control-Chars\" \"*\";";
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setSieveEditHeaderEnabled(true);
            acct1.setAdminSieveScriptBefore(filterScript);
            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            boolean matchFound = false;
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("X-Header-With-Control-Chars".equals(header.getName())) {
                    matchFound = true;
                    break;
                }
            }
            Assert.assertFalse(matchFound);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testMalencodedHeader() throws Exception {
        String script = "require [\"editheader\"];\n"
                    + "deleteheader :matches \"X-Mal-Encoded-Header\" \"*\";";
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setSieveEditHeaderEnabled(true);
            account.setAdminSieveScriptBefore(script);
            account.setMailSieveScript(script);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                    mbox, new ParsedMessage("X-Mal-Encoded-Header: =?ABC?A?GyRCJFskMhsoQg==?=".getBytes(), false), 0,
                    account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Message message = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertNull(message.getMimeMessage().getHeader("X-Mal-Encoded-Header"));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    /*
     * The ascii-numeric comparator should be looked up in the list of the "require".
     */
    @Test
    public void testMissingComparatorNumericDeclaration() throws Exception {
        // Default match type :is is used.
        // No "comparator-i;ascii-numeric" capability text in the require command
        String script = "require [\"editheader\"];\n"
                    + "deleteheader :comparator \"i;ascii-numeric\" \"X-Header\" \"example\";";
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setSieveEditHeaderEnabled(true);
            account.setAdminSieveScriptBefore(script);
            account.setMailSieveScript(script);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                    mbox, new ParsedMessage("X-Header: example".getBytes(), false), 0,
                    account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Message message = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertNotNull(message.getMimeMessage().getHeader("X-Header"));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testBackslashAsciiCasemap4bs() throws Exception {
        // Matches four backslashes
        String script = "require [\"editheader\"];\n"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-Header\"  \"Sample\\\\\\\\\\\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-HeaderA\" \"Sample\\\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-HeaderB\" \"Sample\\\\Pattern\";"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-HeaderC\" \"Sample\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\Pattern\";";
        String pattern = "Sample\\\\\\\\Pattern";
        String msg = "X-Header: " + pattern + "\n"
                   + "X-HeaderA: " + pattern + "\n"
                   + "X-HeaderB: " + pattern + "\n"
                   + "X-HeaderC: " + pattern + "\n";
        boolean result = testBackslash(script, pattern, msg);
        Assert.assertTrue(result);
    }

    @Test
    public void testBackslashAsciiCasemap5bs() throws Exception {
        // Matches five backslashes
        String script = "require [\"editheader\"];\n"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-Header\"  \"Sample\\\\\\\\\\\\\\\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-HeaderA\" \"Sample\\\\\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-HeaderB\" \"Sample\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;ascii-casemap\" \"X-HeaderC\" \"Sample\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\Pattern\";";
        String pattern = "Sample\\\\\\\\\\Pattern";
        String msg = "X-Header: " + pattern + "\n"
                   + "X-HeaderA: " + pattern + "\n"
                   + "X-HeaderB: " + pattern + "\n"
                   + "X-HeaderC: " + pattern + "\n";
        boolean result = testBackslash(script, pattern, msg);
        Assert.assertTrue(result);
    }

    @Test
    public void testBackslashOctet() throws Exception {
        String script = "require [\"editheader\"];\n"
                + "deleteheader :comparator \"i;octet\" \"X-Header\"  \"Sample\\\\\\\\\\\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;octet\" \"X-HeaderA\" \"Sample\\\\\\\\Pattern\";"
                + "deleteheader :comparator \"i;octet\" \"X-HeaderB\" \"Sample\\\\Pattern\";"
                + "deleteheader :comparator \"i;octet\" \"X-HeaderC\" \"Sample\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\Pattern\";";
        String pattern = "Sample\\\\\\\\Pattern";
        String msg = "X-Header: " + pattern + "\n"
                   + "X-HeaderA: " + pattern + "\n"
                   + "X-HeaderB: " + pattern + "\n"
                   + "X-HeaderC: " + pattern + "\n";
        boolean result = testBackslash(script, pattern, msg);
        Assert.assertTrue(result);
    }

    private boolean testBackslash(String script, String pattern, String msg) {
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();
            account.setSieveEditHeaderEnabled(true);
            account.setAdminSieveScriptBefore(script);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox),
                    mbox, new ParsedMessage(msg.getBytes(), false), 0,
                    account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Message message = mbox.getMessageById(null, ids.get(0).getId());
            String[] headers = message.getMimeMessage().getHeader("X-Header");
            Assert.assertNull(headers);
            headers = message.getMimeMessage().getHeader("X-HeaderA");
            Assert.assertNotNull(headers);
            Assert.assertNotSame(0, headers.length);
            Assert.assertEquals(pattern, headers[0]);
            headers = message.getMimeMessage().getHeader("X-HeaderB");
            Assert.assertNotNull(headers);
            Assert.assertNotSame(0, headers.length);
            Assert.assertEquals(pattern, headers[0]);
            headers = message.getMimeMessage().getHeader("X-HeaderC");
            Assert.assertNotNull(headers);
            Assert.assertNotSame(0, headers.length);
            Assert.assertEquals(pattern, headers[0]);
            return true;
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
            return false;
        }
    }
}
