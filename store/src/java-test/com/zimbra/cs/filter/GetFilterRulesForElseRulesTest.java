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

import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.GetFilterRules;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.util.XMLDiffChecker;

public class GetFilterRulesForElseRulesTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testIfElseIf() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        RuleManager.clearCachedRules(acct);
        String ss = "require [\"fileinto\", \"log\"];\n";
        ss += "if anyof header :is \"Subject\" \"important\" {\n";
        ss += "fileinto \"FromP2\"; log \"Move message to FromP2 folder\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"automation\", \"nunit\"]) \n";
        ss += "{ redirect \"redirect@zimbra.com\"; log \"Forward message to qa-automation DL\"; }\n";
        acct.setMailSieveScript(ss);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

        String expectedSoapResponse = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\"><filterRules><filterRule active=\"1\">"
              + "<filterTests condition=\"anyof\"><headerTest stringComparison=\"is\" header=\"Subject\" index=\"0\" value=\"important\"/>"
              + "</filterTests><filterActions><actionFileInto folderPath=\"FromP2\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">Move message to FromP2 folder</actionLog>"
              + "</filterActions><elseRules><elseRule><filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"automation\"/>"
              + "</filterTests><filterActions><actionRedirect a=\"redirect@zimbra.com\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">Forward message to qa-automation DL</actionLog>"
              + "</filterActions></elseRule></elseRules></filterRule></filterRules></GetFilterRulesResponse>";

        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testMultipleIfElseAndLastElse() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        RuleManager.clearCachedRules(acct);
        String ss = "require [\"fileinto\", \"log\"];\n";
        ss += "if anyof header :is \"Subject\" \"important\" {\n";
        ss += "fileinto \"FromP2\"; log \"Move message to FromP2 folder\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"automation\", \"nunit\"]) \n";
        ss += "{log \"first elseif\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"test\", \"nunit\"]) \n";
        ss += "{log \"second elseif \"; }\n";
        ss += "else \n";
        ss += "{log \"last else\"; }\n";
        acct.setMailSieveScript(ss);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

        String expectedSoapResponse = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\"><filterRules><filterRule active=\"1\">"
                + "<filterTests condition=\"anyof\"><headerTest stringComparison=\"is\" header=\"Subject\" index=\"0\" value=\"important\"/>"
                + "</filterTests><filterActions><actionFileInto folderPath=\"FromP2\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">Move message to FromP2 folder</actionLog>"
                + "</filterActions><elseRules><elseRule><filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"automation\"/>"
                + "</filterTests><filterActions><actionLog index=\"0\">first elseif</actionLog></filterActions></elseRule><elseRule><filterTests condition=\"anyof\">"
                + "<headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"test\"/></filterTests><filterActions><actionLog index=\"0\">second elseif </actionLog>"
                + "</filterActions></elseRule><elseRule><filterActions><actionLog index=\"0\">last else</actionLog></filterActions></elseRule></elseRules></filterRule>"
                + "</filterRules></GetFilterRulesResponse>";

        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testNestedIfElseIfInElseBlock() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        RuleManager.clearCachedRules(acct);
        String ss = "require [\"fileinto\", \"log\"];\n";
        ss += "if anyof header :is \"Subject\" \"important\" {\n";
        ss += "fileinto \"FromP2\"; log \"first if\"; }\n";
        ss += "else {\n";
        ss += "if anyof (header :contains \"subject\" [\"automation\", \"nunit\"]) \n";
        ss += "{fileinto \"NestedIf\"; log \"nested if\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"test\", \"junit\"]) \n";
        ss += "{fileinto \"ElseIfInNested\"; log \"second elseif \"; }\n";
        ss += "fileinto \"ElseFolder\"; log \"else\";\n}";
        acct.setMailSieveScript(ss);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

        String expectedSoapResponse = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\"><filterRules><filterRule active=\"1\">"
                + "<filterTests condition=\"anyof\"><headerTest stringComparison=\"is\" header=\"Subject\" index=\"0\" value=\"important\"/>"
                + "</filterTests><filterActions><actionFileInto folderPath=\"FromP2\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">first if</actionLog>"
                + "</filterActions><elseRules><elseRule><filterActions><actionFileInto folderPath=\"ElseFolder\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">else</actionLog>"
                + "</filterActions><nestedRule><filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"automation\"/>"
                + "</filterTests><filterActions><actionFileInto folderPath=\"NestedIf\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">nested if</actionLog></filterActions>"
                + "<elseRules><elseRule><filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"test\"/></filterTests>"
                + "<filterActions><actionFileInto folderPath=\"ElseIfInNested\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">second elseif </actionLog></filterActions>"
                + "</elseRule></elseRules></nestedRule></elseRule></elseRules></filterRule></filterRules></GetFilterRulesResponse>";

        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testNestedIfElseInElseIf() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        RuleManager.clearCachedRules(acct);
        String ss = "require [\"fileinto\", \"log\"];\n";
        ss += "if anyof header :is \"Subject\" \"important\" {\n";
        ss += "  fileinto \"FromP2\"; log \"Move message to FromP2 folder\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"automation\", \"nunit\"]) \n";
        ss += " {log \"first elseif\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"test\", \"nunit\"]) {\n";
        ss += "  if anyof (header :contains \"subject\" [\"nestedif\", \"nunit\"]) \n";
        ss += "    {fileinto \"NestedIf\"; log \"nested if\"; }\n";
        ss += "  elsif anyof (header :contains \"subject\" [\"elseif\", \"junit\"]) \n";
        ss += "    {fileinto \"ElseIfInNested\"; log \"second elseif \"; }\n";
        ss += "log \"second elseif \"; }\n";
        ss += "else \n";
        ss += "{log \"last else\"; }\n";
        acct.setMailSieveScript(ss);

        String expectedSoapResponse = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\"><filterRules> <filterRule active=\"1\">" +
             "<filterTests condition=\"anyof\"><headerTest stringComparison=\"is\" header=\"Subject\" index=\"0\" value=\"important\"/>" +
             "</filterTests> <filterActions><actionFileInto folderPath=\"FromP2\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">Move message to FromP2 folder</actionLog>" +
             "</filterActions><elseRules><elseRule><filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"automation\"/>" +
             "</filterTests><filterActions><actionLog index=\"0\">first elseif</actionLog></filterActions></elseRule><elseRule>" +
             "<filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"test\"/></filterTests>" +
             "<filterActions><actionLog index=\"0\">second elseif </actionLog></filterActions><nestedRule><filterTests condition=\"anyof\">" +
             "<headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"nestedif\"/></filterTests>" +
             "<filterActions><actionFileInto folderPath=\"NestedIf\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">nested if</actionLog></filterActions>" +
             "<elseRules><elseRule><filterTests condition=\"anyof\"><headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"elseif\"/>" +
             "</filterTests><filterActions><actionFileInto folderPath=\"ElseIfInNested\" index=\"0\" copy=\"0\"/><actionLog index=\"1\">second elseif </actionLog>" +
             "</filterActions></elseRule></elseRules></nestedRule></elseRule><elseRule><filterActions><actionLog index=\"0\">last else</actionLog></filterActions>" +
             "</elseRule></elseRules></filterRule></filterRules></GetFilterRulesResponse>";

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }
}
