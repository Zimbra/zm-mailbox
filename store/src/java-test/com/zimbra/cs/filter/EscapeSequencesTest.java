/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Header;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
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
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link EscapeSequencesTest}.
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class EscapeSequencesTest {

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
        Account account = prov.createAccount("test1@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraSieveNotifyActionRFCCompliant, "TRUE");
        prov.createAccount("test2@zimbra.com", "secret", attrs);

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

    /*
     * pattern in filter (To): user\*@zimbra.com ==> applied pattern: user*@zimbra.com
     * (The undefined escape sequence \* will be ignored)
     */
    @Test
    public void testAddressEscape1() {
        String filter = "if address :comparator \"i;ascii-casemap\" :matches \"To\" \"user\\*@zimbra.com\" {"
                + "tag \"list\";}";
        doTestHeaderEscapePattern(filter);
    }

    /*
     * pattern in filter  (To): user\123@zimbra.com ==> applied pattern: user123@zimbrac.om
     * (The undefined escape sequence \1 will be ignored)
     */
    @Test
    public void testAddressEscape2() {
        doTestHeaderEscapePattern("if address :comparator \"i;ascii-casemap\" :matches \"To\" \"user\\123@zimbra.com\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter  (envelope from): user\*@example.com ==> applied pattern: user*@example.com
     * (The undefined escape sequence \* will be ignored)
     */
    @Test
    public void testEnvelopeEscape1() {
        doTestEnvelopeEscapePattern("require \"envelope\";\n"
                + "if envelope :all :comparator \"i;ascii-casemap\" :matches \"from\" \"user\\*@example.com\" {\n"
                + "discard;\n"
                + "}");
    }

    /*
     * pattern in filter  (envelope from): user\123@example.com ==> applied pattern user123@example.com
     * (The undefined escape sequence \1 will be ignored)
     */
    @Test
    public void testEnvelopeEscape2() {
        doTestEnvelopeEscapePattern("require \"envelope\";\n"
                + "if envelope :all :comparator \"i;ascii-casemap\" :matches \"from\" \"user\\123@example.com\" {\n"
                + "discard;\n"
                + "}");
    }

    /*
     * pattern in filter  (Subject): test\\123 ==> applied pattern: test\123
     * (The first backslash escapes the second one)
     */
    @Test
    public void testHeaderEscape1() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"test\\\\123\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\* ==> applied pattern: test*
     * (The undefined escape sequence \* will be ignored)
     */
    @Test
    public void testHeaderEscape2() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"test\\*\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\123 ==> applied pattern: test123
     * (The undefined escape sequence \1 will be ignored)
     */
    @Test
    public void testHeaderEscape3() {
        doTestHeaderEscapePattern("if not header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"test\\123\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : testSample ==> applied pattern: testSample
     */
    @Test
    public void testHeaderEscape4() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"X-Header-0BackSlash\" \"testSample\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\Sample ==> applied pattern: testSample
     * (The undefined escape sequence \1 will be ignored)
     */
    @Test
    public void testHeaderEscape5() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"X-Header-0BackSlash\" \"test\\Sample\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\\Sample ==> applied pattern: test\Sample
     * (The first backslash escapes the second one)
     */
    @Test
    public void testHeaderEscape6() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"X-Header-1BackSlash\" \"test\\\\Sample\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\\\Sample ==> applied pattern: test\Sample
     * (The accepted escape sequence \\ and The undefined escape sequence \1 will be ignored)
     */
    @Test
    public void testHeaderEscape7() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"X-Header-1BackSlash\" \"test\\\\\\Sample\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\\\\Sample ==> applied pattern: test\\Sample
     * (The accepted escape sequence \\ twice)
     * TODO: This case should be testable after ZCS-616
     */
    public void testHeaderEscape8() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"X-Header-2BackSlash\" \"test\\\\\\\\Sample\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\"123 ==> applied pattern: test"123
     * (The first backslash escapes the second double-quote)
     */
    @Test
    public void testHeaderEscape9() {
        doTestHeaderEscapePattern("if header :comparator \"i;ascii-casemap\" :matches \"X-Header-DoubleQuote\" \"test\\\"Sample\" {"
                + "tag \"list\";}");
    }

    /*
     * pattern in filter : test\\123 ==> applied pattern: test\123
     */
    @Ignore
    public void testReplaceheaderEscape1() {
        doTestReplaceheaderEscapePattern("replaceheader :newvalue \"[replaced]\" :matches \"Subject\" \"test\\\\123\";");
    }

    /*
     * pattern in filter : Sample\Message ==> applied pattern: SampleMessage
     * (The undefined escape sequence \M will be ignored)
     */
    @Test
    public void testNotifyEscape1() {
        String filterScript =
                "require [\"enotify\", \"variables\"];\n"
              + "notify :message \"Sample\\Message\"\n"
              + "  :from \"test1@zimbra.com\"\n"
              + "  \"mailto:test2@zimbra.com?Importance=High&body=sample_body\";";
        doTestNotifyEscape(filterScript, "SampleMessage");
    }

    /*
     * pattern in filter : Sample\\Message ==> applied pattern: Sample\Message
     * (The first backslash escapes the second one)
     */
    @Test
    public void testNotifyEscape2() {
        String filterScript =
                "require [\"enotify\", \"variables\"];\n"
              + "notify :message \"Sample\\\\Message\"\n"
              + "  :from \"test1@zimbra.com\"\n"
              + "  \"mailto:test2@zimbra.com?Importance=High&body=sample_body\";";
        doTestNotifyEscape(filterScript, "Sample\\Message");
    }

    /*
     * pattern in filter : Sample\\\Message ==> applied pattern: Sample\Message
     * (The first backslash escapes the second backslash, the 3rd
     * backslash escapes with M that is an undefined escape sequence \M will
     * be ignored)
     */
    @Test
    public void testNotifyEscape3() {
        String filterScript =
                "require [\"enotify\", \"variables\"];\n"
              + "notify :message \"Sample\\\\\\Message\"\n"
              + "  :from \"test1@zimbra.com\"\n"
              + "  \"mailto:test2@zimbra.com?Importance=High&body=sample_body\";";
        doTestNotifyEscape(filterScript, "Sample\\Message");
    }

    /*
     * pattern in filter : Sample\\\\Message ==> applied pattern: Sample\\Message
     * (The 1st backslash and 3rd backslash escapes the 2nd and 4th backslash respectively)
     */
    @Test
    public void testNotifyEscape4() {
        String filterScript =
                "require [\"enotify\", \"variables\"];\n"
              + "notify :message \"Sample\\\\\\\\Message\"\n"
              + "  :from \"test1@zimbra.com\"\n"
              + "  \"mailto:test2@zimbra.com?Importance=High&body=sample_body\";";
        doTestNotifyEscape(filterScript, "Sample\\\\Message");
    }

    /*
     * pattern in filter : Sample\\\\\Message ==> applied pattern: Sample\\Message
     * (The 1st backslash and 3rd backslash escapes the 2nd and 4th backslash respectively.
     * The 5th is ignored as the undefined escape sequence)
     */
    @Test
    public void testNotifyEscape5() {
        String filterScript =
                "require [\"enotify\", \"variables\"];\n"
              + "notify :message \"Sample\\\\\\\\\\Message\"\n"
              + "  :from \"test1@zimbra.com\"\n"
              + "  \"mailto:test2@zimbra.com?Importance=High&body=sample_body\";";
        doTestNotifyEscape(filterScript, "Sample\\\\Message");
    }

    private String triggeringMsg =
              "To: user123@zimbra.com\n"
            + "From: sender@zimbra.com\n"
            + "Subject: test\\123\n"
            + "X-Header-0BackSlash:  testSample\n"
            + "X-Header-1BackSlash:  test\\Sample\n"
            + "X-Header-2BackSlash:  test\\\\Sample\n"
            + "X-Header-DoubleQuote: test\"Sample\n";

    /*
     * MAIL FROM: <user123@zimbra.com>
     * RCPT TO: <test1@zimbra.com>
     * DATA
     * To: user123@zimbra.com
     * From: sender@zimbra.com
     * Subject: test\123
     * X-Header-0BackSlash:  testSample
     * X-Header-1BaskSlash:  test\Sample
     * X-Header-2BackSlash:  test\\Sample
     * X-Header-DoubleQuote: test"Sample
     */
    public void doTestHeaderEscapePattern(String filterScript) {
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<user123@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript(filterScript);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(triggeringMsg.getBytes(), false),
                    0, account.getName(), env, new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(1, msg.getTags().length);
            Assert.assertEquals("list", msg.getTags()[0]);
        } catch (Exception e) {
            fail("No exception should be thrown " + e);
        }
    }

    public void doTestNotifyEscape(String filterScript, String expectedString) {
        try {
            Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
            RuleManager.clearCachedRules(acct1);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1,
                    new ParsedMessage(triggeringMsg.getBytes(), false), 0,
                    acct1.getName(), env, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);

            Integer item = mbox2.getItemIds(null, Mailbox.ID_FOLDER_INBOX)
                    .getIds(MailItem.Type.MESSAGE).get(0);
            Message notifyMsg = mbox2.getMessageById(null, item);

            // Subject header in the notification message
            Assert.assertEquals(expectedString, notifyMsg.getSubject());
        } catch (Exception e) {
            fail("No exception should be thrown " + e);
        }
    }

    public void doTestEnvelopeEscapePattern(String filterScript) {
        String sampleMsg =
                "from: tim@example.com\n"
              + "to: test@zimbra.com\n"
              + "Subject: Example\n";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<user123@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);

        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(triggeringMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(0, ids.size());
        } catch (Exception e) {
            fail("No exception should be thrown " + e);
        }
    }

    private void doTestReplaceheaderEscapePattern(String filterScript) {
        try {
            Account account = Provisioning.getInstance().get(Key.AccountBy.name, "test1@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, 
                    new ParsedMessage(triggeringMsg.getBytes(), false), 0,
                    account.getName(), null, 
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox.getMessageById(null, itemId);
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header header = enumeration.nextElement();
                if ("Subject".equals(header.getName())) {
                    Assert.assertEquals("[replaced]", header.getValue());
                    break;
                }
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }
}
