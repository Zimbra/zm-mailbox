package com.zimbra.cs.mailbox.redis;

import java.util.Collection;

import org.redisson.api.RSet;

import com.zimbra.cs.mailbox.TransactionAwareSet;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public class RedisBackedSet<E> extends TransactionAwareSet<E> {

    public RedisBackedSet(RSet<E> redisSet, TransactionCacheTracker cacheTracker) {
        super(redisSet.getName(), cacheTracker,
                new GreedySetGetter<>(redisSet.getName(), () -> redisSet.readAll()));
    }

    public Collection<E> values() {
        return data();
    }
}
