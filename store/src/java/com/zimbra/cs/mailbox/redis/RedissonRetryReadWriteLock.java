package com.zimbra.cs.mailbox.redis;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;

public class RedissonRetryReadWriteLock extends RedissonRetryDecorator<RReadWriteLock> implements RReadWriteLock {

    public RedissonRetryReadWriteLock(RedissonInitializer<RReadWriteLock> lockInitializer, RedissonRetryClient client) {
        super(lockInitializer, client);
    }

    @Override
    public RLock readLock() {
        checkClientVersion();
        return new RedissonRetryLock(client -> redissonObject.readLock(), client);
    }

    @Override
    public RLock writeLock() {
        checkClientVersion();
        return new RedissonRetryLock(client -> redissonObject.writeLock(), client);
    }
}
