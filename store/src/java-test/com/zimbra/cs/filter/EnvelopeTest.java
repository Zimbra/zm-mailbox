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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

public class EnvelopeTest {
    private static String sampleMsg =
              "from: tim@example.com\n"
            + "to: test@zimbra.com\n"
            + "Subject: Example\n";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        prov.createAccount("original@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testFrom() {
        // RFC 5228 5.4. Test envelope example
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"tim@example.com\" {\n"
                + "discard;\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(0, ids.size());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testTo() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"to\" \"test@zimbra.com\" {\n"
                + "  tag \"To\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            account.setMail("test@zimbra.com");
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("To", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testTo_BccTo() {
        /*
         * RFC 5228 5.4.
         * ----
         * If the SMTP transaction involved several RCPT commands, only the data
         * from the RCPT command that caused delivery to this user is available
         * in the "to" part of the envelope.
         * ----
         * The bcc recipient (who is specified by RCPT command but not on the
         * message header) should not be matched by the 'envelope' test.
         */
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"to\" \"bccTo@zimbra.com\" {\n"
                + "  tag \"Bcc To\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        env.setSender(sender);

        // To address
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.addLocalRecipient(recipient);
        // Bcc address
        recipient = new LmtpAddress("<bccTo@zimbra.com>", null, null);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFrom() {
        /*
         * Check 'ADDRESS-PART' and 'MATCH-TYPE' work
         */
        String filterScript = "require \"envelope\";\n"
                + "if envelope :domain :contains \"from\" \"example\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFromBackslash() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"ti\\\\m@example.com\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<\"ti\\\\m\"@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFromDot() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"ti.m@example.com\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<ti.m@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFromDoubleQuote() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"ti\\\"m@example.com\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<\"ti\\\"m\"@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFromSingleQuote() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"ti'm@example.com\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<ti'm@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFromQuestionMark() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"ti?m@example.com\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<ti?m@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFromComma() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"ti,m@example.com\" {\n"
                + "  tag \"From\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<\"ti,m\"@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("From", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVariable1() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :matches [\"from\"] \"*\" {\n"
                + "  tag \"env_${1}\";\n"
                + "}\n"
                + "if envelope :matches [\"to\"] \"*\" {\n"
                + "  tag \"env_${1}\";\n"
                + "}\n"
                + "if address :matches :comparator \"i;ascii-casemap\" [\"from\"] \"*\" {\n"
                + "  tag \"adr_${1}\";\n"
                + "}\n"
                + "if address :matches :comparator \"i;ascii-casemap\" [\"to\"] \"*\" {\n"
                + "  tag \"adr_${1}\";\n"
                + "}\n"
                + "if header :matches [\"from\"] \"*\" {\n"
                + "  tag \"hdr_${1}\";\n"
                + "}\n"
                + "if header :matches [\"to\"] \"*\" {\n"
                + "  tag \"hdr_${1}\";\n"
                + "}\n";
        testVariable(filterScript);
    }

    /*
     * Once Bug 107044 is solved, this pattern should be tested instead testVariable1()
     */
    public void testVariable2() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :matches [\"from\"] \"*\" {\n"
                + "  tag \"env_${1}\";\n"
                + "}\n"
                + "if envelope :matches [\"to\"] \"*\" {\n"
                + "  tag \"env_${1}\";\n"
                + "}\n"
                + "if address :matches [\"from\"] \"*\" {\n"
                + "  tag \"adr_${1}\";\n"
                + "}\n"
                + "if address :matches [\"to\"] \"*\" {\n"
                + "  tag \"adr_${1}\";\n"
                + "}\n"
                + "if header :matches [\"from\"] \"*\" {\n"
                + "  tag \"hdr_${1}\";\n"
                + "}\n"
                + "if header :matches [\"to\"] \"*\" {\n"
                + "  tag \"hdr_${1}\";\n"
                + "}\n";
        testVariable(filterScript);
    }

    public void testVariable(String filterScript) {
        /*
         * Checks if numeric variable works
         */
        String triggeringMsg =
                "from: message_header_from@example.com\n"
              + "to: message_header_to@zimbra.com\n"
              + "Subject: Example\n";

        String[] expectedTagName = {"env_envelope_from@example.com",
                                    "env_test@zimbra.com",
                                    "adr_message_header_from@example.com",
                                    "adr_message_header_to@zimbra.com",
                                    "hdr_message_header_from@example.com",
                                    "hdr_message_header_to@zimbra.com"};

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<envelope_from@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            Map<String, Object> attrs = Maps.newHashMap();
            attrs = Maps.newHashMap();
            Provisioning.getInstance().getServer(account).modify(attrs);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(triggeringMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String[] tags = msg.getTags();
            Assert.assertTrue(tags != null);
            Assert.assertEquals(expectedTagName.length, tags.length);
            for (int i = 0; i < expectedTagName.length; i++) {
                Assert.assertEquals(expectedTagName[i], tags[i]);
            }
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMailFrom_nullReverse_path() {
        /*
         * RFC 5228 5.4.
         * ---
         * The null reverse-path is matched against as the empty
         * string, regardless of the ADDRESS-PART argument specified.
         * ---
         */
        String filterScript = "require \"envelope\";\n"
                + "if envelope :localpart :is \"from\" \"\" {\n"
                + "  tag \"NullMailFrom\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("NullMailFrom", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testOutgoingFilter() {
        /*
         * As the envelope data is available only when the message is processed during the
         * LMTP session, the 'envelope' test always returns false.
         */
        String filterScript = "require \"envelope\";\n"
                + "if envelope :all :is \"from\" \"\" {\n"
                + "  tag \"outgoing\";\n"
                + "}";

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToOutgoingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false),
                    5, /* sent folder */
                    true, 0, null, Mailbox.ID_AUTO_INCREMENT);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testCompareEmptyStringWithAsciiNumeric() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :comparator \"i;ascii-numeric\" :all :is \"from\" \"\" {\n"
                + "  tag \"testCompareEmptyStringWithAsciiNumeric envelope\";"
                + "}"
                + "if header :comparator \"i;ascii-numeric\" :is \"from\" \"\" {\n"
                + "  tag \"testCompareEmptyStringWithAsciiNumeric header\";"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());

            String[] tags = msg.getTags();
            Assert.assertTrue(tags != null);
            Assert.assertEquals(2, tags.length);
            Assert.assertEquals("testCompareEmptyStringWithAsciiNumeric envelope", tags[0]);
            Assert.assertEquals("testCompareEmptyStringWithAsciiNumeric header", tags[1]);
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testTo_Alias() {
        String filterScript = "require \"envelope\";\n"
                + "set \"rcptto\" \"unknown\";\n"
                + "if envelope :all :matches \"to\" \"*\" {\n"
                + "  set \"rcptto\" \"${1}\";\n"
                + "  tag \"${rcptto}\";\n"
                + "}\n"
                + "if envelope :all :matches \"to\" \"alias1*\" {\n"
                + "  tag \"${1}\";\n"
                + "}\n"
                + "if envelope :all :matches \"to\" \"alias2*\" {\n"
                + "  tag \"bad\";\n"
                + "}\n"
                + "if envelope :count \"eq\" :comparator \"i;ascii-numeric\" \"to\" \"1\" {"
                + "  tag \"1\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<alias1@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.createAccount("original1@zimbra.com", "secret", new HashMap<String, Object>());
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailAdminSieveScriptBefore(filterScript);
            account.setMail("original1@zimbra.com");
            String[] alias = {"alias1@zimbra.com", "alias2@zimbra.com"};
            account.setMailAlias(alias);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());

            String[] tags = msg.getTags();
            Assert.assertTrue(tags != null);
            Assert.assertEquals(3, tags.length);
            Assert.assertEquals("alias1@zimbra.com", tags[0]);
            Assert.assertEquals("@zimbra.com", tags[1]);
            Assert.assertEquals("1", tags[2]);
        } catch (Exception e) {
            fail("No exception should be thrown:" + e);
        }
    }

    @Test
    public void testCountForEmptyFromHeader() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :count \"eq\" :comparator \"i;ascii-numeric\" :all \"FROM\" \"0\" {\n"
                + "tag \"0\";\n"
                + "}\n"
                + "if envelope :all :matches \"from\" \"\" {\n"
                + "  tag \"empty\";\n"
                + "}\n"
                + "if envelope :count \"eq\" :comparator \"i;ascii-numeric\" :all \"to\" \"1\" {\n"
                + "tag \"1\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.createAccount("xyz@zimbra.com", "secret", new HashMap<String, Object>());
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("0", ArrayUtil.getFirstElement(msg.getTags()));
            Assert.assertEquals("empty", msg.getTags()[1]);
            Assert.assertEquals("1", msg.getTags()[2]);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNumericNegativeValueCount() {
        String filterScript = "require [\"envelope\", \"tag\", \"relational\"];\n"
                + "if envelope :all :count \"lt\" :comparator \"i;ascii-numeric\" \"to\" \"-1\" {\n"
                + "  tag \"To\";\n"
                + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            account.setMail("test@zimbra.com");
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testHeaderNameWithLeadingSpace() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :matches \" TO\" \"*@zimbra.com\" {\n"
                + "    tag \"t1\";\n"
                + "}\n"
                ;
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.createAccount("xyz@zimbra.com", "secret", new HashMap<String, Object>());
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testHeaderNameWithTrailingSpace() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :matches \"TO \" \"*@zimbra.com\" {\n"
                + "    tag \"t1\";\n"
                + "}\n"
                ;
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.createAccount("xyz@zimbra.com", "secret", new HashMap<String, Object>());
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testHeaderNameWithLeadingAndTrailingSpace() {
        String filterScript = "require \"envelope\";\n"
                + "if envelope :matches \" TO \" \"*@zimbra.com\" {\n"
                + "    tag \"t1\";\n"
                + "}\n"
                ;
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.createAccount("xyz@zimbra.com", "secret", new HashMap<String, Object>());
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                    account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
}
