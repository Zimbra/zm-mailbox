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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class RedisCalendarDataCache implements CalendarDataCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected Pool<Jedis> jedisPool;


    public RedisCalendarDataCache() {
    }

    protected String key(Key key) {
        return MemcachedKeyPrefix.CAL_SUMMARY + key.getAccountId() + ":" + key.getFolderId();
    }

    protected static String idsPerMailboxKey(String accountId) {
        return MemcachedKeyPrefix.CAL_SUMMARY + accountId + ":folderIds";
    }

    protected Set<String> keys(Set<Key> keys) {
        Set<String> set = new HashSet<>();
        for (Key key: keys) {
            set.add(key(key));
        }
        return set;
    }

    @Override
    public CalendarData get(Key key) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(key));
            if (value == null) {
                return null;
            }
            Metadata meta = new Metadata(value);
            return new CalendarData(meta);
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed deserializing CalendarData from cache", e);
        }
    }

    @Override
    public void put(Key key, CalendarData value) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();

            String idsPerMailboxKey = idsPerMailboxKey(key.getAccountId());
            transaction.sadd(idsPerMailboxKey, Integer.toString(key.getFolderId()));
            if (expirySecs > -1) {
                transaction.expire(idsPerMailboxKey, expirySecs);
            }

            String idKey = key(key);
            transaction.set(idKey, value.encodeMetadata().toString());
            if (expirySecs > -1) {
                transaction.expire(idKey, expirySecs);
            }

            transaction.exec();
        }
    }

    @Override
    public void remove(Key key) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();

            String idsPerMailboxKey = idsPerMailboxKey(key.getAccountId());
            transaction.srem(idsPerMailboxKey, Integer.toString(key.getFolderId()));

            String idKey = key(key);
            transaction.del(idKey);

            transaction.exec();
        }
    }

    @Override
    public void remove(Set<Key> keys) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();

            for (Key key: keys) {
                String idsPerMailboxKey = idsPerMailboxKey(key.getAccountId());
                transaction.srem(idsPerMailboxKey, Integer.toString(key.getFolderId()));

                String idKey = key(key);
                transaction.del(idKey);
            }

            transaction.exec();
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        Set<Key> keysToRemove = new HashSet<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> ids = jedis.smembers(idsPerMailboxKey(mbox.getAccountId()));
            for (String id: ids) {
                keysToRemove.add(new Key(mbox.getAccountId(), new Integer(id)));
            }
        }
        remove(keysToRemove);
    }
}
