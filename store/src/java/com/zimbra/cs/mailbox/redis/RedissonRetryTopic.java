package com.zimbra.cs.mailbox.redis;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RFuture;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.listener.StatusListener;

import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

public class RedissonRetryTopic extends RedissonRetryDecorator<RTopic> implements RTopic {

    private Map<Integer, MessageListenerInfo<?>> messageListeners = new ConcurrentHashMap<>();
    private Map<Integer, StatusListener> statusListeners = new ConcurrentHashMap<>();

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
        int listenerId = runCommand(() -> redissonObject.addListener(type, listener));
        messageListeners.put(listenerId, new MessageListenerInfo<M>(type, listener));
        return listenerId;
    }

    @Override
    public void removeListener(MessageListener<?> listener) {
        removeListenerByReference(listener);
        runCommand(() -> { redissonObject.removeListener(listener); return null; });
    }

    @Override
    public RFuture<Long> publishAsync(Object message) {
        return runCommand(() -> redissonObject.publishAsync(message));
    }

    @Override
    public int addListener(StatusListener listener) {
        int listenerId = runCommand(() -> redissonObject.addListener(listener));
        statusListeners.put(listenerId, listener);
        return listenerId;
    }

    @Override
    public void removeAllListeners() {
        messageListeners.clear();
        statusListeners.clear();
        runCommand(() -> { redissonObject.removeAllListeners(); return null; });
    }

    @Override
    public <M> RFuture<Integer> addListenerAsync(Class<M> type, MessageListener<M> listener) {
        RFuture<Integer> future = runCommand(() -> redissonObject.addListenerAsync(type, listener));
        future.onComplete((listenerId, err) -> {
            if (err == null) {
                messageListeners.put(listenerId, new MessageListenerInfo<M>(type, listener));
            }
        });
        return future;
    }

    @Override
    public RFuture<Integer> addListenerAsync(StatusListener listener) {
        RFuture<Integer> future = runCommand(() -> redissonObject.addListenerAsync(listener));
        future.onComplete((listenerId, err) -> {
            if (err == null) {
                statusListeners.put(listenerId, listener);
            }
        });
        return future;
    }

    @Override
    public RFuture<Long> countSubscribersAsync() {
        return runCommand(() -> redissonObject.countSubscribersAsync());
    }

    @Override
    public RFuture<Void> removeListenerAsync(Integer... arg0) {
        removeListenersById(arg0);
        return runCommand(() -> redissonObject.removeListenerAsync(arg0));
    }

    @Override
    public RFuture<Void> removeListenerAsync(MessageListener<?> arg0) {
        removeListenerByReference(arg0);
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
        removeListenersById(arg0);
        runCommand(() -> { redissonObject.removeListener(arg0); return null; });
    }

    private void removeListenerByReference(MessageListener<?> listener) {
        Iterator<Map.Entry<Integer, MessageListenerInfo<?>>> iter = messageListeners.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, MessageListenerInfo<?>> entry = iter.next();
            if (entry.getValue().getListener() == listener) {
                iter.remove();
                break;
            }
        }
    }

    private void removeListenersById(Integer...listenerIds) {
        for (Integer id: listenerIds) {
            // we don't know if this refers to a message or status listener, try both maps
            if (messageListeners.remove(id) == null) {
                statusListeners.remove(id);
            }
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        String channelName = redissonObject.getChannelNames().get(0);
        if (messageListeners != null && !messageListeners.isEmpty()) {
            ZimbraLog.mailbox.debug("re-attaching %s listeners to pubsub channel %s", messageListeners.size(), channelName);
            for (MessageListenerInfo<?> listenerInfo: messageListeners.values()) {
                listenerInfo.registerListener();
            }
        }
        if (statusListeners != null && !statusListeners.isEmpty()) {
            ZimbraLog.mailbox.debug("re-attaching %s status listeners to pubsub channel %s", statusListeners.size(), channelName);
            for (StatusListener listener: statusListeners.values()) {
                redissonObject.addListener(listener);
            }
        }
    }

    private class MessageListenerInfo<M> extends Pair<Class<M>, MessageListener<? extends M>>{

        public MessageListenerInfo(Class<M> type, MessageListener<? extends M> listener) {
            super(type, listener);
        }

        public Class<M> getType() {
            return getFirst();
        }

        public MessageListener<? extends M> getListener() {
            return getSecond();
        }

        public void registerListener() {
            // we do this on the decorated object directly to avoid infinite recursion,
            // since this is called during redisson client re-initialization
            redissonObject.addListener(getType(), getListener());
        }
    }
}
