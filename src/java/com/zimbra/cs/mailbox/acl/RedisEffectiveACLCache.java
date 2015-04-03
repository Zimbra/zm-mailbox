package com.zimbra.cs.mailbox.acl;
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisEffectiveACLCache implements EffectiveACLCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected JedisPool jedisPool;

    /** Constructor */
    public RedisEffectiveACLCache() {
    }

    protected static String key(String acctId) {
        return MemcachedKeyPrefix.EFFECTIVE_FOLDER_ACL + acctId + ":folders";
    }

    protected static String key(Key key) {
        return MemcachedKeyPrefix.EFFECTIVE_FOLDER_ACL + key.getAccountId() + ":" + key.getFolderId();
    }

    @Override
    public ACL get(Key key) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(key));
            if (value == null) {
                return null;
            }
            try {
                // first try old serialization
                MetadataList meta = new MetadataList(value);
                return new ACL(meta);
            } catch (Exception e) {
                // new serialization
                Metadata meta = new Metadata(value);
                return new ACL(meta);
            }
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed deserializing ACL from cache", e);
        }
    }

    @Override
    public void put(Key key, ACL acl) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String keyStr = key(key);
            Transaction transaction = jedis.multi();
            transaction.set(keyStr, acl.encode().toString());
            if (expirySecs > -1) {
                transaction.expire(keyStr, expirySecs);
            }
            transaction.sadd(key(key.getAccountId()), Integer.toString(key.getFolderId()));
            transaction.exec();
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed serializing ACL for cache", e);
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        Set<Key> keys = new HashSet<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> folderIds = jedis.smembers(key(mbox.getAccountId()));
            for (String folderId: folderIds) {
                keys.add(new Key(mbox.getAccountId(), new Integer(folderId)));
            }
        }
        remove(keys);
    }

    @Override
    public void remove(Set<Key> keys) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();
            for (Key key: keys) {
                String keyStr = key(key);
                transaction.del(keyStr);
                transaction.srem(key(key.getAccountId()), key.getFolderId().toString());
            }
            transaction.exec();
        }
    }
}
