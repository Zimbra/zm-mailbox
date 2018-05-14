package com.zimbra.cs.pubsub;

import java.util.HashMap;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.pubsub.message.PubSubMsg;

public class RedisPubSub extends PubSub {
    private RedissonClient client;
    private final int numChannels;
    private HashMap<String, NotificationChannel> channelListenerMap;

    public RedisPubSub(RedissonClient client) {
        this.client = client;
        this.numChannels = LC.redis_num_pubsub_channels.intValue();
        ZimbraLog.pubsub.info("RedisPubSub() - initializing with %d channels", this.numChannels);
        this.channelListenerMap = new HashMap<>();
    }

    @Override
    public void startup() {
        super.startup();
        this.client = client;
    }

    public void addListener(String channel, ListenerCallback callback) {
        ZimbraLog.pubsub.debug("RedisPubSub.addListener(%s) - '%s'", channel, callback);
        super.addListener(channel, callback);
        if (!channelListenerMap.containsKey(channel)) {
            channelListenerMap.put(channel, new NotificationChannel(this, this.client, channel));
        }
    }

    public boolean removeListener(String channel, ListenerCallback callback) {
        ZimbraLog.pubsub.debug("RedisPubSub.removeListener(%s) - '%s'", channel, callback);
        boolean removed = super.removeListener(channel, callback);
        if (channelListenerMap.containsKey(channel) && removed) {
            NotificationChannel nchannel = channelListenerMap.get(channel);
            nchannel.stopListening();
            channelListenerMap.remove(channel);
        }
        return removed;
    }

    public void publish(String channel, PubSubMsg msg) {
        NotificationChannel nchannel = channelListenerMap.get(channel);
        if (nchannel != null) {
            ZimbraLog.pubsub.debug("RedisPubSub.publish(%s) - '%s'", channel, msg);
            nchannel.publish(msg);
        }
        else {
            ZimbraLog.pubsub.debug("RedisPubSub.publish() - '%s' DOES NOT exist!", channel);
        }
    }

    public void onMessage(String channel, PubSubMsg msg) {
        ZimbraLog.pubsub.debug("RedisPubSub.onMessage(%s, %s) - %s", channel, msg, msg.getClass());
        super.onMessage(channel, msg);
    }

    /**
     * Helper class acting as both the listener and publisher to a Redis notification pubsub channel.
     * This channel handles notifications for multiple accounts to avoid having a separate connection for each
     * mailbox; incoming notifications are routed to the appropriate Subscriber instance.
     */
    public static class NotificationChannel implements MessageListener<PubSubMsg> {
        private PubSub pubSub;
        private int listenerId;
        private RTopic<PubSubMsg> channel;
        private boolean active = false;

        public NotificationChannel(PubSub pubsub, RedissonClient client, String channelName) {
            this.pubSub = pubsub;
            this.channel = client.getTopic(channelName);
            beginListening();
        }

        public void beginListening() {
            if (active) {
                return;
            }
            listenerId = channel.addListener(this);
            ZimbraLog.pubsub.debug("RedisPubSub.NotificationChannel(%s).beginListening()", channel.getChannelNames().get(0));
            active = true;
        }

        public void stopListening() {
            ZimbraLog.pubsub.debug("RedisPubSub.NotificationChannel(%s).stopListening()", channel.getChannelNames().get(0));
            if (!active) {
                return;
            }
            channel.removeListener(this);
            active = false;
        }

        public void onMessage(String channel, PubSubMsg msg) {
            ZimbraLog.mailbox.debug("RedisPubSub.NotificationChannel.onMessage(%s) = %s", channel, msg);
            pubSub.onMessage(channel, msg);
        }

        public long publish(PubSubMsg msg) {
            ZimbraLog.mailbox.debug("NotificationChannel.publish - %s", msg);
            return channel.publish(msg);
        }

    }

    public static class Factory extends PubSub.Factory {

        private static PubSub pubsub;

        protected PubSub initPubSub() {
            if (RedisPubSub.Factory.pubsub == null) {
                RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
                RedisPubSub.Factory.pubsub = new RedisPubSub(client);
            }
            return RedisPubSub.Factory.pubsub;
        }
    }

}
