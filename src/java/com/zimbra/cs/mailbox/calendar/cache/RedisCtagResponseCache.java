/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014 Zimbra, Inc.
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

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


public class RedisCtagResponseCache implements CtagResponseCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected JedisPool jedisPool;

    public RedisCtagResponseCache() {
    }

    protected String key(CtagResponseCache.Key key) {
        return MemcachedKeyPrefix.CALDAV_CTAG_RESPONSE + key.getFirst() + ":" + key.getSecond() + ":" + key.getThird();
    }

    @Override
    public Value get(Key key) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key(key));
            if (value == null) {
                return null;
            }
            Metadata meta = new Metadata(value);
            return new Value(meta);
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed deserializing CtagResponseCache$Value from cache", e);
        }
    }

    @Override
    public void put(Key key, Value value) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String keyStr = key(key);
            Transaction transaction = jedis.multi();
            transaction.set(keyStr, value.encodeMetadata().toString());
            if (expirySecs > -1) {
                transaction.expire(keyStr, expirySecs);
            }
            transaction.exec();
        } catch (Exception e) {
            throw ServiceException.PARSE_ERROR("failed serializing CtagResponseCache$Value for cache", e);
        }
    }
}
