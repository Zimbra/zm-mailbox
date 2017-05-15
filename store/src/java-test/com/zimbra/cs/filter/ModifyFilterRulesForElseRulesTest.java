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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Map;
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
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ModifyFilterRules;
import com.zimbra.cs.service.mail.ServiceTestUtil;


public class ModifyFilterRulesForElseRulesTest {
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
    public void testIfElseIf() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "FromP2");
        Element actionLog =filteraction.addElement(MailConstants.E_ACTION_LOG);
        actionLog.addAttribute(MailConstants.A_INDEX, "1");
        actionLog.addText("Move message to FromP2 folder");
        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
        headerTest.addAttribute(MailConstants.A_VALUE, "important");
        headerTest.addAttribute(MailConstants.A_INDEX, "0");
        Element elseRules = rule.addElement(MailConstants.E_ELSE_RULES);
        Element elseRule = elseRules.addElement(MailConstants.E_ELSE_RULE);
        Element filterTests1 = elseRule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests1.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest1 = filterTests1.addElement(MailConstants.E_HEADER_TEST);
        headerTest1.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest1.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest1.addAttribute(MailConstants.A_VALUE, "automation");
        headerTest1.addAttribute(MailConstants.A_INDEX, "0");
        Element filteraction1 = elseRule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionRedirect1 = filteraction1.addElement(MailConstants.E_ACTION_REDIRECT);
        actionRedirect1.addAttribute("a", "redirect@zimbra.com");
        actionRedirect1.addAttribute(MailConstants.A_INDEX, "0");
        actionRedirect1.addAttribute(MailConstants.A_COPY, "0");
        Element actionLog1 = filteraction1.addElement(MailConstants.E_ACTION_LOG);
        actionLog1.addAttribute(MailConstants.A_INDEX, "1");
        actionLog1.addText("Forward message to qa-automation DL");

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            e.printStackTrace();
            fail("This test is expected not to throw exception. ");
        }
        String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\n";
        expectedScript += "\n";
        expectedScript += "# Test1\n";
        expectedScript += "if anyof (header :is [\"Subject\"] \"important\") {\n";
        expectedScript += "    fileinto \"FromP2\";\n";
        expectedScript += "    log \"Move message to FromP2 folder\";\n";
        expectedScript += "}\n";
        expectedScript += "elsif anyof (header :contains [\"subject\"] \"automation\") {\n";
        expectedScript += "    redirect \"redirect@zimbra.com\";\n";
        expectedScript += "    log \"Forward message to qa-automation DL\";\n";
        expectedScript += "}\n";
        
        assertEquals(expectedScript.replace(" ", "").replace("\n", ""), acct.getMailSieveScript().replace(" ", "").replace("\n", ""));
    }
    
    @Test
    public void testMultipleIfElseAndLastElse() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "FromP2");
        Element actionLog =filteraction.addElement(MailConstants.E_ACTION_LOG);
        actionLog.addAttribute(MailConstants.A_INDEX, "1");
        actionLog.addText("Move message to FromP2 folder");
        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
        headerTest.addAttribute(MailConstants.A_VALUE, "important");
        headerTest.addAttribute(MailConstants.A_INDEX, "0");
        Element elseRules = rule.addElement(MailConstants.E_ELSE_RULES);
        Element elseRule1 = elseRules.addElement(MailConstants.E_ELSE_RULE);
        Element elseRule2 = elseRules.addElement(MailConstants.E_ELSE_RULE);
        Element elseRule3 = elseRules.addElement(MailConstants.E_ELSE_RULE);
        Element filterTests1 = elseRule1.addElement(MailConstants.E_FILTER_TESTS);
        filterTests1.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest1 = filterTests1.addElement(MailConstants.E_HEADER_TEST);
        headerTest1.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest1.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest1.addAttribute(MailConstants.A_VALUE, "automation");
        headerTest1.addAttribute(MailConstants.A_INDEX, "0");
        Element filteraction1 = elseRule1.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionLog1 =filteraction1.addElement(MailConstants.E_ACTION_LOG);
        actionLog1.addAttribute(MailConstants.A_INDEX, "1");
        actionLog1.addText("first elseif");
        Element filterTests2 = elseRule2.addElement(MailConstants.E_FILTER_TESTS);
        filterTests2.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest2 = filterTests2.addElement(MailConstants.E_HEADER_TEST);
        headerTest2.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest2.addAttribute(MailConstants.A_VALUE, "test");
        headerTest2.addAttribute(MailConstants.A_INDEX, "0");
        Element filteraction2 = elseRule2.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionLog2 = filteraction2.addElement(MailConstants.E_ACTION_LOG);
        actionLog2.addAttribute(MailConstants.A_INDEX, "1");
        actionLog2.addText("second elseif");
        Element filteraction3 = elseRule3.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionLog3 = filteraction3.addElement(MailConstants.E_ACTION_LOG);
        actionLog3.addAttribute(MailConstants.A_INDEX, "1");
        actionLog3.addText("last else");

        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            e.printStackTrace();
            fail("This test is expected not to throw exception. ");
        }
        String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\n";
        expectedScript += "\n";
        expectedScript += "# Test1\n";
        expectedScript += "if anyof (header :is [\"Subject\"] \"important\") {\n";
        expectedScript += "    fileinto \"FromP2\";\n";
        expectedScript += "    log \"Move message to FromP2 folder\";\n";
        expectedScript += "}\n";
        expectedScript += "elsif anyof (header :contains [\"subject\"] \"automation\") {\n";
        expectedScript += "    log \"first elseif\";\n";
        expectedScript += "}\n";
        expectedScript += "elsif anyof (header :contains [\"subject\"] \"test\") {\n";
        expectedScript += "    log \"second elseif\";\n";
        expectedScript += "}\n";
        expectedScript += "else {\n";
        expectedScript += "     log \"last else\";\n";
        expectedScript += "}\n";
        
        assertEquals(expectedScript.replace(" ", "").replace("\n", ""), acct.getMailSieveScript().replace(" ", "").replace("\n", ""));
    }

    @Test
    public void testNestedIfElseIfInElseBlock() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
        Element rules = request.addElement(MailConstants.E_FILTER_RULES);
        Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
        rule.addAttribute(MailConstants.A_ACTIVE, true);
        rule.addAttribute(MailConstants.A_NAME, "Test1");
        Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "FromP2");
        Element actionLog =filteraction.addElement(MailConstants.E_ACTION_LOG);
        actionLog.addAttribute(MailConstants.A_INDEX, "1");
        actionLog.addText("first if");
        Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
        filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
        headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
        headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
        headerTest.addAttribute(MailConstants.A_VALUE, "important");
        headerTest.addAttribute(MailConstants.A_INDEX, "0");
        Element elseRules = rule.addElement(MailConstants.E_ELSE_RULES);
        Element elseRule1 = elseRules.addElement(MailConstants.E_ELSE_RULE);
        Element filteraction1 = elseRule1.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto1 = filteraction1.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto1.addAttribute(MailConstants.A_FOLDER_PATH, "ElseFolder");
        Element actionLog1 =filteraction1.addElement(MailConstants.E_ACTION_LOG);
        actionLog1.addAttribute(MailConstants.A_INDEX, "1");
        actionLog1.addText("else");
        Element nestedRule1 = elseRule1.addElement(MailConstants.E_NESTED_RULE);
        Element filterTests1 = nestedRule1.addElement(MailConstants.E_FILTER_TESTS);
        filterTests1.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest1 = filterTests1.addElement(MailConstants.E_HEADER_TEST);
        headerTest1.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest1.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest1.addAttribute(MailConstants.A_VALUE, "automation");
        headerTest1.addAttribute(MailConstants.A_INDEX, "0");
        Element filteraction2 = nestedRule1.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto2 = filteraction2.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto2.addAttribute(MailConstants.A_FOLDER_PATH, "NestedIf");
        Element actionLog2 =filteraction2.addElement(MailConstants.E_ACTION_LOG);
        actionLog2.addAttribute(MailConstants.A_INDEX, "1");
        actionLog2.addText("nested if");
        Element elseRules1 = nestedRule1.addElement(MailConstants.E_ELSE_RULES);
        Element elseRule4 = elseRules1.addElement(MailConstants.E_ELSE_RULE);
        Element filterTests2 = elseRule4.addElement(MailConstants.E_FILTER_TESTS);
        filterTests2.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element headerTest2 = filterTests2.addElement(MailConstants.E_HEADER_TEST);
        headerTest2.addAttribute(MailConstants.A_HEADER, "subject");
        headerTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        headerTest2.addAttribute(MailConstants.A_VALUE, "test");
        headerTest2.addAttribute(MailConstants.A_INDEX, "0");
        Element filteraction3 = elseRule4.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionInto3 = filteraction3.addElement(MailConstants.E_ACTION_FILE_INTO);
        actionInto3.addAttribute(MailConstants.A_FOLDER_PATH, "ElseIfInNested");
        Element actionLog3 = filteraction3.addElement(MailConstants.E_ACTION_LOG);
        actionLog3.addAttribute(MailConstants.A_INDEX, "1");
        actionLog3.addText("second elseif");
        
        try {
            new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (ServiceException e) {
            e.printStackTrace();
            fail("This test is expected not to throw exception. ");
        }
        String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\n";
        expectedScript += "\n";
        expectedScript += "# Test1\n";
        expectedScript += "if anyof (header :is [\"Subject\"] \"important\") {\n";
        expectedScript += "    fileinto \"FromP2\";\n";
        expectedScript += "    log \"first if\";\n";
        expectedScript += "}\n";
        expectedScript += "else {\n";
        expectedScript += "     if anyof (header :contains [\"subject\"] \"automation\") {\n";
        expectedScript += "        fileinto \"NestedIf\";\n";
        expectedScript += "        log \"nested if\";\n";
        expectedScript += "    }\n";
        expectedScript += "    elsif anyof (header :contains [\"subject\"] \"test\") {\n";
        expectedScript += "        fileinto \"ElseIfInNested\";\n";
        expectedScript += "        log \"second elseif\";\n";
        expectedScript += "    }\n";
        expectedScript += "    fileinto \"ElseFolder\";\n";
        expectedScript += "    log \"else\";\n";
        expectedScript += "}\n";
        
        assertEquals(expectedScript.replace(" ", "").replace("\n", ""), acct.getMailSieveScript().replace(" ", "").replace("\n", ""));
    }

@Test
public void testNestedIfElseInElseIf() throws Exception {
    Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
    Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
    Element rules = request.addElement(MailConstants.E_FILTER_RULES);
    Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
    rule.addAttribute(MailConstants.A_ACTIVE, true);
    rule.addAttribute(MailConstants.A_NAME, "Test1");
    Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
    actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "FromP2");
    Element actionLog =filteraction.addElement(MailConstants.E_ACTION_LOG);
    actionLog.addAttribute(MailConstants.A_INDEX, "1");
    actionLog.addText("Move message to FromP2 folder");
    Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
    filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
    headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
    headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
    headerTest.addAttribute(MailConstants.A_VALUE, "important");
    headerTest.addAttribute(MailConstants.A_INDEX, "0");
    Element elseRules = rule.addElement(MailConstants.E_ELSE_RULES);
    Element elseRule1 = elseRules.addElement(MailConstants.E_ELSE_RULE);
    Element elseRule2 = elseRules.addElement(MailConstants.E_ELSE_RULE);
    Element elseRule3 = elseRules.addElement(MailConstants.E_ELSE_RULE);
    
    Element filterTests1 = elseRule1.addElement(MailConstants.E_FILTER_TESTS);
    filterTests1.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest1 = filterTests1.addElement(MailConstants.E_HEADER_TEST);
    headerTest1.addAttribute(MailConstants.A_HEADER, "subject");
    headerTest1.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
    headerTest1.addAttribute(MailConstants.A_VALUE, "automation");
    headerTest1.addAttribute(MailConstants.A_INDEX, "0");
    Element filteraction1 = elseRule1.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionLog1 =filteraction1.addElement(MailConstants.E_ACTION_LOG);
    actionLog1.addAttribute(MailConstants.A_INDEX, "1");
    actionLog1.addText("first elseif");
    Element filterTests2 = elseRule2.addElement(MailConstants.E_FILTER_TESTS);
    filterTests2.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest2 = filterTests2.addElement(MailConstants.E_HEADER_TEST);
    headerTest2.addAttribute(MailConstants.A_HEADER, "subject");
    headerTest2.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
    headerTest2.addAttribute(MailConstants.A_VALUE, "test");
    headerTest2.addAttribute(MailConstants.A_INDEX, "0");
    Element filteraction2 = elseRule2.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionLog2 =filteraction2.addElement(MailConstants.E_ACTION_LOG);
    actionLog2.addAttribute(MailConstants.A_INDEX, "1");
    actionLog2.addText("second elseif");
    Element nestedRule1 = elseRule2.addElement(MailConstants.E_NESTED_RULE);
    Element filterTests3 = nestedRule1.addElement(MailConstants.E_FILTER_TESTS);
    filterTests3.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest3 = filterTests3.addElement(MailConstants.E_HEADER_TEST);
    headerTest3.addAttribute(MailConstants.A_HEADER, "subject");
    headerTest3.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
    headerTest3.addAttribute(MailConstants.A_VALUE, "nestedif");
    headerTest3.addAttribute(MailConstants.A_INDEX, "0");
    Element filteraction3 = nestedRule1.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionInto3 = filteraction3.addElement(MailConstants.E_ACTION_FILE_INTO);
    actionInto3.addAttribute(MailConstants.A_FOLDER_PATH, "NestedIf");
    Element actionLog3 =filteraction3.addElement(MailConstants.E_ACTION_LOG);
    actionLog3.addAttribute(MailConstants.A_INDEX, "1");
    actionLog3.addText("nested if");
    Element elseRules1 = nestedRule1.addElement(MailConstants.E_ELSE_RULES);
    Element elseRule4 = elseRules1.addElement(MailConstants.E_ELSE_RULE);
    Element filterTests4 = elseRule4.addElement(MailConstants.E_FILTER_TESTS);
    filterTests4.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest4 = filterTests4.addElement(MailConstants.E_HEADER_TEST);
    headerTest4.addAttribute(MailConstants.A_HEADER, "subject");
    headerTest4.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
    headerTest4.addAttribute(MailConstants.A_VALUE, "elseif");
    headerTest4.addAttribute(MailConstants.A_INDEX, "0");
    Element filteraction4 = elseRule4.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionInto4 = filteraction4.addElement(MailConstants.E_ACTION_FILE_INTO);
    actionInto4.addAttribute(MailConstants.A_FOLDER_PATH, "ElseIfInNested");
    Element actionLog4 = filteraction4.addElement(MailConstants.E_ACTION_LOG);
    actionLog4.addAttribute(MailConstants.A_INDEX, "1");
    actionLog4.addText("second elseif");
    Element filteraction5 = elseRule3.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionLog5 =filteraction5.addElement(MailConstants.E_ACTION_LOG);
    actionLog5.addAttribute(MailConstants.A_INDEX, "1");
    actionLog5.addText("last else");
    
    try {
        new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
    } catch (ServiceException e) {
        e.printStackTrace();
        fail("This test is expected not to throw exception. ");
    }
    String expectedScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\n";
    expectedScript += "\n";
    expectedScript += "# Test1\n";
    expectedScript += "if anyof (header :is [\"Subject\"] \"important\") {\n";
    expectedScript += "    fileinto \"FromP2\";\n";
    expectedScript += "    log \"Move message to FromP2 folder\";\n";
    expectedScript += "}\n";
    expectedScript += "elsif anyof (header :contains [\"subject\"] \"automation\") {\n";
    expectedScript += "    log \"first elseif\";\n";
    expectedScript += "}\n";
    expectedScript += "elsif anyof (header :contains [\"subject\"] \"test\") {\n";
    expectedScript += "    if anyof (header :contains [\"subject\"] \"nestedif\") {\n";
    expectedScript += "        fileinto \"NestedIf\";\n";
    expectedScript += "        log \"nested if\";\n";
    expectedScript += "    }\n";
    expectedScript += "    elsif anyof (header :contains [\"subject\"] \"elseif\") {\n";
    expectedScript += "        fileinto \"ElseIfInNested\";\n";
    expectedScript += "        log \"second elseif\";\n";
    expectedScript += "    }\n";
    expectedScript += "    log \"second elseif\";\n";
    expectedScript += "}\n";
    expectedScript += "else {\n";
    expectedScript += "     log \"last else\";\n";
    expectedScript += "}\n";
    
    assertEquals(expectedScript.replace(" ", "").replace("\n", ""), acct.getMailSieveScript().replace(" ", "").replace("\n", ""));
}

@Test
public void testNegative_ElseIfAfterElse() throws Exception {
    Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
    Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
    Element rules = request.addElement(MailConstants.E_FILTER_RULES);
    Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
    rule.addAttribute(MailConstants.A_ACTIVE, true);
    rule.addAttribute(MailConstants.A_NAME, "Test1");
    Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
    actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "FromP2");
    Element actionLog =filteraction.addElement(MailConstants.E_ACTION_LOG);
    actionLog.addAttribute(MailConstants.A_INDEX, "1");
    actionLog.addText("Move message to FromP2 folder");
    Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
    filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
    headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
    headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
    headerTest.addAttribute(MailConstants.A_VALUE, "important");
    headerTest.addAttribute(MailConstants.A_INDEX, "0");
    Element elseRules = rule.addElement(MailConstants.E_ELSE_RULES);
    Element elseRule1 = elseRules.addElement(MailConstants.E_ELSE_RULE);
    Element elseRule2 = elseRules.addElement(MailConstants.E_ELSE_RULE);
    Element filteraction1 = elseRule1.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionRedirect1 = filteraction1.addElement(MailConstants.E_ACTION_REDIRECT);
    actionRedirect1.addAttribute("a", "redirect@zimbra.com");
    actionRedirect1.addAttribute(MailConstants.A_INDEX, "0");
    actionRedirect1.addAttribute(MailConstants.A_COPY, "0");
    Element actionLog1 = filteraction1.addElement(MailConstants.E_ACTION_LOG);
    actionLog1.addAttribute(MailConstants.A_INDEX, "1");
    actionLog1.addText("Forward message to qa-automation DL");
    Element filterTests1 = elseRule2.addElement(MailConstants.E_FILTER_TESTS);
    filterTests1.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest1 = filterTests1.addElement(MailConstants.E_HEADER_TEST);
    headerTest1.addAttribute(MailConstants.A_HEADER, "subject");
    headerTest1.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
    headerTest1.addAttribute(MailConstants.A_VALUE, "automation");
    headerTest1.addAttribute(MailConstants.A_INDEX, "0");
    Element filteraction2 = elseRule2.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionLog2 = filteraction2.addElement(MailConstants.E_ACTION_LOG);
    actionLog1.addAttribute(MailConstants.A_INDEX, "1");
    actionLog1.addText("Elsif after Else");

    try {
        new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        fail("This test is expected to throw exception. ");
    } catch (ServiceException e) {
        String expected = "invalid request: else or elsif can't follow else";
        assertTrue(e.getMessage().indexOf(expected)  != -1);
        assertNotNull(e);
    }
}

@Test
public void testNegative_ElseNestedInElse() throws Exception {
    Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
    Element request = new Element.XMLElement(MailConstants.MODIFY_FILTER_RULES_REQUEST);
    Element rules = request.addElement(MailConstants.E_FILTER_RULES);
    Element rule = rules.addElement(MailConstants.E_FILTER_RULE);
    rule.addAttribute(MailConstants.A_ACTIVE, true);
    rule.addAttribute(MailConstants.A_NAME, "Test1");
    Element filteraction = rule.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionInto = filteraction.addElement(MailConstants.E_ACTION_FILE_INTO);
    actionInto.addAttribute(MailConstants.A_FOLDER_PATH, "FromP2");
    Element actionLog =filteraction.addElement(MailConstants.E_ACTION_LOG);
    actionLog.addAttribute(MailConstants.A_INDEX, "1");
    actionLog.addText("Move message to FromP2 folder");
    Element filterTests = rule.addElement(MailConstants.E_FILTER_TESTS);
    filterTests.addAttribute(MailConstants.A_CONDITION, "anyof");
    Element headerTest = filterTests.addElement(MailConstants.E_HEADER_TEST);
    headerTest.addAttribute(MailConstants.A_HEADER, "Subject");
    headerTest.addAttribute(MailConstants.A_STRING_COMPARISON, "is");
    headerTest.addAttribute(MailConstants.A_VALUE, "important");
    headerTest.addAttribute(MailConstants.A_INDEX, "0");
    Element elseRules = rule.addElement(MailConstants.E_ELSE_RULES);
    Element elseRule1 = elseRules.addElement(MailConstants.E_ELSE_RULE);
    Element filteraction1 = elseRule1.addElement(MailConstants.E_FILTER_ACTIONS);
    Element actionLog1 = filteraction1.addElement(MailConstants.E_ACTION_LOG);
    actionLog1.addAttribute(MailConstants.A_INDEX, "1");
    actionLog1.addText("Inside Else block");
    Element nestedElseRules = elseRule1.addElement(MailConstants.E_ELSE_RULES);
    Element nestedElseRule = nestedElseRules.addElement(MailConstants.E_ELSE_RULE);
    Element actionLog2 = nestedElseRule.addElement(MailConstants.E_ACTION_LOG);
    actionLog2.addAttribute(MailConstants.A_INDEX, "1");
    actionLog2.addText("Else nested in another Else");

    try {
        new ModifyFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        fail("This test is expected to throw exception. ");
    } catch (ServiceException e) {
        String expected = "invalid request: elseRule can't have nested elseRule";
        assertTrue(e.getMessage().indexOf(expected)  != -1);
        assertNotNull(e);
    }
}
}
