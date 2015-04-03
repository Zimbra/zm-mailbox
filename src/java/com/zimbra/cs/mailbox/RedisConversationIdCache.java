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

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisConversationIdCache implements ConversationIdCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected JedisPool jedisPool;

    /** Constructor */
    public RedisConversationIdCache() {
    }

    protected static String key(Mailbox mbox, String subjectHash) {
        return MemcachedKeyPrefix.MBOX_CONVERSATION + mbox.getAccountId() + ":" + subjectHash;
    }

    @Override
    public Integer get(Mailbox mbox, String subjectHash) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(mbox, subjectHash));
            if (value == null) {
                return null;
            }
            return new Integer(value);
        }
    }

    @Override
    public void put(Mailbox mbox, String subjectHash, int conversationId) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = key(mbox, subjectHash);
            Transaction transaction = jedis.multi();
            transaction.set(key, Integer.toString(conversationId));
            if (expirySecs > -1) {
                transaction.expire(key, expirySecs);
            }
            transaction.exec();
        }
    }

    @Override
    public void remove(Mailbox mbox, String subjectHash) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key(mbox, subjectHash));
        }
    }
}
