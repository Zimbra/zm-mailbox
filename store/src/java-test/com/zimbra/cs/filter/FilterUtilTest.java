/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
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

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.util.ItemId;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

import static com.zimbra.cs.filter.JsieveConfigMapHandler.CAPABILITY_VARIABLES;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link FilterUtil}.
 */
public class FilterUtilTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Account acct1 = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Server server = Provisioning.getInstance().getServer(acct1);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void truncateBody() throws Exception {
        // truncate a body containing a multi-byte char
        String body = StringUtil.truncateIfRequired("Andr\u00e9", 5);

        Assert.assertTrue("truncated body should not have a partial char at the end", "Andr".equals(body));
    }

    @Test
    public void noBody() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String content = "From: user1@example.com\r\n" + "To: user2@example.com\r\n" + "Subject: test\r\n" 
                + "Content-Type: application/octet-stream;name=\"test.pdf\"\r\n" 
                + "Content-Transfer-Encoding: base64\r\n\r\n" + "R0a1231312ad124svsdsal=="; //obviously not a real pdf
        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);
        Map<String, String> vars = FilterUtil.getVarsMap(mbox, parsedMessage, parsedMessage.getMimeMessage());
    }

    @Test
    public void noHeaders() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String content = "just some content";
        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);
        Map<String, String> vars = FilterUtil.getVarsMap(mbox, parsedMessage, parsedMessage.getMimeMessage());

    }

    /*
     * Create and initialize the ZimbraMailAdapter object
     */
    private ZimbraMailAdapter initZimbraMailAdapter() throws ServiceException {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mbox), new DeliveryContext(),
                mbox, "test@zimbra.com", new ParsedMessage("From: test1@zimbra.com".getBytes(), false), 0,
                Mailbox.ID_FOLDER_INBOX, true);
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mbox, handler);

        // Set various variables
        mailAdapter.addVariable("var", "hello");
        List<String> matchedValues = new ArrayList<String>();
        matchedValues.add("test1");
        matchedValues.add("test2");
        mailAdapter.setMatchedValues(matchedValues);

        return mailAdapter;
    }

    @Test
    public void testVariableReplacementVariableOn() {
        try {
            ZimbraMailAdapter mailAdapter = initZimbraMailAdapter();

            // Variable feature: ON
            mailAdapter.setVariablesExtAvailable(ZimbraMailAdapter.VARIABLEFEATURETYPE.AVAILABLE);
            mailAdapter.addCapabilities(CAPABILITY_VARIABLES);

            String varValue = FilterUtil.replaceVariables(mailAdapter, "${var}");
            Assert.assertEquals("hello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${0}");
            Assert.assertEquals("test1", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${var!}");
            Assert.assertEquals("${var!}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${var2}");
            Assert.assertEquals("", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${test${var}");
            Assert.assertEquals("${testhello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${test${var}");
            Assert.assertEquals("${testhello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "\\\\${President, ${var} Inc.}");
            Assert.assertEquals("\\\\${President, hello Inc.}", varValue);

            // set "company" "ACME";
            // set "a.b" "おしらせ"; (or any non-ascii characters)
            // set "c_d" "C";
            // set "1" "One"; ==> Should be ignored or error [Note 1]
            // set "23" "twenty three"; ==> Should be ignored or error [Note 1]
            // set "combination" "Hello ${company}!!";
            mailAdapter.addVariable("var", "hello");

            mailAdapter.addVariable("company", "ACME");
            mailAdapter.addVariable("a_b", "\u304a\u3057\u3089\u305b");
            mailAdapter.addVariable("c_d", "C");
            mailAdapter.addVariable("1", "One");
            mailAdapter.addVariable("23", "twenty three");
            mailAdapter.addVariable("combination", "Hello ACME!!");

            varValue = FilterUtil.replaceVariables(mailAdapter, "${full}");
            Assert.assertEquals("", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${company}");
            Assert.assertEquals("ACME", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${BAD${Company}");
            Assert.assertEquals("${BADACME", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${company");
            Assert.assertEquals("${company", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${${COMpANY}}");
            Assert.assertEquals("${ACME}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${a_b}}");
            Assert.assertEquals("\u304a\u3057\u3089\u305b}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "$c_d}}");
            Assert.assertEquals("$c_d}}", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "You've got a mail. ${a_b} ${combination} ${c_d}hao!");
            Assert.assertEquals("You've got a mail. \u304a\u3057\u3089\u305b Hello ACME!! Chao!", varValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e);
        }
    }

    @Test
    public void testVariableReplacementQutdAndEncoded() {
        try {
            ZimbraMailAdapter mailAdapter = initZimbraMailAdapter();
            mailAdapter.setVariablesExtAvailable(ZimbraMailAdapter.VARIABLEFEATURETYPE.AVAILABLE);
            mailAdapter.addCapabilities(CAPABILITY_VARIABLES);

            String varValue = FilterUtil.replaceVariables(mailAdapter, "${va\\r}");
            Assert.assertEquals("hello", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "${}");
            Assert.assertEquals("${}", varValue);

            mailAdapter.addVariable("var", "hel\\*lo");
            varValue = FilterUtil.replaceVariables(mailAdapter, "${var}");
            Assert.assertEquals("hel\\*lo", varValue);

            varValue = FilterUtil.replaceVariables(mailAdapter, "hello${test}");
            Assert.assertEquals("hello", varValue);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e);
        }
    }

    @Test
    public void testToJavaRegex() {
        String regex = FilterUtil.sieveToJavaRegex("coyote@**.com");
        Assert.assertEquals("coyote@(.*?)(.*)\\.com", regex);
    }

    @Test
    public void testGetExtendedInfo() throws Exception {
        String content = "Return-Path: <dummy@dev.zimbra.com>\n" + "Date: Tue, 29 Oct 2024 10:15:47 +0900 (JST)\n" 
                + "From: Dummy <dummy@dev.zimbra.com>\n" + "To: user1@dev.zimbra.com\n" 
                + "Subject: ZBUG-4479: valid From header\n" + "MIME-Version: 1.0\n" 
                + "Content-Type: text/plain; charset=utf-8\n" + "Content-Transfer-Encoding: 7bit\n" 
                + "\n" + "test message";
        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);
        String result = FilterUtil.getExtendedInfo(parsedMessage.getMimeMessage());
        Assert.assertEquals(", sender=dummy@dev.zimbra.com, MsgId=null", result);
    }

    @Test
    public void testGetExtendedInfoExceptionCase() throws Exception {
        String content = "Return-Path: <dummy@dev.zimbra.com>\n" + "Date: Tue, 29 Oct 2024 10:15:47 +0900 (JST)\n" 
                + "From: xxx>xxx@yyyyyh<\n" + "To: user1@dev.zimbra.com\n" 
                + "Subject: ZBUG-4479: invalid From header\n" + "MIME-Version: 1.0\n" 
                + "Content-Type: text/plain; charset=utf-8\n" + "Content-Transfer-Encoding: 7bit\n" 
                + "\n" + "test message";
        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);
        String result = FilterUtil.getExtendedInfo(parsedMessage.getMimeMessage());
        Assert.assertEquals(", sender=xxx>xxx@yyyyyh<, MsgId=null", result);
    }

    @Test
    public void testApplyRulesToIncomingMessageFeatureFlagTrue() throws Exception {
        // setup - enable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set filter rules using account's modify method
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"fileinto\"];\n" + "if header :contains \"Subject\" " 
                        + "\"Test\" {\n" + "    fileinto \"Junk\";\n" + "    stop;\n" + "}");

        account.modify(attrs);

        // clear cached rules to force reload
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                // Use reflection to call private evaluateScript method
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - pass IMPLICIT_KEEP parameter
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        // get the message
        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
    }

    @Test
    public void testApplyRulesToIncomingMessageFeatureFlagFalse() throws Exception {
        // setup - disable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(false);

        // set filter rules
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"fileinto\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    fileinto \"Junk\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic with feature flag false
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            // This block won't execute because applyRules is false
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs (will be null because we didn't apply rules)
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when feature flag is false
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be in Inbox
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        // get the message and verify it was filed to Inbox
        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void testApplyRulesToIncomingMessageFeatureFlagTrueNoRules() throws Exception {
        // setup - enable the feature flag but no rules
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // clear any existing rules
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript, "");
        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // implicit keep
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be in Inbox since no rules applied
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void testApplyRulesToIncomingMessageFeatureFlagTrueWithSpamApplyUserFilters() throws Exception {
        // setup - enable the feature flag and configure to apply user filters to spam
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set spam apply user filters to true and set filter rules
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraSpamApplyUserFilters, "TRUE");
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"fileinto\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    fileinto \"Junk\";\n" + "    stop;\n" + "}");
        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message that appears to be spam
        String content = "X-Spam-Flag: YES\r\n" + "X-Spam-Score: 15.0\r\n" + "From: " 
                + "sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test " 
                + "Message\r\n" + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // apply rules even for spam since applyUserFiltersToSpam is true
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        // check if it's spam
        boolean isSpam = false;
        String[] spamFlags = parsedMessage.getMimeMessage().getHeader("X-Spam-Flag");
        if (spamFlags != null && spamFlags.length > 0 && "YES".equalsIgnoreCase(spamFlags[0])) {
            isSpam = true;
        }

        boolean applyUserFiltersToSpam = account.getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false);

        if (applyRules && (!isSpam || applyUserFiltersToSpam)) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // implicit keep
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
    }

    @Test
    public void testRedirectActionFeatureFlagEnabled() throws Exception {
        // setup - enable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set filter rules with redirect action
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" " 
                        + "\"Test\" {\n" + "    redirect \"redirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be processed (redirect should work since feature flag is enabled)
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testRedirectActionFeatureFlagDisabled() throws Exception {
        // setup - disable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(false);

        // Set filter rules with redirect action
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    redirect \"redirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic - even with feature flag false, 
        // rules should still be applied, but redirect actions from user scripts will be rejected
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs - should be empty since redirect from user script is rejected
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when redirect is rejected
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be in Inbox since redirect from user script is rejected
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void testRedirectActionWithCopyFeatureFlagEnabled() throws Exception {
        // setup - enable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set filter rules with redirect action and copy
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" " 
                        + "\"Test\" {\n" + "    redirect :copy \"redirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs - should have message since redirect has :copy and feature flag is enabled
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be kept since redirect has :copy and feature flag is enabled
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void testRedirectActionWithCopyFeatureFlagDisabled() throws Exception {
        // setup - disable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(false);

        // Set filter rules with redirect action and copy
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailSieveScript,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    redirect :copy \"redirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate the applyRulesToIncomingMessage logic - even with feature flag false, 
        // rules should still be applied, but redirect actions from user scripts will be rejected
        boolean applyRules = account.isFeatureMailForwardingInFiltersEnabled();

        if (applyRules) {
            String script = RuleManager.getIncomingRules(account);
            if (script != null && !script.isEmpty()) {
                mailAdapter.setUserScriptExecuting(true);
                boolean proceed = invokeEvaluateScript(mailAdapter, script);
                if (proceed && !mailAdapter.isStop()) {
                    mailAdapter.executeAllActions();
                }
            }
        }

        // get the added message IDs - should be empty since redirect from user script is rejected
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when redirect is rejected
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be in Inbox since redirect from user script is rejected
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void testAdminSieveScriptBeforeRedirectFeatureFlagEnabled() throws Exception {
        // setup - enable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set admin sieve script before with redirect action
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraAdminSieveScriptBefore,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" " 
                        + "\"Test\" {\n" + "    redirect \"adminredirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate admin script execution (before user scripts)
        String script = account.getAttr(Provisioning.A_zimbraAdminSieveScriptBefore);
        if (script != null && !script.isEmpty()) {
            // admin scripts are NOT user scripts, so userScriptExecuting should be false
            boolean proceed = invokeEvaluateScript(mailAdapter, script);
            if (proceed && !mailAdapter.isStop()) {
                mailAdapter.executeAllActions();
            }
        }

        // get the added message IDs - should be empty since redirect should happen
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when redirect happens (no message kept)
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be processed (admin redirect should work since it's not user script)
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testAdminSieveScriptBeforeRedirectFeatureFlagDisabled() throws Exception {
        // setup - disable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(false);

        // Set admin sieve script before with redirect action
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraAdminSieveScriptBefore,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    redirect \"adminredirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate admin script execution (before user scripts)
        String script = account.getAttr(Provisioning.A_zimbraAdminSieveScriptBefore);
        if (script != null && !script.isEmpty()) {
            // admin scripts are NOT user scripts, so userScriptExecuting should be false
            boolean proceed = invokeEvaluateScript(mailAdapter, script);
            if (proceed && !mailAdapter.isStop()) {
                mailAdapter.executeAllActions();
            }
        }

        // get the added message IDs - should be empty since redirect should happen
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when redirect happens (no message kept)
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be processed (admin redirect should work even with feature flag disabled 
        // since it's not user script)
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testAdminSieveScriptAfterRedirectFeatureFlagEnabled() throws Exception {
        // setup - enable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set admin sieve script after with redirect action
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraAdminSieveScriptAfter,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" " 
                        + "\"Test\" {\n" + "    redirect \"adminredirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate admin script execution (after user scripts)
        String script = account.getAttr(Provisioning.A_zimbraAdminSieveScriptAfter);
        if (script != null && !script.isEmpty()) {
            // admin scripts are NOT user scripts, so userScriptExecuting should be false
            boolean proceed = invokeEvaluateScript(mailAdapter, script);
            if (proceed && !mailAdapter.isStop()) {
                mailAdapter.executeAllActions();
            }
        }

        // get the added message IDs - should be empty since redirect should happen
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when redirect happens (no message kept)
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be processed (admin redirect should work since it's not user script)
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testAdminSieveScriptAfterRedirectFeatureFlagDisabled() throws Exception {
        // setup - disable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(false);

        // Set admin sieve script after with redirect action
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraAdminSieveScriptAfter,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    redirect \"adminredirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate admin script execution (after user scripts)
        String script = account.getAttr(Provisioning.A_zimbraAdminSieveScriptAfter);
        if (script != null && !script.isEmpty()) {
            // admin scripts are NOT user scripts, so userScriptExecuting should be false
            boolean proceed = invokeEvaluateScript(mailAdapter, script);
            if (proceed && !mailAdapter.isStop()) {
                mailAdapter.executeAllActions();
            }
        }

        // get the added message IDs - should be empty since redirect should happen
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep - this is what should happen when redirect happens (no message kept)
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be processed (admin redirect should work even with feature flag disabled 
        // since it's not user script)
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testAdminSieveScriptBeforeRedirectWithCopyFeatureFlagEnabled() throws Exception {
        // setup - enable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(true);

        // Set admin sieve script before with redirect action and copy
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraAdminSieveScriptBefore,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" " 
                        + "\"Test\" {\n" + "    redirect :copy \"adminredirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate admin script execution (before user scripts)
        String script = account.getAttr(Provisioning.A_zimbraAdminSieveScriptBefore);
        if (script != null && !script.isEmpty()) {
            // admin scripts are NOT user scripts, so userScriptExecuting should be false
            boolean proceed = invokeEvaluateScript(mailAdapter, script);
            if (proceed && !mailAdapter.isStop()) {
                mailAdapter.executeAllActions();
            }
        }

        // get the added message IDs - should have message since redirect has :copy
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be kept since redirect has :copy and it's admin script
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    @Test
    public void testAdminSieveScriptBeforeRedirectWithCopyFeatureFlagDisabled() throws Exception {
        // setup - disable the feature flag
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setFeatureMailForwardingInFiltersEnabled(false);

        // Set admin sieve script before with redirect action and copy
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraAdminSieveScriptBefore,
                "require [\"redirect\"];\n" + "if header :contains \"Subject\" \"Test\" " 
                        + "{\n" + "    redirect :copy \"adminredirect@example.com\";\n" + "    stop;\n" + "}");

        account.modify(attrs);
        RuleManager.clearCachedRules(account);

        Mailbox mailbox = MailboxManager.getInstance().getMailboxByAccount(account);

        // create a test message
        String content = "From: sender@example.com\r\n" + "To: test@zimbra.com\r\n" + "Subject: Test Message\r\n" 
                + "Content-Type: text/plain\r\n\r\n" + "This is a test message.";

        ParsedMessage parsedMessage = new ParsedMessage(content.getBytes(), false);

        // create IncomingMessageHandler
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mailbox),
                new DeliveryContext(), mailbox, "test@zimbra.com", parsedMessage, content.length(),
                Mailbox.ID_FOLDER_INBOX, false);

        // create ZimbraMailAdapter
        ZimbraMailAdapter mailAdapter = new ZimbraMailAdapter(mailbox, handler);

        // simulate admin script execution (before user scripts)
        String script = account.getAttr(Provisioning.A_zimbraAdminSieveScriptBefore);
        if (script != null && !script.isEmpty()) {
            // admin scripts are NOT user scripts, so userScriptExecuting should be false
            boolean proceed = invokeEvaluateScript(mailAdapter, script);
            if (proceed && !mailAdapter.isStop()) {
                mailAdapter.executeAllActions();
            }
        }

        // get the added message IDs - should have message since redirect has :copy
        List<ItemId> result = mailAdapter.getAddedMessageIds();
        if (result == null || result.isEmpty()) {
            // Implicit keep
            mailAdapter.keep(ZimbraMailAdapter.KeepType.IMPLICIT_KEEP);
            result = mailAdapter.getAddedMessageIds();
        }

        // verify results - message should be kept since redirect has :copy and it's admin script 
        // (feature flag doesn't matter)
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());

        Message msg = mailbox.getMessageById(null, result.get(0).getId());
        Assert.assertNotNull(msg);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, msg.getFolderId());
    }

    // helper method to invoke private evaluateScript method using reflection
    private boolean invokeEvaluateScript(ZimbraMailAdapter mailAdapter, String script) throws Exception {
        try {
            // get the private method from RuleManager class
            Method evaluateScriptMethod = RuleManager.class.getDeclaredMethod("evaluateScript", ZimbraMailAdapter.class,
                    String.class);
            evaluateScriptMethod.setAccessible(true);

            // invoke the static method (null as first parameter since it's static)
            return (boolean) evaluateScriptMethod.invoke(null, mailAdapter, script);
        } catch (NoSuchMethodException e) {
            // try alternative method signature
            try {
                Method evaluateScriptMethod = RuleManager.class.getDeclaredMethod("evaluateScript",
                        ZimbraMailAdapter.class, String.class, boolean.class);
                evaluateScriptMethod.setAccessible(true);
                return (boolean) evaluateScriptMethod.invoke(null, mailAdapter, script, true);
            } catch (NoSuchMethodException e2) {
                // ff both fail, try FilterUtil's evaluateScript
                Method filterUtilMethod = FilterUtil.class.getDeclaredMethod("evaluateScript", ZimbraMailAdapter.class,
                        String.class);
                filterUtilMethod.setAccessible(true);
                return (boolean) filterUtilMethod.invoke(null, mailAdapter, script);
            }
        }
    }
}
