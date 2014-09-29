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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedFoldersAndTagsCache}.
 */
public final class MemcachedFoldersAndTagsCacheTest extends AbstractFoldersAndTagsCacheTest {

    @Override
    protected FoldersAndTagsCache constructCache() throws ServiceException {
        FoldersAndTagsCache cache = new MemcachedFoldersAndTagsCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.getInstance().getLocalServer().addMemcachedClientServerList("localhost");
        Provisioning.getInstance().getLocalServer().setMemcachedClientTimeoutMillis(20);
        Provisioning.getInstance().getLocalServer().setMemcachedClientExpirySeconds(10);
        MemcachedConnector.startup();
        AbstractFoldersAndTagsCacheTest.init();
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return MemcachedConnector.isConnected();
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        MemcachedConnector.getClient().flush();
    }
}
