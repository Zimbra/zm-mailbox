package com.zimbra.cs.pubsub;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.pubsub.message.PubSubMsg;

public class ListenerCallback {
    public void onMessage(String channel, PubSubMsg payload) {
        ZimbraLog.pubsub.info("PubSub:ListenerCallback.onMessage: Message received on channel '%s' - '%s'", channel, payload);
    }
}

