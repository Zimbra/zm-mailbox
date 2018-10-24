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
package com.zimbra.cs.mailbox;

import java.util.Map;
import org.junit.Ignore;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.PrefExternalSendersType;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.ZTestWatchman;

import junit.framework.Assert;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class NotificationTest {
    
    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("testZCS3546@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();

    }

    @Test
    public void testOOOWhenSpecificDomainSenderNotSet() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "testZCS3546@zimbra.com");
        acct1.setPrefOutOfOfficeSuppressExternalReply(true);
        acct1.unsetInternalSendersDomain();
        acct1.unsetPrefExternalSendersType();
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        boolean skipOOO = Notification.skipOutOfOfficeMsg("test3@synacor.com", acct1, mbox1);
        Assert.assertEquals(true, skipOOO);
    }

    @Test
    public void testOOOWhenSpecificDomainSenderIsSet() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "testZCS3546@zimbra.com");
        acct1.setPrefOutOfOfficeSuppressExternalReply(true);
        acct1.setPrefExternalSendersType(PrefExternalSendersType.INSD);
        String[] domains = {"synacor.com"};
        acct1.setPrefOutOfOfficeSpecificDomains(domains);
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        boolean skipOOO = Notification.skipOutOfOfficeMsg("test3@synacor.com", acct1, mbox1);
        Assert.assertEquals(false, skipOOO);
    }

    @Test
    public void testOOOMsgWhenSpecificDomainSenderIsSetWithSpecificDomainSender() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "testZCS3546@zimbra.com");
        acct1.setPrefOutOfOfficeExternalReplyEnabled(true);
        acct1.setPrefExternalSendersType(PrefExternalSendersType.INSD);
        String[] domains = {"synacor.com"};
        acct1.setPrefOutOfOfficeSpecificDomains(domains);
        acct1.setInternalSendersDomain(domains);
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        boolean customMsg = Notification.sendOutOfOfficeExternalReply("test3@synacor.com", acct1, mbox1);
        Assert.assertEquals(true, customMsg);
    }

    @Test
    public void testOOOMsgWhenSpecificDomainSenderIsSetWithInternalSender() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "testZCS3546@zimbra.com");
        acct1.setPrefOutOfOfficeExternalReplyEnabled(true);
        acct1.setPrefExternalSendersType(PrefExternalSendersType.INSD);
        String[] domains = {"synacor.com"};
        acct1.setPrefOutOfOfficeSpecificDomains(domains);
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        boolean customMsg = Notification.sendOutOfOfficeExternalReply("test2@zimbra.com", acct1, mbox1);
        Assert.assertEquals(false, customMsg);
    }
}
