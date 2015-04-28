package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


/**
 * Redis-based {@link MailItemCache} adapter. This cache is realized using 4 Redis data structures:
 *
 * 1. Item by id (a Redis key). key=MailItem#getId(), value=serialized MailItem.
 *
 * 2. Item id by uuid (a Redis key). key=MailItem#getUuid(), value=MailItem.getId().
 *
 * 3. Item id by mailbox (a Redis set). key=Mailbox#getAccountId() + "-ids", member=MailItem#getId().
 *
 * 4. Item uuid by mailbox (a Redis set). key=Mailbox#getAccountId() + "-uuids", member=MailItem#getUuid()
 */
public class RedisMailItemCache implements MailItemCache {
    static final int DEFAULT_EXPIRY_SECS = 24 * 3600;
    protected int expirySecs = DEFAULT_EXPIRY_SECS;
    @Autowired protected Pool<Jedis> jedisPool;

    /** Constructor */
    public RedisMailItemCache() {
    }

    protected static String idKey(Mailbox mbox, int itemId) {
        return MemcachedKeyPrefix.MBOX_MAILITEM + mbox.getAccountId() + ":" + itemId;
    }

    protected static String uuidKey(Mailbox mbox, String itemUuid) {
        return MemcachedKeyPrefix.MBOX_MAILITEM + mbox.getAccountId() + ":" + itemUuid;
    }

    protected static String idsPerMailboxKey(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_MAILITEM + mbox.getAccountId() + "-ids";
    }

    protected static String uuidsPerMailboxKey(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_MAILITEM + mbox.getAccountId() + "-uuids";
    }

    @Override
    public MailItem get(Mailbox mbox, int itemId) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(idKey(mbox, itemId));
            if (value == null) {
                return null;
            }
            Metadata meta = new Metadata(value);
            MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
            ud.deserialize(meta);
            return MailItem.constructItem(mbox, ud, true);
        }
    }

    @Override
    public MailItem get(Mailbox mbox, String uuid) throws ServiceException {
        String itemId = null;
        try (Jedis jedis = jedisPool.getResource()) {
            itemId = jedis.get(uuidKey(mbox, uuid));
        }
        if (itemId == null) {
            return null;
        }
        return get(mbox, Integer.parseInt(itemId));
    }

    @Override
    public void put(Mailbox mbox, MailItem item) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();

            String idsPerMailboxKey = idsPerMailboxKey(mbox);
            transaction.sadd(idsPerMailboxKey, Integer.toString(item.getId()));
            if (expirySecs > -1) {
                transaction.expire(idsPerMailboxKey, expirySecs);
            }

            String idKey = idKey(mbox, item.getId());
            transaction.set(idKey, item.serializeUnderlyingData().toString());
            if (expirySecs > -1) {
                transaction.expire(idKey, expirySecs);
            }

            if (item.getUuid() != null) {
                String uuidsPerMailboxKey = uuidsPerMailboxKey(mbox);
                transaction.sadd(uuidsPerMailboxKey, Integer.toString(item.getId()));
                if (expirySecs > -1) {
                    transaction.expire(uuidsPerMailboxKey, expirySecs);
                }

                String uuidKey = uuidKey(mbox, item.getUuid());
                transaction.set(uuidKey, Integer.toString(item.getId()));
                if (expirySecs > -1) {
                    transaction.expire(uuidKey, expirySecs);
                }
            }
            transaction.exec();
        }
    }

    @Override
    public MailItem remove(Mailbox mbox, int itemId) throws ServiceException {
        MailItem item = get(mbox, itemId);
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction transaction = jedis.multi();
            transaction.srem(idsPerMailboxKey(mbox), Integer.toString(item.getId()));
            transaction.del(idKey(mbox, itemId));
            if (item.getUuid() != null) {
                transaction.srem(uuidsPerMailboxKey(mbox), item.getUuid());
                transaction.del(uuidKey(mbox, item.getUuid()));
            }
            transaction.exec();
            return item;
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            Response<Set<String>> idsResponse = pipeline.smembers(idsPerMailboxKey(mbox));
            Response<Set<String>> uuidsResponse = pipeline.smembers(uuidsPerMailboxKey(mbox));
            pipeline.sync();
            Set<String> ids = idsResponse.get();
            Set<String> uuids = uuidsResponse.get();

            Transaction transaction = jedis.multi();
            transaction.del(idsPerMailboxKey(mbox));
            transaction.del(uuidsPerMailboxKey(mbox));
            for (String id: ids) {
                transaction.del(idKey(mbox, Integer.parseInt(id)));
            }
            for (String uuid: uuids) {
                transaction.del(uuidKey(mbox, uuid));
            }
            transaction.exec();
        }
    }
}
