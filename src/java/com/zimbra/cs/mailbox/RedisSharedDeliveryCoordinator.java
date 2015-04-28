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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class RedisSharedDeliveryCoordinator implements SharedDeliveryCoordinator {
    static final int EXPIRY_SECS = 4 * 3600; // automatically expire mailbox-specific shared delivery state older than this
    static final int WAIT_MS = 3000;
    public static final String ALLOWED_SUFFIX = "-allowed";
    public static final String COUNT_SUFFIX = "-count";
    @Autowired protected Pool<Jedis> jedisPool;

    /** Constructor */
    public RedisSharedDeliveryCoordinator() {
    }

    protected String getAllowedKeyName(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_SHARED_DELIVERY_COORD + mbox.getAccountId() + ALLOWED_SUFFIX;
    }

    protected String getCountKeyName(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_SHARED_DELIVERY_COORD + mbox.getAccountId() + COUNT_SUFFIX;
    }

    @Override
    public boolean beginSharedDelivery(Mailbox mbox) throws ServiceException {

        // If request for other ops is pending on this mailbox, don't allow
        // any more shared deliveries from starting.
        if (!isSharedDeliveryAllowed(mbox)) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = getCountKeyName(mbox);
            Transaction transaction = jedis.multi();
            transaction.incr(key);
            transaction.expire(key, EXPIRY_SECS);
            List<Object> result = transaction.exec();
            int newCount = Integer.parseInt(result.get(0).toString());
            ZimbraLog.mailbox.debug("# of shared deliv incr to " + newCount +
                    " for mailbox " + mbox.getId());
            return true;
        }
    }

    @Override
    public void endSharedDelivery(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getCountKeyName(mbox);
            Transaction transaction = jedis.multi();
            transaction.decr(key);
            transaction.expire(key, EXPIRY_SECS);
            List<Object> result = transaction.exec();
            int newCount = Integer.parseInt(result.get(0).toString());
            ZimbraLog.mailbox.debug("# of shared deliv decr to " + newCount +
                    " for mailbox " + mbox.getId());
        }
    }

    @Override
    public boolean isSharedDeliveryAllowed(Mailbox mbox) throws ServiceException {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getAllowedKeyName(mbox);
            String value = jedis.get(key);
            return value == null || Boolean.TRUE.toString().equals(value);
        }
    }

    @Override
    public void setSharedDeliveryAllowed(Mailbox mbox, boolean allow) throws ServiceException {
        // If someone is changing allow from true to false, wait for completion of active shared deliveries
        // before allowing the change
        if (allow == false && isSharedDeliveryAllowed(mbox)) {
            ZimbraLog.mailbox.debug("Waiting for completion of active shared delivery before allowing disabling; mailbox={}", mbox.getId());
            waitUntilSharedDeliveryCompletes(mbox);
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = getAllowedKeyName(mbox);
            Transaction transaction = jedis.multi();
            transaction.set(key, Boolean.toString(allow));
            transaction.expire(key, EXPIRY_SECS);
            transaction.exec();
        }
    }

    @Override
    public void waitUntilSharedDeliveryCompletes(Mailbox mbox) throws ServiceException {
        String key = getCountKeyName(mbox);
        while (true) {
            int count;
            try (Jedis jedis = jedisPool.getResource()) {
                String countStr = jedis.get(key);
                count = countStr == null ? 0 : Integer.parseInt(countStr);
                if (count < 1) {
                    break;
                }
            }

            try {
                synchronized (this) {
                    wait(WAIT_MS);
                }
                ZimbraLog.misc.info("wake up from wait for completion of shared delivery; mailbox=%d, # of shared deliv=%d",
                        mbox.getId(), count);
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public boolean isSharedDeliveryComplete(Mailbox mbox) throws ServiceException {
        String key = getCountKeyName(mbox);
        try (Jedis jedis = jedisPool.getResource()) {
            String countStr = jedis.get(key);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);
            return count < 1;
        }
    }
}
