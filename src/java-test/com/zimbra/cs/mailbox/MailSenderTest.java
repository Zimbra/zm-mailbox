/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import javax.mail.internet.InternetAddress;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link MailSender}.
 *
 * @author ysasaki
 */
public final class MailSenderTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        Provisioning prov = Provisioning.getInstance();
        prov.deleteAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void getSenderHeaders() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MailSender mailSender = new MailSender();
        Pair<InternetAddress, InternetAddress> pair;

        pair = mailSender.getSenderHeaders(null, null, account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress("test@zimbra.com"), null, account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(null, new InternetAddress("test@zimbra.com"), account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress("test@zimbra.com"), new InternetAddress("test@zimbra.com"), account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), null, account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), new InternetAddress("bar@zimbra.com"), account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), new InternetAddress("test@zimbra.com"), account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());

        pair = mailSender.getSenderHeaders(new InternetAddress("test@zimbra.com"), new InternetAddress("foo@zimbra.com"), account, account, false);
        Assert.assertEquals("test@zimbra.com", pair.getFirst().toString());
        Assert.assertNull(pair.getSecond());
    }

    @Test
    public void getCalSenderHeaders() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        MailSender calSender = new MailSender().setCalendarMode(true);
        Pair<InternetAddress, InternetAddress> pair;

        // Calendar mode allows send-obo without grants.
        pair = calSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), new InternetAddress("test@zimbra.com"), account, account, false);
        Assert.assertEquals("foo@zimbra.com", pair.getFirst().toString());
        Assert.assertEquals("test@zimbra.com", pair.getSecond().toString());

        // Even in calendar mode, Sender must be the user's own address.
        pair = calSender.getSenderHeaders(new InternetAddress("foo@zimbra.com"), new InternetAddress("bar@zimbra.com"), account, account, false);
        Assert.assertEquals("foo@zimbra.com", pair.getFirst().toString());
        Assert.assertEquals("test@zimbra.com", pair.getSecond().toString());
    }

}
