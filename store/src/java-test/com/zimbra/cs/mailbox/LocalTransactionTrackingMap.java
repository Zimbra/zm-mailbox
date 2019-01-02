package com.zimbra.cs.mailbox;

import java.util.Map;

public class LocalTransactionTrackingMap<K, V> extends TransactionAwareMap<K, V> {

    public LocalTransactionTrackingMap(Map<K, V> map, TransactionCacheTracker cacheTracker, String mapName, ReadPolicy readPolicy, WritePolicy writePolicy) {
        super(mapName, cacheTracker, new LocalGetter<>(mapName, map), readPolicy, writePolicy);
    }

    private static class LocalGetter<K, V> extends GreedyCachingGetter<Map<K, V>> {

        private Map<K, V> map;

        public LocalGetter(String objectName, Map<K, V> map) {
            super(objectName, CachePolicy.SINGLE_VALUE);
            this.map = map;
        }

        @Override
        protected Map<K, V> loadObject() {
            return map;
        }
    }
}
