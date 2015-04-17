package com.zimbra.cs.mailbox;
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

import redis.clients.jedis.JedisCluster;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisClusterConversationIdCache implements ConversationIdCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    protected JedisCluster jedisCluster;

    /** Constructor */
    public RedisClusterConversationIdCache(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    protected static String key(Mailbox mbox, String subjectHash) {
        return MemcachedKeyPrefix.MBOX_CONVERSATION + mbox.getAccountId() + ":" + subjectHash;
    }

    @Override
    public Integer get(Mailbox mbox, String subjectHash) throws ServiceException {
        String value = jedisCluster.get(key(mbox, subjectHash));
        if (value == null) {
            return null;
        }
        return new Integer(value);
    }

    @Override
    public void put(Mailbox mbox, String subjectHash, int conversationId) throws ServiceException {
        String key = key(mbox, subjectHash);
        if (expirySecs > -1) {
            jedisCluster.setex(key, expirySecs, Integer.toString(conversationId));
        } else {
            jedisCluster.set(key, Integer.toString(conversationId));
        }
    }

    @Override
    public void remove(Mailbox mbox, String subjectHash) throws ServiceException {
        jedisCluster.del(key(mbox, subjectHash));
    }
}
