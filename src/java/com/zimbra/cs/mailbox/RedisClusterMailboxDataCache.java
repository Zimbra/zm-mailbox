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

import redis.clients.jedis.JedisCluster;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisClusterMailboxDataCache implements MailboxDataCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    protected JedisCluster jedisCluster;
    protected ObjectMapper mapper = new ObjectMapper();

    /** Constructor */
    public RedisClusterMailboxDataCache(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    protected static String key(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_DATA + mbox.getAccountId();
    }

    @Override
    public MailboxData get(Mailbox mbox) throws ServiceException {
        try {
            String value = jedisCluster.get(key(mbox));
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
        try {
            String key = key(mbox);
            if (expirySecs > -1) {
                jedisCluster.setex(key, expirySecs, mapper.writer().writeValueAsString(mailboxData));
            } else {
                jedisCluster.set(key, mapper.writer().writeValueAsString(mailboxData));
            }
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed serializing MailboxData for cache", e);
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        jedisCluster.del(key(mbox));
    }
}
