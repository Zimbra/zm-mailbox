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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedOnLocalhostZimbraMemcachedClientConfigurer;
import com.zimbra.cs.memcached.ZimbraMemcachedClientConfigurer;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedMailboxDataCache}.
 */
public final class MemcachedMailboxDataCacheTest extends AbstractMailboxDataCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", LocalConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected MailboxDataCache constructCache() throws ServiceException {
        MailboxDataCache cache = new MemcachedMailboxDataCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(cache, "mailboxDataCache");
        return cache;
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return Zimbra.getAppContext().getBean(ZimbraMemcachedClient.class).isConnected();
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        Zimbra.getAppContext().getBean(ZimbraMemcachedClient.class).flush();
    }

    @Test
    public void testGet() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        cache.get(mbox);
    }


    /**
     * Use local (in-mem) adapters for everything possible (LocalCachingZimbraConfig), to allow
     * this test to have dedicated use of memcached.
     **/
    @Configuration
    static class LocalConfig extends LocalCachingZimbraConfig {
        @Override
        public ZimbraMemcachedClientConfigurer memcachedClientConfigurerBean() throws Exception {
            return new MemcachedOnLocalhostZimbraMemcachedClientConfigurer();
        }
    }
}
