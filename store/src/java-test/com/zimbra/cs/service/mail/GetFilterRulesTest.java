/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import com.google.common.collect.Maps;
import org.junit.Ignore;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.MockHttpServletRequest;
import com.zimbra.cs.util.XMLDiffChecker;
import com.zimbra.soap.MockSoapEngine;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.common.soap.SoapProtocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class GetFilterRulesTest {
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
    public void testBug71036_SingleRuleSimpleNestedIfwithSOAP() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String ss = "# test\n";
        ss += "if anyof header :is \"Subject\" \"important\" {\n";
        ss += "    if anyof header :contains \"Subject\" \"confidential\" {\n";
        ss += "        flag \"priority\";\n";
        ss += "    }\n";
        ss += "}";
        acct.setMailSieveScript(ss);
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"anyof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"anyof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }
    // allof(with multi conditions) then anyof
    @Test
    public void testBug71036_SingleRuleSimpleNestedIfwithMultiConditions01() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String ss = "# test\n";
        ss += "if allof (header :is \"Subject\" \"important\",\n";
        ss += "          header :contains \"From\" \"zimbra\") {\n";
        ss += "    if anyof header :contains \"Subject\" \"confidential\" {\n";
        ss += "        flag \"priority\";\n";
        ss += "    }\n";
        ss += "}";
        acct.setMailSieveScript(ss);
        //acct.setMailSieveScript("# test\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}");
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"allof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "      <headerTest index=\"1\" value=\"zimbra\" stringComparison=\"contains\" header=\"From\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"anyof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }

    // anyof then allof(with multi conditions)
    @Test
    public void testBug71036_SingleRuleSimpleNestedIfwithMultiConditions02() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String ss = "# test\n";
        ss += "if anyof header :is \"Subject\" \"important\" {\n";
        ss += "    if allof (header :contains \"Subject\" \"confidential\", \n";
        ss += "              size :over 1k) {\n";
        ss += "        flag \"priority\";\n";
        ss += "    }\n";
        ss += "}";
        acct.setMailSieveScript(ss);
        //acct.setMailSieveScript("# test\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}");
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"anyof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"allof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "        <sizeTest index=\"1\" numberComparison=\"over\" s=\"1\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }

    // allof(with multi conditions) then allof(with multi conditions)
    @Test
    public void testBug71036_SingleRuleSimpleNestedIfwithMultiConditions03() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String ss = "# test\n";
        ss += "if allof (header :is \"Subject\" \"important\", \n";
        ss += "          header :contains \"From\" \"zimbra\") {\n";
        ss += "    if allof (header :contains \"Subject\" \"confidential\", \n";
        ss += "              size :over 1k) {\n";
        ss += "        flag \"priority\";\n";
        ss += "    }\n";
        ss += "}";
        acct.setMailSieveScript(ss);
        //acct.setMailSieveScript("# test\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}");
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"allof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "      <headerTest index=\"1\" value=\"zimbra\" stringComparison=\"contains\" header=\"From\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"allof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "        <sizeTest index=\"1\" numberComparison=\"over\" s=\"1\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }

    // allof(with multi conditions) then anyof(with multi conditions)
    @Test
    public void testBug71036_SingleRuleSimpleNestedIfwithMultiConditions() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String ss = "# test\n";
        ss += "if allof (header :is \"Subject\" \"important\", \n";
        ss += "          header :contains \"From\" \"zimbra\") {\n";
        ss += "    if anyof (header :contains \"Subject\" \"confidential\", \n";
        ss += "              size :over 1k) {\n";
        ss += "        flag \"priority\";\n";
        ss += "    }\n";
        ss += "}";
        acct.setMailSieveScript(ss);
        //acct.setMailSieveScript("# test\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}");
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"allof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "      <headerTest index=\"1\" value=\"zimbra\" stringComparison=\"contains\" header=\"From\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"anyof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "        <sizeTest index=\"1\" numberComparison=\"over\" s=\"1\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }

    @Test
    public void testBug71036_SingleRuleSimpleNestedIfwithJSON() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        acct.setMailSieveScript("# test2\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}");
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.SoapJS));
        context.put(SoapServlet.SERVLET_REQUEST, new MockHttpServletRequest("test".getBytes("UTF-8"), new URL("http://localhost:7070/service/FooRequest"), ""));
        context.put(SoapEngine.ZIMBRA_ENGINE, new MockSoapEngine(new MailService()));
        //return context;

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, context);


        String expectedJson = "{\n";
        expectedJson += "  \"filterRule\": [{\n";
        expectedJson += "      \"name\": \"test2\",\n";
        expectedJson += "      \"active\": true,\n";
        expectedJson += "      \"filterTests\": [{\n";
        expectedJson += "          \"condition\": \"anyof\",\n";
        expectedJson += "          \"headerTest\": [{\n";
        expectedJson += "              \"index\": 0,\n";
        expectedJson += "              \"header\": \"Subject\",\n";
        expectedJson += "              \"stringComparison\": \"is\",\n";
        expectedJson += "              \"value\": \"important\"\n";
        expectedJson += "            }]\n";
        expectedJson += "        }],\n";
        //expectedJson += "      \"filterActions\": [{}],\n";
        expectedJson += "      \"nestedRule\": [{\n";
        expectedJson += "          \"filterTests\": [{\n";
        expectedJson += "              \"condition\": \"anyof\",\n";
        expectedJson += "              \"headerTest\": [{\n";
        expectedJson += "                  \"index\": 0,\n";
        expectedJson += "                  \"header\": \"Subject\",\n";
        expectedJson += "                  \"stringComparison\": \"contains\",\n";
        expectedJson += "                  \"value\": \"confidential\"\n";
        expectedJson += "                }]\n";
        expectedJson += "            }],\n";
        expectedJson += "          \"filterActions\": [{\n";
        expectedJson += "              \"actionFlag\": [{\n";
        expectedJson += "                  \"flagName\": \"priority\",\n";
        expectedJson += "                  \"index\": 0\n";
        expectedJson += "                }]\n";
        expectedJson += "            }]\n";
        expectedJson += "        }]\n";
        expectedJson += "    }]\n";
        expectedJson += "}";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedJson);
        Assert.assertEquals(expectedJson, rules.prettyPrint());

    }

    @Test
    public void testBug71036_SingleRuleMultiNestedIf() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        RuleManager.clearCachedRules(acct);
        // set One rule with multi nested if
        String ss = "# test3\nif anyof header :is \"Subject\" \"important\" {\n";
        ss += "    if anyof header :contains \"Subject\" \"confidential\" {\n";
        ss += "                if anyof header :contains \"Subject\" \"secret\" {\n";
        ss += "                         if anyof header :contains \"Subject\" \"project\" {\n";
        ss += "flag \"priority\";\n";
        ss += "}\n";
        ss += "}\n";
        ss += "}\n";
        ss += "}";

        acct.setMailSieveScript(ss);


        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test3\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"anyof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"anyof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "      </filterTests>\n";
        //expectedSoap += "      <filterActions/>\n";
        expectedSoap += "      <nestedRule>\n";
        expectedSoap += "        <filterTests condition=\"anyof\">\n";
        expectedSoap += "          <headerTest index=\"0\" value=\"secret\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "        </filterTests>\n";
        //expectedSoap += "        <filterActions/>\n";
        expectedSoap += "        <nestedRule>\n";
        expectedSoap += "          <filterTests condition=\"anyof\">\n";
        expectedSoap += "            <headerTest index=\"0\" value=\"project\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "          </filterTests>\n";
        expectedSoap += "          <filterActions>\n";
        expectedSoap += "            <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "          </filterActions>\n";
        expectedSoap += "        </nestedRule>\n";
        expectedSoap += "      </nestedRule>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }

    @Test
    public void testBug1281_SingleRuleElsif() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

        RuleManager.clearCachedRules(acct);
        // set One rule with elsif
        String ss = "require [\"fileinto\", \"log\"];\n";
        ss += "if anyof address :is \"from\" \"p2@zqa-380.eng.zimbra.com\" {\n";
        ss += "fileinto \"FromP2\"; log \"Move message to FromP2 folder\"; }\n";
        ss += "elsif anyof (header :contains \"subject\" [\"automation\", \"nunit\"]) \n";
        ss += "{ redirect \"qa-automation@zqa-380.eng.zimbra.com\"; log \"Forward message to qa-automation DL\"; }\n";

        acct.setMailSieveScript(ss);
        try {
            List<FilterRule> ids = RuleManager.getIncomingRulesAsXML(acct);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception should be thrown");
        }
    }

    @Test
    public void testBug71036_MultiRuleSimpleNestedIf() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        RuleManager.clearCachedRules(acct);

        // set Multi rule withs one nested if for each
        String ss = "# test4\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}";
        ss = ss + "\n";
        ss = ss + "# test5\nif anyof header :is \"Subject\" \"important\" {\nif anyof header :contains \"Subject\" \"confidential\" { flag \"priority\"; }}";

        acct.setMailSieveScript(ss);
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

        String expectedSoap = "<filterRules>\n";
        expectedSoap += "  <filterRule name=\"test4\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"anyof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"anyof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "  <filterRule name=\"test5\" active=\"1\">\n";
        expectedSoap += "    <filterTests condition=\"anyof\">\n";
        expectedSoap += "      <headerTest index=\"0\" value=\"important\" stringComparison=\"is\" header=\"Subject\"/>\n";
        expectedSoap += "    </filterTests>\n";
        //expectedSoap += "    <filterActions/>\n";
        expectedSoap += "    <nestedRule>\n";
        expectedSoap += "      <filterTests condition=\"anyof\">\n";
        expectedSoap += "        <headerTest index=\"0\" value=\"confidential\" stringComparison=\"contains\" header=\"Subject\"/>\n";
        expectedSoap += "      </filterTests>\n";
        expectedSoap += "      <filterActions>\n";
        expectedSoap += "        <actionFlag index=\"0\" flagName=\"priority\"/>\n";
        expectedSoap += "      </filterActions>\n";
        expectedSoap += "    </nestedRule>\n";
        expectedSoap += "  </filterRule>\n";
        expectedSoap += "</filterRules>";


        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        XMLDiffChecker.assertXMLEquals(expectedSoap, rules.prettyPrint());

    }

    @Test
    public void testEnvelopeTest() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String filterScript = "require \"envelope\";\n"
            + "#test\n if anyof envelope :all :is \"from\" \"tim@example.com\" {\n"
            + "tag \"t2\";\n"
            + "}";
        acct.setMailSieveScript(filterScript);
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
            + "<filterRules><filterRule active=\"1\"><filterTests condition=\"anyof\">"
            + "<envelopeTest stringComparison=\"is\" part=\"all\" header=\"from\" index=\"0\" value=\"tim@example.com\"/>"
            + "</filterTests><filterActions><actionTag index=\"0\" tagName=\"t2\"/>"
            + "</filterActions></filterRule></filterRules></GetFilterRulesResponse>";

        XMLDiffChecker.assertXMLEquals(expectedSoap, response.prettyPrint());

    }

    @Test
    public void testEnvelopeTestNumericComparison() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // set One rule with one nested if
        RuleManager.clearCachedRules(acct);
        String filterScript = "require \"envelope\";\n"
            + "#test\n if anyof envelope :count \"eq\" :all \"from\" \"1\" {\n"
            + "tag \"t2\";\n"
            + "}";
        acct.setMailSieveScript(filterScript);
        //ZimbraLog.filter.info(acct.getMailSieveScript());

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));


        String expectedSoap = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
            + "<filterRules><filterRule active=\"1\"><filterTests condition=\"anyof\">"
            + "<envelopeTest countComparison=\"eq\" part=\"all\" header=\"from\" index=\"0\" value=\"1\"/>"
            + "</filterTests><filterActions><actionTag index=\"0\" tagName=\"t2\"/>"
            + "</filterActions></filterRule></filterRules></GetFilterRulesResponse>";

        XMLDiffChecker.assertXMLEquals(expectedSoap, response.prettyPrint());

    }

    @Test
    public void testFilterVariables() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            RuleManager.clearCachedRules(acct);

            String filterScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\n" +
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
            acct.setMailSieveScript(filterScript);

            Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
            Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

            String expectedSoap = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">\n" +
                "  <filterRules>\n" +
                "    <filterRule name=\"t60\" active=\"1\">\n" +
                "      <filterVariables index=\"0\">\n" +
                "        <filterVariable name=\"var\" value=\"testTag\"/>\n" +
                "        <filterVariable name=\"var_new\" value=\"${var}\"/>\n" +
                "      </filterVariables>\n" +
                "      <filterTests condition=\"anyof\">\n" +
                "        <headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"test\"/>\n" +
                "      </filterTests>\n" +
                "      <filterActions>\n" +
                "        <actionTag index=\"0\" tagName=\"${var_new}\"/>\n" +
                "        <filterVariables index=\"0\">\n" +
                "          <filterVariable name=\"v1\" value=\"blah blah\"/>\n" +
                "          <filterVariable name=\"v2\" value=\"${v1}\"/>\n" +
                "          <filterVariable name=\"t1\" value=\"ttttt\"/>\n" +
                "        </filterVariables>\n" +
                "      </filterActions>\n" +
                "      <nestedRule>\n" +
                "        <filterTests condition=\"anyof\">\n" +
                "          <headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"abc\"/>\n" +
                "        </filterTests>\n" +
                "        <filterActions>\n" +
                "          <actionTag index=\"0\" tagName=\"${v2}\"/>\n" +
                "          <actionTag index=\"1\" tagName=\"${t1}\"/>\n" +
                "          <filterVariables index=\"0\">\n" +
                "            <filterVariable name=\"v3\" value=\"bbbbbbbbbbbb\"/>\n" +
                "          </filterVariables>\n" +
                "        </filterActions>\n" +
                "        <nestedRule>\n" +
                "          <filterTests condition=\"anyof\">\n" +
                "            <headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"def\"/>\n" +
                "          </filterTests>\n" +
                "          <filterActions>\n" +
                "            <filterVariables index=\"0\">\n" +
                "              <filterVariable name=\"v4\" value=\"${v3}\"/>\n" +
                "            </filterVariables>\n" +
                "          </filterActions>\n" +
                "          <nestedRule>\n" +
                "            <filterTests condition=\"anyof\">\n" +
                "              <headerTest stringComparison=\"contains\" header=\"subject\" index=\"0\" value=\"def\"/>\n" +
                "            </filterTests>\n" +
                "            <filterActions>\n" +
                "              <filterVariables index=\"0\">\n" +
                "                <filterVariable name=\"v5\" value=\"${v4}\"/>\n" +
                "              </filterVariables>\n" +
                "            </filterActions>\n" +
                "          </nestedRule>\n" +
                "        </nestedRule>\n" +
                "      </nestedRule>\n" +
                "    </filterRule>\n" +
                "  </filterRules>\n" +
                "</GetFilterRulesResponse>";

            XMLDiffChecker.assertXMLEquals(expectedSoap, response.prettyPrint());
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testFilterVariablesForMatchVariables() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            RuleManager.clearCachedRules(acct);

            String filterScript = "require [\"fileinto\", \"reject\", \"tag\", \"flag\", \"variables\", \"log\", \"enotify\"];\n" +
                "\n" +
                "# t60\n" +
                "set \"var\" \"testTag\";\n" +
                "set \"var_new\" \"${var}\";\n" +
                "if anyof (header :matches [\"subject\"] \"test\") {\n" +
                "    tag \"${var_new}\";\n" +
                "    set \"v1\" \"blah blah\";\n" +
                "    set \"v2\" \"${1}\";\n" +
                "    set \"v3\" \"${2}\";\n" +
                "}";
            acct.setMailSieveScript(filterScript);

            Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
            Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));

            String expectedSoap = "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">\n" +
                "  <filterRules>\n" +
                "    <filterRule name=\"t60\" active=\"1\">\n" +
                "      <filterVariables index=\"0\">\n" +
                "        <filterVariable name=\"var\" value=\"testTag\"/>\n" +
                "        <filterVariable name=\"var_new\" value=\"${var}\"/>\n" +
                "      </filterVariables>\n" +
                "      <filterTests condition=\"anyof\">\n" +
                "        <headerTest stringComparison=\"matches\" header=\"subject\" index=\"0\" value=\"test\"/>\n" +
                "      </filterTests>\n" +
                "      <filterActions>\n" +
                "        <actionTag index=\"0\" tagName=\"${var_new}\"/>\n" +
                "        <filterVariables index=\"1\">\n" +
                "          <filterVariable name=\"v1\" value=\"blah blah\"/>\n" +
                "          <filterVariable name=\"v2\" value=\"${1}\"/>\n" +
                "          <filterVariable name=\"v3\" value=\"${2}\"/>\n" +
                "        </filterVariables>\n" +
                "      </filterActions>\n" +
                "    </filterRule>\n" +
                "  </filterRules>\n" +
                "</GetFilterRulesResponse>";

            XMLDiffChecker.assertXMLEquals(expectedSoap, response.prettyPrint());
        } catch (Exception e) {
            fail("No exception should be thrown" + e);
        }
    }

    @Test
    public void testNegativeCaseAddheaderAction() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            RuleManager.clearCachedRules(acct);

            String filterScript = "require [\"editheader\"];\n" +
                "\n" +
                "# t60\n" +
                "addheader \"X-Test-Header\" \"test value\";\n";
            acct.setMailSieveScript(filterScript);

            Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
            new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals("parse error: Invalid addheader action: addheader action is not allowed in user scripts", e.getMessage());
        }
    }

    @Test
    public void testNegativeCaseDeleteheaderAction() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            RuleManager.clearCachedRules(acct);

            String filterScript = "require [\"editheader\"];\n" +
                "\n" +
                "# t60\n" +
                "deleteheader \"X-Test-Header\";\n";
            acct.setMailSieveScript(filterScript);

            Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
            new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals("parse error: Invalid deleteheader action: deleteheader action is not allowed in user scripts", e.getMessage());
        }
    }

    @Test
    public void testNegativeCaseReplaceheaderAction() {
        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
            RuleManager.clearCachedRules(acct);

            String filterScript = "require [\"editheader\"];\n" +
                "\n" +
                "# t60\n" +
                "replaceheader :newvalue \"[test]\" :is \"X-Test-Header\" \"test value\";\n";
            acct.setMailSieveScript(filterScript);

            Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
            new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(acct));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals("parse error: Invalid replaceheader action: replaceheader action is not allowed in user scripts", e.getMessage());
        }
    }
}
