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
import com.zimbra.common.account.Key;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.MockHttpServletRequest;
import com.zimbra.soap.MockSoapEngine;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.common.soap.SoapProtocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GetFilterRulesTest {
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
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedSoap);
        Assert.assertEquals(expectedSoap, rules.prettyPrint());

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
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedSoap);
        Assert.assertEquals(expectedSoap, rules.prettyPrint());

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

        /* Expected Response:
        <filterRules>
        <filterRule name="test" active="1">
            <filterTests condition="anyof">
                <headerTest stringComparison="is" header="Subject" index="0" value="important"/>
            </filterTests>
            <nestedRule>
                <filterTests condition="allof">
                    <headerTest stringComparison="contains" header="Subject" index="0" value="confidential"/>
                    <sizeTest s="1" numberComparison="over" index="1"/>
                </filterTests>
                <filterActions>
                    <actionFlag flagName="priority" index="0"/>
                </filterActions>
            </nestedRule>
        </filterRule>
        </filterRules>
        */

        Element rules = response.getOptionalElement(MailConstants.E_FILTER_RULES);
        Assert.assertEquals("anyof", rules.getElement("filterRule").getElement("filterTests").getAttribute("condition"));
        Assert.assertEquals("is",rules.getElement("filterRule").getElement("filterTests").getElement("headerTest").getAttribute("stringComparison"));
        Assert.assertEquals("Subject",rules.getElement("filterRule").getElement("filterTests").getElement("headerTest").getAttribute("header"));
        Assert.assertEquals("important",rules.getElement("filterRule").getElement("filterTests").getElement("headerTest").getAttribute("value"));
        Assert.assertEquals("0",rules.getElement("filterRule").getElement("filterTests").getElement("headerTest").getAttribute("index"));
        Assert.assertEquals("allof",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getAttribute("condition"));
        Assert.assertEquals("contains",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("headerTest").getAttribute("stringComparison"));
        Assert.assertEquals("Subject",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("headerTest").getAttribute("header"));
        Assert.assertEquals("confidential",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("headerTest").getAttribute("value"));
        Assert.assertEquals("0",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("headerTest").getAttribute("index"));
        Assert.assertEquals("1",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("sizeTest").getAttribute("s"));
        Assert.assertEquals("over",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("sizeTest").getAttribute("numberComparison"));
        Assert.assertEquals("1",rules.getElement("filterRule").getElement("nestedRule").getElement("filterTests").getElement("sizeTest").getAttribute("index"));
        Assert.assertEquals("priority",rules.getElement("filterRule").getElement("nestedRule").getElement("filterActions").getElement("actionFlag").getAttribute("flagName"));
        Assert.assertEquals("0",rules.getElement("filterRule").getElement("nestedRule").getElement("filterActions").getElement("actionFlag").getAttribute("index"));
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
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedSoap);
        Assert.assertEquals(expectedSoap, rules.prettyPrint());

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
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedSoap);
        Assert.assertEquals(expectedSoap, rules.prettyPrint());

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
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedSoap);
        Assert.assertEquals(expectedSoap, rules.prettyPrint());

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
        //ZimbraLog.filter.info(rules.prettyPrint());
        //ZimbraLog.filter.info(expectedSoap);
        Assert.assertEquals(expectedSoap, rules.prettyPrint());

    }

}
