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

import redis.clients.jedis.JedisCluster;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisClusterEffectiveACLCache implements EffectiveACLCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    protected JedisCluster jedisCluster;

    /** Constructor */
    public RedisClusterEffectiveACLCache(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    protected static String key(String acctId) {
        return MemcachedKeyPrefix.EFFECTIVE_FOLDER_ACL + acctId + ":folders";
    }

    protected static String key(Key key) {
        return MemcachedKeyPrefix.EFFECTIVE_FOLDER_ACL + key.getAccountId() + ":" + key.getFolderId();
    }

    @Override
    public ACL get(Key key) throws ServiceException {
        try {
            String value = jedisCluster.get(key(key));
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
        try {
            String keyStr = key(key);
            if (expirySecs > -1) {
                jedisCluster.setex(keyStr, expirySecs, acl.encode().toString());
            } else {
                jedisCluster.set(keyStr, acl.encode().toString());
            }
            jedisCluster.sadd(key(key.getAccountId()), Integer.toString(key.getFolderId()));
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed serializing ACL for cache", e);
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        Set<Key> keys = new HashSet<>();
        Set<String> folderIds = jedisCluster.smembers(key(mbox.getAccountId()));
        for (String folderId: folderIds) {
            keys.add(new Key(mbox.getAccountId(), new Integer(folderId)));
        }
        remove(keys);
    }

    @Override
    public void remove(Set<Key> keys) throws ServiceException {
        for (Key key: keys) {
            String keyStr = key(key);
            jedisCluster.del(keyStr);
            jedisCluster.srem(key(key.getAccountId()), key.getFolderId().toString());
        }
    }
}
