package com.zimbra.cs.mailbox;

import java.util.Map;

public class LocalTransactionTrackingMap<K, V> extends TransactionAwareMap<K, V> {

    private Map<K, V> map;

    public LocalTransactionTrackingMap(Map<K, V> map, TransactionCacheTracker tracker, String mapName) {
        super(tracker, mapName);
        this.map = map;
    }

    @Override
    protected Map<K, V> initLocalCache() {
        return map;
    }

}
