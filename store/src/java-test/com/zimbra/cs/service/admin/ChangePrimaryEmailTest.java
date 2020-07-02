/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.Map;
import org.junit.Ignore;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.ChangePrimaryEmailRequest;
import com.zimbra.soap.type.AccountSelector;

import junit.framework.Assert;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ChangePrimaryEmailTest {
    public static String zimbraServerDir = "";

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new TestWatchman() {

        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            System.out.println(method.getName() + " " + e.getClass().getSimpleName());
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();

        prov.createDomain("zimbra.com", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("old@zimbra.com", "secret", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraIsAdminAccount, true);
        prov.createAccount("admin@zimbra.com", "secret", attrs);
        RightManager.getInstance().getAllAdminRights();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.clearData();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testChangePrimaryEmail() throws Exception {
        Account admin = Provisioning.getInstance().get(Key.AccountBy.name, "admin@zimbra.com");
        admin.setIsAdminAccount(true);
        ChangePrimaryEmailRequest request = new ChangePrimaryEmailRequest(AccountSelector.fromName("old@zimbra.com"), "new@zimbra.com");
        Element req = JaxbUtil.jaxbToElement(request);
        ChangePrimaryEmail handler = new ChangePrimaryEmail();
        handler.setResponseQName(AdminConstants.CHANGE_PRIMARY_EMAIL_REQUEST);
        handler.handle(req, ServiceTestUtil.getRequestContext(admin));
        //getting new account with old name as mock provisioning doesn't rename account
        Account newAcc = Provisioning.getInstance().get(Key.AccountBy.name, "old@zimbra.com");
        String change = newAcc.getPrimaryEmailChangeHistory()[0];
        Assert.assertEquals("old@zimbra.com", change.substring(0, change.indexOf("|")));
        Assert.assertEquals("old@zimbra.com", newAcc.getAliases()[0]);
    }

}
