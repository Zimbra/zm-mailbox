package com.zimbra.cs.mailbox.redis;

import org.redisson.api.RMap;

import com.zimbra.cs.mailbox.TransactionAwareMap;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

/**
 * This class tracks changes to an underlying redis map that get batched over the course of a transaction
 */
public class RedisBackedMap<K, V> extends TransactionAwareMap<K, V> {

    protected RMap<K, V> redisMap;

    public RedisBackedMap(RMap<K, V> redisMap, TransactionCacheTracker tracker) {
        this(redisMap, tracker, true);
    }

    public RedisBackedMap(RMap<K, V> redisMap, TransactionCacheTracker tracker, boolean greedyLoad) {
        super(redisMap.getName(), tracker, greedyLoad ?
            new GreedyMapGetter<>(redisMap.getName(), () -> redisMap.readAllMap()) :
            new LazyMapGetter<>(redisMap.getName(), (key) -> redisMap.get(key)));
        this.redisMap = redisMap;
    }

    public RMap<K, V> getMap() {
        return redisMap;
    }
}
