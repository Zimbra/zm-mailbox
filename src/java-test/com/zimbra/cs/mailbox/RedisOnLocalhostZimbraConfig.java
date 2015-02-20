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

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.ZimbraConfig;

/**
 * Spring Configuration used by Redis-based unit tests that will use a localhost-based Redis.
 */
@Configuration
public class RedisOnLocalhostZimbraConfig extends ZimbraConfig {

    @Override
    public boolean isRedisAvailable() throws ServiceException {
        HostAndPort hostAndPort = redisUris().iterator().next();
        JedisPool jedisPool = new JedisPool(hostAndPort.getHost(), hostAndPort.getPort());
        try {
            Jedis jedis = jedisPool.getResource();
            jedisPool.returnResource(jedis);
            return true;
        } catch (Exception e) {
            ZimbraLog.test.warn("Failed connecting to Redis", e);
            return false;
        } finally {
            jedisPool.destroy();
        }
    }

    @Override
    public boolean isRedisClusterAvailable() throws ServiceException {
        try {
            JedisCluster jedisCluster = new JedisCluster(redisUris());
            jedisCluster.get("");
            return true;
        } catch (Exception e) {
            ZimbraLog.test.warn("Failed connecting to Redis", e);
            return false;
        }
    }

    @Override
    public Set<HostAndPort> redisUris() throws ServiceException {
        Set<HostAndPort> set = new HashSet<>();
        set.add(new HostAndPort("127.0.0.1", 6379));
        return set;
    }
}
