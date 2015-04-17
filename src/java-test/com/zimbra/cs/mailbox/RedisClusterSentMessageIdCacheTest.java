/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra Software, LLC.
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
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;

/**
 * Unit test for {@link RedisClusterSentMessageIdCache}.
 */
public final class RedisClusterSentMessageIdCacheTest extends RedisSentMessageIdCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", MyZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected SentMessageIdCache constructCache() throws ServiceException {
        SentMessageIdCache cache = new RedisClusterSentMessageIdCache(Zimbra.getAppContext().getBean(JedisCluster.class));
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisClusterAvailable();
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        Map<String,JedisPool> clusterNodes = Zimbra.getAppContext().getBean(JedisCluster.class).getClusterNodes();
        for (JedisPool jedisPool: clusterNodes.values()) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.flushDB();
            }
        }
    }


    // A configuration that uses all local or mock non-Redis adapters.
    @Configuration
    static class MyZimbraConfig extends LocalCachingZimbraConfig {

        @Override
        public Set<HostAndPort> redisUris() throws ServiceException {
            return RedisTestHelper.getRedisUris();
        }
    }
}
