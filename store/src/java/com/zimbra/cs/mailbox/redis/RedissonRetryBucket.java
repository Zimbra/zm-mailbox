package com.zimbra.cs.mailbox.redis;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RFuture;

public class RedissonRetryBucket<V> extends RedissonRetryExpirable<RBucket<V>> implements RBucket<V> {

    public RedissonRetryBucket(RedissonInitializer<RBucket<V>> bucketInitializer, RedissonRetryClient client) {
        super(bucketInitializer, client);
    }


    @Override
    public V get() {
        return runCommand(() -> redissonObject.get());
    }

    @Override
    public void set(V value) {
        runCommand(() -> { redissonObject.set(value); return null; });
    }

    @Override
    public void set(V value, long timeToLive, TimeUnit timeUnit) {
        runCommand(() -> { redissonObject.set(value, timeToLive, timeUnit); return null; });
    }


    @Override
    public RFuture<Long> sizeAsync() {
        return runCommand(() -> redissonObject.sizeAsync());
    }


    @Override
    public RFuture<V> getAsync() {
        return runCommand(() -> redissonObject.getAsync());
    }


    @Override
    public RFuture<V> getAndDeleteAsync() {
        return runCommand(() -> redissonObject.getAndDeleteAsync());
    }


    @Override
    public RFuture<Boolean> trySetAsync(V value) {
        return runCommand(() -> redissonObject.trySetAsync(value));
    }


    @Override
    public RFuture<Boolean> trySetAsync(V value, long timeToLive, TimeUnit timeUnit) {
        return runCommand(() -> redissonObject.trySetAsync(value, timeToLive, timeUnit));
    }


    @Override
    public RFuture<Boolean> compareAndSetAsync(V expect, V update) {
        return runCommand(() -> redissonObject.compareAndSetAsync(expect, update));
    }


    @Override
    public RFuture<V> getAndSetAsync(V newValue) {
        return runCommand(() -> redissonObject.getAndSetAsync(newValue));
    }


    @Override
    public RFuture<Void> setAsync(V value) {
        return runCommand(() -> redissonObject.setAsync(value));
    }


    @Override
    public RFuture<Void> setAsync(V value, long timeToLive, TimeUnit timeUnit) {
        return runCommand(() -> redissonObject.setAsync(value, timeToLive, timeUnit));
    }


    @Override
    public long size() {
        return runCommand(() -> redissonObject.size());
    }


    @Override
    public V getAndDelete() {
        return runCommand(() -> redissonObject.getAndDelete());
    }


    @Override
    public boolean trySet(V value) {
        return runCommand(() -> redissonObject.trySet(value));
    }


    @Override
    public boolean trySet(V value, long timeToLive, TimeUnit timeUnit) {
        return runCommand(() -> redissonObject.trySet(value, timeToLive, timeUnit));
    }


    @Override
    public boolean compareAndSet(V expect, V update) {
        return runCommand(() -> redissonObject.compareAndSet(expect, update));
    }


    @Override
    public V getAndSet(V newValue) {
        return runCommand(() -> redissonObject.getAndSet(newValue));
    }
}
