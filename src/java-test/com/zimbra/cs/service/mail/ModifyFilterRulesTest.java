/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ZimbraLog;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;


/**
 * @author zimbra
 *
 */
public class ModifyFilterRulesTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

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

        String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];\n\n";
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

        String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];\n\n";
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
        nactionInto2.addAttribute(MailConstants.A_FOLDER_PATH, "Trush");
        nfilteraction2.addElement(MailConstants.E_ACTION_STOP);

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            fail("This test is expected not to throw exception. ");
        }

        String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];\n\n";
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
        expectedScript += "        fileinto \"Trush\";\n";
        expectedScript += "        stop;\n";
        expectedScript += "    }\n";
        expectedScript += "}\n";


        //ZimbraLog.filter.info(acct.getMailSieveScript());
        //ZimbraLog.filter.info(expectedScript);
        assertEquals(expectedScript, acct.getMailSieveScript());

    }
}
