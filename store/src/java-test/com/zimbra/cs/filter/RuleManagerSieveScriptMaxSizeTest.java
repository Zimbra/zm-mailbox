/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;

/**
 * Unit test for {@link com.zimbra.cs.filter.RuleManager} and
 * sieve script size limits.
 *
 * @author bsinger
 */
public final class RuleManagerSieveScriptMaxSizeTest {

    private static Account account;
    private static Config config;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain("testdomain.biz", new HashMap<String, Object>());
        account = prov.createAccount("test@testdomain.biz", "secret", new HashMap<String, Object>());
        config = prov.getConfig();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testSieveScriptBelowThreshold() throws Exception {
        try {
            config.setMailSieveScriptMaxSize(100000L);
            account.setMailSieveScriptMaxSize(100000L);
            account.unsetMailSieveScript();

            RuleManager.setIncomingXMLRules(account, generateSmallFilterRules());

            Assert.assertNotNull(account.getMailSieveScript());
        } catch (Exception e) {
            Assert.fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSieveScriptReducedSizeButExceedsThreshold() throws Exception {
        try {
            config.setMailSieveScriptMaxSize(100000L);
            account.setMailSieveScriptMaxSize(100000L);
            account.unsetMailSieveScript();

            RuleManager.setIncomingXMLRules(account, generateLargeFilterRules());
            long oldSieveScriptSize = account.getMailSieveScript().getBytes(StandardCharsets.UTF_8).length;

            account.setMailSieveScriptMaxSize(1L);

            RuleManager.setIncomingXMLRules(account, generateSmallFilterRules());
            long newSieveScriptSize = account.getMailSieveScript().getBytes(StandardCharsets.UTF_8).length;

            Assert.assertFalse(newSieveScriptSize == oldSieveScriptSize);
        } catch (Exception e) {
            Assert.fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testOutgoingSieveScriptReducedSizeButExceedsThreshold() throws Exception {
        try {
            config.setMailSieveScriptMaxSize(100000L);
            account.setMailSieveScriptMaxSize(100000L);
            account.unsetMailOutgoingSieveScript();

            RuleManager.setOutgoingXMLRules(account, generateLargeFilterRules());
            long oldSieveScriptSize = account.getMailOutgoingSieveScript().getBytes(StandardCharsets.UTF_8).length;

            account.setMailSieveScriptMaxSize(1L);

            RuleManager.setOutgoingXMLRules(account, generateSmallFilterRules());
            long newSieveScriptSize = account.getMailOutgoingSieveScript().getBytes(StandardCharsets.UTF_8).length;

            Assert.assertFalse(newSieveScriptSize == oldSieveScriptSize);
        } catch (Exception e) {
            Assert.fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test(expected = ServiceException.class)
    public void testSieveScriptExceedsThreshold() throws Exception {
        config.setMailSieveScriptMaxSize(100000L);
        account.setMailSieveScriptMaxSize(1L);
        account.unsetMailSieveScript();

        RuleManager.setIncomingXMLRules(account, generateSmallFilterRules());
    }

    @Test(expected = ServiceException.class)
    public void testSieveScriptExceedsGlobalThreshold() throws Exception {
        config.setMailSieveScriptMaxSize(1L);
        account.unsetMailSieveScriptMaxSize();
        account.unsetMailSieveScript();

        RuleManager.setIncomingXMLRules(account, generateSmallFilterRules());
    }

    @Test(expected = ServiceException.class)
    public void testSieveScriptLargerAndExceedsThreshold() throws Exception {
        config.setMailSieveScriptMaxSize(100000L);
        account.setMailSieveScriptMaxSize(100000L);
        account.unsetMailSieveScript();

        RuleManager.setIncomingXMLRules(account, generateSmallFilterRules());
        long oldSieveScriptSize = account.getMailSieveScript().getBytes(StandardCharsets.UTF_8).length;

        account.setMailSieveScriptMaxSize(1L);

        RuleManager.setIncomingXMLRules(account, generateLargeFilterRules());
    }

    @Test(expected = ServiceException.class)
    public void testOutgoingSieveScriptLargerAndExceedsThreshold() throws Exception {
        config.setMailSieveScriptMaxSize(100000L);
        account.setMailSieveScriptMaxSize(100000L);
        account.unsetMailOutgoingSieveScript();

        RuleManager.setOutgoingXMLRules(account, generateSmallFilterRules());
        long oldSieveScriptSize = account.getMailOutgoingSieveScript().getBytes(StandardCharsets.UTF_8).length;

        account.setMailSieveScriptMaxSize(1L);

        RuleManager.setOutgoingXMLRules(account, generateLargeFilterRules());
    }

    private List<FilterRule> generateLargeFilterRules() {
        return generateFilterRules(true);
    }

    private List<FilterRule> generateSmallFilterRules() {
        return generateFilterRules(false);
    }

    private List<FilterRule> generateFilterRules(boolean large) {
        FilterRule rule = new FilterRule("testFilterRule", true);

        FilterTest.AddressTest test = new FilterTest.AddressTest();
        test.setHeader("to");
        test.setStringComparison("is");
        test.setPart("all");
        test.setValue("testValue");
        test.setIndex(0);

        FilterTests tests = new FilterTests("anyof");
        tests.addTest(test);
        rule.setFilterTests(tests);

        FilterAction keepAction = new FilterAction.KeepAction();
        keepAction.setIndex(0);
        rule.addFilterAction(keepAction);

        if (large) {
            FilterAction stopAction = new FilterAction.StopAction();
            stopAction.setIndex(1);
            rule.addFilterAction(stopAction);
        }

        List<FilterRule> filterRuleList = new ArrayList<FilterRule>();
        filterRuleList.add(rule);

        return filterRuleList;
    }
}
