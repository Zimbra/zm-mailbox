package com.zimbra.cs.mailbox.redis;

import java.util.List;

import org.redisson.api.RFuture;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.listener.StatusListener;

public class RedissonRetryTopic extends RedissonRetryDecorator<RTopic> implements RTopic {

    public RedissonRetryTopic(RedissonInitializer<RTopic> topicInitializer, RedissonRetryClient client) {
        super(topicInitializer, client);
    }

    @Override
    public List<String> getChannelNames() {
        return runCommand(() -> redissonObject.getChannelNames());
    }

    @Override
    public long publish(Object message) {
        return runCommand(() -> redissonObject.publish(message));
    }

    @Override
    public <M> int addListener(Class<M> type, MessageListener<? extends M> listener) {
        return runCommand(() -> redissonObject.addListener(type, listener));
    }

    @Override
    public void removeListener(MessageListener<?> listener) {
        runCommand(() -> { redissonObject.removeListener(listener); return null; });
    }

    @Override
    public RFuture<Long> publishAsync(Object message) {
        return runCommand(() -> redissonObject.publishAsync(message));
    }

    @Override
    public int addListener(StatusListener listener) {
        return runCommand(() -> redissonObject.addListener(listener));
    }

    @Override
    public void removeAllListeners() {
        runCommand(() -> { redissonObject.removeAllListeners(); return null; });
    }

    @Override
    public <M> RFuture<Integer> addListenerAsync(Class<M> type, MessageListener<M> listener) {
        return runCommand(() -> redissonObject.addListenerAsync(type, listener));
    }

    @Override
    public RFuture<Integer> addListenerAsync(StatusListener listener) {
        return runCommand(() -> redissonObject.addListenerAsync(listener));
    }

    @Override
    public RFuture<Long> countSubscribersAsync() {
        return runCommand(() -> redissonObject.countSubscribersAsync());
    }

    @Override
    public RFuture<Void> removeListenerAsync(Integer... arg0) {
        return runCommand(() -> redissonObject.removeListenerAsync(arg0));
    }

    @Override
    public RFuture<Void> removeListenerAsync(MessageListener<?> arg0) {
        return runCommand(() -> redissonObject.removeListenerAsync(arg0));
    }

    @Override
    public int countListeners() {
        return runCommand(() -> redissonObject.countListeners());
    }

    @Override
    public long countSubscribers() {
        return runCommand(() -> redissonObject.countSubscribers());
    }

    @Override
    public void removeListener(Integer... arg0) {
        runCommand(() -> { redissonObject.removeListener(arg0); return null; });

    }
}
