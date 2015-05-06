/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.pushnotifications.filters;

import java.io.IOException;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;

public class FilterManagerTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link com.zimbra.cs.pushnotifications.filters.FilterManager#executeDefaultFilters(com.zimbra.cs.account.Account)}
     * .
     *
     * @throws ServiceException
     */
    @Test
    public void testExecuteDefaultFilters() throws ServiceException {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        acct.setPrefZmgPushNotificationEnabled(true);
        acct.setAccountStatus(AccountStatus.active);
        Assert.assertTrue(FilterManager.executeDefaultFilters(acct));

        Assert.assertFalse(FilterManager.executeDefaultFilters(null));

        acct.setPrefZmgPushNotificationEnabled(false);
        Assert.assertFalse(FilterManager.executeDefaultFilters(acct));

        acct.setPrefZmgPushNotificationEnabled(false);
        acct.setAccountStatus(AccountStatus.locked);
        Assert.assertFalse(FilterManager.executeDefaultFilters(acct));
    }

    /**
     * Test method for
     * {@link com.zimbra.cs.pushnotifications.filters.FilterManager#executeNewMessageFilters(com.zimbra.cs.account.Account, com.zimbra.cs.mailbox.Message)}
     * .
     *
     * @throws ServiceException
     * @throws IOException
     */
    @Test
    public void testExecuteNewMessageFilters() throws ServiceException, IOException {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acct.getId());
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        opt.setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null, new ParsedMessage(
            "From: test@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);
        acct.setPrefZmgPushNotificationEnabled(true);
        acct.setAccountStatus(AccountStatus.active);
        Assert.assertTrue(FilterManager.executeNewMessageFilters(acct, msg));

        Assert.assertFalse(FilterManager.executeNewMessageFilters(acct, null));

        opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        msg = mbox.addMessage(null, new ParsedMessage(
            "From: test@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);
        Assert.assertFalse(FilterManager.executeNewMessageFilters(acct, msg));

        opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_SENT);
        opt.setFlags(Flag.BITMASK_UNREAD);
        msg = mbox.addMessage(null, new ParsedMessage(
            "From: test@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);
        Assert.assertFalse(FilterManager.executeNewMessageFilters(acct, msg));
    }

}
