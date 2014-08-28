/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedMailboxDataCache}.
 */
public final class MemcachedMailboxDataCacheTest extends AbstractMailboxDataCacheTest {

    @Override
    protected MailboxDataCache constructCache() throws ServiceException {
        MailboxDataCache cache = new MemcachedMailboxDataCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.getInstance().getLocalServer().addMemcachedClientServerList("localhost");
        Provisioning.getInstance().getLocalServer().setMemcachedClientTimeoutMillis(20);
        Provisioning.getInstance().getLocalServer().setMemcachedClientExpirySeconds(10);
        MemcachedConnector.startup();
        AbstractCacheTest.init();
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return MemcachedConnector.isConnected();
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        MemcachedConnector.getClient().flush();
    }

    @Test
    public void testGet() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        MemcachedMailboxDataCache mailboxDataCache = new MemcachedMailboxDataCache();
        mailboxDataCache.get(mbox);
    }
}
