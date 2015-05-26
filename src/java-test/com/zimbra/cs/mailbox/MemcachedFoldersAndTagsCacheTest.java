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
import org.springframework.context.annotation.Configuration;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedOnLocalhostZimbraMemcachedClientConfigurer;
import com.zimbra.cs.memcached.ZimbraMemcachedClientConfigurer;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedFoldersAndTagsCache}.
 */
public final class MemcachedFoldersAndTagsCacheTest extends AbstractFoldersAndTagsCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", LocalConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected FoldersAndTagsCache constructCache() throws ServiceException {
        FoldersAndTagsCache cache = new MemcachedFoldersAndTagsCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(cache, "foldersAndTagsCache");
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


    /**
     * Use local (in-mem) adapters for everything possible (LocalCachingZimbraConfig), to allow
     * this test to have dedicated use of memcached.
     **/
    @Configuration
    static class LocalConfig extends LocalCachingZimbraConfig {
        @Override
        public ZimbraMemcachedClientConfigurer memcachedClientConfigurer() {
            return new MemcachedOnLocalhostZimbraMemcachedClientConfigurer();
        }
    }
}
