package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.ZimbraLog;

public abstract class TransactionAwareMap<K, V> extends TransactionAware<Map<K, V>, TransactionAwareMap.MapChange> implements Map<K, V> {

    public TransactionAwareMap(String name, TransactionCacheTracker cacheTracker, Getter<Map<K, V>, ?> getter) {
        super(name, cacheTracker, getter);
    }

    @Override
    public int size() {
        return data().size();
    }

    @Override
    public boolean isEmpty() {
        return data().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return data().get(key);
    }

    @Override
    public V put(K key, V value) {
        V val = data().put(key, value);
        addChange(new MapPutOp(key, value));
        return val;
    }

    @Override
    public V remove(Object key) {
        V val = data().remove(key);
        addChange(new MapRemoveOp(key));
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        data().putAll(m);
        addChange(new MapPutAllOp(m));
    }

    @Override
    public void clear() {
        data().clear();
        addChange(new MapClearOp());
    }

    @Override
    public Set<K> keySet() {
        return data().keySet();
    }

    @Override
    public Collection<V> values() {
        return data().values();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return data().entrySet();
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

    @FunctionalInterface
    protected static interface MapLoader<K, V> {
        public Map<K, V> loadMap();
    }

    @FunctionalInterface
    protected static interface MapValueLoader<V> {
        public V loadValue(Object key);
    }

    protected static class GreedyMapGetter<K, V> extends GreedyGetter<Map<K, V>> {

        private MapLoader<K, V> loader;

        public GreedyMapGetter(String objectName, MapLoader<K, V> loader) {
            super(objectName);
            this.loader = loader;
        }

        @Override
        protected Map<K, V> loadObject() {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("loading map %s", objectName);
            }
            return loader.loadMap();
        }
    }

    protected static class LazyMapGetter<K, V> extends Getter<Map<K, V>, LazyMapCachedObject<K, V>> {

        protected MapValueLoader<V> loader;

        public LazyMapGetter(String objectName, MapValueLoader<V> loader) {
            super(objectName);
            this.loader = loader;
        }

        @Override
        protected LazyMapCachedObject<K, V> loadCacheValue() {
            return new LazyMapCachedObject<>(getObjectName(), loader);
        }

    }

    protected static class LazyMapCachedObject<K, V> extends CachedObject<Map<K, V>> implements Map<K, V> {

        private MapValueLoader<V> loader;
        private Map<Object, V> cachedMapEntries;

        protected LazyMapCachedObject(String objectName, MapValueLoader<V> loader) {
            super(objectName);
            this.loader = loader;
            this.cachedMapEntries = new HashMap<>();
        }


        @Override
        public Map<K, V> getObject() {
            return this;
        }

        @Override
        public V get(Object key) {
            V value = cachedMapEntries.get(key);
            if (value == null) {
                if (ZimbraLog.cache.isTraceEnabled()) {
                    ZimbraLog.cache.trace("lazily fetching key %s for %s", key, objectName);
                }
                value = loader.loadValue(key);
                if (value != null) {
                    cachedMapEntries.put(key, value);
                }
            }
            return value;
        }

        @Override
        public V put(K key, V value) {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("setting %s=%s for %s", key, value, objectName);
            }
            return cachedMapEntries.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return cachedMapEntries.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            cachedMapEntries.putAll(m);
        }

        @Override
        public void clear() {
            cachedMapEntries.clear();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("size() cannot be used with a lazily-loaded TransactionAwareMap");
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("isEmpty() cannot be used with a lazily-loaded TransactionAwareMap");
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException("containsKey() cannot be used with a lazily-loaded TransactionAwareMap");
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("containsValue() cannot be used with a lazily-loaded TransactionAwareMap");
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException("keySet() cannot be used with a lazily-loaded TransactionAwareMap");
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException("values() cannot be used with a lazily-loaded TransactionAwareMap");
        }

        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException("entrySet() cannot be used with a lazily-loaded TransactionAwareMap");
        }

    }
}