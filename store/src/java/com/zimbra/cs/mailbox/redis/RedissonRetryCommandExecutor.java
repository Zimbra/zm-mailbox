package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.List;

import org.redisson.SlotCallback;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandExecutor;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.MasterSlaveEntry;
import org.redisson.liveobject.core.RedissonObjectBuilder;

public class RedissonRetryCommandExecutor extends RedissonRetryDecorator<CommandExecutor> implements CommandExecutor {

    public RedissonRetryCommandExecutor(RedissonInitializer<CommandExecutor> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }

    @Override
    public <V> V get(RFuture<V> future) {
        return runCommand(() -> redissonObject.get(future));
    }

    @Override
    public <T, R> R read(String key, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.read(key, command, params));
    }

    @Override
    public <T, R> R read(String key, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.read(key, codec, command, params));
    }

    @Override
    public <T, R> R evalRead(String key, RedisCommand<T> evalCommandType, String script, List<Object> keys,
            Object... params) {
        return runCommand(() -> redissonObject.evalRead(key, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> R evalRead(String key, Codec codec, RedisCommand<T> evalCommandType, String script, List<Object> keys,
            Object... params) {
        return runCommand(() -> redissonObject.evalRead(key, codec, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> R evalWrite(String key, RedisCommand<T> evalCommandType, String script, List<Object> keys,
            Object... params) {
        return runCommand(() -> redissonObject.evalWrite(key, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> R evalWrite(String key, Codec codec, RedisCommand<T> evalCommandType, String script,
            List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalWrite(key, codec, evalCommandType, script, keys, params));
    }

    @Override
    public ConnectionManager getConnectionManager() {
        return redissonObject.getConnectionManager();
    }

    @Override
    public RedissonObjectBuilder getObjectBuilder() {
        return redissonObject.getObjectBuilder();
    }

    @Override
    public CommandAsyncExecutor enableRedissonReferenceSupport(RedissonClient redisson) {
        return redissonObject.enableRedissonReferenceSupport(redisson);
    }

    @Override
    public CommandAsyncExecutor enableRedissonReferenceSupport(RedissonReactiveClient redissonReactive) {
        return redissonObject.enableRedissonReferenceSupport(redissonReactive);
    }

    @Override
    public CommandAsyncExecutor enableRedissonReferenceSupport(RedissonRxClient redissonReactive) {
        return redissonObject.enableRedissonReferenceSupport(redissonReactive);
    }

    @Override
    public boolean isRedissonReferenceSupportEnabled() {
        return redissonObject.isRedissonReferenceSupportEnabled();
    }

    @Override
    public <V> RedisException convertException(RFuture<V> future) {
        return redissonObject.convertException(future);
    }

    @Override
    public void syncSubscription(RFuture<?> future) {
        redissonObject.syncSubscription(future);

    }

    @Override
    public <V> V getInterrupted(RFuture<V> future) throws InterruptedException {
        return redissonObject.getInterrupted(future);
    }

    @Override
    public <T, R> RFuture<R> writeAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> command,
            Object... params) {
        return runCommand(() -> redissonObject.writeAsync(entry, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> writeAsync(byte[] key, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.writeAsync(key, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(RedisClient client, MasterSlaveEntry entry, Codec codec, RedisCommand<T> command,
            Object... params) {
        return runCommand(() -> redissonObject.readAsync(client, entry, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(RedisClient client, String name, Codec codec, RedisCommand<T> command,
            Object... params) {
        return runCommand(() -> redissonObject.readAsync(client, name, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(RedisClient client, byte[] key, Codec codec, RedisCommand<T> command,
            Object... params) {
        return runCommand(() -> redissonObject.readAsync(client, key, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(RedisClient client, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAsync(client, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> evalWriteAllAsync(RedisCommand<T> command, SlotCallback<T, R> callback, String script,
            List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalWriteAllAsync(command, callback, script, keys, params));
    }

    @Override
    public <R, T> RFuture<R> writeAllAsync(RedisCommand<T> command, SlotCallback<T, R> callback, Object... params) {
        return runCommand(() -> redissonObject.writeAllAsync(command, callback, params));
    }

    @Override
    public <T, R> RFuture<Collection<R>> readAllAsync(Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAllAsync(codec, command, params));
    }

    @Override
    public <R, T> RFuture<R> readAllAsync(RedisCommand<T> command, SlotCallback<T, R> callback, Object... params) {
        return runCommand(() -> redissonObject.readAllAsync(command, callback, params));
    }

    @Override
    public <T, R> RFuture<Collection<R>> readAllAsync(Collection<R> results, Codec codec, RedisCommand<T> command,
            Object... params) {
        return runCommand(() -> redissonObject.readAllAsync(results, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> evalReadAsync(RedisClient client, String name, Codec codec,
            RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalReadAsync(client, name, codec, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> RFuture<R> evalReadAsync(String key, Codec codec, RedisCommand<T> evalCommandType, String script,
            List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalReadAsync(key, codec, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> RFuture<R> evalReadAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> evalCommandType,
            String script, List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalReadAsync(entry, codec, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> RFuture<R> evalWriteAsync(String key, Codec codec, RedisCommand<T> evalCommandType, String script,
            List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalWriteAsync(key, codec, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> RFuture<R> evalWriteAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> evalCommandType,
            String script, List<Object> keys, Object... params) {
        return runCommand(() -> redissonObject.evalWriteAsync(entry, codec, evalCommandType, script, keys, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(byte[] key, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAsync(key, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(String key, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAsync(key, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> writeAsync(String key, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.writeAsync(key, codec, command, params));
    }

    @Override
    public <T, R> RFuture<Collection<R>> readAllAsync(RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAllAsync(command, params));
    }

    @Override
    public <R, T> RFuture<R> writeAllAsync(Codec codec, RedisCommand<T> command, SlotCallback<T, R> callback,
            Object... params) {
        return runCommand(() -> redissonObject.writeAllAsync(codec, command, callback, params));
    }

    @Override
    public <T> RFuture<Void> writeAllAsync(RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.writeAllAsync(command, params));
    }

    @Override
    public <T, R> RFuture<R> writeAsync(String key, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.writeAsync(key, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(String key, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAsync(key, command, params));
    }

    @Override
    public <T, R> RFuture<R> readAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readAsync(entry, codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readRandomAsync(Codec codec, RedisCommand<T> command, Object... params) {
        return runCommand(() -> redissonObject.readRandomAsync(codec, command, params));
    }

    @Override
    public <T, R> RFuture<R> readRandomAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> command,
            Object... params) {
        return runCommand(() -> redissonObject.readRandomAsync(entry, codec, command, params));
    }

    @Override
    public <V> RFuture<V> pollFromAnyAsync(String name, Codec codec, RedisCommand<Object> command, long secondsTimeout,
            String... queueNames) {
        return runCommand(() -> redissonObject.pollFromAnyAsync(name, codec, command, secondsTimeout, queueNames));
    }

    @Override
    public <T, R> RFuture<R> readBatchedAsync(Codec codec, RedisCommand<T> command, SlotCallback<T, R> callback,
            String... keys) {
        return runCommand(() -> redissonObject.readBatchedAsync(codec, command, callback, keys));
    }

    @Override
    public <T, R> RFuture<R> writeBatchedAsync(Codec codec, RedisCommand<T> command, SlotCallback<T, R> callback,
            String... keys) {
        return runCommand(() -> redissonObject.writeBatchedAsync(codec, command, callback, keys));
    }
}
