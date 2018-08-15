package com.zimbra.cs.mailbox.redis;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RExpirable;
import org.redisson.api.RFuture;
import org.redisson.client.codec.Codec;

public abstract class RedissonRetryExpirable<R extends RExpirable> extends RedissonRetryDecorator<R> implements RExpirable {

    public RedissonRetryExpirable(RedissonInitializer<R> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }


    @Override
    public String getName() {
        return redissonObject.getName();
    }

    @Override
    public Codec getCodec() {
        return redissonObject.getCodec();
    }

    @Override
    public boolean touch() {
        return runCommand(() -> redissonObject.touch());
    }

    @Override
    public void migrate(String host, int port, int database, long timeout) {
        runCommand(() -> { redissonObject.migrate(host, port, database, timeout); return null; });
    }

    @Override
    public void copy(String host, int port, int database, long timeout) {
        runCommand(() -> { redissonObject.copy(host, port, database, timeout); return null; });
    }

    @Override
    public boolean move(int database) {
        return runCommand(() -> redissonObject.move(database));
    }

    @Override
    public boolean delete() {
        return runCommand(() -> redissonObject.delete());
    }

    @Override
    public boolean unlink() {
        return runCommand(() -> redissonObject.unlink());
    }

    @Override
    public void rename(String newName) {
        runCommand(() -> {redissonObject.rename(newName); return null; });

    }

    @Override
    public boolean renamenx(String newName) {
        return runCommand(() -> redissonObject.renamenx(newName));
    }

    @Override
    public boolean isExists() {
        return runCommand(() -> redissonObject.isExists());
    }

    @Override
    public boolean expire(long timeToLive, TimeUnit timeUnit) {
        return runCommand(() -> redissonObject.expire(timeToLive, timeUnit));
    }

    @Override
    public boolean expireAt(long timestamp) {
        return runCommand(() -> redissonObject.expireAt(timestamp));
    }

    @Override
    public boolean expireAt(Date timestamp) {
        return runCommand(() -> redissonObject.expireAt(timestamp));
    }

    @Override
    public boolean clearExpire() {
        return runCommand(() -> redissonObject.clearExpire());
    }

    @Override
    public long remainTimeToLive() {
        return runCommand(() -> redissonObject.remainTimeToLive());
    }


    @Override
    public RFuture<Boolean> touchAsync() {
        return runCommand(() -> redissonObject.touchAsync());
    }


    @Override
    public RFuture<Void> migrateAsync(String host, int port, int database, long timeout) {
        return runCommand(() -> redissonObject.migrateAsync(host, port, database, timeout));
    }


    @Override
    public RFuture<Void> copyAsync(String host, int port, int database, long timeout) {
        return runCommand(() -> redissonObject.copyAsync(host, port, database, timeout));
    }


    @Override
    public RFuture<Boolean> moveAsync(int database) {
        return runCommand(() -> redissonObject.moveAsync(database));
    }


    @Override
    public RFuture<Boolean> deleteAsync() {
        return runCommand(() -> redissonObject.deleteAsync());
    }


    @Override
    public RFuture<Boolean> unlinkAsync() {
        return runCommand(() -> redissonObject.unlinkAsync());
    }


    @Override
    public RFuture<Void> renameAsync(String newName) {
        return runCommand(() -> redissonObject.renameAsync(newName));
    }


    @Override
    public RFuture<Boolean> renamenxAsync(String newName) {
        return runCommand(() -> redissonObject.renamenxAsync(newName));
    }


    @Override
    public RFuture<Boolean> isExistsAsync() {
        return runCommand(() -> redissonObject.isExistsAsync());
    }


    @Override
    public RFuture<Boolean> expireAsync(long timeToLive, TimeUnit timeUnit) {
        return runCommand(() -> redissonObject.expireAsync(timeToLive, timeUnit));
    }


    @Override
    public RFuture<Boolean> expireAtAsync(Date timestamp) {
        return runCommand(() -> redissonObject.expireAtAsync(timestamp));
    }


    @Override
    public RFuture<Boolean> expireAtAsync(long timestamp) {
        return runCommand(() -> redissonObject.expireAtAsync(timestamp));
    }


    @Override
    public RFuture<Boolean> clearExpireAsync() {
        return runCommand(() -> redissonObject.clearExpireAsync());
    }


    @Override
    public RFuture<Long> remainTimeToLiveAsync() {
        return runCommand(() -> redissonObject.remainTimeToLiveAsync());
    }
}
