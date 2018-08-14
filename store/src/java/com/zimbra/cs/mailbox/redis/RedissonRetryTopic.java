package com.zimbra.cs.mailbox.redis;

import java.util.List;

import org.redisson.api.RFuture;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.listener.StatusListener;

public class RedissonRetryTopic<M> extends RedissonRetryDecorator<RTopic<M>> implements RTopic<M>{

    public RedissonRetryTopic(RedissonInitializer<RTopic<M>> topicInitializer, RedissonRetryClient client) {
        super(topicInitializer, client);
    }

    @Override
    public List<String> getChannelNames() {
        return runCommand(() -> redissonObject.getChannelNames());
    }

    @Override
    public long publish(M message) {
        return runCommand(() -> redissonObject.publish(message));
    }

    @Override
    public int addListener(MessageListener<M> listener) {
        return runCommand(() -> redissonObject.addListener(listener));
    }

    @Override
    public void removeListener(MessageListener<?> listener) {
        runCommand(() -> { redissonObject.removeListener(listener); return null; });
    }

    @Override
    public RFuture<Long> publishAsync(M message) {
        return runCommand(() -> redissonObject.publishAsync(message));
    }

    @Override
    public int addListener(StatusListener listener) {
        return runCommand(() -> redissonObject.addListener(listener));
    }


    @Override
    public void removeListener(int listenerId) {
        runCommand(() -> { redissonObject.removeListener(listenerId); return null; });

    }

    @Override
    public void removeAllListeners() {
        runCommand(() -> { redissonObject.removeAllListeners(); return null; });
    }
}
