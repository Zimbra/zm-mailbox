package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class TransactionAwareMap<K, V> extends TransactionAware<Map<K, V>, TransactionAwareMap.MapChange> implements Map<K, V> {

    public TransactionAwareMap(TransactionCacheTracker tracker, String mapName) {
        super(tracker, mapName);
    }

    @Override
    public int size() {
        return getLocalCache().size();
    }

    @Override
    public boolean isEmpty() {
        return getLocalCache().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return getLocalCache().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getLocalCache().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return getLocalCache().get(key);
    }

    @Override
    public V put(K key, V value) {
        V val = getLocalCache().put(key, value);
        addChange(new MapPutOp(key, value));
        return val;
    }

    @Override
    public V remove(Object key) {
        V val = getLocalCache().remove(key);
        addChange(new MapRemoveOp(key));
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        getLocalCache().putAll(m);
        addChange(new MapPutAllOp(m));
    }

    @Override
    public void clear() {
        getLocalCache().clear();
        addChange(new MapClearOp());
        clearLocalCache();
    }

    @Override
    public Set<K> keySet() {
        return getLocalCache().keySet();
    }

    @Override
    public Collection<V> values() {
        return getLocalCache().values();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return getLocalCache().entrySet();
    }

    public static abstract class MapChange extends TransactionAware.Change {

        private MapChange(ChangeType changeType) {
            super(changeType);
        }
    }

    public class MapRemoveOp extends MapChange {

        private Object key;

        public MapRemoveOp(Object key) {
            super(ChangeType.REMOVE);
            this.key = key;
        }

        public Object getKey() {
            return key;
        }

        @Override
        public String toString() {
            return toStringHelper().add("key", key).toString();
        }
    }

    public class MapPutAllOp extends MapChange {

        private Map<? extends K, ? extends V> changes;

        public MapPutAllOp(Map<? extends K, ? extends V> m){
            super(ChangeType.MAP_PUT_ALL);
            this.changes = m;
        }

        public Map<? extends K, ? extends V> getChanges() {
            return changes;
        }

        @Override
        public String toString() {
            return toStringHelper().add("entries", changes).toString();
        }

    }

    public class MapPutOp extends MapChange  {

        private K key;
        private V value;

        public MapPutOp(K key, V value) {
            super(ChangeType.MAP_PUT);
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return toStringHelper().add(key.toString(), value).toString();
        }
    }

    public class MapClearOp extends MapChange  {

        public MapClearOp() {
            super(ChangeType.CLEAR);
        }

        @Override
        public String toString() {
            return toStringHelper().toString();
        }
    }
}