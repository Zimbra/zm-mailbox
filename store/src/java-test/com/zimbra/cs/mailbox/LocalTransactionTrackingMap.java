package com.zimbra.cs.mailbox;

import java.util.Map;

public class LocalTransactionTrackingMap<K, V> extends TransactionAwareMap<K, V> {

    public LocalTransactionTrackingMap(Map<K, V> map, TransactionCacheTracker cacheTracker, String mapName) {
        super(mapName, cacheTracker, new LocalGetter<>(mapName, map));
    }

    private static class LocalGetter<K, V> extends GreedyGetter<Map<K, V>> {

        private Map<K, V> map;

        public LocalGetter(String objectName, Map<K, V> map) {
            super(objectName);
            this.map = map;
        }

        @Override
        protected Map<K, V> loadObject() {
            return map;
        }
    }
}
