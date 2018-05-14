package com.zimbra.cs.pubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;


public class MemoryPubSub extends PubSub {

    public static class Factory extends PubSub.Factory {

        private static MemoryPubSub pubsub;

        protected PubSub initPubSub() {
            if (Factory.pubsub == null) {
                Factory.pubsub = new MemoryPubSub();
            }
            return Factory.pubsub;
        }
    }
}
