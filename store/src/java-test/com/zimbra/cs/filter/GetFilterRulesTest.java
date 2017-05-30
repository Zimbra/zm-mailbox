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

import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.GetFilterRules;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.util.XMLDiffChecker;

public class GetFilterRulesTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testIfWithoutAllof() throws Exception {
        // - no 'allof' 'anyof' tests
        String filterScript
                    = "require \"tag\";"
                    + "if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"*\" {"
                    + "  fileinto \"if-block\";"
                    + "}";
        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
                "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
                + "<filterRules>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<headerTest stringComparison=\"matches\" header=\"Subject\" index=\"0\" value=\"*\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"if-block\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                + "</filterRules>"
              + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testNestedIfWithoutAllof() throws Exception {
        // - no 'allof' 'anyof' tests
        // - nested if
        String filterScript
                    = "require \"tag\";"
                    + "if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"*\" {"
                    + "  if header :matches \"From\" \"*\" {"
                    + "    fileinto \"nested-if-block\";"
                    + "  }"
                    + "}";
        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
              "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
              + "<filterRules>"
                + "<filterRule active=\"1\">"
                  + "<filterTests condition=\"allof\">"
                    + "<headerTest stringComparison=\"matches\" header=\"Subject\" index=\"0\" value=\"*\"/>"
                  + "</filterTests>"
                  + "<nestedRule>"
                    + "<filterTests condition=\"allof\">"
                      + "<headerTest stringComparison=\"matches\" header=\"From\" index=\"0\" value=\"*\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"nested-if-block\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</nestedRule>"
                + "</filterRule>"
              + "</filterRules>"
            + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testNestedIfAllofAnyof() throws Exception {
        // - nested if
        // - no 'allof' and 'anyof' test for the outer if block
        // - 'anyof' test for the inner if block
        String filterScript
                    = "require \"tag\";"
                    + "if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"*\" {"
                    + "  if anyof (header :matches \"From\" \"*\","
                    + "            header :matches \"To\"   \"*\") {"
                    + "    fileinto \"nested-if-block\";"
                    + "  }"
                    + "}";
        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
              "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
              + "<filterRules>"
                + "<filterRule active=\"1\">"
                  + "<filterTests condition=\"allof\">"
                    + "<headerTest stringComparison=\"matches\" header=\"Subject\" index=\"0\" value=\"*\"/>"
                  + "</filterTests>"
                  + "<nestedRule>"
                    + "<filterTests condition=\"anyof\">"
                      + "<headerTest stringComparison=\"matches\" header=\"From\" index=\"0\" value=\"*\"/>"
                      + "<headerTest stringComparison=\"matches\" header=\"To\"   index=\"1\" value=\"*\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"nested-if-block\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</nestedRule>"
                + "</filterRule>"
              + "</filterRules>"
            + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testWithoutIfBlock1() throws Exception {
        // - no if block
        String filterScript
                    = "require \"tag\";"
                    + "fileinto \"no-if-block\";";
        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
                "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
                + "<filterRules>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<trueTest index=\"0\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"no-if-block\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                + "</filterRules>"
              + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testWithoutIfBlock2() throws Exception {
        // - multiple no if block
        String filterScript
                    = "require \"tag\";"
                    + "fileinto \"no-if-block1\";"
                    + "fileinto \"no-if-block2\";";

        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
                "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
                + "<filterRules>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<trueTest index=\"0\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"no-if-block1\" index=\"0\"/>"
                      + "<actionFileInto folderPath=\"no-if-block2\" index=\"1\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                + "</filterRules>"
              + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testWithoutIfBlock3() throws Exception {
        // - no if block, then nested if without allof/anyof
        String filterScript
                    = "require \"tag\";"
                    + "fileinto \"no-if-block\";"
                    + "if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"*\" {"
                    + "  if header :matches \"From\" \"*\" {"
                    + "    fileinto \"nested-if-block\";"
                    + "  }"
                    + "}";

        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
                "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
                + "<filterRules>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<trueTest index=\"0\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"no-if-block\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                  + "<filterRule active=\"1\">"
                  + "<filterTests condition=\"allof\">"
                    + "<headerTest stringComparison=\"matches\" header=\"Subject\" index=\"0\" value=\"*\"/>"
                    + "</filterTests>"
                    + "<nestedRule>"
                      + "<filterTests condition=\"allof\">"
                        + "<headerTest stringComparison=\"matches\" header=\"From\" index=\"0\" value=\"*\"/>"
                      + "</filterTests>"
                      + "<filterActions>"
                        + "<actionFileInto folderPath=\"nested-if-block\" index=\"0\"/>"
                      + "</filterActions>"
                    + "</nestedRule>"
                  + "</filterRule>"
                + "</filterRules>"
              + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testWithoutIfBlock4() throws Exception {
        // - nested if without allof/anyof, then no if block
        String filterScript
                    = "require \"tag\";"
                    + "if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"*\" {"
                    + "  if header :matches \"From\" \"*\" {"
                    + "    fileinto \"nested-if-block\";"
                    + "  }"
                    + "}"
                    + "fileinto \"no-if-block\";";

        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
                "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
                + "<filterRules>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<headerTest stringComparison=\"matches\" header=\"Subject\" index=\"0\" value=\"*\"/>"
                    + "</filterTests>"
                    + "<nestedRule>"
                      + "<filterTests condition=\"allof\">"
                        + "<headerTest stringComparison=\"matches\" header=\"From\" index=\"0\" value=\"*\"/>"
                      + "</filterTests>"
                      + "<filterActions>"
                        + "<actionFileInto folderPath=\"nested-if-block\" index=\"0\"/>"
                      + "</filterActions>"
                    + "</nestedRule>"
                  + "</filterRule>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<trueTest index=\"0\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"no-if-block\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                + "</filterRules>"
              + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }

    @Test
    public void testWithoutIfBlock5() throws Exception {
        // - combination of nested if without allof/anyof, and no if block
        String filterScript
                    = "require \"tag\";"
                    + "fileinto \"no-if-block1\";"
                    + "if header :comparator \"i;ascii-casemap\" :matches \"Subject\" \"*\" {"
                    + "  if header :matches \"From\" \"*\" {"
                    + "    fileinto \"nested-if-block\";"
                    + "  }"
                    + "}"
                    + "fileinto \"no-if-block2\";";

        Account account = Provisioning.getInstance().getAccount(
                MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        account.setMailSieveScript(filterScript);

        Element request = new Element.XMLElement(MailConstants.GET_FILTER_RULES_REQUEST);
        Element response = new GetFilterRules().handle(request, ServiceTestUtil.getRequestContext(account));

        String expectedSoapResponse =
                "<GetFilterRulesResponse xmlns=\"urn:zimbraMail\">"
                + "<filterRules>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<trueTest index=\"0\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"no-if-block1\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<headerTest stringComparison=\"matches\" header=\"Subject\" index=\"0\" value=\"*\"/>"
                    + "</filterTests>"
                    + "<nestedRule>"
                      + "<filterTests condition=\"allof\">"
                        + "<headerTest stringComparison=\"matches\" header=\"From\" index=\"0\" value=\"*\"/>"
                      + "</filterTests>"
                      + "<filterActions>"
                        + "<actionFileInto folderPath=\"nested-if-block\" index=\"0\"/>"
                      + "</filterActions>"
                    + "</nestedRule>"
                  + "</filterRule>"
                  + "<filterRule active=\"1\">"
                    + "<filterTests condition=\"allof\">"
                      + "<trueTest index=\"0\"/>"
                    + "</filterTests>"
                    + "<filterActions>"
                      + "<actionFileInto folderPath=\"no-if-block2\" index=\"0\"/>"
                    + "</filterActions>"
                  + "</filterRule>"
                + "</filterRules>"
              + "</GetFilterRulesResponse>";
        XMLDiffChecker.assertXMLEquals(expectedSoapResponse, response.prettyPrint());
    }
}