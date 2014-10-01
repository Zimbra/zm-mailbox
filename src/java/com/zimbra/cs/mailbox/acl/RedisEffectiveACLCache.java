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
import com.zimbra.common.util.Pair;
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

    protected static String key(String acctId, int folderId) {
        return MemcachedKeyPrefix.EFFECTIVE_FOLDER_ACL + acctId + ":" + folderId;
    }

    @Override
    public ACL get(String acctId, int folderId) throws ServiceException {
        Jedis jedis = jedisPool.getResource();
        try {
            String value = jedis.get(key(acctId, folderId));
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
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @Override
    public void put(String acctId, int folderId, ACL acl) throws ServiceException {
        Jedis jedis = jedisPool.getResource();
        try {
            String key = key(acctId, folderId);
            Transaction transaction = jedis.multi();
            transaction.set(key, acl.encode().toString());
            if (expirySecs > -1) {
                transaction.expire(key, expirySecs);
            }
            transaction.sadd(key(acctId), Integer.toString(folderId));
            transaction.exec();
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed serializing ACL for cache", e);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        Set<Pair<String, Integer>> keys = new HashSet<>();
        Jedis jedis = jedisPool.getResource();
        try {
            Set<String> folderIds = jedis.smembers(key(mbox.getAccountId()));
            for (String folderId: folderIds) {
                keys.add(new Pair<>(mbox.getAccountId(), new Integer(folderId)));
            }
        } finally {
            jedisPool.returnResource(jedis);
        }
        remove(keys);
    }

    @Override
    public void remove(Set<Pair<String, Integer>> keys) throws ServiceException {
        Jedis jedis = jedisPool.getResource();
        try {
            Transaction transaction = jedis.multi();
            for (Pair<String, Integer> key: keys) {
                String acctId = key.getFirst();
                Integer folderId = key.getSecond();
                transaction.del(key(acctId, folderId));
                transaction.srem(key(acctId), folderId.toString());
            }
            transaction.exec();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }
}
