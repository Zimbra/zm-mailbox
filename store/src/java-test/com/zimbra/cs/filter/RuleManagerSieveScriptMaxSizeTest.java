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
import com.zimbra.cs.filter.RuleManager;
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

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain("zimbra.com", new HashMap<String, Object>());
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testSieveScriptBelowThreshold() throws Exception {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            RuleManager.clearCachedRules(account);

            account.setMailSieveScriptMaxSize(100000L);

            List<FilterRule> filterRuleList = generateFilterRules();
            RuleManager.setIncomingXMLRules(account, filterRuleList);

            String sieve = account.getMailSieveScript();
            Assert.assertNotNull(sieve);
        } catch (Exception e) {
            Assert.fail("No exception should be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSieveScriptExceedsThreshold() throws Exception {
        try {
            Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            RuleManager.clearCachedRules(account);

            account.setMailSieveScriptMaxSize(1L);

            List<FilterRule> filterRuleList = generateFilterRules();
            RuleManager.setIncomingXMLRules(account, filterRuleList);

            String sieve = account.getMailSieveScript();
            Assert.assertNull(sieve);
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        } catch (Exception e) {
            Assert.fail("No exception should be thrown: " + e.getMessage());
        }
    }

    private List<FilterRule> generateFilterRules() {
        FilterRule rule = new FilterRule("testFilterRule", true);

        FilterTest.AddressTest test = new FilterTest.AddressTest();
        test.setHeader("to");
        test.setStringComparison("is");
        test.setPart("all");
        test.setValue("testValue");
        test.setIndex(0);

        FilterTests tests = new FilterTests("anyof");
        tests.addTest(test);

        FilterAction action = new FilterAction.KeepAction();
        action.setIndex(0);

        rule.setFilterTests(tests);
        rule.addFilterAction(action);

        List<FilterRule> filterRuleList = new ArrayList<FilterRule>();
        filterRuleList.add(rule);

        return filterRuleList;
    }
}
