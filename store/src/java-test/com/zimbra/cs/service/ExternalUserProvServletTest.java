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
package com.zimbra.cs.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.FeatureAddressVerificationStatus;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

import junit.framework.Assert;

public class ExternalUserProvServletTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testHandleAddressVerificationExpired() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        HashMap<String, String> headers = new HashMap<String, String>();
        HttpServletRequest req = new MockHttpServletRequest(null, null, null, 123, "127.0.0.1", headers);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        ExternalUserProvServlet servlet = new ExternalUserProvServlet();
        servlet.handleAddressVerification(req, resp, acct1.getId(), "test2@zimbra.com", true);
        Assert.assertNull(acct1.getPrefMailForwardingAddress());
        Assert.assertEquals(FeatureAddressVerificationStatus.expired, acct1.getFeatureAddressVerificationStatus());
    }

    @Test
    public void testHandleAddressVerificationSuccess() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        HashMap<String, String> headers = new HashMap<String, String>();
        HttpServletRequest req = new MockHttpServletRequest(null, null, null, 123, "127.0.0.1", headers);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        ExternalUserProvServlet servlet = new ExternalUserProvServlet();
        servlet.handleAddressVerification(req, resp, acct1.getId(), "test2@zimbra.com", false);
        Assert.assertEquals("test2@zimbra.com", acct1.getPrefMailForwardingAddress());
        Assert.assertEquals(FeatureAddressVerificationStatus.verified, acct1.getFeatureAddressVerificationStatus());
    }
}
