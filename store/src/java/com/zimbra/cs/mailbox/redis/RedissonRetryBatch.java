package com.zimbra.cs.mailbox.redis;

import java.util.concurrent.TimeUnit;

import org.redisson.api.BatchResult;
import org.redisson.api.RAtomicDoubleAsync;
import org.redisson.api.RAtomicLongAsync;
import org.redisson.api.RBatch;
import org.redisson.api.RBitSetAsync;
import org.redisson.api.RBlockingDequeAsync;
import org.redisson.api.RBlockingQueueAsync;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RDequeAsync;
import org.redisson.api.RFuture;
import org.redisson.api.RGeoAsync;
import org.redisson.api.RHyperLogLogAsync;
import org.redisson.api.RKeysAsync;
import org.redisson.api.RLexSortedSetAsync;
import org.redisson.api.RListAsync;
import org.redisson.api.RMapAsync;
import org.redisson.api.RMapCacheAsync;
import org.redisson.api.RMultimapAsync;
import org.redisson.api.RMultimapCacheAsync;
import org.redisson.api.RQueueAsync;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RScript;
import org.redisson.api.RScriptAsync;
import org.redisson.api.RSetAsync;
import org.redisson.api.RSetCacheAsync;
import org.redisson.api.RStreamAsync;
import org.redisson.api.RTopicAsync;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;

public class RedissonRetryBatch extends RedissonRetryDecorator<RBatch> implements RBatch {

    public RedissonRetryBatch(RedissonInitializer<RBatch> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }

    @Override
    public <K, V> RMapAsync<K, V> getMap(String name) {
        return runCommand(() -> redissonObject.getMap(name));
    }

    @Override
    public <K, V> RMapAsync<K, V> getMap(String name, Codec codec) {
        return runCommand(() -> redissonObject.getMap(name, codec));
    }

    @Override
    public <V> RSetAsync<V> getSet(String name) {
        return runCommand(() -> redissonObject.getSet(name));
    }

    @Override
    public <V> RSetAsync<V> getSet(String name, Codec codec) {
        return runCommand(() -> redissonObject.getSet(name, codec));
    }

    @Override
    public <V> RScoredSortedSetAsync<V> getScoredSortedSet(String name) {
        return runCommand(() -> redissonObject.getScoredSortedSet(name));
    }

    @Override
    public <V> RScoredSortedSetAsync<V> getScoredSortedSet(String name, Codec codec) {
        return runCommand(() -> redissonObject.getScoredSortedSet(name, codec));
    }

    @Override
    public <V> RGeoAsync<V> getGeo(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RGeoAsync<V> getGeo(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getSetMultimap(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getSetMultimap(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapCacheAsync<K, V> getSetMultimapCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapCacheAsync<K, V> getSetMultimapCache(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSetCacheAsync<V> getSetCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSetCacheAsync<V> getSetCache(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMapCacheAsync<K, V> getMapCache(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMapCacheAsync<K, V> getMapCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBucketAsync<V> getBucket(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBucketAsync<V> getBucket(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RHyperLogLogAsync<V> getHyperLogLog(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RHyperLogLogAsync<V> getHyperLogLog(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RListAsync<V> getList(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RListAsync<V> getList(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getListMultimap(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getListMultimap(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getListMultimapCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getListMultimapCache(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RQueueAsync<V> getQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RQueueAsync<V> getQueue(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingQueueAsync<V> getBlockingQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingQueueAsync<V> getBlockingQueue(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RDequeAsync<V> getDeque(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RDequeAsync<V> getDeque(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingDequeAsync<V> getBlockingDeque(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingDequeAsync<V> getBlockingDeque(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RAtomicLongAsync getAtomicLong(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RAtomicDoubleAsync getAtomicDouble(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLexSortedSetAsync getLexSortedSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RBitSetAsync getBitSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScriptAsync getScript() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RKeysAsync getKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BatchResult<?> execute() throws RedisException {
        return runCommand(() -> redissonObject.execute());
    }

    @Override
    public RFuture<BatchResult<?>> executeAsync() {
        return runCommand(() -> redissonObject.executeAsync());
    }

    @Override
    public RBatch atomic() {
        return redissonObject.atomic();
    }

    @Override
    public RBatch skipResult() {
        return redissonObject.skipResult();
    }

    @Override
    public RBatch syncSlaves(int slaves, long timeout, TimeUnit unit) {
        return redissonObject.syncSlaves(slaves, timeout, unit);
    }

    @Override
    public RBatch timeout(long timeout, TimeUnit unit) {
        return redissonObject.timeout(timeout, unit);
    }

    @Override
    public RBatch retryInterval(long retryInterval, TimeUnit unit) {
        return redissonObject.retryInterval(retryInterval, unit);
    }

    @Override
    public RBatch retryAttempts(int retryAttempts) {
        return redissonObject.retryAttempts(retryAttempts);
    }

    @Override
    public RScript getScript(Codec arg0) {
        return redissonObject.getScript(arg0);
    }

    @Override
    public <K, V> RStreamAsync<K, V> getStream(String arg0) {
        return redissonObject.getStream(arg0);
    }

    @Override
    public <K, V> RStreamAsync<K, V> getStream(String arg0, Codec arg1) {
        return redissonObject.getStream(arg0, arg1);
    }

    @Override
    public RTopicAsync getTopic(String arg0) {
        return redissonObject.getTopic(arg0);
    }

    @Override
    public RTopicAsync getTopic(String arg0, Codec arg1) {
        return redissonObject.getTopic(arg0, arg1);
    }
}
