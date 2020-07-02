package com.zimbra.cs.mailbox.redis;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RFuture;

public class RedissonRetryAtomicLong extends RedissonRetryExpirable<RAtomicLong> implements RAtomicLong {

    public RedissonRetryAtomicLong(RedissonInitializer<RAtomicLong> atomicLongInitializer, RedissonRetryClient client) {
        super(atomicLongInitializer, client);
    }

    @Override
    public long addAndGet(long delta) {
        return runCommand(() -> redissonObject.addAndGet(delta));
    }

    @Override
    public RFuture<Boolean> compareAndSetAsync(long expect, long update) {
        return runCommand(() -> redissonObject.compareAndSetAsync(expect, update));
    }

    @Override
    public RFuture<Long> addAndGetAsync(long delta) {
        return runCommand(() -> redissonObject.addAndGetAsync(delta));
    }

    @Override
    public RFuture<Long> decrementAndGetAsync() {
        return runCommand(() -> redissonObject.decrementAndGetAsync());
    }

    @Override
    public RFuture<Long> getAsync() {
        return runCommand(() -> redissonObject.getAsync());
    }

    @Override
    public RFuture<Long> getAndDeleteAsync() {
        return runCommand(() -> redissonObject.getAndDeleteAsync());
    }

    @Override
    public RFuture<Long> getAndAddAsync(long delta) {
        return runCommand(() -> redissonObject.getAndAddAsync(delta));
    }

    @Override
    public RFuture<Long> getAndSetAsync(long newValue) {
        return runCommand(() -> redissonObject.getAndSetAsync(newValue));
    }

    @Override
    public RFuture<Long> incrementAndGetAsync() {
        return runCommand(() -> redissonObject.incrementAndGetAsync());
    }

    @Override
    public RFuture<Long> getAndIncrementAsync() {
        return runCommand(() -> redissonObject.getAndIncrementAsync());
    }

    @Override
    public RFuture<Long> getAndDecrementAsync() {
        return runCommand(() -> redissonObject.getAndDecrementAsync());
    }

    @Override
    public RFuture<Void> setAsync(long newValue) {
        return runCommand(() -> redissonObject.setAsync(newValue));
    }

    @Override
    public long getAndDecrement() {
        return runCommand(() -> redissonObject.getAndDecrement());
    }

    @Override
    public boolean compareAndSet(long expect, long update) {
        return runCommand(() -> redissonObject.compareAndSet(expect, update));
    }

    @Override
    public long decrementAndGet() {
        return runCommand(() -> redissonObject.decrementAndGet());
    }

    @Override
    public long get() {
        return runCommand(() -> redissonObject.get());
    }

    @Override
    public long getAndDelete() {
        return runCommand(() -> redissonObject.getAndDelete());
    }

    @Override
    public long getAndAdd(long delta) {
        return runCommand(() -> redissonObject.getAndAdd(delta));
    }

    @Override
    public long getAndSet(long newValue) {
        return runCommand(() -> redissonObject.getAndSet(newValue));
    }

    @Override
    public long incrementAndGet() {
        return runCommand(() -> redissonObject.incrementAndGet());
    }

    @Override
    public long getAndIncrement() {
        return runCommand(() -> redissonObject.getAndIncrement());
    }

    @Override
    public void set(long newValue) {
        runCommand(() -> { redissonObject.set(newValue); return null; });
    }
}
