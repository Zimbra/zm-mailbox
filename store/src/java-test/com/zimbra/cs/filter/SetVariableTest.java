/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016, 2017 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.mail.Header;

import org.apache.jsieve.exception.SyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.filter.jsieve.SetVariable;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.lmtpserver.LmtpEnvelope;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class SetVariableTest {
    private String filterScript = "";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.createAccount("test1@zimbra.com", "secret", new HashMap<String, Object>());
        Server server = Provisioning.getInstance().getServer(acct);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testSetVar() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);

            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "set \"var\" \"hello\"\n;"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testSetVarAndUseInHeader() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"var\" \"hello\";\n"
                         + "if header :contains \"Subject\" \"${var}\" {\n"
                         + "  tag \"blue\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: hello version 1.0 is out\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("blue", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testSetVarAndUseInAction() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "if header :matches \"Subject\" \"*\"{\n"
                         + "  set \"var\" \"hello\";\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: hello version 1.0 is out\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testReplacement() {
        Map<String, String> variables = new TreeMap<String, String>();
        Map<String, String> testCases = new TreeMap<String, String>();
        // RFC 5229 Section 3. Examples
        variables.put("company", "ACME");
        variables.put("foo", "bar");

        testCases.put("${full}", "");
        testCases.put("${company}", "ACME");
        testCases.put("${BAD${Company}", "${BADACME");
        testCases.put("${President, ${Company} Inc.}", "${President, ACME Inc.}");
        testCases.put("${company", "${company");
        testCases.put("${${company}}", "${ACME}");
        testCases.put("${${${company}}}", "${${ACME}}");
        testCases.put("${company}.${company}.${company}", "ACME.ACME.ACME");
        testCases.put("&%${}!", "&%${}!");
        testCases.put("${doh!}", "${doh!}");
        testCases.put("${fo\\o}",   "bar");   /* ${foo}   */
        testCases.put("${fo\\\\o}", "${fo\\\\o}"); /* First it is converted to ${fo\o}, which is an illegal identifier ==> left verbatim. */
        /* For the following two cases, the backslash ouside the variable name
         * should be handled at the separate place */
        /* testCases.put("\\${foo}",   "bar");   /* ${foo}   */
        /* testCases.put("\\\\${foo}", "\\bar"); /* \\${foo} */
        testCases.put("${foo\\}", "bar");

        // More examples from RFC 5229 Section 3. and RFC 5228 Section 8.1.
        // variable-ref        =  "${" [namespace] variable-name "}"
        // namespace           =  identifier "." *sub-namespace
        // sub-namespace       =  variable-name "."
        // variable-name       =  num-variable / identifier
        // num-variable        =  1*DIGIT
        // identifier          = (ALPHA / "_") *(ALPHA / DIGIT / "_")
        variables.put("a_b", "\u304a\u3057\u3089\u305b");
        variables.put("c_d", "C");
        variables.put("_1", "One");
        variables.put("_23", "twenty three");
        variables.put("uppercase", "upper case");

        testCases.put("${a_b}", "\u304a\u3057\u3089\u305b");
        testCases.put("${c_d}", "C");
        testCases.put("${1}", "${1}");       // Invalid variable name
        testCases.put("${23}", "${23}");     // Not defined
        testCases.put("${123}", "${123}");   // Invalid variable name
        testCases.put("${a_b} ${COMpANY} ${c_d}hao!", "\u304a\u3057\u3089\u305b ACME Chao!");
        testCases.put("${a_b} ${def} ${c_d}hao!", "\u304a\u3057\u3089\u305b  Chao!"); // 1st valid variable, 2nd undefined, 3rd valid variable
        testCases.put("${upperCase}", "upper case");
        testCases.put("${UPPERCASE}", "upper case");
        testCases.put("${uppercase}", "upper case");

        for (Map.Entry<String, String> entry : testCases.entrySet()) {
            String result = FilterUtil.leastGreedyReplace(variables, entry.getKey());
            Assert.assertEquals(entry.getValue(), result);
        }
    }


    @Test
    public void testSetVarWithModifiersValid() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"var\" \"hello\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set :length \"var\" \"hello\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("5", ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set :lower \"var\" \"heLLo\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("hello", ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set :upper \"var\" \"test\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("TEST", ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set :lowerfirst \"var\" \"WORLD\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("wORLD", ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set :UPPERFIRST \"var\" \"example\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("Example", ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"enotify\", \"variables\"];\n"
                         + "set :encodeurl :lower \"body_param\" \"Safe body&evil=evilbody\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${body_param}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("safe+body%26evil%3Devilbody", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testSetVarWithModifiersInValid() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            try {
                filterScript = "require [\"variables\"];\n"
                             + "set \"hello\";\n";
                account.setMailSieveScript(filterScript);
                RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                        new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                        Mailbox.ID_FOLDER_INBOX, true);
            } catch (Exception e) {
                if (e instanceof SyntaxException) {
                    SyntaxException se = (SyntaxException) e;

                    assertTrue(se.getMessage().indexOf("Atleast 2 argument are needed. Found Arguments: [[hello]]") > -1);
                }
            }

            try {
                filterScript = "require [\"variables\"];\n"
                             + "set :lownner \"var\" \"hello\";\n";
                account.setMailSieveScript(filterScript);
                RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                        new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                        Mailbox.ID_FOLDER_INBOX, true);
            } catch (Exception e) {
                if (e instanceof SyntaxException) {
                    SyntaxException se = (SyntaxException) e;
                    assertTrue(se.getMessage().indexOf("Invalid variable modifier:") > -1);
                }
            }

            try {
                filterScript = "require [\"variables\"];\n"
                             + "set :lower \"var\";\n";
                account.setMailSieveScript(filterScript);
                RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                        new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                        Mailbox.ID_FOLDER_INBOX, true);
            } catch (Exception e) {
                if (e instanceof SyntaxException) {
                    SyntaxException se = (SyntaxException) e;
                    assertTrue(se.getMessage().indexOf("Invalid variable modifier:") > -1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testApplyModifiers() {
        String [] modifiers = new String [SetVariable.OPERATIONS_IDX];
        modifiers[SetVariable.getIndex(SetVariable.ALL_LOWER_CASE)] =  SetVariable.ALL_LOWER_CASE;
        modifiers[SetVariable.getIndex(SetVariable.UPPERCASE_FIRST)] = SetVariable.UPPERCASE_FIRST;
        String value = SetVariable.applyModifiers("juMBlEd lETteRS", modifiers);
        Assert.assertEquals("Jumbled letters", value);

        modifiers = new String [SetVariable.OPERATIONS_IDX];
        modifiers[SetVariable.getIndex(SetVariable.STRING_LENGTH)] =  SetVariable.STRING_LENGTH;
        value = SetVariable.applyModifiers("juMBlEd lETteRS", modifiers);
        Assert.assertEquals("15", value);

        modifiers = new String [SetVariable.OPERATIONS_IDX];
        modifiers[SetVariable.getIndex(SetVariable.QUOTE_WILDCARD)] =  SetVariable.QUOTE_WILDCARD;
        modifiers[SetVariable.getIndex(SetVariable.ALL_UPPER_CASE)] =  SetVariable.ALL_UPPER_CASE;
        modifiers[SetVariable.getIndex(SetVariable.LOWERCASE_FIRST)] = SetVariable.LOWERCASE_FIRST;
        value = SetVariable.applyModifiers("j?uMBlEd*lETte\\RS", modifiers);

        Assert.assertEquals("j\\?UMBLED\\*LETTE\\\\RS", value);
    }

    //    set "a" "juMBlEd lETteRS";             => "juMBlEd lETteRS"
    //    set :length "b" "${a}";                => "15"
    //    set :lower "b" "${a}";                 => "jumbled letters"
    //    set :upperfirst "b" "${a}";            => "JuMBlEd lETteRS"
    //    set :upperfirst :lower "b" "${a}";
    @Test
    public void testModifier() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"a\" \"juMBlEd lETteRS\" ;\n"
                         + "set :length \"b\" \"${a}\";\n"
                         + "set :lower \"b\" \"${a}\";\n"
                         + "set :upperfirst \"c\" \"${b}\";"
                         + "set :upperfirst :lower \"d\" \"${c}\"; "
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${d}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("Jumbled letters", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
    
    //    set :upperfirst lowerfirst "b" "${a}";            => "JuMBlEd lETteRS"
    @Test
    public void testModifierSamePrecendenceInSingleSet() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"a\" \"juMBlEd lETteRS\" ;\n"
                         + "set :upperfirst :lowerfirst \"c\" \"${b}\";"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${d}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertTrue(ArrayUtil.getFirstElement(msg.getTags()) == null);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
    
    
    //    set :upperfirst lowerfirst "b" "${a}";            => "JuMBlEd lETteRS"
    @Test
    public void testModifierSamePrecendenceInSingleSet2() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"a\" \"juMBlEd lETteRS\" ;\n"
                         + "set :upperfirst :lower :lowerfirst :lower \"c\" \"${b}\";"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${d}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertTrue(ArrayUtil.getFirstElement(msg.getTags()) == null);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }
    
//  set :upperfirst lowerfirst "b" "${a}";            => "JuMBlEd lETteRS"
  @Test
  public void testModifierDiffPrecendenceInSingleSet() {
      try {
          Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
          RuleManager.clearCachedRules(account);
          Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
          filterScript = "require [\"variables\"];\n"
                       + "set \"a\" \"juMBlEd lETteRS\" ;\n"
                       + "set :upperfirst :lower \"d\" \"${a}\"; "
                       + "if header :matches \"Subject\" \"*\" {\n"
                       + "  tag \"${d}\";\n"
                       + "}\n";
          account.setMailSieveScript(filterScript);
          String raw = "From: sender@zimbra.com\n"
                     + "To: test1@zimbra.com\n"
                     + "Subject: Test\n"
                     + "\n"
                     + "Hello World.";
          List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                  new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                  Mailbox.ID_FOLDER_INBOX, true);
          Message msg = mbox.getMessageById(null, ids.get(0).getId());
          Assert.assertEquals("Jumbled letters", ArrayUtil.getFirstElement(msg.getTags()));
      } catch (Exception e) {
          fail("No exception should be thrown");
      }
  }

    @Test
    public void testVariablesCombo() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            // set "company" "ACME";
            // set "a.b" "おしらせ"; (or any non-ascii characters [\u304a\u3057\u3089\u305b])
            // set "c_d" "C";
            // set "1" "One"; ==> Should be ignored or error [Note 1]
            // set "23" "twenty three"; ==> Should be ignored or error [Note 1]
            // set "combination" "Hello ${company}!!";
            filterScript = "require [\"variables\"];\n"
                         + "set \"company\" \"\u304a\u3057\u3089\u305b\" ;\n"
                         + "set  \"c_d\" \"C\";\n"
                         + "set  \"combination\" \"Hello ${company}!!\"; "
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${combination}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("Hello \u304a\u3057\u3089\u305b!!", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testStringInterpretation() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"a\" \"juMBlEd lETteRS\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${d}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            try {
                List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                        new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                        Mailbox.ID_FOLDER_INBOX, true);

                Message msg = mbox.getMessageById(null, ids.get(0).getId());
            } catch (MailServiceException e) {
                String t = e.getArgumentValue("name");
                assertTrue(e.getCode().equals("mail.INVALID_NAME"));
                assertEquals("", t);
            }

            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"a\" \"juMBlEd lETteRS\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${}\";\n"
                         + "}";
            account.setMailSieveScript(filterScript);
            try {
                List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                        new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                        Mailbox.ID_FOLDER_INBOX, true);
            } catch (MailServiceException e) {
                String t = e.getArgumentValue("name");
                assertTrue(e.getCode().equals("mail.INVALID_NAME"));
                assertEquals("${}", t);
            }
            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"ave\" \"juMBlEd lETteRS\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${ave!}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            try {
                List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                        new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                        Mailbox.ID_FOLDER_INBOX, true);
            } catch (MailServiceException e) {
                String t = e.getArgumentValue("name");
                assertTrue(e.getCode().equals("mail.INVALID_NAME"));
                assertEquals("${ave!}", t);
            }
        } catch (Exception e) {   
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testStringTest() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);   
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            RuleManager.clearCachedRules(account);
            filterScript = "require [\"variables\", \"comparator-i;ascii-numeric\"];\n"
                         + "set :lower :upperfirst \"name\" \"Joe\";\n"
                         + "if string :is :comparator \"i;ascii-numeric\" \"${name}\" [ \"Joe\", \"Hello\", \"User\" ]{\n"
                         + "  tag \"sales-1\";\n"
                         + "}"
                         + "if string :comparator \"i;ascii-numeric\" :is \"${name}\" [ \"Joe\", \"Hello\", \"User\" ]{\n"
                         + "  tag \"sales-2\";\n"
                         + "}"
                         + "if string :is  \"${name}\" [ \"Joe\", \"Hello\", \"User\" ]{\n"
                         + "  tag \"sales-3\";\n"
                         + "}";
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());

            String[] tags = msg.getTags();
            Assert.assertEquals(3, tags.length);
            Assert.assertEquals("sales-1", tags[0]);
            Assert.assertEquals("sales-2", tags[1]);
            Assert.assertEquals("sales-3", tags[2]);
        } catch (Exception e) {   
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testSetMatchVarAndUseInHeader() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "if header :matches [\"To\", \"Cc\"] [\"coyote@**.com\",\"wile@**.com\"]{\n"
                         + "  log \"Match 1 ${1}\";\n"
                         + "  tag \"${2}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: hello version 1.0 is out\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("ACME.Example", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testSetMatchVarAndUseInHeaderSingleOccurrence() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "if header :matches [\"To\", \"Cc\"] [\"coyote@??M?.Example.com\",\"wile@**.com\"]{\n"
                         + "  log \"Match 1 ${1}\";\n"
                         + "  tag \"${3}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: hello version 1.0 is out\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("E", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Ignore
    public void testSetMatchVarAndUseInHeaderSingleOccurrenceReplaceHeader() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                + "replaceheader :newname \"X-New-Header\" :newvalue \"[new]0: ${0}, 1: ${1}\" "
                + ":comparator \"i;ascii-casemap\" :matches \"Subject\" \"test C-91 replac?header\";\n"
                + "if header :matches \"X-New-Header\" \"*\" {\n"
                + "log \"${1}\";\n"
                + "}\n";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: test C-91 replaceheader\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("[new]0: test C-91 replaceheader, 1: e", ArrayUtil.getFirstElement(msg.getMimeMessage().getHeader("X-New-Header")));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVariables_Address_HeaderList_KeyList() {
        try {
            String raw = "From: user2@ykomiyam.local\n"
                + "To: coyote@ACME.Example.COM\n"
                + "Subject: hello version 1.0 is out\n"
                + "\n"
                + "Hello World.";
            filterScript = "require [\"variables\", \"tag\"];\n"
                + "set \"from_address_One\" \"user1@ykomiyam.local\";"
                + "set \"from_address_Two\" \"user2@ykomiyam.local\";"
                + "set \"from_header_name\" \"From\";"
                + "set \"to_header_name\" \"To\";"
                + "if address :comparator \"i;ascii-casemap\" :is \"From\" [\"${from_address_One}\",\"${from_address_Two}\"] {"
                + "  tag \"KeyListTag\";\n" + "}"
                + "if address :comparator \"i;ascii-casemap\" :is [\"${from_header_name}\",\"${to_header_name}\"] \"user2@ykomiyam.local\" {"
                + "  tag \"HeaderListTag\";\n" + "}";

            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String[] tags = msg.getTags();
            Assert.assertEquals(2, tags.length);
            Assert.assertEquals("KeyListTag", tags[0]);
            Assert.assertEquals("HeaderListTag", tags[1]);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVariables_Envelope_KeyList() {
        String sampleMsg =
            "from: tim@example.com\n"
          + "to: test@zimbra.com\n"
          + "Subject: Example\n";
        String filterScript = "require [\"envelope\", \"variables\", \"tag\"];\n"
            + "set \"from_address\" \"tim@example.com\";"
            + "set \"to_address\" \"test1@zimbra.com\";" + "set \"from_header_name\" \"From\";"
            + "if envelope :all :is \"from\" [\"to_address\", \"${from_address}\"] {\n"
            + "    tag \"KeyListTag\";" + "}";

        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test1@zimbra.com>", null, null);
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
            String[] tags = msg.getTags();
            Assert.assertEquals(1, tags.length);
            Assert.assertEquals("KeyListTag", tags[0]);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVariables_Header_HeaderNames() {
        try {
           String sampleMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
                + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
                + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "Received: from localhost (localhost [127.0.0.1])\n"
                + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
                + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "x-priority: 1\n"
                + "from: tim@zimbra.com\n"
                + "Subject: =?ISO-2022-JP?B?GyRCJDMkcyRLJEEkTxsoQg==?=\n"
                + "to: test1@zimbra.com, test2@zimbra.com\n";
            String filterScript = "require [\"variables\", \"tag\"];\n"
                + "set \"header_name\" \"x-priority\";"
                + "if header :contains [\"${header_name}\"] [\"\"] { tag \"zimbra\"; }";

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<tim@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient1 = new LmtpAddress("<test1@zimbra.com>", null, null);
            LmtpAddress recipient2 = new LmtpAddress("<test2@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient1);
            env.addLocalRecipient(recipient2);
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
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
            Assert.assertEquals("zimbra", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVariables_String_Source() {
        try {
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            filterScript = "require [\"variables\", \"tag\"];\n"
                + "set :lower :upperfirst \"name\" \"Joe\";\n"
                + "if string :is :comparator \"i;ascii-casemap\" \"{name}\" [\"{name}\", \"Bob\"]{\n"
                + "  tag \"SourceTag\";\n" + "}";

            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String[] tags = msg.getTags();
            Assert.assertEquals(1, tags.length);
            Assert.assertEquals("SourceTag", tags[0]);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Ignore
    public void testVariables_AddHeader_FieldName_Value() {
        try {
          String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
                + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
                + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "Received: from localhost (localhost [127.0.0.1])\n"
                + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
                + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "from: test2@zimbra.com\n"
                + "Subject: example\n"
                + "to: test1@zimbra.com\n";
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                + "set \"header_name\" \"my-new-header\";"
                + "set \"header_value\" \"my-new-header-value\";"
                + "if header :contains \"Subject\" \"example\" {\n"
                + " addheader \"${header_name}\" \"${header_value}\" \r\n" + "  ;\n" + "}";

            Account acct = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct);
            RuleManager.clearCachedRules(acct);
            acct.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct.getName(),
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

    @Ignore
    public void testVariables_DeleteHeader_FieldName() {
        try {
            String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
                + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
                + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "Received: from localhost (localhost [127.0.0.1])\n"
                + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
                + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "X-Test-Header: test1\n"
                + "X-Test-Header: test2\n"
                + "X-Test-Header: test3\n"
                + "from: test2@zimbra.com\n"
                + "Subject: example\n"
                + "to: test1@zimbra.com\n";
            String filterScript = "require [\"editheader\", \"variables\"];\n"
                + "set \"header_name\" \"X-Test-Header\";"
                + "set \"header_value\" \"test2\";"
                + "deleteheader :is \"${header_name}\" \"${header_value}\" \r\n" + "  ;\n";

            Account acct1 = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
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

    @Ignore
    public void testVariables_ReplaceHeader_FieldName() {
        try {
            String sampleBaseMsg = "Received: from edge01e.zimbra.com ([127.0.0.1])\n"
                + "\tby localhost (edge01e.zimbra.com [127.0.0.1]) (amavisd-new, port 10032)\n"
                + "\twith ESMTP id DN6rfD1RkHD7; Fri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "Received: from localhost (localhost [127.0.0.1])\n"
                + "\tby edge01e.zimbra.com (Postfix) with ESMTP id 9245B13575C;\n"
                + "\tFri, 24 Jun 2016 01:45:31 -0400 (EDT)\n"
                + "X-Test-Header: test1\n"
                + "X-Test-Header: test2\n"
                + "X-Test-Header: test3\n"
                + "from: test2@zimbra.com\n"
                + "Subject: example\n"
                + "to: test1@zimbra.com\n";
            String filterScript = "require [\"editheader\"];\n"
                + "set \"header_name\" \"Subject\";"
                + "set \"header_value\" \"example\";"
                + "set \"new_value\" \"my subject\";"
                + "replaceheader :newvalue \"${new_value}\" :contains \"${header_name}\" \"${header_value}\" \r\n"
                + "  ;\n";

            Account acct1 = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
            RuleManager.clearCachedRules(acct1);
            acct1.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox1), mbox1, new ParsedMessage(
                            sampleBaseMsg.getBytes(), false), 0, acct1.getName(),
                            null, new DeliveryContext(),
                            Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox1.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message message = mbox1.getMessageById(null, itemId);
            String newSubject = "";
            for (Enumeration<Header> enumeration = message.getMimeMessage().getAllHeaders(); enumeration.hasMoreElements();) {
                Header temp = enumeration.nextElement();
                if ("Subject".equals(temp.getName())) {
                    newSubject = temp.getValue();
                    break;
                }
            }
            Assert.assertEquals("my subject", newSubject);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSetMatchVarAndUseInHeader2() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);

            Map<String, Object> attrs = Maps.newHashMap();
            attrs = Maps.newHashMap();
            Provisioning.getInstance().getServer(account).modify(attrs);

            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "set \"var\" \"hello\";\n"
                         + "if header :matches \"Subject\" \"${var}\" {\n"
                         + "  tag \"${var} world!\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: hello\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("hello world!", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown" + e.getStackTrace());
        }
    }

    @Test
    public void testSetMatchVarAndFileInto() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"fileinto\", \"log\", \"variables\"];\n"
                         + "set \"sub\" \"test\";\n"
                         + "if header :contains \"subject\" \"${sub}\" {\n"
                         + "  log \"Subject has test\";\n"
                         + "  fileinto \"${sub}\";\n"
                         + "}";

            System.out.println(filterScript);
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@in.telligent.com\n" 
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: test\n" + "\n" + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                       new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                       Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Folder folder  = mbox.getFolderById(null, msg.getFolderId());
            Assert.assertEquals("test", folder.getName());

            RuleManager.clearCachedRules(account);
            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();

            filterScript = "require [\"fileinto\", \"log\", \"variables\"];\n"
                         + "set \"sub\" \"test\";\n"
                         + "if header :contains \"subject\" \"Hello ${sub}\" {\n"
                         + "  log \"Subject has test\";\n"
                         + "  fileinto \"${sub}\";\n"
                         + "}";

            System.out.println(filterScript);
            account.setMailSieveScript(filterScript);
            raw = "From: sender@in.telligent.com\n" 
                + "To: coyote@ACME.Example.COM\n"
                + "Subject: Hello test\n"
                + "\n"
                + "Hello World.";
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);
                Assert.assertEquals(1, ids.size());
            msg = mbox.getMessageById(null, ids.get(0).getId());
            folder  = mbox.getFolderById(null, msg.getFolderId());
            Assert.assertEquals("test", folder.getName());

            RuleManager.clearCachedRules(account);
            account.unsetAdminSieveScriptBefore();
            account.unsetMailSieveScript();
            account.unsetAdminSieveScriptAfter();
            filterScript = "require [\"fileinto\", \"variables\"];\n"
                    + "set \"var5\" \"var test 5\";\n"
                    + "if allof (header :matches [\"subject\"] \"${var5}*\") {\n"
                    + "  fileinto \"${var5}\";\n"
                    + "  set \"var6\" \"var test 6\";\n"
                    + "  if allof (header :matches [\"subject\"] \"*${var6}\") {\n"
                    + "    fileinto \"${1}\";\n"
                    + "  }\n"
                    + "}";

            System.out.println(filterScript);
            account.setMailSieveScript(filterScript);
            raw = "From: sender@in.telligent.com\n"
                    + "To: coyote@ACME.Example.COM\n"
                    + "Subject: var test 5 var test 6\n"
                    + "\n"
                    + "Hello World.";
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            msg = mbox.getMessageById(null, ids.get(0).getId());
            folder  = mbox.getFolderById(null, msg.getFolderId());
            Assert.assertEquals("var test 5", folder.getName());
        } catch (Exception e) {
            fail("No exception should be thrown");
        }

    }

    @Ignore
    public void testSetMatchVarWithEnvelope() {
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"log\", \"variables\", \"envelope\" ];\n"
                         + "if envelope :matches [\"To\"] \"*\" {\n"
                         + "  set \"rcptto\" \"${1}\";\n"
                         + "  log \":matches ==> ${1}\";\n"
                         + "  log \"variables ==> ${rcptto}\";\n"
                         + "  tag \"${rcptto}\";\n"
                         + "}";

            account.setMailSieveScript(filterScript);
            account.setMail("test@zimbra.com");
            String raw = "From: sender@in.telligent.com\n" 
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(raw.getBytes(), false), 0, account.getName(), env, new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Folder folder  = mbox.getFolderById(null, msg.getFolderId());
            Assert.assertEquals("coyote@ACME.Example.COM", ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            fail("No exception should be thrown");
        }

    }

    @Ignore
    public void testSetMatchVarMultiLineWithEnvelope() {
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"log\", \"variables\", \"envelope\" ];\n"
                         + "if envelope :matches [\"To\"] \"*\" {"
                         + "  set \"rcptto\" \"${1}\";\n"
                         + "}\n"
                         + "if header :matches [\"From\"] \"*\" {"
                         + "  set \"fromheader\" \"${1}\";\n"
                         + "}\n"
                         + "if header :matches [\"Subject\"] \"*\" {"
                         + "  set \"subjectheader\" \"${1}\";\n"
                         + "}\n"
                         + "set \"bodyparam\" text: # This is a comment\r\n"
                         + "Message delivered to  ${rcptto}\n"
                         + "Sent : ${fromheader}\n"
                         + "Subject : ${subjectheader} \n"
                         + ".\r\n"
                         + ";\n"
                         +"log \"${bodyparam}\"; \n";
 
            System.out.println(filterScript);
            account.setMailSieveScript(filterScript);
            account.setMail("test@zimbra.com");
            String raw = "From: sender@in.telligent.com\n" 
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(raw.getBytes(), false), 0, account.getName(), env, new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);
               
        } catch (Exception e) {
            fail("No exception should be thrown");
        }

    }
 
    @Ignore
    public void testSetMatchVarMultiLineWithEnvelope2() {
        LmtpEnvelope env = new LmtpEnvelope();
        LmtpAddress sender = new LmtpAddress("<tim@example.com>", new String[] { "BODY", "SIZE" }, null);
        LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
        env.setSender(sender);
        env.addLocalRecipient(recipient);
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
               
            filterScript = "require [\"log\", \"variables\", \"envelope\" ];\n"
                         + "if envelope :matches [\"To\"] \"*\" {"
                         + "  set \"rcptto\" \"${1}\";\n"
                         + "}\n"
                         + "if header :matches [\"Date\"] \"*\" {"
                         + "  set \"dateheader\" \"${1}\";\n"
                         + "}\n"
                         + "if header :matches [\"From\"] \"*\" {"
                         + "  set \"fromheader\" \"${1}\";\n"
                         + "}\n"
                         + "if header :matches [\"Subject\"] \"*\" {"
                         + "  set \"subjectheader\" \"${1}\";\n"
                         + "}\n"
                         + "if anyof(not envelope :is [\"From\"] \"\" ){\n"
                         + "  set \"subjectparam\" \"Notification\";\n"
                         + "  set \"bodyparam\" text: # This is a comment\r\n"
                         + "Message delivered to  ${rcptto}\n"
                         + "Sent : ${fromheader}\n"
                         + "Subject : ${subjectheader} \n"
                         + ".\r\n"
                         + "  ;\n"
                         + "  log \"${bodyparam}\"; \n"
                         + "  log \"subjectparam ==> [${subjectparam}]\";\n"
                         + "  log \"rcptto ==> [${rcptto}]\";\n"
                         + "  log \"mailfrom ==> [${mailfrom}]\";\n"
                         + "}\n";

            System.out.println(filterScript);
            account.setMailSieveScript(filterScript);
            account.setMail("test@zimbra.com");
            String raw = "From: sender@in.telligent.com\n" 
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: test\n" + "\n" + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(raw.getBytes(), false), 0, account.getName(), env, new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);

        } catch (Exception e) {
            fail("No exception should be thrown");
        }

    }
    
    @Test
    public void testSetMatchVarAndUseInHeaderForAddress() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n" 
                         + "if address :comparator \"i;ascii-casemap\" :matches \"To\" \"coyote@**.com\"{\n"
                         + "  tag \"${2}\";\n"
                         + "}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@in.telligent.com\n" 
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: hello version 1.0 is out\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("ACME.Example", ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /*
     * Verify that any unassigned Matched Variables should replaced by the "".
     */
    @Test
    public void testSetMatchVarOutOfRange() {
        try {
            filterScript = "require [\"variables\", \"editheader\"];\n"
                    + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"*C-*baa*123*\" { \r\n"
                    + "  addheader \"X-New-Header1\" \"${0}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${1}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${2}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${3}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${4}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${5}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${6}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${7}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${8}\";\r\n"
                    + "  addheader \"X-New-Header1\" \"${9}\";\r\n"
                    + "}\n"
                    + "if envelope :matches :comparator \"i;ascii-casemap\" [\"From\"] \"*yo*@*COM\" { \r\n"
                    + "  addheader \"X-New-Header2\" \"${0}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${1}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${2}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${3}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${4}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${5}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${6}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${7}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${8}\";\r\n"
                    + "  addheader \"X-New-Header2\" \"${9}\";\r\n"
                    + "}\n"
                    + "if address :matches :comparator \"i;ascii-casemap\" [\"To\"] \"*t@zimbra.com\" { \r\n"
                    + "  addheader \"X-New-Header3\" \"${0}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${1}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${2}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${3}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${4}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${5}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${6}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${7}\";\r\n"
                    + "  addheader \"X-New-Header3\" \"${8}\";\r\n"
                    + "  addheader \"X-New-Header\" \"${9}\";\r\n"
                    + "}\n"
                    ;
            String raw = "To: test@zimbra.com\n"
                    + "From: coyote@ACME.Example.COM\n"
                    + "Subject: test C-51 abc sample foo bar hoge piyo 123 456 789 sieve test\n"
                    + "\n"
                    + "Hello world.";
            String expectedHeaderValue1[] = {
                    "test C-51 abc sample foo bar hoge piyo 123 456 789 sieve test",
                    "test",
                    "51 abc sample foo",
                    "hoge piyo",
                    "456 789 sieve test"
            };
            String expectedHeaderValue2[] = {
                    "coyote@ACME.Example.COM",
                    "co",
                    "te",
                    "ACME.Example."
            };
            String expectedHeaderValue3[] = {
                    "test@zimbra.com",
                    "tes"
            };

            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Map<String, Object> attrs = Maps.newHashMap();
            attrs = Maps.newHashMap();
            Provisioning.getInstance().getServer(account).modify(attrs);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<coyote@ACME.Example.COM>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            RuleManager.clearCachedRules(account);

            account.setMailSieveScript(filterScript);
            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage(raw.getBytes(), false),
                    0, account.getName(), env, new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            Message mdnMsg = mbox.getMessageById(null, itemId);
            for (Enumeration<Header> e = mdnMsg.getMimeMessage().getAllHeaders(); e.hasMoreElements();) {
                Header temp = e.nextElement();
                check("X-New-Header1", temp, expectedHeaderValue1);
                check("X-New-Header2", temp, expectedHeaderValue2);
                check("X-New-Header3", temp, expectedHeaderValue3);
            }
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    private void check(String headerName, Header temp, String[] expectedHeaderValue) {
        if (headerName.equalsIgnoreCase(temp.getName())) {
            boolean match = false;
            String value = temp.getValue();
            if (value == null || value.length() == 0) {
                match = true;
            } else {
                for (int i = 0; i < expectedHeaderValue.length; i++) {
                    if (expectedHeaderValue[i].equalsIgnoreCase(value)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) Assert.assertEquals("hoge", value);
            Assert.assertTrue(match);
        }
    }

    /*
     * Verify that the Matched Variabes keeps strings from the most
     * recently evaluated successful match of type ":matches".
     * In this test script, "the most recently evaluated successful" value
     * for the replaceheader command is the value of "Subject".  And if
     * the value of the Subject is empty, the ${1} should be replaced by
     * the empty string.
     */
    @Ignore
    public void testSetMatchVarEmptyMatch() {
        try {
            filterScript = "require [\"variables\", \"editheader\"];\n"
                    /* Replaceheader */
                    + "if header :matches \"Date\" \"*\" { \r\n"
                    + "  set \"dateheader\" \"${1}\";\r\n"
                    + "}\r\n"
                    + "if exists \"Subject\" {\r\n"
                    + "  replaceheader :newvalue \"[Replace Subject]${1}\" :matches \"Subject\" \"*\";\r\n"
                    + "}\n"
                    /* Test header */
                    + "if header :matches \"Date\" \"*\" { \r\n"
                    + "  set \"dateheader\" \"${1}\";\r\n"
                    + "}\r\n"
                    + "if header :matches :comparator \"i;ascii-casemap\" [\"X-Header\"] \"*\" {\r\n"
                    + "  tag \"tag1${1}\";\n"
                    + "}\n"
                    /* Test envelope */
                    + "if address :matches :comparator \"i;ascii-casemap\" [\"To\"] \"*t@zimbra.com\" { \r\n"
                    + "  set \"toheader\" \"${1}\";\r\n"
                    + "}\r\n"
                    + "if envelope :matches :comparator \"i;ascii-casemap\" [\"From\"] \"*\" {\r\n"
                    + "  tag \"tag1${1}\";\n"
                    + "}\n"
                    /* Test string */
                    + "if header :matches \"Date\" \"*\" { \r\n"
                    + "  set \"dateheader\" \"${1}\";\r\n"
                    + "}\r\n"
                    + "if string :matches :comparator \"i;ascii-casemap\" [\"X-String\"] \"*\" {\r\n"
                    + "  tag \"tag2${1}\";\n"
                    + "}\n"
                    ;
            String raw = "From: \n"
                    + "To: coyote@ACME.Example.COM\n"
                    + "Date: Thu, 08 Dec 2016 07:10:48 +0900\n"
                    + "Subject: \n"
                    + "X-String: \n"
                    + "X-Header: \n"
                    + "\n"
                    + "Hello world.";

            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Map<String, Object> attrs = Maps.newHashMap();
            attrs = Maps.newHashMap();
            Provisioning.getInstance().getServer(account).modify(attrs);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            RuleManager.clearCachedRules(account);
            account.setMailSieveScript(filterScript);

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<xyz@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox, new ParsedMessage(raw.getBytes(), false),
                    0, account.getName(), env, new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Integer itemId = mbox.getItemIds(null, Mailbox.ID_FOLDER_INBOX).getIds(MailItem.Type.MESSAGE).get(0);
            String value = "";
            int totalNewHeader = 0;
            Message msg = mbox.getMessageById(null, itemId);
            Assert.assertEquals("[Replace Subject]", msg.getSubject());
            String[] tags = msg.getTags();
            Assert.assertEquals(2, tags.length);
            Assert.assertEquals("tag1", tags[0]);
            Assert.assertEquals("tag2X-String", tags[1]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    /**
     * To verify that the variable ${name} is evaluated only once.
     *  Before fix: when the match pattern *${dollar}{sample}* was defined,
     *   first, the ${dollar} part was replaced to the $, then it was treated
     *   as a parameter name ${sample}, and replaced to the test_text.  As a result,
     *   the Subject was mistakenly compared to the pattern *test text*.
     *  After fix: the Subject is compared to the *${sample}*.
     */
    @Test
    public void testDollar() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\"];\n"
                         + "set \"dollar\" \"$\";\n"
                         + "set \"sample\" \"test text\";\n"
                         + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"*${dollar}{sample}*\" {\n"
                         + "  tag \"=${1}=\";\n"
                         + "  tag \"=${2}=\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: abc${sample}xyz test text 123\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(2, msg.getTags().length);
            Assert.assertEquals("=abc=", msg.getTags()[0]);
            Assert.assertEquals("=xyz test text 123=", msg.getTags()[1]);
            Assert.assertNotSame("=abctest textxyz =", msg.getTags()[0]);
            Assert.assertNotSame("= 123", msg.getTags()[1]);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    /**
     *  When the match pattern ${dollar} was defined as '$' and used in the
     *  header/address/envelope test, this '$' should be treated as a string,
     *  not a part of the wild-card.
     */
    @Test
    public void testDollar2() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            filterScript = "require [\"variables\", \"envelope\"];\n"
                         + "set \"dollar\" \"$\";\n"
                         + "set \"val\" \"xyz\";\n"
                         + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"${dollar}${val}\" {\n"
                         + "  tag \"header-matches\";\n"
                         + "}\n"
                         + "if header :is :comparator \"i;ascii-casemap\" \"Subject\" \"${dollar}${val}\" {\n"
                         + "  tag \"header-is\";\n"
                         + "}\n"
                         + "if header :contains :comparator \"i;ascii-casemap\" \"Subject\" \"${dollar}${val}\" {\n"
                         + "  tag \"header-contains\";\n"
                         + "}\n"
                         + "if address :all :matches :comparator \"i;ascii-casemap\" \"To\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"address-matches\";\n"
                         + "}\n"
                         + "if address :all :is :comparator \"i;ascii-casemap\" \"To\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"address-is\";\n"
                         + "}\n"
                         + "if address :all :contains :comparator \"i;ascii-casemap\" \"To\" \"${dollar}${val}\" {\n"
                         + "  tag \"address-contains\";\n"
                         + "}"
                         + "if envelope :all :matches :comparator \"i;ascii-casemap\" \"From\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"envelope-matches\";\n"
                         + "}\n"
                         + "if envelope :all :is :comparator \"i;ascii-casemap\" \"From\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"envelope-is\";\n"
                         + "}\n"
                         + "if envelope :all :contains :comparator \"i;ascii-casemap\" \"From\" \"${dollar}${val}\" {\n"
                         + "  tag \"envelope-contains\";\n"
                         + "}"
                         + "if not header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"${dollar}${val}\" {\n"
                         + "  tag \"header-not-matches\";\n"
                         + "}\n"
                         + "if not header :is :comparator \"i;ascii-casemap\" \"Subject\" \"${dollar}${val}\" {\n"
                         + "  tag \"header-not-is\";\n"
                         + "}\n"
                         + "if not header :contains :comparator \"i;ascii-casemap\" \"Subject\" \"${dollar}${val}\" {\n"
                         + "  tag \"header-not-contains\";\n"
                         + "}\n"
                         + "if not address :all :matches :comparator \"i;ascii-casemap\" \"To\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"address-not-matches\";\n"
                         + "}\n"
                         + "if not address :all :is :comparator \"i;ascii-casemap\" \"To\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"address-not-is\";\n"
                         + "}\n"
                         + "if not address :all :contains :comparator \"i;ascii-casemap\" \"To\" \"${dollar}${val}\" {\n"
                         + "  tag \"address-not-contains\";\n"
                         + "}"
                         + "if not envelope :all :matches :comparator \"i;ascii-casemap\" \"From\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"envelope-not-matches\";\n"
                         + "}\n"
                         + "if not envelope :all :is :comparator \"i;ascii-casemap\" \"From\" \"${dollar}${val}@zimbra.com\" {\n"
                         + "  tag \"envelope-not-is\";\n"
                         + "}\n"
                         + "if not envelope :all :contains :comparator \"i;ascii-casemap\" \"From\" \"${dollar}${val}\" {\n"
                         + "  tag \"envelope-not-contains\";\n"
                         + "}\n"
                         + "if header :contains \"X-Header1\" \"${dollar}\" {\n"
                         + "  tag \"dollar\";\n"
                         + "}\n"
                         + "if header :contains \"X-Header2\" \"${dollar}{\" {\n"
                         + "  tag \"dollar-opening-brace\";\n"
                         + "}\n"
                         + "if header :contains \"X-Header3\" \"${dollar}}\" {\n"
                         + "  tag \"dollar-closing-brace\";\n"
                         + "}\n"
                         + "if header :contains \"X-Header4\" \"${dollar}\" {\n"
                         + "  tag \"dollar-middle\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: \"$xyz\"@zimbra.com\n"
                       + "Subject: $xyz\n"
                       + "X-Header1: $\n"
                       + "X-Header2: ${\n"
                       + "X-Header3: $}\n"
                       + "X-Header4: abc$def\n"
                       + "\n"
                       + "Hello World.";

            LmtpEnvelope env = new LmtpEnvelope();
            LmtpAddress sender = new LmtpAddress("<$xyz@zimbra.com>", new String[] { "BODY", "SIZE" }, null);
            LmtpAddress recipient = new LmtpAddress("<test@zimbra.com>", null, null);
            env.setSender(sender);
            env.addLocalRecipient(recipient);

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), env,
                    new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(13, msg.getTags().length);
            Assert.assertEquals("header-matches", msg.getTags()[0]);
            Assert.assertEquals("header-is", msg.getTags()[1]);
            Assert.assertEquals("header-contains", msg.getTags()[2]);
            Assert.assertEquals("address-matches", msg.getTags()[3]);
            Assert.assertEquals("address-is", msg.getTags()[4]);
            Assert.assertEquals("address-contains", msg.getTags()[5]);
            Assert.assertEquals("envelope-matches", msg.getTags()[6]);
            Assert.assertEquals("envelope-is", msg.getTags()[7]);
            Assert.assertEquals("envelope-contains", msg.getTags()[8]);
            Assert.assertEquals("dollar", msg.getTags()[9]);
            Assert.assertEquals("dollar-opening-brace", msg.getTags()[10]);
            Assert.assertEquals("dollar-closing-brace", msg.getTags()[11]);
            Assert.assertEquals("dollar-middle", msg.getTags()[12]);
        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Ignore
    public void testString() {
        try {
            filterScript =
                    "set \"dollar\" \"$\";\n"
                  + "set \"sample\" \"test text\";\n"
                  + "set \"hello\" \"world\";\n"
                  + "set \"number\" \"7\";\n"
                  + "# set \"abc${dollar}{sample}\" in source\n"
                  + "if string :matches :comparator \"i;ascii-casemap\" \"abc${dollar}{sample}\" \"*${sample}\" {\n"
                  + "  addheader :last \"X-New-Header-1\" \"${1}\";\n"
                  + "  addheader :last \"X-New-Header-2\" \"${2}\";\n"
                  + "}\n"
                  + "if string :contains :comparator \"i;ascii-casemap\" \"abc${dollar}{sample}\" \"${sample}\" {\n"
                  + "  addheader :last \"X-New-Header-3\" \"contains\";\n"
                  + "}\n"
                  + "if string :is :comparator \"i;ascii-casemap\" \"abc${dollar}{sample}\" \"abc${sample}\" {\n"
                  + "  addheader :last \"X-New-Header-4\" \"is\";\n"
                  + "}\n"
                  + "# set \"${dollar}{sample}\" in source\n"
                  + "if string :matches :comparator \"i;ascii-casemap\" \"${dollar}{sample}\" \"*${sample}\" {\n"
                  + "  addheader :last \"X-New-Header-5\" \"${1}\";\n"
                  + "  addheader :last \"X-New-Header-6\" \"${2}\";\n"
                  + "}\n"
                  + "if string :contains :comparator \"i;ascii-casemap\" \"${dollar}{sample}\" \"${sample}\" {\n"
                  + "  addheader :last \"X-New-Header-7\" \"contains\";\n"
                  + "}\n"
                  + "if string :is :comparator \"i;ascii-casemap\" \"${dollar}{sample}\" \"${sample}\" {\n"
                  + "  addheader :last \"X-New-Header-8\" \"is\";\n"
                  + "}"
                  + "if string :matches :comparator \"i;ascii-casemap\" \"${dollar}{sample}\" \"${dollar}*\" {\n"
                  + "  addheader :last \"X-New-Header-9\" \"${1}\";\n"
                  + "}"
                  + "if string :contains :comparator \"i;ascii-casemap\" \"${dollar}{sample}\" \"{sample}\" {\n"
                  + "  addheader :last \"X-New-Header-10\" \"contains\";\n"
                  + "}"
                  + "if string :is :comparator \"i;ascii-casemap\" \"${dollar}{sample}\" \"${dollar}{sample}\" {\n"
                  + "  addheader :last \"X-New-Header-11\" \"is\";\n"
                  + "}"
                  // String comparison of "test text" ?? "M:middle of alphabet list"
                  + "if string :value \"gt\" :comparator \"i;ascii-casemap\" \"${sample}\" \"M\" {"
                  + "  addheader :last \"X-New-Header-12\" \"string test value gt\";\n"
                  + "}"
                  + "if string :value \"ge\" :comparator \"i;ascii-casemap\" \"${sample}\" \"M\" {"
                  + "  addheader :last \"X-New-Header-13\" \"string test value ge\";\n"
                  + "}"
                  + "if string :value \"lt\" :comparator \"i;ascii-casemap\" \"${sample}\" \"M\" {"
                  + "  addheader :last \"X-New-Header-14\" \"string test value lt\";\n"
                  + "}"
                  + "if string :value \"le\" :comparator \"i;ascii-casemap\" \"${sample}\" \"M\" {"
                  + "  addheader :last \"X-NewHeader-15\" \"string test value le\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"${sample}\" \"M\" {"
                  + "  addheader :last \"X-New-Header-16\" \"string test value eq\";\n"
                  + "}"
                  + "if string :value \"ne\" :comparator \"i;ascii-casemap\" \"${sample}\" \"M\" {"
                  + "  addheader :last \"X-New-Header-17\" \"string test value ne\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test Text\" {"
                  + "  addheader :last \"X-New-Header-18\" \"string test value (case insensitive) eq\";\n"
                  + "}"
                  // String comparison of "M" ?? "test text"
                  + "if string :value \"gt\" :comparator \"i;ascii-casemap\" \"M\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-19\" \"string test value gt\";\n"
                  + "}"
                  + "if string :value \"ge\" :comparator \"i;ascii-casemap\" \"M\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-20\" \"string test value ge\";\n"
                  + "}"
                  + "if string :value \"lt\" :comparator \"i;ascii-casemap\" \"M\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-21\" \"string test value lt\";\n"
                  + "}"
                  + "if string :value \"le\" :comparator \"i;ascii-casemap\" \"M\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-22\" \"string test value le\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"M\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-23\" \"string test value eq\";\n"
                  + "}"
                  + "if string :value \"ne\" :comparator \"i;ascii-casemap\" \"M\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-24\" \"string test value ne\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"Test Text\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-25\" \"string test value (case insensitive) eq\";\n"
                  + "}"
                  // String comparison of "test text" ?? "Test"
                  + "if string :value \"gt\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test\" {"
                  + "  addheader :last \"X-New-Header-26\" \"string test value gt\";\n"
                  + "}"
                  + "if string :value \"ge\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test\" {"
                  + "  addheader :last \"X-New-Header-27\" \"string test value ge\";\n"
                  + "}"
                  + "if string :value \"lt\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test\" {"
                  + "  addheader :last \"X-New-Header-28\" \"string test value lt\";\n"
                  + "}"
                  + "if string :value \"le\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test\" {"
                  + "  addheader :last \"X-NewHeader-29\" \"string test value le\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test\" {"
                  + "  addheader :last \"X-New-Header-30\" \"string test value eq\";\n"
                  + "}"
                  + "if string :value \"ne\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test\" {"
                  + "  addheader :last \"X-New-Header-31\" \"string test value ne\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"${sample}\" \"Test Text\" {"
                  + "  addheader :last \"X-New-Header-32\" \"string test value (case insensitive) eq\";\n"
                  + "}"
                  // String comparison of "Test" ?? "test text"
                  + "if string :value \"gt\" :comparator \"i;ascii-casemap\" \"Test\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-33\" \"string test value gt\";\n"
                  + "}"
                  + "if string :value \"ge\" :comparator \"i;ascii-casemap\" \"Test\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-34\" \"string test value ge\";\n"
                  + "}"
                  + "if string :value \"lt\" :comparator \"i;ascii-casemap\" \"Test\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-35\" \"string test value lt\";\n"
                  + "}"
                  + "if string :value \"le\" :comparator \"i;ascii-casemap\" \"Test\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-36\" \"string test value le\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"Test\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-37\" \"string test value eq\";\n"
                  + "}"
                  + "if string :value \"ne\" :comparator \"i;ascii-casemap\" \"Test\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-38\" \"string test value ne\";\n"
                  + "}"
                  + "if string :value \"eq\" :comparator \"i;ascii-casemap\" \"Test Text\" \"${sample}\" {"
                  + "  addheader :last \"X-New-Header-39\" \"string test value (case insensitive) eq\";\n"
                  + "}"
                  // :count operator
                  + "if string :count \"eq\" :comparator \"i;ascii-numeric\" \"${undefined}\" \"0\" {"
                  + "  addheader :last \"X-New-Header-40\" \"string test count eq empty\";\n"
                  + "}"
                  + "if string :count \"eq\" :comparator \"i;ascii-numeric\" [\"${sample}\"] \"1\" {"
                  + "  addheader :last \"X-New-Header-41\" \"string test count eq one\";\n"
                  + "}"
                  + "if string :count \"eq\" :comparator \"i;ascii-numeric\" [\"${sample}\",\"${hello}\"] \"2\" {"
                  + "  addheader :last \"X-New-Header-42\" \"string test count eq two\";\n"
                  + "}"
                  + "if string :count \"eq\" :comparator \"i;ascii-numeric\" [\"${sample}\",\"${unknown}\"] [\"3\",\"2\",\"1\"] {"
                  + "  addheader :last \"X-New-Header-43\" \"string test count eq one or two\";\n"
                  + "}"
                  // Default comparator
                  + "if string :value \"gt\" \"${number}\" \"1\" {"
                  + "  addheader :last \"X-New-Header-44\" \"string test value numeric gt\";\n"
                  + "}"
                  + "if string :count \"eq\" \"${number}\" \"1\" {"
                  + "  addheader :last \"X-New-Header-45\" \"string test count numeric eq\";\n"
                  + "}"
                  // Compare the empty string with ascii-numeric comparator
                  + "if string :value \"eq\" :comparator \"i;ascii-numeric\" \"${sample}\" \"${undefined}\" {"
                  + "  addheader :last \"X-New-Header-46\" \"string test value numeric eq positive infinity\";\n"
                  + "}"
                  + "if string :count \"lt\" \"${number}\" \"\" {"
                  + "  addheader :last \"X-New-Header-47\" \"string test count numeric lt positive infinity\";\n"
                  + "}"
                  ;
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            RuleManager.clearCachedRules(account);
            account.setMailSieveScript(filterScript);
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(
                    new OperationContext(mbox), mbox,
                    new ParsedMessage("From: test@zimbra.com\nSubject: hello".getBytes(), false), 0,
                    account.getName(), null, new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String value[] = null;
            value = msg.getMimeMessage().getHeader("X-New-Header-9");
            Assert.assertEquals("{sample}", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-10");
            Assert.assertEquals("contains", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-11");
            Assert.assertEquals("is", value[0]);

            value = msg.getMimeMessage().getHeader("X-New-Header-12");
            Assert.assertEquals("string test value gt", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-13");
            Assert.assertEquals("string test value ge", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-17");
            Assert.assertEquals("string test value ne", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-18");
            Assert.assertEquals("string test value (case insensitive) eq", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-21");
            Assert.assertEquals("string test value lt", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-22");
            Assert.assertEquals("string test value le", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-24");
            Assert.assertEquals("string test value ne", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-25");
            Assert.assertEquals("string test value (case insensitive) eq", value[0]);

            value = msg.getMimeMessage().getHeader("X-New-Header-26");
            Assert.assertEquals("string test value gt", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-27");
            Assert.assertEquals("string test value ge", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-31");
            Assert.assertEquals("string test value ne", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-32");
            Assert.assertEquals("string test value (case insensitive) eq", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-35");
            Assert.assertEquals("string test value lt", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-36");
            Assert.assertEquals("string test value le", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-38");
            Assert.assertEquals("string test value ne", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-39");
            Assert.assertEquals("string test value (case insensitive) eq", value[0]);

            value = msg.getMimeMessage().getHeader("X-New-Header-40");
            Assert.assertEquals("string test count eq empty", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-41");
            Assert.assertEquals("string test count eq one", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-42");
            Assert.assertEquals("string test count eq two", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-43");
            Assert.assertEquals("string test count eq one or two", value[0]);

            value = msg.getMimeMessage().getHeader("X-New-Header-44");
            Assert.assertEquals("string test value numeric gt", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-45");
            Assert.assertEquals("string test count numeric eq", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-46");
            Assert.assertEquals("string test value numeric eq positive infinity", value[0]);
            value = msg.getMimeMessage().getHeader("X-New-Header-47");
            Assert.assertEquals("string test count numeric lt positive infinity", value[0]);
        } catch (Exception e) {
            fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSetVarNameWithDigits() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);

            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "set \"var2\" \"hello\"\n;"
                         + "set \"var_2\" \"hellovar_2\"\n;"
                         + "set \"_var2\" \"hello_var2\"\n;"
                         + "set \"_var2_ad\" \"hello_var2_ad\"\n;"
                         + "set \"_var2_\" \"hello_var2_\"\n;"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var2}\";\n"
                         + "  tag \"${var_2}\";\n"
                         + "  tag \"${_var2}\";\n"
                         + "  tag \"${_var2_ad}\";\n"
                         + "  tag \"${_var2_}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("hello", msg.getTags()[0]);
            Assert.assertEquals("hellovar_2", msg.getTags()[1]);
            Assert.assertEquals("hello_var2", msg.getTags()[2]);
            Assert.assertEquals("hello_var2_ad", msg.getTags()[3]);
            Assert.assertEquals("hello_var2_", msg.getTags()[4]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNumericVarNames() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);

            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "set  \"1\" \"One\";"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var2}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String tag = msg.getTags()[0];
            // 1 is not a valid identifier. So, no tag will be set because of a Sieve syntax Exception
            // while setting the variable and we will get an ArrayIndexOutOfBoundsException while fetching
            // the tag.
            fail("Should not reach here");

        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVarNamesWithDot() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);

            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require [\"variables\"];\n"
                         + "set  \"a.b\" \"${a}\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var2}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String tag = msg.getTags()[0];
            // a.b is not a valid identifier. So, no tag will be set because of a Sieve syntax Exception
            // while setting the variable and we will get an ArrayIndexOutOfBoundsException while fetching
            // the tag.
            fail("Should not reach here");

        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testVarIndexWithLeadingZeroes() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";\n"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"*C*a*c*ple*oo *ge*yo 123 *56*89 sie*e*t\" { "
                       + "tag \"${001}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test C-51 abc sample foo bar hoge piyo 123 456 789 sieve test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("test", msg.getTags()[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNegativeVarIndex() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"*C*a*c*ple*oo *ge*yo 123 *56*89 sie*e*t\" { "
                       + "tag \"${-1}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test C-51 abc sample foo bar hoge piyo 123 456 789 sieve test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String tag = msg.getTags()[0];
            // ${-1} is not valid variable index. So, no tag will be set because of a Sieve syntax Exception
            fail("Should not reach here");

        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testOutofRangeVarIndex() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"*C*a*c*ple*oo *ge*yo 123 *56*89 sie*e*t\" { "
                       + "tag \"${10}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test C-51 abc sample foo bar hoge piyo 123 456 789 sieve test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String tag = msg.getTags()[0];
            // ${10} is not valid variable index. So, no tag will be set because of a Sieve syntax Exception
            fail("Should not reach here");

        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testOutofRangeVarIndexWithLeadingZeroes() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"*C*a*c*ple*oo *ge*yo 123 *56*89 sie*e*t\" { "
                       + "tag \"${0010}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test C-51 abc sample foo bar hoge piyo 123 456 789 sieve test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String tag = msg.getTags()[0];
            // ${0010} is not valid variable index. So, no tag will be set because of a Sieve syntax Exception
            fail("Should not reach here");

        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testWildCardGreedyMatch() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";\n"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"sample*test\" { "
                       + "tag \"${1}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: sample abc test 123 test ABC test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("abc test 123 test ABC", msg.getTags()[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMultipleWildCardMatch() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";\n"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"[*] *\" { "
                       + "tag \"${1}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: [acme-users] [fwd] version 1.0 is out";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("acme-users", msg.getTags()[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMultipleWildCardMatch2() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";\n"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"[*] *\" { "
                       + "tag \"${2}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: [acme-users] [fwd] version 1.0 is out";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("[fwd] version 1.0 is out", msg.getTags()[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testMultipleWildCardMatch3() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "require \"variables\";\n"
                       + "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"test*sample*\" { "
                       + "tag \"${2}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test sample message abc sample foo";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals("message abc sample foo", msg.getTags()[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testStringNumericNegativeTest() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            
            filterScript = "require [\"variables\", \"tag\", \"comparator-i;ascii-numeric\"];\n"
                    + "set \"negative\" \"-123\";\n"
                    + "if string :is :comparator \"i;ascii-numeric\" \"${negative}\" \"-123\" {\n"
                    + "  tag \"negative\";\n"
                    + "}"
                    + "tag \"123\";";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@in.telligent.com\n" 
                       + "To: coyote@ACME.Example.COM\n"
                       + "Subject: hello version 1.0 is out\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));

        } catch (Exception e) {
            fail("No exception should be thrown");
        }
    }

    @Ignore
    public void testNonExistingVarIndexWithLeadingZeroes() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"t*t*\" { "
                       + "addheader :last \"X-New-Header\" \"${009}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test 123";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String[] value = msg.getMimeMessage().getHeader("X-New-Header");
            Assert.assertEquals("", value[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Ignore
    public void testNonExistingVarIndexWithLeadingZeroesForQuestionMark() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            filterScript = "if header :matches :comparator \"i;ascii-casemap\" \"Subject\" \"t??t\" { "
                       + "addheader :last \"X-New-Header\" \"${005}\";}";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            String[] value = msg.getMimeMessage().getHeader("X-New-Header");
            Assert.assertEquals("", value[0]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testNoRequireDeclaration() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            // No "variable" require
            filterScript = "set \"var\" \"hello\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${var}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: Test\n"
                       + "\n"
                       + "Hello World.";
            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            // No "enotify" require (for :encodeurl modifier)
            filterScript = "require [\"variables\"];\n"
                         + "set :encodeurl :lower \"body_param\" \"Safe body&evil=evilbody\";\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  tag \"${body_param}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));

            RuleManager.clearCachedRules(account);
            // No "variables" require, no ${..} replacement 
            //    ==> ${1} should be appeared as is.
            filterScript = "require [\"fileinto\"];\n"
                         + "if header :matches \"Subject\" \"*\" {\n"
                         + "  fileinto \"${1}\";\n"
                         + "}\n";
            account.setMailSieveScript(filterScript);
            ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            msg = mbox.getMessageById(null, ids.get(0).getId());
            Folder folder = mbox.getFolderById(null, msg.getFolderId());
            Assert.assertEquals("${1}", folder.getName());
        } catch (Exception e) {
            fail("No exception should be thrown: " + e);
        }
    }

    /*
     * The ascii-numeric comparator should be looked up in the list of the "require".
     */
    @Test
    public void testMissingComparatorNumericDeclaration() {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            // Default match type :is is used.
            // No "comparator-i;ascii-numeric" capability text in the require command
            filterScript = "require \"variables\";"
                    + "set \"state\" \"1\";\n"
                    + "if string :comparator \"i;ascii-numeric\" \"${state}\" \"1\" {\n"
                    + "  tag \"is\";\n"
                    + "} else {\n"
                    + "  tag \"not is\";\n"
                    + "}\n";

            account.setMailSieveScript(filterScript);
            String raw = "From: sender@zimbra.com\n"
                       + "To: test1@zimbra.com\n"
                       + "Subject: test";

            List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                    new ParsedMessage(raw.getBytes(), false), 0, account.getName(), new DeliveryContext(),
                    Mailbox.ID_FOLDER_INBOX, true);
            Assert.assertEquals(1, ids.size());
            Message msg = mbox.getMessageById(null, ids.get(0).getId());
            Assert.assertEquals(null, ArrayUtil.getFirstElement(msg.getTags()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }
}
