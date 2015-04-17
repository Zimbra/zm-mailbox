package com.zimbra.cs.mailbox;
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

import redis.clients.jedis.JedisCluster;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisClusterSentMessageIdCache implements SentMessageIdCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    protected JedisCluster jedisCluster;

    /** Constructor */
    public RedisClusterSentMessageIdCache(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    protected static String key(Mailbox mbox, String msgidHeader) {
        return MemcachedKeyPrefix.MBOX_SENTMSGID + mbox.getAccountId() + ":" + msgidHeader;
    }

    @Override
    public Integer get(Mailbox mbox, String msgidHeader) throws ServiceException {
        String value = jedisCluster.get(key(mbox, msgidHeader));
        if (value == null) {
            return null;
        }
        return new Integer(value);
    }

    @Override
    public void put(Mailbox mbox, String msgidHeader, int messageId) throws ServiceException {
        String key = key(mbox, msgidHeader);
        if (expirySecs > -1) {
            jedisCluster.setex(key, expirySecs, Integer.toString(messageId));
        } else {
            jedisCluster.set(key, Integer.toString(messageId));
        }
    }
}
