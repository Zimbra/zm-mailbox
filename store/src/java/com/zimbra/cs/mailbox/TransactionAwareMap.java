package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

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
        addChange(new PutOp(key, value));
        return val;
    }

    @Override
    public V remove(Object key) {
        V val = getLocalCache().remove(key);
        addChange(new RemoveOp(key));
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        getLocalCache().putAll(m);
        addChange(new PutAllOp(m));
    }

    @Override
    public void clear() {
        getLocalCache().clear();
        addChange(new ClearOp());
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

    public static enum MapChangeType {
        CLEAR, PUT, PUT_ALL, REMOVE;
    }

    public static abstract class MapChange extends TransactionAware.Change {

        protected MapChangeType changeType;

        private MapChange(MapChangeType changeType) {
            this.changeType = changeType;
        }

        public MapChangeType getChangeType() {
            return changeType;
        }

        protected ToStringHelper toStringHelper() {
            return MoreObjects.toStringHelper(this);
        }
    }

    public class RemoveOp extends MapChange {

        private Object key;

        public RemoveOp(Object key) {
            super(MapChangeType.REMOVE);
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

    public class PutAllOp extends MapChange {

        private Map<? extends K, ? extends V> changes;

        public PutAllOp(Map<? extends K, ? extends V> m){
            super(MapChangeType.PUT_ALL);
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

    public class PutOp extends MapChange  {

        private K key;
        private V value;

        public PutOp(K key, V value) {
            super(MapChangeType.PUT);
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

    public class ClearOp extends MapChange  {

        public ClearOp() {
            super(MapChangeType.CLEAR);
        }

        @Override
        public String toString() {
            return toStringHelper().toString();
        }
    }
}