package com.zimbra.cs.mailbox.redis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.PendingEntry;
import org.redisson.api.PendingResult;
import org.redisson.api.RFuture;
import org.redisson.api.RStream;
import org.redisson.api.StreamConsumer;
import org.redisson.api.StreamGroup;
import org.redisson.api.StreamInfo;
import org.redisson.api.StreamMessageId;

public class RedissonRetryStream<K, V> extends RedissonRetryExpirable<RStream<K, V>> implements RStream<K, V> {

    public RedissonRetryStream(RedissonInitializer<RStream<K, V>> initializer, RedissonRetryClient client) {
        super(initializer, client);
    }

    @Override
    public RFuture<Long> ackAsync(String arg0, StreamMessageId... arg1) {
        return runCommand(() -> redissonObject.ackAsync(arg0, arg1));
    }

    @Override
    public RFuture<StreamMessageId> addAllAsync(Map<K, V> arg0) {
        return runCommand(() -> redissonObject.addAllAsync(arg0));
    }

    @Override
    public RFuture<Void> addAllAsync(StreamMessageId arg0, Map<K, V> arg1) {
        return runCommand(() -> redissonObject.addAllAsync(arg0, arg1));
    }

    @Override
    public RFuture<StreamMessageId> addAllAsync(Map<K, V> arg0, int arg1, boolean arg2) {
        return runCommand(() -> redissonObject.addAllAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Void> addAllAsync(StreamMessageId arg0, Map<K, V> arg1, int arg2, boolean arg3) {
        return runCommand(() -> redissonObject.addAllAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<StreamMessageId> addAsync(K arg0, V arg1) {
        return runCommand(() -> redissonObject.addAsync(arg0, arg1));
    }

    @Override
    public RFuture<Void> addAsync(StreamMessageId arg0, K arg1, V arg2) {
        return runCommand(() -> redissonObject.addAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<StreamMessageId> addAsync(K arg0, V arg1, int arg2, boolean arg3) {
        return runCommand(() -> redissonObject.addAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Void> addAsync(StreamMessageId arg0, K arg1, V arg2, int arg3, boolean arg4) {
        return runCommand(() -> redissonObject.addAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> claimAsync(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId... arg4) {
        return runCommand(() -> redissonObject.claimAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Void> createGroupAsync(String arg0) {
        return runCommand(() -> redissonObject.createGroupAsync(arg0));
    }

    @Override
    public RFuture<Void> createGroupAsync(String arg0, StreamMessageId arg1) {
        return runCommand(() -> redissonObject.createGroupAsync(arg0, arg1));
    }

    @Override
    public RFuture<List<StreamMessageId>> fastClaimAsync(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId... arg4) {
        return runCommand(() -> redissonObject.fastClaimAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<StreamInfo<K, V>> getInfoAsync() {
        return runCommand(() -> redissonObject.getInfoAsync());
    }

    @Override
    public RFuture<PendingResult> getPendingInfoAsync(String arg0) {
        return runCommand(() -> redissonObject.getPendingInfoAsync(arg0));
    }

    @Override
    public RFuture<List<StreamConsumer>> listConsumersAsync(String arg0) {
        return runCommand(() -> redissonObject.listConsumersAsync(arg0));
    }

    @Override
    public RFuture<List<StreamGroup>> listGroupsAsync() {
        return runCommand(() -> redissonObject.listGroupsAsync());
    }

    @Override
    public RFuture<PendingResult> listPendingAsync(String arg0) {
        return runCommand(() -> redissonObject.listPendingAsync(arg0));
    }

    @Override
    public RFuture<List<PendingEntry>> listPendingAsync(String arg0, StreamMessageId arg1, StreamMessageId arg2,
            int arg3) {
        return runCommand(() -> redissonObject.listPendingAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<List<PendingEntry>> listPendingAsync(String arg0, String arg1, StreamMessageId arg2,
            StreamMessageId arg3, int arg4) {
        return runCommand(() -> redissonObject.listPendingAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> pendingRangeAsync(String arg0, StreamMessageId arg1,
            StreamMessageId arg2, int arg3) {
        return runCommand(() -> redissonObject.pendingRangeAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> pendingRangeAsync(String arg0, String arg1, StreamMessageId arg2,
            StreamMessageId arg3, int arg4) {
        return runCommand(() -> redissonObject.pendingRangeAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> rangeAsync(StreamMessageId arg0, StreamMessageId arg1) {
        return runCommand(() -> redissonObject.rangeAsync(arg0, arg1));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> rangeAsync(int arg0, StreamMessageId arg1, StreamMessageId arg2) {
        return runCommand(() -> redissonObject.rangeAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> rangeReversedAsync(StreamMessageId arg0, StreamMessageId arg1) {
        return runCommand(() -> redissonObject.rangeReversedAsync(arg0, arg1));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> rangeReversedAsync(int arg0, StreamMessageId arg1,
            StreamMessageId arg2) {
        return runCommand(() -> redissonObject.rangeReversedAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readAsync(StreamMessageId... arg0) {
        return runCommand(() -> redissonObject.readAsync(arg0));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readAsync(int arg0, StreamMessageId... arg1) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(StreamMessageId arg0,
            Map<String, StreamMessageId> arg1) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readAsync(long arg0, TimeUnit arg1, StreamMessageId... arg2) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(StreamMessageId arg0, String arg1,
            StreamMessageId arg2) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(int arg0, StreamMessageId arg1,
            Map<String, StreamMessageId> arg2) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readAsync(int arg0, long arg1, TimeUnit arg2,
            StreamMessageId... arg3) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(int arg0, StreamMessageId arg1, String arg2,
            StreamMessageId arg3) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(long arg0, TimeUnit arg1,
            StreamMessageId arg2, Map<String, StreamMessageId> arg3) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(StreamMessageId arg0, String arg1,
            StreamMessageId arg2, String arg3, StreamMessageId arg4) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(long arg0, TimeUnit arg1,
            StreamMessageId arg2, String arg3, StreamMessageId arg4) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(int arg0, long arg1, TimeUnit arg2,
            StreamMessageId arg3, Map<String, StreamMessageId> arg4) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(int arg0, StreamMessageId arg1, String arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(int arg0, long arg1, TimeUnit arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(long arg0, TimeUnit arg1,
            StreamMessageId arg2, String arg3, StreamMessageId arg4, String arg5, StreamMessageId arg6) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readAsync(int arg0, long arg1, TimeUnit arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7) {
        return runCommand(() -> redissonObject.readAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readGroupAsync(String arg0, String arg1, StreamMessageId... arg2) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readGroupAsync(String arg0, String arg1, int arg2,
            StreamMessageId... arg3) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1,
            StreamMessageId arg2, Map<String, StreamMessageId> arg3) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readGroupAsync(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId... arg4) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, int arg2,
            StreamMessageId arg3, Map<String, StreamMessageId> arg4) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1,
            StreamMessageId arg2, String arg3, StreamMessageId arg4) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public RFuture<Map<StreamMessageId, Map<K, V>>> readGroupAsync(String arg0, String arg1, int arg2, long arg3,
            TimeUnit arg4, StreamMessageId... arg5) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, long arg2,
            TimeUnit arg3, StreamMessageId arg4, Map<String, StreamMessageId> arg5) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, int arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1,
            StreamMessageId arg2, String arg3, StreamMessageId arg4, String arg5, StreamMessageId arg6) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, long arg2,
            TimeUnit arg3, StreamMessageId arg4, String arg5, StreamMessageId arg6) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, int arg2,
            long arg3, TimeUnit arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, int arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, long arg2,
            TimeUnit arg3, StreamMessageId arg4, String arg5, StreamMessageId arg6, String arg7, StreamMessageId arg8) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
    }

    @Override
    public RFuture<Map<String, Map<StreamMessageId, Map<K, V>>>> readGroupAsync(String arg0, String arg1, int arg2,
            long arg3, TimeUnit arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7, String arg8,
            StreamMessageId arg9) {
        return runCommand(() -> redissonObject.readGroupAsync(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9));
    }

    @Override
    public RFuture<Long> removeAsync(StreamMessageId... arg0) {
        return runCommand(() -> redissonObject.removeAsync(arg0));
    }

    @Override
    public RFuture<Long> removeConsumerAsync(String arg0, String arg1) {
        return runCommand(() -> redissonObject.removeConsumerAsync(arg0, arg1));
    }

    @Override
    public RFuture<Void> removeGroupAsync(String arg0) {
        return runCommand(() -> redissonObject.removeGroupAsync(arg0));
    }

    @Override
    public RFuture<Long> sizeAsync() {
        return runCommand(() -> redissonObject.sizeAsync());
    }

    @Override
    public RFuture<Long> trimAsync(int arg0) {
        return runCommand(() -> redissonObject.trimAsync(arg0));
    }

    @Override
    public RFuture<Long> trimNonStrictAsync(int arg0) {
        return runCommand(() -> redissonObject.trimNonStrictAsync(arg0));
    }

    @Override
    public RFuture<Void> updateGroupMessageIdAsync(String arg0, StreamMessageId arg1) {
        return runCommand(() -> redissonObject.updateGroupMessageIdAsync(arg0, arg1));
    }

    @Override
    public long ack(String arg0, StreamMessageId... arg1) {
        return runCommand(() -> redissonObject.ack(arg0, arg1));
    }

    @Override
    public StreamMessageId add(K arg0, V arg1) {
        return runCommand(() -> redissonObject.add(arg0, arg1));
    }

    @Override
    public void add(StreamMessageId arg0, K arg1, V arg2) {
        runCommand(() -> { redissonObject.add(arg0, arg1, arg2); return null; });
    }

    @Override
    public StreamMessageId add(K arg0, V arg1, int arg2, boolean arg3) {
        return runCommand(() -> redissonObject.add(arg0, arg1, arg2, arg3));
    }

    @Override
    public void add(StreamMessageId arg0, K arg1, V arg2, int arg3, boolean arg4) {
        runCommand(() -> { redissonObject.add(arg0, arg1, arg2, arg3, arg4); return null; });
    }

    @Override
    public StreamMessageId addAll(Map<K, V> arg0) {
        return runCommand(() -> redissonObject.addAll(arg0));
    }

    @Override
    public void addAll(StreamMessageId arg0, Map<K, V> arg1) {
        runCommand(() -> { redissonObject.addAll(arg0, arg1); return null; });
    }

    @Override
    public StreamMessageId addAll(Map<K, V> arg0, int arg1, boolean arg2) {
        return runCommand(() -> redissonObject.addAll(arg0, arg1, arg2));
    }

    @Override
    public void addAll(StreamMessageId arg0, Map<K, V> arg1, int arg2, boolean arg3) {
        runCommand(() -> { redissonObject.addAll(arg0, arg1, arg2, arg3); return null; });
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> claim(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId... arg4) {
        return runCommand(() -> redissonObject.claim(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public void createGroup(String arg0) {
        runCommand(() -> { redissonObject.createGroup(arg0); return null; });
    }

    @Override
    public void createGroup(String arg0, StreamMessageId arg1) {
        runCommand(() -> { redissonObject.createGroup(arg0, arg1); return null; });
    }

    @Override
    public List<StreamMessageId> fastClaim(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId... arg4) {
        return runCommand(() -> redissonObject.fastClaim(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public StreamInfo<K, V> getInfo() {
        return runCommand(() -> redissonObject.getInfo());
    }

    @Override
    public PendingResult getPendingInfo(String arg0) {
        return runCommand(() -> redissonObject.getPendingInfo(arg0));
    }

    @Override
    public List<StreamConsumer> listConsumers(String arg0) {
        return runCommand(() -> redissonObject.listConsumers(arg0));
    }

    @Override
    public List<StreamGroup> listGroups() {
        return runCommand(() -> redissonObject.listGroups());
    }

    @Override
    public PendingResult listPending(String arg0) {
        return runCommand(() -> redissonObject.listPending(arg0));
    }

    @Override
    public List<PendingEntry> listPending(String arg0, StreamMessageId arg1, StreamMessageId arg2, int arg3) {
        return runCommand(() -> redissonObject.listPending(arg0, arg1, arg2, arg3));
    }

    @Override
    public List<PendingEntry> listPending(String arg0, String arg1, StreamMessageId arg2, StreamMessageId arg3,
            int arg4) {
        return runCommand(() -> redissonObject.listPending(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> pendingRange(String arg0, StreamMessageId arg1, StreamMessageId arg2,
            int arg3) {
        return runCommand(() -> redissonObject.pendingRange(arg0, arg1, arg2, arg3));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> pendingRange(String arg0, String arg1, StreamMessageId arg2,
            StreamMessageId arg3, int arg4) {
        return runCommand(() -> redissonObject.pendingRange(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> range(StreamMessageId arg0, StreamMessageId arg1) {
        return runCommand(() -> redissonObject.range(arg0, arg1));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> range(int arg0, StreamMessageId arg1, StreamMessageId arg2) {
        return runCommand(() -> redissonObject.range(arg0, arg1, arg2));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> rangeReversed(StreamMessageId arg0, StreamMessageId arg1) {
        return runCommand(() -> redissonObject.rangeReversed(arg0, arg1));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> rangeReversed(int arg0, StreamMessageId arg1, StreamMessageId arg2) {
        return runCommand(() -> redissonObject.rangeReversed(arg0, arg1, arg2));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> read(StreamMessageId... arg0) {
        return runCommand(() -> redissonObject.read(arg0));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> read(int arg0, StreamMessageId... arg1) {
        return runCommand(() -> redissonObject.read(arg0, arg1));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(StreamMessageId arg0, Map<String, StreamMessageId> arg1) {
        return runCommand(() -> redissonObject.read(arg0, arg1));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> read(long arg0, TimeUnit arg1, StreamMessageId... arg2) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(StreamMessageId arg0, String arg1, StreamMessageId arg2) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(int arg0, StreamMessageId arg1,
            Map<String, StreamMessageId> arg2) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> read(int arg0, long arg1, TimeUnit arg2, StreamMessageId... arg3) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(int arg0, StreamMessageId arg1, String arg2,
            StreamMessageId arg3) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(long arg0, TimeUnit arg1, StreamMessageId arg2,
            Map<String, StreamMessageId> arg3) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(StreamMessageId arg0, String arg1, StreamMessageId arg2,
            String arg3, StreamMessageId arg4) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(long arg0, TimeUnit arg1, StreamMessageId arg2,
            String arg3, StreamMessageId arg4) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(int arg0, long arg1, TimeUnit arg2, StreamMessageId arg3,
            Map<String, StreamMessageId> arg4) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(int arg0, StreamMessageId arg1, String arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(int arg0, long arg1, TimeUnit arg2, StreamMessageId arg3,
            String arg4, StreamMessageId arg5) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(long arg0, TimeUnit arg1, StreamMessageId arg2,
            String arg3, StreamMessageId arg4, String arg5, StreamMessageId arg6) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> read(int arg0, long arg1, TimeUnit arg2, StreamMessageId arg3,
            String arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7) {
        return runCommand(() -> redissonObject.read(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> readGroup(String arg0, String arg1, StreamMessageId... arg2) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> readGroup(String arg0, String arg1, int arg2, StreamMessageId... arg3) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, StreamMessageId arg2,
            Map<String, StreamMessageId> arg3) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> readGroup(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId... arg4) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, int arg2,
            StreamMessageId arg3, Map<String, StreamMessageId> arg4) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, StreamMessageId arg2,
            String arg3, StreamMessageId arg4) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4));
    }

    @Override
    public Map<StreamMessageId, Map<K, V>> readGroup(String arg0, String arg1, int arg2, long arg3, TimeUnit arg4,
            StreamMessageId... arg5) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId arg4, Map<String, StreamMessageId> arg5) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, int arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, int arg2, long arg3,
            TimeUnit arg4, StreamMessageId arg5, Map<String, StreamMessageId> arg6) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, StreamMessageId arg2,
            String arg3, StreamMessageId arg4, String arg5, StreamMessageId arg6) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId arg4, String arg5, StreamMessageId arg6) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, int arg2,
            StreamMessageId arg3, String arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, int arg2, long arg3,
            TimeUnit arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, long arg2, TimeUnit arg3,
            StreamMessageId arg4, String arg5, StreamMessageId arg6, String arg7, StreamMessageId arg8) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
    }

    @Override
    public Map<String, Map<StreamMessageId, Map<K, V>>> readGroup(String arg0, String arg1, int arg2, long arg3,
            TimeUnit arg4, StreamMessageId arg5, String arg6, StreamMessageId arg7, String arg8, StreamMessageId arg9) {
        return runCommand(() -> redissonObject.readGroup(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9));
    }

    @Override
    public long remove(StreamMessageId... arg0) {
        return runCommand(() -> redissonObject.remove(arg0));
    }

    @Override
    public long removeConsumer(String arg0, String arg1) {
        return runCommand(() -> redissonObject.removeConsumer(arg0, arg1));
    }

    @Override
    public void removeGroup(String arg0) {
        runCommand(() -> { redissonObject.removeGroup(arg0); return null; });
    }

    @Override
    public long size() {
        return runCommand(() -> redissonObject.size());
    }

    @Override
    public long trim(int arg0) {
        return runCommand(() -> redissonObject.trim(arg0));
    }

    @Override
    public long trimNonStrict(int arg0) {
        return runCommand(() -> redissonObject.trimNonStrict(arg0));
    }

    @Override
    public void updateGroupMessageId(String arg0, StreamMessageId arg1) {
        runCommand(() -> { redissonObject.updateGroupMessageId(arg0, arg1); return null; });
    }

}
