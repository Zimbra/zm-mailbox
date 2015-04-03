/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class RedisCtagInfoCache implements CtagInfoCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected JedisPool jedisPool;

    public RedisCtagInfoCache() {
    }

    protected String key(String accountId, int folderId) {
        return MemcachedKeyPrefix.CTAGINFO + accountId + ":" + folderId;
    }

    protected static String idsPerMailboxKey(String accountId) {
        return MemcachedKeyPrefix.CTAGINFO + accountId + ":folderIds";
    }

    protected Collection<String> keys(Collection<Pair<String,Integer>> pairs) {
        Collection<String> result = new ArrayList<>();
        for (Pair<String,Integer> pair: pairs) {
            result.add(key(pair.getFirst(), pair.getSecond()));
        }
        return result;
    }

    @Override
    public CtagInfo get(String accountId, int folderId) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(accountId, folderId));
            if (value == null) {
                return null;
            }
            Metadata meta = new Metadata(value);
            return new CtagInfo(meta);
        }
    }

    @Override
    public Map<Pair<String,Integer>, CtagInfo> get(List<Pair<String,Integer>> keys) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            // Fetch data
            Pipeline pipeline = jedis.pipelined();
            Map<Pair<String,Integer>,Response<String>> fetchPipelineRequestResponseMap = new HashMap<>();
            for (Pair<String,Integer> key: keys) {
                String keyStr = key(key.getFirst(), key.getSecond());
                Response<String> jedisRes = pipeline.get(keyStr);
                fetchPipelineRequestResponseMap.put(key, jedisRes);
            }
            pipeline.sync();

            // Prepare results & hydrate CtagInfo objects from string-based values returned from cache
            Map<Pair<String,Integer>, CtagInfo> result = new HashMap<>();
            for (Pair<String,Integer> key: fetchPipelineRequestResponseMap.keySet()) {
                String value = fetchPipelineRequestResponseMap.get(key).get();
                if (value == null) {
                    result.put(key, null);
                } else {
                    Metadata meta = new Metadata(value);
                    result.put(key, new CtagInfo(meta));
                }
            }
            return result;
        }
    }

    @Override
    public void put(String accountId, int folderId, CtagInfo value) throws ServiceException {
        Map<Pair<String,Integer>, CtagInfo> map = new HashMap<>();
        map.put(new Pair<>(accountId, folderId), value);
        put(map);
    }

    @Override
    public void put(Map<Pair<String,Integer>, CtagInfo> pairs) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();
            for (Pair<String,Integer> key: pairs.keySet()) {
                String accountId = key.getFirst();
                Integer folderId = key.getSecond();
                CtagInfo value = pairs.get(key);

                String idsPerMailboxKey = idsPerMailboxKey(accountId);
                transaction.sadd(idsPerMailboxKey, Integer.toString(folderId));
                if (expirySecs > -1) {
                    transaction.expire(idsPerMailboxKey, expirySecs);
                }

                String idKey = key(accountId, folderId);
                transaction.set(idKey, value.encodeMetadata().toString());
                if (expirySecs > -1) {
                    transaction.expire(idKey, expirySecs);
                }
            }
            transaction.exec();
        }
    }

    @Override
    public void remove(String accountId, int folderId) throws ServiceException {
        List<Pair<String,Integer>> list = new ArrayList<>();
        list.add(new Pair<>(accountId, folderId));
        remove(list);
    }

    @Override
    public void remove(List<Pair<String,Integer>> keys) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();
            for (Pair<String,Integer> key: keys) {
                String accountId = key.getFirst();
                Integer folderId = key.getSecond();

                String keyStr = key(accountId, folderId);
                transaction.del(keyStr);
                transaction.srem(idsPerMailboxKey(accountId), Integer.toString(folderId));
            }
            transaction.exec();
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> ids = jedis.smembers(idsPerMailboxKey(mbox.getAccountId()));

            Transaction transaction = jedis.multi();
            transaction.del(idsPerMailboxKey(mbox.getAccountId()));
            for (String id: ids) {
                transaction.del(key(mbox.getAccountId(), Integer.parseInt(id)));
            }
            transaction.exec();
        }
    }
}
