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
import org.junit.Ignore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ZTestWatchman;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class HeaderTest {
    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();
    
    private static String sampleMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
            + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
            + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "Received: from localhost (localhost [127.0.0.1])\n"
            + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
            + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
            + "x-priority: 1\n"
            + "X-Spam-score: -5\n"
            + "X-Minus: -abc\n"
            + "from: xyz@example.com\n"
            + "Subject: =?ISO-2022-JP?B?GyRCJDMkcyRLJEEkTxsoQg==?=\n"
            + "to: foo@example.com, baz@example.com\n"
            + "cc: qux@example.com\n";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
       
    }

    @Before
    public void setUp() throws Exception {
       System.out.println(testName.getMethodName());
       Provisioning prov = Provisioning.getInstance();
       prov.createAccount("testHdr@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void testHeaderExistsNonEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"x-priority\"] [\"1\"] { tag \"zimbra\"; }";
        doTest(filterScript, "zimbra");
    }

    @Test
    public void testHeaderExistsEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"x-priority\"] [\"\"] { tag \"zimbra\"; }";
        doTest(filterScript, "zimbra");
    }

    @Test
    public void testHeaderDoesNotExists() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"not-exist\"] [\"1\"] { tag \"zimbra\"; }";
        doTest(filterScript, null);
    }

    @Test
    public void testHeaderDoesNotExistsEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"not-exist\"] [\"\"] { tag \"zimbra\"; }";
        doTest(filterScript, null);
    }

    @Test
    public void testEmptyHeaderEmptyKey() {
        String filterScript = "require [\"tag\", \"flag\"];\n"
                + "if header :contains [\"\"] [\"\"] { tag \"zimbra\"; }";
        doTest(filterScript, null);
    }

    // Due to the negative value test, the filter execution is cancelled;
    // and none of tag commands should be executed.
    @Test
    public void testNumericNegativeValueValue() {
        String filterScript = "require [\"fileinto\", \"tag\", \"flag\", \"log\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                + "if header :value \"ge\" :comparator \"i;ascii-numeric\" "
                + "[\"X-Spam-score\"] [\"500\"] { tag \"XSpamScore\";}"
                + "tag \"Negative\";";
        doTest(filterScript, null);
    }

    // Due to the negative value test, the filter execution is cancelled;
    // and none of tag commands should be executed.
    @Test
    public void testNumericNegativeValueCounts() {
        String filterScript = "require [\"fileinto\", \"tag\", \"flag\", \"log\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                + "if header :count \"ge\" :comparator \"i;ascii-numeric\" "
                + "[\"Received\"] [\"-1\"] { tag \"Received\";}"
                + "tag \"Negative\";";
        doTest(filterScript, null);
    }

    // Due to the negative value test, the filter execution is cancelled;
    // and none of tag commands should be executed.
    @Test
    public void testNumericNegativeValueIs() {
        String filterScript = "require [\"fileinto\", \"tag\", \"flag\", \"log\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                + "if header :is :comparator \"i;ascii-numeric\" "
                + "[\"X-Spam-score\"] [\"-5\"] { tag \"XSpamScore\";}"
                + "tag \"Negative\";";
        doTest(filterScript, null);
    }

    // The "X-Minus: -abc" is not a negative value, but positive infinity as it is just a string.
    @Test
    public void testNumericMinusCharacterValueIs() {
        String filterScript = "require [\"fileinto\", \"tag\", \"flag\", \"log\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                + "if header :is :comparator \"i;ascii-numeric\" "
                + "[\"X-Minus\"] [\"\"] { tag \"Xminus\";}";
        doTest(filterScript, "Xminus");
    }

    // RFC 4790 Section 9.1.1.
    // | strings that do not start with a digit represent positive infinity.
    // Hence the Subject text is treated as positive infinity, and so is an empty string
    @Test
    public void testNumericEmptyIs() {
        String filterScript = "require [\"fileinto\", \"tag\", \"flag\", \"log\", \"relational\", \"comparator-i;ascii-numeric\"];\n"
                + "if header :is :comparator \"i;ascii-numeric\" "
                + "[\"Subject\"] [\"\"] { tag \"subject\";}";
        doTest(filterScript, "subject");
    }

    private void doTest(String filterScript, String expectedResult) {
        try {
            LmtpEnvelope env = setEnvelopeInfo();
            Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage(sampleMsg.getBytes(), false), 0,
                    account.getName(), env,
                    new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(expectedResult, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    private LmtpEnvelope setEnvelopeInfo() {
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<abc@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient1 = new LmtpAddress("<xyz@zimbra.com>", null, null);
        LmtpAddress recipient2 = new LmtpAddress("<uvw@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient1);
        env.addLocalRecipient(recipient2);
        return env;
    }

    public void singleMimePart() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header \"Subject\" \"important\" { flag \"priority\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        String msgContent = "From: testHdr@zimbra.com\nSubject: important";
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(msgContent.getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertTrue(msg.isTagged(FlagInfo.PRIORITY));
    }

    @Test
    public void RFC822Attached() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header :is \"Subject\" \"Attached HTML message\" { flag \"priority\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        InputStream is = getClass().getResourceAsStream("TestFilter-testBodyContains.msg");
        MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(mm, false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertFalse(msg.isTagged(FlagInfo.PRIORITY));
    }

    @Test
    public void fileAttached() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript("if header :contains \"Content-Disposition\" \"attachment.txt\" { flag \"priority\"; }");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        InputStream is = getClass().getResourceAsStream("TestFilter-testBodyContains.msg");
        MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(mm, false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertFalse(msg.isTagged(FlagInfo.PRIORITY));
    }

    /**
     * <ul>
     *  <li> a header with one backslash matches a filter with one escaped backslash (\\)
     *  <li> a header with two backslashes match a filter with two sets of escaped backslash (\\ and \\)
     *  <li> a header with three backslashes match a filter with three sets of escaped backslash (\\ x 3)
     *  <li> a header with four backslashes match a filter with four sets of escaped backslash (\\ x 4)
     *  <li> a header with five backslashes match a filter with five sets of escaped backslash (\\ x 5)
     *  <li> when the nested-if tests the same header (X-HeaderN), the same value matches both outer and inner 'if' condition.
     * </ul>
     * @throws Exception
     */
    @Test
    public void testBackslash() throws Exception {
        String script = "require [\"variables\"];\n"
          + "if header :matches \"X-Header1\" \"sample\\\\pattern\"             { tag \"01\"; }"
          + "if header :matches \"X-Header2\" \"sample\\\\\\\\pattern\"         { tag \"02\"; }"
          + "if header :matches \"X-Header3\" \"sample\\\\\\\\\\\\pattern\"     { tag \"03\"; }"
          + "if header :matches \"X-Header4\" \"sample\\\\\\\\\\\\\\\\pattern\" { tag \"04\"; }"
          + "if header :matches \"X-Header5\" \"sample\\\\\\\\\\\\\\\\\\\\\"    { tag \"05\"; }"
          + "if header :matches \"X-Header1\" \"*\" { set \"var1\" \"${1}\"; if header :matches \"X-Header1\" \"${var1}\" { tag \"11\"; }}"
          + "if header :matches \"X-Header2\" \"*\" { set \"var2\" \"${1}\"; if header :matches \"X-Header2\" \"${var2}\" { tag \"12\"; }}"
          + "if header :matches \"X-Header3\" \"*\" { set \"var3\" \"${1}\"; if header :matches \"X-Header3\" \"${var3}\" { tag \"13\"; }}"
          + "if header :matches \"X-Header4\" \"*\" { set \"var4\" \"${1}\"; if header :matches \"X-Header4\" \"${var4}\" { tag \"14\"; }}"
          + "if header :matches \"X-Header5\" \"*\" { set \"var5\" \"${1}\"; if header :matches \"X-Header5\" \"${var5}\" { tag \"15\"; }}"
          + "if header :comparator \"i;octet\" :matches \"X-Header1\" \"sample\\\\pattern\"             { tag \"21\"; }"
          + "if header :comparator \"i;octet\" :matches \"X-Header2\" \"sample\\\\\\\\pattern\"         { tag \"22\"; }"
          + "if header :comparator \"i;octet\" :matches \"X-Header3\" \"sample\\\\\\\\\\\\pattern\"     { tag \"23\"; }"
          + "if header :comparator \"i;octet\" :matches \"X-Header4\" \"sample\\\\\\\\\\\\\\\\pattern\" { tag \"24\"; }"
          + "if header :comparator \"i;octet\" :matches \"X-Header5\" \"sample\\\\\\\\\\\\\\\\\\\\\"    { tag \"25\"; }"
          + "if header :comparator \"i;octet\" :matches \"X-Header1\" \"*\" { set \"var1\" \"${1}\"; if header :comparator \"i;octet\" :matches \"X-Header1\" \"${var1}\" { tag \"31\"; }}"
          + "if header :comparator \"i;octet\" :matches \"X-Header2\" \"*\" { set \"var2\" \"${1}\"; if header :comparator \"i;octet\" :matches \"X-Header2\" \"${var2}\" { tag \"32\"; }}"
          + "if header :comparator \"i;octet\" :matches \"X-Header3\" \"*\" { set \"var3\" \"${1}\"; if header :comparator \"i;octet\" :matches \"X-Header3\" \"${var3}\" { tag \"33\"; }}"
          + "if header :comparator \"i;octet\" :matches \"X-Header4\" \"*\" { set \"var4\" \"${1}\"; if header :comparator \"i;octet\" :matches \"X-Header4\" \"${var4}\" { tag \"34\"; }}"
          + "if header :comparator \"i;octet\" :matches \"X-Header5\" \"*\" { set \"var5\" \"${1}\"; if header :comparator \"i;octet\" :matches \"X-Header5\" \"${var5}\" { tag \"35\"; }}"
          ;
        String sourceMsg =
            "X-Header1: sample\\pattern\n"
          + "X-Header2: sample\\\\pattern\n"
          + "X-Header3: sample\\\\\\pattern\n"
          + "X-Header4: sample\\\\\\\\pattern\n"
          + "X-Header5: sample\\\\\\\\\\\n";
        try {
            Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
            RuleManager.clearCachedRules(account);
            account.setAdminSieveScriptBefore(script);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(sourceMsg.getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());

            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(20, msg.getTags().length);
            Assert.assertEquals("01", msg.getTags()[0]);
            Assert.assertEquals("02", msg.getTags()[1]);
            Assert.assertEquals("03", msg.getTags()[2]);
            Assert.assertEquals("04", msg.getTags()[3]);
            Assert.assertEquals("05", msg.getTags()[4]);
            Assert.assertEquals("11", msg.getTags()[5]);
            Assert.assertEquals("12", msg.getTags()[6]);
            Assert.assertEquals("13", msg.getTags()[7]);
            Assert.assertEquals("14", msg.getTags()[8]);
            Assert.assertEquals("15", msg.getTags()[9]);
            Assert.assertEquals("21", msg.getTags()[10]);
            Assert.assertEquals("22", msg.getTags()[11]);
            Assert.assertEquals("23", msg.getTags()[12]);
            Assert.assertEquals("24", msg.getTags()[13]);
            Assert.assertEquals("25", msg.getTags()[14]);
            Assert.assertEquals("31", msg.getTags()[15]);
            Assert.assertEquals("32", msg.getTags()[16]);
            Assert.assertEquals("33", msg.getTags()[17]);
            Assert.assertEquals("34", msg.getTags()[18]);
            Assert.assertEquals("35", msg.getTags()[19]);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testHeaderMatchWithItself() throws Exception {
        String script = "require [\"variables\"];\n"
                + "if header :matches \"X-Header1\" \"*\" {"
                + "    if header :matches \"X-Header1\" \"${1}\" {"
                + "        tag \"01\";"
                + "    }"
                + "}"
                + "if header :matches \"X-Header1\" \"*\" {"
                + "    if header :is \"X-Header1\" \"${1}\" {"
                + "        tag \"02\";"
                + "    }"
                + "}"
                + "if header :matches \"X-Header1\" \"*\" {"
                + "    set \"myvar1\" \"${1}\";"
                + "    if header :matches \"X-Header1\" \"${myvar1}\" {"
                + "        tag \"03\";"
                + "    }"
                + "}"
                + "if header :matches \"X-Header1\" \"*\" {"
                + "    set :quotewildcard \"myvar2\" \"${1}\";"
                + "    if string :matches \"sample\\\\\\\\\\\\\\\\pattern\" \"${myvar2}\" {"
                + "        tag \"04\";"
                + "    }"
                + "}"
                ;
        String sourceMsg =
            "X-Header1: sample\\\\pattern\n";
            try {
            Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
            RuleManager.clearCachedRules(account);
            account.setAdminSieveScriptBefore(script);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(sourceMsg.getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());

            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(4, msg.getTags().length);
            Assert.assertEquals("01", msg.getTags()[0]);
            Assert.assertEquals("02", msg.getTags()[1]);
            Assert.assertEquals("03", msg.getTags()[2]);
            Assert.assertEquals("04", msg.getTags()[3]);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testHeaderNamesWithSpaces() throws Exception {
        String script = "require [\"tag\"];\n"
                + "if header :matches \" X-Header1\" \"*\" {"
                + "    tag \"01\";"
                + "}"
                + "if header :matches \"X-Header1 \" \"*\" {"
                + "    tag \"02\";"
                + "}"
                + "if header :matches \" X-Header1 \" \"*\" {"
                + "    tag \"03\";"
                + "}"
                + "if header :matches \"X-He ader1\" \"*\" {"
                + "    tag \"04\";"
                + "}"
                ;
        String sourceMsg =
            "X-Header1: sample\\\\pattern\n";
            try {
            Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
            RuleManager.clearCachedRules(account);
            account.setAdminSieveScriptBefore(script);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(sourceMsg.getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());

            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(0, msg.getTags().length);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMalencodedHeader() throws Exception {
        String script = "if header :matches [\"Subject\"] \"*\" { tag \"321321\"; }";
        String sourceMsg = "Subject: =?ABC?A?GyRCJFskMhsoQg==?=";
        try {
            Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
            RuleManager.clearCachedRules(account);
            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();
            account.setAdminSieveScriptBefore(script);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(sourceMsg.getBytes(), false),
                    0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());

            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(1, msg.getTags().length);
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
        String filterScript = "require [\"tag\"];"
                + "if header :comparator \"i;ascii-numeric\" \"Subject\" \"こんにちは\" {\n"
                + "  tag \"is\";\n"
                + "} else {\n"
                + "  tag \"not is\";\n"
                + "}";
        try {
            LmtpEnvelope env = setEnvelopeInfo();
            Account account = Provisioning.getInstance().getAccountByName("testHdr@zimbra.com");
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();
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
            fail("No exception should be thrown" + e);
        }
    }
    
    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
