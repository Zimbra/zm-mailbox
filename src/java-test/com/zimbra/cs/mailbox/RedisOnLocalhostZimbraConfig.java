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

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.ZimbraConfig;

/**
 * Spring Configuration used by Redis-based unit tests that will use a localhost-based Redis.
 */
@Configuration
public class RedisOnLocalhostZimbraConfig extends ZimbraConfig {

    @Override
    public boolean isRedisAvailable() throws ServiceException {
        JedisPool jedisPool = new JedisPool(redisUri().getHost(), redisUri().getPort());
        try {
            Jedis jedis = jedisPool.getResource();
            jedisPool.returnResource(jedis);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            jedisPool.destroy();
        }
    }

    @Override
    public URI redisUri() throws ServiceException {
        try {
            return new URI("redis://localhost:6379");
        } catch (URISyntaxException e) {
            throw ServiceException.PARSE_ERROR("Invalid Redis URI", e);
        }
    }
}
