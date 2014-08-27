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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link RedisConversationIdCache}.
 */
public final class RedisConversationIdCacheTest extends AbstractConversationIdCacheTest {

    @Override
    protected ConversationIdCache constructCache() throws ServiceException {
        ConversationIdCache cache = new RedisConversationIdCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        try {
            Jedis jedis = jedisPool.getResource();
            jedisPool.returnResource(jedis);
            return true;
        } catch (Exception e) {
            return false;
        }
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
