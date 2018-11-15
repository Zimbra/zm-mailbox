package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.Set;

import org.redisson.api.RSet;

import com.zimbra.cs.mailbox.TransactionAwareSet;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public class RedisBackedSet<E> extends TransactionAwareSet<E> {

    private RSet<E> redisSet;

    public RedisBackedSet(RSet<E> redisSet, TransactionCacheTracker cacheTracker) {
        super(cacheTracker, redisSet.getName());
        this.redisSet = redisSet;
    }

    @Override
    protected Set<E> initLocalCache() {
        return redisSet.readAll();
    }

    public Collection<E> values() {
        return getLocalCache();
    }

}
