package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisFoldersAndTagsCache implements FoldersAndTagsCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected JedisPool jedisPool;

    /** Constructor */
    public RedisFoldersAndTagsCache() {
    }

    protected static String key(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_FOLDERS_TAGS + mbox.getAccountId();
    }

    @Override
    public FoldersAndTags get(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(mbox));
            if (value == null) {
                return null;
            }

            Metadata meta = new Metadata(value);
            return FoldersAndTags.decode(meta);
        }
    }

    @Override
    public void put(Mailbox mbox, FoldersAndTags foldersAndTags) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = key(mbox);
            if (expirySecs > -1) {
                jedis.setex(key, expirySecs, foldersAndTags.encode().toString());
            } else {
                jedis.set(key, foldersAndTags.encode().toString());
            }
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key(mbox));
        }
    }
}
