/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import com.google.common.base.Objects;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


/**
 * A {@link MailboxLock} adapter that uses Redis.
 */
public class RedisMailboxLock implements MailboxLock {
    protected Mailbox mbox;
    static final int EXPIRY_SECS = 4 * 3600;
    static final int WAIT_MS = 3000;
    public static final String MODES_LIST_SUFFIX = "-modes"; // a set of read
    public static final String META_HSET_SUFFIX = "-meta"; // metadata captured during initial lock (thread, worker)
    public static final String META_HSET_WORKER = "worker";
    @Autowired protected JedisPool jedisPool;

    /** Constructor */
    public RedisMailboxLock(Mailbox mbox) {
        this.mbox = mbox;
    }

    protected String getMetaKeyName(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_LOCK + mbox.getAccountId() + META_HSET_SUFFIX;
    }

    protected String getListKeyName(Mailbox mbox) {
        return MemcachedKeyPrefix.MBOX_LOCK + mbox.getAccountId() + MODES_LIST_SUFFIX;
    }

    /** Returns the hold count */
    public int getHoldCount() {
        Jedis jedis = jedisPool.getResource();
        try {
            Long count = jedis.llen(getListKeyName(mbox));
            return count.intValue();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /** Returns whether the lock is not currently obtained */
    public boolean isUnlocked() {
        return getHoldCount() < 1;
    }

    /** Returns whether the lock is currently obtained in read mode, and for the current thread */
    public boolean isReadLockedByCurrentThread() {
        Jedis jedis = jedisPool.getResource();
        try {
            // Is locked by current thread?
            String value = jedis.hget(getMetaKeyName(mbox), META_HSET_WORKER);
            if (!Objects.equal(workerName(), value)) {
                return false;
            }

            // Is read locked?
            List<String> set = jedis.lrange(getListKeyName(mbox), 0, -1);
            for (String str: set) {
                if ("R".equals(str)) {
                    return true;
                }
            }
            return false;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /** Returns whether the lock is currently obtained in read-write mode, and for the current thread */
    public boolean isWriteLockedByCurrentThread() {
        Jedis jedis = jedisPool.getResource();
        try {
            // Is locked by current thread?
            String value = jedis.hget(getMetaKeyName(mbox), META_HSET_WORKER);
            if (!Objects.equal(workerName(), value)) {
                return false;
            }

            // Is write locked?
            List<String> set = jedis.lrange(getListKeyName(mbox), 0, -1);
            for (String str: set) {
                if ("W".equals(str)) {
                    return true;
                }
            }
            return false;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /** Acquire the lock in read-write mode, or increments the hold count if the lock is already acquired */
    public void lock() {
        lock(true);
    }

    /** Acquire the lock in read or read-write mode, or increments the hold count if the lock is already acquired */
    public void lock(boolean write) {
        write = write || mbox.requiresWriteLock();
        ZimbraLog.mailbox.trace("LOCK %s", (write ? "WRITE" : "READ"));

        // Assert when a write lock is requested if a read lock is already in progress
        if (write && isReadLockedByCurrentThread()) {
            ZimbraLog.mailbox.error("read lock held before write", new Exception());
            assert(false);
        }

        Jedis jedis = jedisPool.getResource();
        try {
            Transaction transaction = jedis.multi();

            String key = getListKeyName(mbox);
            transaction.rpush(key, write ? "W" : "R");
            transaction.expire(key, EXPIRY_SECS);
            transaction.llen(key);

            key = getMetaKeyName(mbox);
            transaction.hset(key, META_HSET_WORKER, workerName());
            transaction.expire(key, EXPIRY_SECS);

            List<Object> result = transaction.exec();
            int newCount = Integer.parseInt(result.get(2).toString());
            ZimbraLog.mailbox.debug("# of locks incr to " + newCount + " for mailbox " + mbox.getId());
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /** Release the lock */
    public void release() {
        Jedis jedis = jedisPool.getResource();
        try {
            String key = getListKeyName(mbox);
            Transaction transaction = jedis.multi();
            transaction.rpop(key);
            transaction.expire(key, EXPIRY_SECS);
            transaction.llen(key);
            List<Object> result = transaction.exec();
            int newCount = Integer.parseInt(result.get(2).toString());
            ZimbraLog.mailbox.debug("# of locks decr to " + newCount + " for mailbox " + mbox.getId());
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    /** Return a unique workername (hostname & threadid) - keep names qless-compatible just in case */
    protected String workerName() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "localhost";
        }
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        return hostname + "-" + pid;
    }
}
