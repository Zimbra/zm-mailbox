/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.LocalMailboxLock.LockFailedException;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;


/**
 * A MailboxLock adapter that uses Redis to arbitrate which host can hold locks.
 * Once the local host has obtained the first lock in Redis, then all subsequent locks
 * are managed in-process by the LocalMailboxLock adapter.
 *
 * This implementation aims to avoid unnecessary calls to Redis, using it minimally for coordination.
 */
public class RedisCoordinatedLocalMailboxLock implements MailboxLock {
    static final String WORKER_SUFFIX = "-worker";
    static final String CHANNEL_SUFFIX = "-channel";
    static final String UNLOCK_NOTIFY_MESSAGE = "unlock";
    protected JedisPool jedisPool;
    protected LocalMailboxLock localMailboxLock;
    protected Server localServer;
    protected Mailbox mbox;

    // This script will return a string if the lock is held by someone else, or a "1" if we obtain it
    static final String luaAcquireHostLockScript =
            "if redis.call('setnx', KEYS[1], KEYS[2]) == 0 then return redis.call('get', KEYS[1]) end " +
            "return 1";


    public RedisCoordinatedLocalMailboxLock(JedisPool jedisPool, Mailbox mbox) throws ServiceException {
        this.jedisPool = jedisPool;
        this.mbox = mbox;
        localMailboxLock = new LocalMailboxLock(mbox.getAccountId(), mbox);
        localServer = Provisioning.getInstance().getLocalServer();
    }

    protected String key(String suffix) {
        return MemcachedKeyPrefix.MBOX_LOCK + mbox.getAccountId() + (suffix == null ? "" : suffix);
    }

    protected boolean isLockedByAnotherHost() {
        if (!localMailboxLock.isUnlocked()) {
            return false;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            String worker = jedis.get(key(WORKER_SUFFIX));
            if (StringUtil.isNullOrEmpty(worker)) {
                return false;
            }
            if (Objects.equal(worker, workerName())) {
                return false;
            }
            return true;
        }
    }

    public int getHoldCount() {
        int n = localMailboxLock.getHoldCount();
        if (n > 0) {
            return n;
        }

        return isLockedByAnotherHost() ? 1 : 0;
    }

    public boolean isUnlocked() {
        if (!localMailboxLock.isUnlocked()) {
            return false;
        }
        return !isLockedByAnotherHost();
    }

    public boolean isWriteLockedByCurrentThread() {
        if (isLockedByAnotherHost()) {
            return false;
        } else {
            return localMailboxLock.isWriteLockedByCurrentThread();
        }
    }

    public void lock() throws LockFailedException {
        lock(true);
    }

    /** Acquire the lock in read or read-write mode, or increments the hold count if the lock is already acquired */
    public void lock(boolean write) throws LockFailedException {
        // Once a first lock is held locally, all subsequent locks are directed locally without I/O to Redis
        if (localMailboxLock.getHoldCount() > 0) {
            localMailboxLock.lock(write);
            return;
        }

        // Attempt to be the first to own the lock in Redis
        if (acquireHostLockInRedis()) {
            localMailboxLock.lock(write);
            return;
        }

        // Subscribe and wait
        WaitForUnlock waitForUnlock = new WaitForUnlock();
        boolean unlocked = waitForUnlock.wait(localServer.getMailBoxLockTimeout() * 1000);
        if (!unlocked) {
            throw new RuntimeException("Lock timeout");
        }

        // Attempt to own the lock in Redis
        if (acquireHostLockInRedis()) {
            localMailboxLock.lock(write);
            return;
        }

        throw new RuntimeException("Failed to acquire mailbox lock within timeout");
    }

    protected boolean acquireHostLockInRedis() {
        String[] luaScriptArgs = new String[] {key(WORKER_SUFFIX), workerName()};
        try (Jedis jedis = jedisPool.getResource()) {
            Object reply = jedis.eval(luaAcquireHostLockScript, luaScriptArgs.length, luaScriptArgs);
            if (reply instanceof String) {
                if (Objects.equal(reply, workerName())) { // is lock held by us?
                    return true;
                }
                return false;
            }
            else if (reply instanceof Long && ((Long)reply).longValue() == 1) {
                return true;
            }
        }
        return false;
    }

    /** Release the lock */
    public void release() {
        localMailboxLock.release();

        // If the last lock was released, relinquish the Redis record, and notify whomever might
        // be blocked in another process waiting for their lock() to return.
        if (getHoldCount() < 1) {
            try (Jedis jedis = jedisPool.getResource()) {
                Transaction transaction = jedis.multi();
                transaction.del(key(WORKER_SUFFIX));
                transaction.publish(key(CHANNEL_SUFFIX), UNLOCK_NOTIFY_MESSAGE);
                transaction.exec();
            }
        }
    }

    /** Return a unique workername (server name + threadid) - keep names qless-compatible just in case */
    protected String workerName() {
        String hostname = localServer.getName();
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        return hostname + "-" + pid;
    }


    class WaitForUnlock {
        Semaphore semaphore = new Semaphore(0);

        boolean wait(int timeoutMillis) {
            MyPubSubListener pubSubListener = new MyPubSubListener(semaphore);
            Thread thread = new Thread() {
                public void run() {
                    String channel = key(CHANNEL_SUFFIX);
                    try (Jedis jedis = jedisPool.getResource()) {
                        jedis.subscribe(pubSubListener, channel);
                    }
                }
            };
            thread.start();

            try {
                return semaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }


    class MyPubSubListener extends JedisPubSub {
        Semaphore semaphore;

        MyPubSubListener(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        public void onMessage(String channel, String message) {
            if (UNLOCK_NOTIFY_MESSAGE.equals(message)) {
                semaphore.release(1);
            }
        }
        public void onPMessage(String pattern, String channel, String message) {}
        public void onSubscribe(String channel, int subscribedChannels) {};
        public void onUnsubscribe(String channel, int subscribedChannels) {};
        public void onPUnsubscribe(String pattern, int subscribedChannels) {};
        public void onPSubscribe(String pattern, int subscribedChannels) {};
    }
}
