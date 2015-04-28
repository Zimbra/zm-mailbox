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
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisSentMessageIdCache implements SentMessageIdCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected Pool<Jedis> jedisPool;

    /** Constructor */
    public RedisSentMessageIdCache() {
    }

    protected static String key(Mailbox mbox, String msgidHeader) {
        return MemcachedKeyPrefix.MBOX_SENTMSGID + mbox.getAccountId() + ":" + msgidHeader;
    }

    @Override
    public Integer get(Mailbox mbox, String msgidHeader) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(mbox, msgidHeader));
            if (value == null) {
                return null;
            }
            return new Integer(value);
        }
    }

    @Override
    public void put(Mailbox mbox, String msgidHeader, int messageId) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();
            String key = key(mbox, msgidHeader);
            transaction.set(key, Integer.toString(messageId));
            if (expirySecs > -1) {
                transaction.expire(key, expirySecs);
            }
            transaction.exec();
        }
    }
}
