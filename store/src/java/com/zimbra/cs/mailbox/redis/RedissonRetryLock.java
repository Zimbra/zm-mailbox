package com.zimbra.cs.mailbox.redis;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.redisson.api.RFuture;
import org.redisson.api.RLock;

public class RedissonRetryLock extends RedissonRetryDecorator<RLock> implements RLock {

    public RedissonRetryLock(RedissonInitializer<RLock> lockInitializer, RedissonRetryClient client) {
        super(lockInitializer, client);
    }

    @Override
    public void lock() {
        runCommand(() -> { redissonObject.lock(); return null; });
    }

    @Override
    public boolean tryLock() {
        return runCommand(() -> redissonObject.tryLock());
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return runCommand(() -> redissonObject.tryLock(time, unit));
    }

    @Override
    public void unlock() {
        runCommand(() -> { redissonObject.unlock(); return null; });
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        return runCommand(() -> redissonObject.tryLock(waitTime, leaseTime, unit));
    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        runCommand(() -> { redissonObject.lock(leaseTime, unit); return null; });
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return runCommand(() -> redissonObject.isHeldByCurrentThread());
    }

    @Override
    public int getHoldCount() {
        return runCommand(() -> redissonObject.getHoldCount());
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        runCommand(() -> { redissonObject.lockInterruptibly(); return null; });

    }

    @Override
    public Condition newCondition() {
        return runCommand(() -> redissonObject.newCondition());
    }

    @Override
    public RFuture<Boolean> forceUnlockAsync() {
        return runCommand(() -> redissonObject.forceUnlockAsync());
    }

    @Override
    public RFuture<Void> unlockAsync() {
        return runCommand(() -> redissonObject.unlockAsync());
    }

    @Override
    public RFuture<Void> unlockAsync(long threadId) {
        return runCommand(() -> redissonObject.unlockAsync(threadId));
    }

    @Override
    public RFuture<Boolean> tryLockAsync() {
        return runCommand(() -> redissonObject.tryLockAsync());
    }

    @Override
    public RFuture<Void> lockAsync() {
        return runCommand(() -> redissonObject.lockAsync());
    }

    @Override
    public RFuture<Void> lockAsync(long threadId) {
        return runCommand(() -> redissonObject.lockAsync(threadId));
    }

    @Override
    public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit) {
        return runCommand(() -> redissonObject.lockAsync(leaseTime, unit));
    }

    @Override
    public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit, long threadId) {
        return runCommand(() -> redissonObject.lockAsync(leaseTime, unit, threadId));
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long threadId) {
        return runCommand(() -> redissonObject.tryLockAsync(threadId));
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long waitTime, TimeUnit unit) {
        return runCommand(() -> redissonObject.tryLockAsync(waitTime, unit));
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit) {
        return runCommand(() -> redissonObject.tryLockAsync(waitTime, leaseTime, unit));
    }

    @Override
    public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
        return runCommand(() -> redissonObject.tryLockAsync(waitTime, leaseTime, unit, threadId));
    }

    @Override
    public void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException {
        runCommand(() -> { redissonObject.lockInterruptibly(leaseTime, unit); return null; });
    }

    @Override
    public boolean forceUnlock() {
        return runCommand(() -> redissonObject.forceUnlock());

    }

    @Override
    public boolean isLocked() {
        return runCommand(() -> redissonObject.isLocked());
    }

    @Override
    public RFuture<Integer> getHoldCountAsync() {
        return runCommand(() -> redissonObject.getHoldCountAsync());
    }

    @Override
    public RFuture<Boolean> isLockedAsync() {
        return runCommand(() -> redissonObject.isLockedAsync());
    }

    @Override
    public RFuture<Long> remainTimeToLiveAsync() {
        return runCommand(() -> redissonObject.remainTimeToLiveAsync());
    }

    @Override
    public String getName() {
        return redissonObject.getName();
    }

    @Override
    public boolean isHeldByThread(long arg0) {
        return runCommand(() -> redissonObject.isHeldByThread(arg0));
    }

    @Override
    public long remainTimeToLive() {
        return runCommand(() -> redissonObject.remainTimeToLive());
    }
}
