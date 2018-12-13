package com.zimbra.cs.pubsub;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.pubsub.message.PubSubMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class PubSub {

    private ConcurrentHashMap<String, CopyOnWriteArraySet<ListenerCallback>> listenerMap;

    public PubSub() {
        ZimbraLog.pubsub.debug("PubSub()");
        startup();
    }

    public void startup() {
        if (listenerMap == null) {
            ZimbraLog.pubsub.debug("PubSub.startup()");
            listenerMap = new ConcurrentHashMap<>();
        }
    }

    /**
     * Adds a new ListenerCallback to the specified channel
     * @param channel name of the channel
     * @param callback instance of a ListenerCallback to be called on receipt of a message on the channel
     */
    public void addListener(String channel, ListenerCallback callback) {
        ZimbraLog.pubsub.debug("PubSub.addListener(%s) - '%s'", channel, callback);
        CopyOnWriteArraySet<ListenerCallback> listeners = listenerMap.get(channel);
        if (listeners == null) {
            listeners = new CopyOnWriteArraySet<>();
        }
        listeners.add(callback);
        CopyOnWriteArraySet<ListenerCallback> previousValue = listenerMap.putIfAbsent(channel, listeners);
        if (previousValue != null) {
            previousValue.add(callback);
        }
    }

    /**
     * Removes the specified ListenerCallback from the channel
     * @param channel name of the channel
     * @param callback ListenerCallback to be removed from the channel
     * @return true if callback list is empty after removal false if not
     */
    public boolean removeListener(String channel, ListenerCallback callback) {
        ZimbraLog.pubsub.debug("PubSub.removeListener(%s) - '%s'", channel, callback);
        CopyOnWriteArraySet<ListenerCallback> listeners = listenerMap.get(channel);
        if (listeners == null) {
            return true;
        }
        if (listeners.contains(callback)) {
            listeners.remove(callback);
        }
        return listeners.isEmpty();
    }

    /**
     * Processes and delivers messages to all registered ListenerCallback(s)
     * @param channel name of the channel
     * @param msg PubSubMsg object containing the message received from the channel
     */
    public void onMessage(CharSequence channel, PubSubMsg msg) {
        ZimbraLog.pubsub.debug("PubSub.onMessage(%s, %s) - %s", channel, msg, msg.getClass());
        CopyOnWriteArraySet<ListenerCallback> listeners = listenerMap.get(channel.toString());
        if (listeners == null) {
            return;
        }
        for (ListenerCallback listener: listeners) {
            listener.onMessage(channel, msg);
        }
    }

    /**
     * Publishes a message to listeners of a channel
     * @param channel name of the channel
     * @param msg PubSubMsg to send to all registered listeners
     */
    public void publish(String channel, PubSubMsg msg) {
        ZimbraLog.pubsub.debug("PubSub.publish(%s, %s)", channel, msg);
        this.onMessage(channel, msg);
    }

    /**
     * Retrieve the list of ListenerCallbacks for the channel
     * @param channel name of the channel
     * @return list of subscribed listeners for the channel
     */
    public List<ListenerCallback> getListeners(String channel) {
        CopyOnWriteArraySet<ListenerCallback> listeners = listenerMap.get(channel);
        if (listeners == null) {
            listeners = new CopyOnWriteArraySet<>();
        }
        return new ArrayList<ListenerCallback>(listeners);
    }

    public static abstract class Factory {
        protected abstract PubSub initPubSub();
    }
}
