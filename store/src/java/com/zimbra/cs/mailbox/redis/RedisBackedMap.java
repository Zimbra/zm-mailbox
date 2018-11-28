package com.zimbra.cs.mailbox.redis;

import java.util.Map;

import org.redisson.api.RMap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.TransactionCacheTracker;
import com.zimbra.cs.mailbox.TransactionAwareMap;

/**
 * This class tracks changes to an underlying redis map that get batched over the course of a transaction
 */
public class RedisBackedMap<K, V> extends TransactionAwareMap<K, V> {

    protected RMap<K, V> redisMap;

    public RedisBackedMap(RMap<K, V> redisMap, TransactionCacheTracker tracker) {
        super(tracker, redisMap.getName());
        this.redisMap = redisMap;
    }

    public RMap<K, V> getMap() {
        return redisMap;
    }

    @Override
    public void clearLocalCache() {
        ZimbraLog.cache.trace("clearing cache of redis map %s", getName());
        super.clearLocalCache();
    }

    @Override
    protected Map<K, V> initLocalCache() {
        return redisMap.readAllMap();
    }
}
