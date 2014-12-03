/**
 * Copyright 2014 Nikita Koksharov, Nickolay Borbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;

import com.google.common.base.Objects;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

/**
 * ReddisonLock, derived from the Redisson project and ported to Jedis.
 *
 * Distributed implementation of {@link java.util.concurrent.locks.Lock}
 * Implements reentrant lock.
 *
 * @author Nikita Koksharov
 */
public class RedissonLock implements Lock {
    final Log LOGGER = ZimbraLog.mailbox;
    protected final UUID id;
    protected final String name;
    protected final JedisPool jedisPool;
    static final Integer unlockMessage = 0;
    protected static final ConcurrentMap<String, RedissonLockEntry> ENTRIES = new ConcurrentHashMap<String, RedissonLockEntry>();
    protected SubscribeThread subscribeThread;

    RedissonLock(JedisPool jedisPool, String name, UUID id) {
        this.id = id;
        this.name = name;
        this.jedisPool = jedisPool;
    }

    private String getName() {
        return name;
    }

    private void unsubscribe() {
        while (true) {
            RedissonLockEntry entry = ENTRIES.get(getEntryName());
            if (entry == null) {
                return;
            }
            RedissonLockEntry newEntry = new RedissonLockEntry(entry);
            newEntry.release();
            if (ENTRIES.replace(getEntryName(), entry, newEntry)) {
                if (newEntry.isFree() && ENTRIES.remove(getEntryName(), newEntry)) {
                    subscribeThread.unsubscribe();
                    subscribeThread = null;
                }
                return;
            }
        }
    }

    private String getEntryName() {
        return id + ":" + getName();
    }

    private CompletableFuture<Boolean> aquire() {
        while (true) {
            RedissonLockEntry entry = ENTRIES.get(getEntryName());
            if (entry != null) {
                RedissonLockEntry newEntry = new RedissonLockEntry(entry);
                newEntry.aquire();
                if (ENTRIES.replace(getEntryName(), entry, newEntry)) {
                    return newEntry.getPromise();
                }
            } else {
                return null;
            }
        }
    }

    private CompletableFuture<Boolean> subscribe() {
        CompletableFuture<Boolean> promise = aquire();
        if (promise != null) {
            return promise;
        }

        CompletableFuture<Boolean> newPromise = new CompletableFuture<>();
        final RedissonLockEntry lockEntry = new RedissonLockEntry(newPromise);
        lockEntry.aquire();
        RedissonLockEntry oldValue = ENTRIES.putIfAbsent(getEntryName(), lockEntry);
        if (oldValue != null) {
            CompletableFuture<Boolean> oldPromise = aquire();
            if (oldPromise == null) {
                return subscribe();
            }
            return oldPromise;
        }

        subscribeThread = new SubscribeThread(lockEntry);
        subscribeThread.start();

        return newPromise;
    }

    private String getChannelName() {
        return "redisson__lock__channel__" + getName();
    }

    @Override
    public void lock() {
        try {
            lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    public void lock(long leaseTime, TimeUnit unit) {
        try {
            lockInterruptibly(leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lockInterruptibly(-1, null);
    }

    public void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException {
        boolean subscribed = false;
        try {
            while (true) {
                Long ttl;
                if (leaseTime != -1) {
                    ttl = tryLockInner(leaseTime, unit);
                } else {
                    ttl = tryLockInner();
                }
                if (ttl == null) {
                    break;
                }

                subscribe().join();
                subscribed = true;
                // waiting for message
                RedissonLockEntry entry = ENTRIES.get(getEntryName());
                if (ttl >= 0) {
                    entry.getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                } else {
                    entry.getLatch().acquire();
                }
            }
        } finally {
            if (subscribed) {
                unsubscribe();
            }
        }
    }

    @Override
    public boolean tryLock() {
        return tryLockInner() == null;
    }

    private Long tryLockInner() {
        final LockValue currentLock = new LockValue(id, Thread.currentThread().getId());
        currentLock.incCounter();

        Jedis jedis = jedisPool.getResource();
        try {
            long reply = jedis.setnx(getName(), new ObjectMapper().writeValueAsString(currentLock));
            boolean wasSet = reply == 1;
            if (!wasSet) {
                LockValue lock = getCurrentLock();
                if (Objects.equal(lock, currentLock)) {
                    lock.incCounter();
                    jedis.set(getName(), new ObjectMapper().writeValueAsString(lock));
                    return null;
                }
                Long ttl = jedis.pttl(getName());
                return ttl;
            }
            return null;
        } catch (IOException e) {
            LOGGER.warn("Failed writing lock to Redis", e);
            return null;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    private Long tryLockInner(final long leaseTime, final TimeUnit unit) {
        final LockValue currentLock = new LockValue(id, Thread.currentThread().getId());
        currentLock.incCounter();

        Jedis jedis = jedisPool.getResource();
        try {
            String reply = jedis.set(getName(), new ObjectMapper().writeValueAsString(currentLock), "NX", "PX", leaseTime);
            if ("OK".equals(reply)) {
                return null;
            } else {
                LockValue lock = getCurrentLock();
                if (Objects.equal(lock, currentLock)) {
                    lock.incCounter();
                    jedis.psetex(getName(), (int)unit.toMillis(leaseTime), new ObjectMapper().writeValueAsString(lock));
                    return null;
                }
                Long ttl = jedis.pttl(getName());
                return ttl;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed writing lock to Redis", e);
            return null;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        long time = unit.toMillis(waitTime);
        boolean subscribed = false;
        try {
            while (true) {
                Long ttl;
                if (leaseTime != -1) {
                    ttl = tryLockInner(leaseTime, unit);
                } else {
                    ttl = tryLockInner();
                }
                if (ttl == null) {
                    break;
                }

                if (time <= 0) {
                    return false;
                }

                try {
                    if (!subscribe().get(time, unit)) { // awaitUninterruptibly(time, unit)) {
                        return false;
                    }
                } catch (TimeoutException | ExecutionException e) {
                    return false;
                }

                subscribed = true;
                // waiting for message
                long current = System.currentTimeMillis();
                RedissonLockEntry entry = ENTRIES.get(getEntryName());

                if (ttl >= 0 && ttl < time) {
                    entry.getLatch().tryAcquire(ttl, TimeUnit.MILLISECONDS);
                } else {
                    entry.getLatch().tryAcquire(time, TimeUnit.MILLISECONDS);
                }

                long elapsed = System.currentTimeMillis() - current;
                time -= elapsed;
            }
            return true;
        } finally {
            if (subscribed) {
                unsubscribe();
            }
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return tryLock(time, -1, unit);
    }

    @Override
    public void unlock() {
        LockValue lock = getCurrentLock();
        if (lock != null) {
            LockValue currentLock = new LockValue(id, Thread.currentThread().getId());
            if (lock.equals(currentLock)) {
                Jedis jedis = jedisPool.getResource();
                try {
                    if (lock.getCounter() > 1) {
                        lock.decCounter();
                        jedis.set(getName(), new ObjectMapper().writeValueAsString(lock));
                    } else {
                        unlock(jedis);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed writing lock to Redis", e);
                } finally {
                    jedisPool.returnResource(jedis);
                }
            } else {
                throw new IllegalMonitorStateException(
                      "Attempt to unlock lock, not locked by current id: "
                              + id + " thread-id: "
                              + Thread.currentThread().getId());                }
        } else {
            // could be deleted
        }
    }

    private void unlock(Jedis jedis) {
        int counter = 0;
        while (counter < 5) {
            Transaction transaction = jedis.multi();
            transaction.del(getName());
            transaction.publish(getChannelName(), Integer.toString(unlockMessage));
            List<Object> res = transaction.exec();
            if (res.size() == 2) {
                return;
            }
            counter++;
        }
        throw new IllegalStateException(
                "Can't unlock lock after 5 attempts. Current id: " + id
                        + " thread-id: " + Thread.currentThread().getId());
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    public void forceUnlock() {
        Jedis jedis = jedisPool.getResource();
        try {
            unlock(jedis);
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public boolean isLocked() {
        return getCurrentLock() != null;
    }

    private LockValue getCurrentLock() {
        Jedis jedis = jedisPool.getResource();
        try {
            String value = jedis.get(getName());
            if (value == null) {
                return null;
            }
            return new ObjectMapper().readValue(value, LockValue.class);
        } catch (IOException e) {
            LOGGER.error("Failed getting current lock", e);
            return null;
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    public boolean isHeldByCurrentThread() {
        LockValue lock = getCurrentLock();
        LockValue currentLock = new LockValue(id, Thread.currentThread()
                .getId());
        return lock != null && lock.equals(currentLock);
    }

    public int getHoldCount() {
        LockValue lock = getCurrentLock();
        LockValue currentLock = new LockValue(id, Thread.currentThread()
                .getId());
        if (lock != null && lock.equals(currentLock)) {
            return lock.getCounter();
        }
        return 0;
    }

    public void delete() {
        forceUnlock();
    }


    static class LockValue implements Serializable {
        private static final long serialVersionUID = -8895632286065689476L;
        @JsonProperty private UUID id;
        @JsonProperty private Long threadId;
        @JsonProperty private int counter; // need for reentrant support

        public LockValue() {
        }

        public LockValue(UUID id, Long threadId) {
            super();
            this.id = id;
            this.threadId = threadId;
        }

        public void decCounter() {
            counter--;
        }

        public void incCounter() {
            counter++;
        }

        public int getCounter() {
            return counter;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result
                    + ((threadId == null) ? 0 : threadId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LockValue other = (LockValue) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (threadId == null) {
                if (other.threadId != null)
                    return false;
            } else if (!threadId.equals(other.threadId))
                return false;
            return true;
        }

    }


    private static class RedissonLockEntry {
        private int counter;
        private final Semaphore latch;
        private final CompletableFuture<Boolean> promise;

        public RedissonLockEntry(RedissonLockEntry source) {
            counter = source.counter;
            latch = source.latch;
            promise = source.promise;
        }

        public RedissonLockEntry(CompletableFuture<Boolean> promise) {
            super();
            this.latch = new Semaphore(1);
            this.promise = promise;
        }

        public boolean isFree() {
            return counter == 0;
        }

        public void aquire() {
            counter++;
        }

        public void release() {
            counter--;
        }

        public CompletableFuture<Boolean> getPromise() {
            return promise;
        }

        public Semaphore getLatch() {
            return latch;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + counter;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RedissonLockEntry other = (RedissonLockEntry) obj;
            if (counter != other.counter)
                return false;
            return true;
        }
    }


    class SubscribeThread extends Thread {
        JedisPubSub listener;
        RedissonLockEntry lockEntry;

        public SubscribeThread(RedissonLockEntry lockEntry) {
            this.lockEntry = lockEntry;

            listener = new JedisPubSub() {
                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    if (getChannelName().equals(channel)) {
                        lockEntry.getPromise().complete(true);
                    }
                }

                @Override
                public void onMessage(String channel, String message) {
                    if (message.equals(unlockMessage) && getChannelName().equals(channel)) {
                        lockEntry.getLatch().release();
                    }
                }

                public void onPMessage(String pattern, String channel, String message) {}
                public void onUnsubscribe(String channel, int subscribedChannels) {};
                public void onPUnsubscribe(String pattern, int subscribedChannels) {};
                public void onPSubscribe(String pattern, int subscribedChannels) {};
            };

        }

        public void run() {
            Jedis jedis = jedisPool.getResource();
            try {
                jedis.subscribe(listener, getChannelName());
            } finally {
                jedisPool.returnResource(jedis);
            }
        }

        public void unsubscribe() {
            listener.unsubscribe(getChannelName());
        }
    }
}
