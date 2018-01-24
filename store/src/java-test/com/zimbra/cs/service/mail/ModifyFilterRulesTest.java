/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.filter.SoapToSieve;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;


/**
 * @author zimbra
 *
 */
public class ModifyFilterRulesTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MockProvisioning prov = new MockProvisioning();
        Provisioning.setInstance(prov);

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testBug82649_BlankFilter() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);

        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "Junk");
        filteraction.addElement(MailConstants.E_ACTION_STOP);
        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
            fail("This test is expected to throw exception. ");
        } catch (ServiceException e) {
            String expected = "invalid request: missing required attribute: value";
            assertTrue(e.getMessage().indexOf(expected)  != -1);
            assertNotNull(e);
        }
    }

    @Test
    public void testBug71036_NonNestedIfSingleRule() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);

        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "Junk");
        filteraction.addElement(MailConstants.E_ACTION_STOP);
        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest.addAttribute(MailConstants.A_VALUE, "important");

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            fail("This test is expected not to throw exception. ");
        }

        String expectedScript = "require [" + SoapToSieve.requireCommon + "];\n\n";
        expectedScript += "# Test1\n";
        expectedScript += "if anyof (header :contains [\"subject\"] \"important\") {\n";
        expectedScript += "    fileinto \"Junk\";\n";
        expectedScript += "    stop;\n";
        expectedScript += "}\n";


        //ZimbraLog.filter.info(acct.getMailSieveScript());
        //ZimbraLog.filter.info(expectedScript);
        assertEquals(expectedScript, acct.getMailSieveScript());

    }

    @Test
    public void testBug71036_MultiNestedIfSingleRule() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);

        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");

        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest.addAttribute(MailConstants.A_VALUE, "important");

        Element nestedRule = rule.addElement(MailConstants.E_NESTED_RULE);

        Element childFilterTests = nestedRule.addElement(MailConstants.E_FILTER_TESTS);
        childFilterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element childheaderTest2 = childFilterTests.addElement(MailConstants.E_HEADER_TEST);
        childheaderTest2.addAttribute(MailConstants.A_HEADER, "subject");
        childheaderTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
        childheaderTest2.addAttribute(MailConstants.A_VALUE, "confifential");

        Element nestedRule2 = nestedRule.addElement(MailConstants.E_NESTED_RULE);

        Element child2FilterTests = nestedRule2.addElement(MailConstants.E_FILTER_TESTS);
        child2FilterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element child2headerTest2 = child2FilterTests.addElement(MailConstants.E_HEADER_TEST);
        child2headerTest2.addAttribute(MailConstants.A_HEADER, "from");
        child2headerTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        child2headerTest2.addAttribute(MailConstants.A_VALUE, "test");

        Element filteraction = nestedRule2.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "Junk");
        filteraction.addElement(MailConstants.E_ACTION_STOP);

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            fail("This test is expected not to throw exception. ");
        }

        String expectedScript = "require [" + SoapToSieve.requireCommon + "];\n\n";
        expectedScript += "# Test1\n";
        expectedScript += "if anyof (header :contains [\"subject\"] \"important\") {\n";
        expectedScript += "    if anyof (header :is [\"subject\"] \"confifential\") {\n";
        expectedScript += "        if anyof (header :contains [\"from\"] \"test\") {\n";
        expectedScript += "            fileinto \"Junk\";\n";
        expectedScript += "            stop;\n";
        expectedScript += "        }\n";
        expectedScript += "    }\n";
        expectedScript += "}\n";


        //ZimbraLog.filter.info(acct.getMailSieveScript());
        //ZimbraLog.filter.info(expectedScript);
        assertEquals(expectedScript, acct.getMailSieveScript());

    }

    @Test
    public void testBug71036_NestedIfMultiRulesWithMultiConditions() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);

        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");

        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest.addAttribute(MailConstants.A_VALUE, "important");

        Element nestedRule = rule.addElement(MailConstants.E_NESTED_RULE);

        Element childFilterTests = nestedRule.addElement(MailConstants.E_FILTER_TESTS);
        childFilterTests.addAttribute(MailConstants.A_CONDITION, "allof");
        Element childheaderTest = childFilterTests.addElement(MailConstants.E_HEADER_TEST);
        childheaderTest.addAttribute(MailConstants.A_HEADER, "Subject");
        childheaderTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
        childheaderTest.addAttribute(MailConstants.A_VALUE, "confifential");
        Element childheaderTest2 = childFilterTests.addElement(MailConstants.E_HEADER_TEST);
        childheaderTest2.addAttribute(MailConstants.A_HEADER, "Subject");
        childheaderTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        childheaderTest2.addAttribute(MailConstants.A_VALUE, "secret");

        Element filteraction = nestedRule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "Junk");
        filteraction.addElement(MailConstants.E_ACTION_STOP);

        Element rule2 = rules.addElement(MailConstants.E_FILTER_RULE);
        rule2.addAttribute(MailConstants.A_ACTIVE, true);
        rule2.addAttribute(MailConstants.A_NAME, "Test2");
        //Element filteraction2 = rule2.addElement(MailConstants.E_FILTER_ACTIONS);
        //Element actionInto2 = filteraction2.addElement(MailConstants.E_ACTION_FILE_INTO);
        //actionInto2.addAttribute(MailConstants.A_FOLDER_PATH, "Trush");
        //filteraction2.addElement(MailConstants.E_ACTION_STOP);
        Element filterTests2 = rule2.addElement(MailConstants.E_FILTER_TESTS);
        filterTests2.addAttribute(MailConstants.A_CONDITION, "allof");
        Element headerTest21 = filterTests2.addElement(MailConstants.E_HEADER_TEST);
        headerTest21.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest21.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest21.addAttribute(MailConstants.A_VALUE, "important");
        Element headerTest22 = filterTests2.addElement(MailConstants.E_HEADER_TEST);
        headerTest22.addAttribute(MailConstants.A_HEADER, "from");
        headerTest22.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest22.addAttribute(MailConstants.A_VALUE, "solutions");

        Element nestedRule2 = rule2.addElement(MailConstants.E_NESTED_RULE);

        Element childFilterTests2 = nestedRule2.addElement(MailConstants.E_FILTER_TESTS);
        childFilterTests2.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element childheaderTest21 = childFilterTests2.addElement(MailConstants.E_HEADER_TEST);
        childheaderTest21.addAttribute(MailConstants.A_HEADER, "subject");
        childheaderTest21.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
        childheaderTest21.addAttribute(MailConstants.A_VALUE, "confifential");
        Element childheaderTest22 = childFilterTests2.addElement(MailConstants.E_HEADER_TEST);
        childheaderTest22.addAttribute(MailConstants.A_HEADER, "Cc");
        childheaderTest22.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        childheaderTest22.addAttribute(MailConstants.A_VALUE, "test");

        Element nfilteraction2 = nestedRule2.addElement(MailConstants.E_FILTER_ACTIONS);
        Element nactionInto2 = nfilteraction2.addElement(MailConstants.E_ACTION_FILE_INTO);
        nactionInto2.addAttribute(MailConstants.A_FOLDER_PATH, "Trash");
        nfilteraction2.addElement(MailConstants.E_ACTION_STOP);

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            fail("This test is expected not to throw exception. ");
        }

        String expectedScript = "require [" + SoapToSieve.requireCommon + "];\n\n";
        expectedScript += "# Test1\n";
        expectedScript += "if anyof (header :contains [\"Subject\"] \"important\") {\n";
        expectedScript += "    if allof (header :is [\"Subject\"] \"confifential\",\n";
        expectedScript += "      header :contains [\"Subject\"] \"secret\") {\n";
        expectedScript += "        fileinto \"Junk\";\n";
        expectedScript += "        stop;\n";
        expectedScript += "    }\n";
        expectedScript += "}\n";
        expectedScript += "\n";
        expectedScript += "# Test2\n";
        expectedScript += "if allof (header :contains [\"subject\"] \"important\",\n";
        expectedScript += "  header :contains [\"from\"] \"solutions\") {\n";
        expectedScript += "    if anyof (header :is [\"subject\"] \"confifential\",\n";
        expectedScript += "      header :contains [\"Cc\"] \"test\") {\n";
        expectedScript += "        fileinto \"Trash\";\n";
        expectedScript += "        stop;\n";
        expectedScript += "    }\n";
        expectedScript += "}\n";


        //ZimbraLog.filter.info(acct.getMailSieveScript());
        //ZimbraLog.filter.info(expectedScript);
        assertEquals(expectedScript, acct.getMailSieveScript());

    }

    /**
     * Bug 92309: text "null" was set to To/Cc/From field
     *
     * When the To, Cc, or From field in the message filter rule
     * composing dialogue window is left empty, ZWC automatically
     * put a text "null" to the filed.  This test case tests that
     * the text "null" will not be set if the To/Cc/From field
     * is empty.
     */
    @Test
    public void testBug92309_SetIncomingXMLRules_EmptyAddress() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            // Construct a filter rule with 'address' test whose value is empty (null)
            FilterRule rule = new FilterRule("testSetIncomingXMLRules_EmptyAddress", true);

            FilterTest.AddressTest test = new FilterTest.AddressTest();
            test.setHeader("to");
            test.setStringComparison("is");
            test.setPart("all");
            test.setValue(null);
            test.setIndex(0);

            FilterTests tests = new FilterTests("anyof");
            tests.addTest(test);

            FilterAction action = new FilterAction.KeepAction();
            action.setIndex(0);

            rule.setFilterTests(tests);
            rule.addFilterAction(action);

            List<FilterRule> filterRuleList = new ArrayList<FilterRule>();
            filterRuleList.add(rule);

            // When the ModifyFilterRulesRequest is submitted from the Web client,
            // eventually this RuleManager.setIncomingXMLRules is called to convert
            // the request in JSON to the SIEVE rule text.
            RuleManager.setIncomingXMLRules(account, filterRuleList);

            // Verify that the saved zimbraMailSieveScript
            String sieve = account.getMailSieveScript();
            int result = sieve.indexOf("address :all :is :comparator \"i;ascii-casemap\" [\"to\"] \"\"");
            Assert.assertNotSame(-1, result);
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void SetIncomingXMLRulesForEnvelope() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            // Construct a filter rule with 'address' test whose value is empty (null)
            FilterRule rule = new FilterRule("testSetIncomingXMLRulesForEnvelope", true);

            FilterTest.EnvelopeTest test = new FilterTest.EnvelopeTest();
            test.setHeader("to");
            test.setStringComparison("is");
            test.setPart("all");
            test.setValue("u1@zimbra.com");
            test.setIndex(0);

            FilterTests tests = new FilterTests("anyof");
            tests.addTest(test);

            FilterAction action = new FilterAction.KeepAction();
            action.setIndex(0);

            rule.setFilterTests(tests);
            rule.addFilterAction(action);

            List<FilterRule> filterRuleList = new ArrayList<FilterRule>();
            filterRuleList.add(rule);

            // When the ModifyFilterRulesRequest is submitted from the Web client,
            // eventually this RuleManager.setIncomingXMLRules is called to convert
            // the request in JSON to the SIEVE rule text.
            RuleManager.setIncomingXMLRules(account, filterRuleList);

            // Verify that the saved zimbraMailSieveScript
            String sieve = account.getMailSieveScript();
            int result = sieve.indexOf("envelope :all :is :comparator \"i;ascii-casemap\" [\"to\"] \"u1@zimbra.com\"");
            Assert.assertNotSame(-1, result);
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void SetIncomingXMLRulesForEnvelopeCountComparison() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            // Construct a filter rule with 'address' test whose value is empty (null)
            FilterRule rule = new FilterRule("testSetIncomingXMLRulesForEnvelope", true);

            FilterTest.EnvelopeTest test = new FilterTest.EnvelopeTest();
            test.setHeader("to");
            test.setCountComparison("eq");
            test.setPart("all");
            test.setValue("1");
            test.setIndex(0);

            FilterTests tests = new FilterTests("anyof");
            tests.addTest(test);

            FilterAction action = new FilterAction.KeepAction();
            action.setIndex(0);

            rule.setFilterTests(tests);
            rule.addFilterAction(action);

            List<FilterRule> filterRuleList = new ArrayList<FilterRule>();
            filterRuleList.add(rule);

            // When the ModifyFilterRulesRequest is submitted from the Web client,
            // eventually this RuleManager.setIncomingXMLRules is called to convert
            // the request in JSON to the SIEVE rule text.
            RuleManager.setIncomingXMLRules(account, filterRuleList);

            // Verify that the saved zimbraMailSieveScript
            String sieve = account.getMailSieveScript();
            int result = sieve.indexOf("envelope :count \"eq\" :all :comparator \"i;ascii-numeric\" [\"to\"] \"1\"");
            Assert.assertNotSame(-1, result);
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testFilterVariables() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\"><filterRules>"
                + "<filterRule name=\"t60\" active=\"1\">"
                + "<filterVariables index=\"0\">"
                + "<filterVariable name=\"var\" value=\"testTag\"/>"
                + "<filterVariable name=\"var_new\" value=\"${var}\"/>"
                + "</filterVariables>"
                + "<filterTests condition=\"anyof\">"
                + "<headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"test\"/>"
                + "</filterTests>"
                + "<filterActions>"
                + "<actionTag index=\"0\" tagName=\"${var_new}\"/>"
                + "<filterVariables index=\"0\">"
                + "<filterVariable name=\"v1\" value=\"blah blah\"/>"
                + "<filterVariable name=\"v2\" value=\"${v1}\"/>"
                + "<filterVariable name=\"t1\" value=\"ttttt\"/>"
                + "</filterVariables>"
                + "</filterActions>"
                + "<nestedRule>"
                + "<filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"abc\"/>"
                + "</filterTests>"
                + "<filterActions>"
                + "<actionTag index=\"0\" tagName=\"${v2}\"/>"
                + "<actionTag index=\"1\" tagName=\"${t1}\"/>"
                + "<filterVariables index=\"0\">"
                + "<filterVariable name=\"v3\" value=\"bbbbbbbbbbbb\"/>"
                + "</filterVariables>"
                + "</filterActions>"
                + "<nestedRule>"
                + "<filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"def\"/></filterTests>"
                + "<filterActions>"
                + "<filterVariables index=\"0\">"
                + "<filterVariable name=\"v4\" value=\"${v3}\"/>"
                + "</filterVariables>"
                + "</filterActions>"
                + "<nestedRule>"
                + "<filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"def\"/></filterTests>"
                + "<filterActions>"
                + "<filterVariables index=\"0\">"
                + "<filterVariable name=\"v5\" value=\"${v4}\"/>"
                + "</filterVariables>"
                + "</filterActions>"
                + "</nestedRule>"
                + "</nestedRule>"
                + "</nestedRule>"
                + "</filterRule>"
                + "</filterRules>"
                + "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

            String expectedScript = "require [" + SoapToSieve.requireCommon + "];\n" +
                "\n" +
                "# t60\n" +
                "set \"var\" \"testTag\";\n" +
                "set \"var_new\" \"${var}\";\n" +
                "if anyof (header :contains [\"subject\"] \"test\") {\n" +
                "    tag \"${var_new}\";\n" +
                "    set \"v1\" \"blah blah\";\n" +
                "    set \"v2\" \"${v1}\";\n" +
                "    set \"t1\" \"ttttt\";\n" +
                "    if anyof (header :contains [\"subject\"] \"abc\") {\n" +
                "        tag \"${v2}\";\n" +
                "        tag \"${t1}\";\n" +
                "        set \"v3\" \"bbbbbbbbbbbb\";\n" +
                "        if anyof (header :contains [\"subject\"] \"def\") {\n" +
                "            set \"v4\" \"${v3}\";\n" +
                "            if anyof (header :contains [\"subject\"] \"def\") {\n" +
                "                set \"v5\" \"${v4}\";\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "";

            assertEquals(expectedScript, account.getMailSieveScript());
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testFilterVariablesForMatchVariables() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\">\n" +
                "      <filterRules>\n" +
                "        <filterRule name=\"t60\" active=\"1\">\n" +
                "          <filterVariables index=\"0\">\n" +
                "            <filterVariable name=\"var\" value=\"testTag\" index=\"0\"/>\n" +
                "            <filterVariable name=\"var_new\" value=\"${var}\" index=\"1\"/>\n" +
                "          </filterVariables>\n" +
                "          <filterTests condition=\"anyof\" index=\"1\">\n" +
                "            <headerTest stringComparison=\"matches\" header=\"subject\" value=\"test\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions index=\"2\">\n" +
                "            <actionTag tagName=\"${var_new}\"  index=\"0\"/>\n" +
                "            <filterVariables index=\"1\">\n" +
                "              <filterVariable name=\"v1\" value=\"blah blah\" index=\"0\"/>\n" +
                "              <filterVariable name=\"v2\" value=\"${1}\" index=\"1\"/>\n" +
                "              <filterVariable name=\"v3\" value=\"${2}\" index=\"1\"/>\n" +
                "            </filterVariables>\n" +
                "          </filterActions>\n" +
                "        </filterRule>\n" +
                "      </filterRules>\n" +
                "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

            String expectedScript = "require [" + SoapToSieve.requireCommon + "];\n" +
                "\n" +
                "# t60\n" +
                "set \"var\" \"testTag\";\n" +
                "set \"var_new\" \"${var}\";\n" +
                "if anyof (header :matches [\"subject\"] \"test\") {\n" +
                "    tag \"${var_new}\";\n" +
                "    set \"v1\" \"blah blah\";\n" +
                "    set \"v2\" \"${1}\";\n" +
                "    set \"v3\" \"${2}\";\n" +
                "}\n" +
                "";

            assertEquals(expectedScript, account.getMailSieveScript());
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testFilterVariablesWithNoName() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\">\n" +
                "      <filterRules>\n" +
                "        <filterRule name=\"t60\" active=\"1\">\n" +
                "          <filterVariables index=\"0\">\n" +
                "            <filterVariable name=\"var\" value=\"testTag\" index=\"0\"/>\n" +
                "            <filterVariable value=\"${var}\" index=\"1\"/>\n" +
                "          </filterVariables>\n" +
                "          <filterTests condition=\"anyof\" index=\"1\">\n" +
                "            <headerTest stringComparison=\"matches\" header=\"subject\" value=\"test\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions index=\"2\">\n" +
                "            <actionTag tagName=\"${var_new}\"  index=\"0\"/>\n" +
                "            <filterVariables index=\"1\">\n" +
                "              <filterVariable name=\"v1\" value=\"blah blah\" index=\"0\"/>\n" +
                "              <filterVariable name=\"v2\" value=\"${1}\" index=\"1\"/>\n" +
                "              <filterVariable name=\"v3\" value=\"${2}\" index=\"1\"/>\n" +
                "            </filterVariables>\n" +
                "          </filterActions>\n" +
                "        </filterRule>\n" +
                "      </filterRules>\n" +
                "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

            fail("Should not reach here. Exception is expected");
        } catch (ServiceException e) {
            assertTrue("parse error: Filter variable should have a name".equals(e.getMessage()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testFilterVariablesWithNoValue() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\">\n" +
                "      <filterRules>\n" +
                "        <filterRule name=\"t60\" active=\"1\">\n" +
                "          <filterVariables index=\"0\">\n" +
                "            <filterVariable name=\"var\" value=\"testTag\" index=\"0\"/>\n" +
                "            <filterVariable name=\"var_new\" index=\"1\"/>\n" +
                "          </filterVariables>\n" +
                "          <filterTests condition=\"anyof\" index=\"1\">\n" +
                "            <headerTest stringComparison=\"matches\" header=\"subject\" value=\"test\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions index=\"2\">\n" +
                "            <actionTag tagName=\"${var_new}\"  index=\"0\"/>\n" +
                "            <filterVariables index=\"1\">\n" +
                "              <filterVariable name=\"v1\" value=\"blah blah\" index=\"0\"/>\n" +
                "              <filterVariable name=\"v2\" value=\"${1}\" index=\"1\"/>\n" +
                "              <filterVariable name=\"v3\" value=\"${2}\" index=\"1\"/>\n" +
                "            </filterVariables>\n" +
                "          </filterActions>\n" +
                "        </filterRule>\n" +
                "      </filterRules>\n" +
                "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

            fail("Should not reach here. Exception is expected");
        } catch (ServiceException e) {
            assertTrue("parse error: Filter variable should have a value".equals(e.getMessage()));
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testZCS1173SingleIfNoAllofAnyofRule() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);

        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_TAG);
        actionInto.addAttribute(MailConstants.A_TAG_NAME, "123-456");
        filteraction.addElement(MailConstants.E_ACTION_STOP);

        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        // Not set the "condition=" parameter.

        Element headerTest1 = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest1.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest1.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest1.addAttribute(MailConstants.A_VALUE, "123");

        Element headerTest2 = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest2.addAttribute(MailConstants.A_HEADER, "X-Header");
        headerTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest2.addAttribute(MailConstants.A_VALUE, "456");

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            fail("This test is expected not to throw exception. " + e);
        }

        String expectedScript = "require [" + SoapToSieve.requireCommon + "];\n\n";
        expectedScript += "# null\n";
        expectedScript += "if allof (header :contains [\"subject\"] \"123\",\n";
        expectedScript += "  header :contains [\"X-Header\"] \"456\") {\n";
        expectedScript += "    tag \"123-456\";\n";
        expectedScript += "    stop;\n";
        expectedScript += "}\n";

        assertEquals(expectedScript, acct.getMailSieveScript());
    }

    @Test
    public void testNegativeAddheaderAction() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\">\n" +
                "      <filterRules>\n" +
                "        <filterRule name=\"t60\" active=\"1\">\n" +
                "          <filterTests condition=\"anyof\" index=\"1\">\n" +
                "            <headerTest stringComparison=\"matches\" header=\"subject\" value=\"*\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions index=\"2\">\n" +
                "            <actionAddheader>\n" +
                "              <headerName>sub2</headerName>\n" +
                "              <headerValue>${1}</headerValue>\n" +
                "            </actionAddheader>\n" +
                "          </filterActions>\n" +
                "        </filterRule>\n" +
                "      </filterRules>\n" +
                "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals("parse error: Invalid addheader action: addheader action is not allowed in user scripts", e.getMessage());
        }
    }

    @Test
    public void testNegativeDeleteheaderAction() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\">\n" +
                "      <filterRules>\n" +
                "        <filterRule name=\"t60\" active=\"1\">\n" +
                "          <filterTests condition=\"anyof\" index=\"1\">\n" +
                "            <headerTest stringComparison=\"matches\" header=\"subject\" value=\"*\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions index=\"2\">\n" +
                "            <actionDeleteheader>\n" +
                "              <headerName>sub2</headerName>\n" +
                "            </actionDeleteheader>\n" +
                "          </filterActions>\n" +
                "        </filterRule>\n" +
                "      </filterRules>\n" +
                "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals("parse error: Invalid deleteheader action: deleteheader action is not allowed in user scripts", e.getMessage());
        }
    }

    @Test
    public void testNegativeReplaceheaderAction() {
        try {
            Account account = Provisioning.getInstance().getAccount(
                    MockProvisioning.DEFAULT_ACCOUNT_ID);
            RuleManager.clearCachedRules(account);

            String xml = "<ModifyFilterRulesRequest xmlns=\"urn:zimbraMail\">\n" +
                "      <filterRules>\n" +
                "        <filterRule name=\"t60\" active=\"1\">\n" +
                "          <filterTests condition=\"anyof\" index=\"1\">\n" +
                "            <headerTest stringComparison=\"matches\" header=\"subject\" value=\"*\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions index=\"2\">\n" +
                "            <actionReplaceheader>\n" +
                "              <newValue>new_header_value</newValue>\n" +
                "              <test>\n" +
                "                <headerName>sub2</headerName>\n" +
                "                <headerValue>test testing</headerValue>\n" +
                "              </test>\n" +
                "            </actionReplaceheader>\n" +
                "          </filterActions>\n" +
                "        </filterRule>\n" +
                "      </filterRules>\n" +
                "</ModifyFilterRulesRequest>";

            Element request = Element.parseXML(xml);
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals("parse error: Invalid replaceheader action: replaceheader action is not allowed in user scripts", e.getMessage());
        }
    }
}
