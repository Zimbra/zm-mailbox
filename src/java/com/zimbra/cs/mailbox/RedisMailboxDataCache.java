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

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisMailboxDataCache implements MailboxDataCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected Pool<Jedis> jedisPool;
    protected ObjectMapper mapper = new ObjectMapper();

    /** Constructor */
    public RedisMailboxDataCache() {
    }

    protected static String key(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_DATA + mbox.getAccountId();
    }

    @Override
    public MailboxData get(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(mbox));
            if (value == null) {
                return null;
            }
            return mapper.readValue(value, MailboxData.class);
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed deserializing MailboxData from cache", e);
        }
    }

    @Override
    public void put(Mailbox mbox, MailboxData mailboxData) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = key(mbox);
            Transaction transaction = jedis.multi();
            transaction.set(key, mapper.writer().writeValueAsString(mailboxData));
            if (expirySecs > -1) {
                transaction.expire(key, expirySecs);
            }
            transaction.exec();
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed serializing MailboxData for cache", e);
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key(mbox));
        }
    }
}
