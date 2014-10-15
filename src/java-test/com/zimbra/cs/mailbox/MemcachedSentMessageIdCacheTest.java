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

import org.junit.BeforeClass;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedSentMessageIdCache}.
 */
public final class MemcachedSentMessageIdCacheTest extends AbstractSentMessageIdCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", MemcachedOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected SentMessageIdCache constructCache() throws ServiceException {
        SentMessageIdCache cache = new MemcachedSentMessageIdCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(cache, "sentMessageIdCache");
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
}
