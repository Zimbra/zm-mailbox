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
package com.zimbra.cs.service.mail;

import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.util.ZTestWatchman;

import junit.framework.Assert;

public class AutoCompleteTest {

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new ZTestWatchman();

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test3951@zimbra.com", "secret", attrs);
    }

    @Test
    public void test3951() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test3951@zimbra.com");
        Element request = new Element.XMLElement(MailConstants.AUTO_COMPLETE_REQUEST);
        request.addAttribute("name", " ");
        boolean exceptionThrown;
        try {
            new AutoComplete().handle(request, ServiceTestUtil.getRequestContext(acct));
            exceptionThrown = false;
        } catch (ServiceException e) {
            exceptionThrown = true;
            Assert.assertEquals("invalid request: name parameter is empty", e.getMessage());
        }
        Assert.assertEquals(true, exceptionThrown);
    }

    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
