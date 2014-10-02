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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.acl.RedisEffectiveACLCache;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;

/**
 * Unit test for {@link RedisEffectiveACLCache}.
 */
public final class RedisEffectiveACLCacheTest extends AbstractEffectiveACLCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        LC.zimbra_class_shareddeliverycoordinator.setDefault(RedisQlessSharedDeliveryCoordinator.class.getName());
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected EffectiveACLCache constructCache() throws ServiceException {
        EffectiveACLCache cache = new RedisEffectiveACLCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisAvailable();
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.flushDB();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }
}
