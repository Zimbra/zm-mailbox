package com.zimbra.cs.mailbox.redis;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.mapreduce.RMapReduce;

public class RedissonRetryMap<K, V> extends RedissonRetryExpirable<RMap<K, V>> implements RMap<K, V> {

    public RedissonRetryMap(RedissonInitializer<RMap<K, V>> mapInitializer, RedissonRetryClient client) {
        super(mapInitializer, client);
    }

    @Override
    public int size() {
        return runCommand(() -> redissonObject.size());
    }

    @Override
    public boolean isEmpty() {
        return runCommand(() -> redissonObject.isEmpty());
    }

    @Override
    public boolean containsKey(Object key) {
        return runCommand(() -> redissonObject.containsKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return runCommand(() -> redissonObject.containsValue(value));
    }

    @Override
    public void clear() {
        runCommand(() -> { redissonObject.clear(); return null; });
    }

    @Override
    public V get(Object key) {
        return runCommand(() -> redissonObject.get(key));
    }

    @Override
    public V put(K key, V value) {
        return runCommand(() -> redissonObject.put(key, value));
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return runCommand(() -> redissonObject.putIfAbsent(key, value));
    }

    @Override
    public V addAndGet(K key, Number delta) {
        return runCommand(() -> redissonObject.addAndGet(key, delta));
    }

    @Override
    public V remove(Object key) {
        return runCommand(() -> redissonObject.remove(key));
    }

    @Override
    public long fastRemove(K... keys) {
        return runCommand(() -> redissonObject.fastRemove(keys));
    }

    @Override
    public boolean fastPut(K key, V value) {
        return runCommand(() -> redissonObject.fastPut(key, value));
    }

    @Override
    public Collection<V> readAllValues() {
        return runCommand(() -> redissonObject.readAllValues());
    }

    @Override
    public RFuture<Void> loadAllAsync(boolean replaceExistingValues, int parallelism) {
        return runCommand(() -> redissonObject.loadAllAsync(replaceExistingValues, parallelism));
    }

    @Override
    public RFuture<Void> loadAllAsync(Set<? extends K> keys, boolean replaceExistingValues, int parallelism) {
        return runCommand(() -> redissonObject.loadAllAsync(keys, replaceExistingValues, parallelism));
    }

    @Override
    public RFuture<Integer> valueSizeAsync(K key) {
        return runCommand(() -> redissonObject.valueSizeAsync(key));
    }

    @Override
    public RFuture<Map<K, V>> getAllAsync(Set<K> keys) {
        return runCommand(() -> redissonObject.getAllAsync(keys));
    }

    @Override
    public RFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) {
        return runCommand(() -> redissonObject.putAllAsync(map));
    }

    @Override
    public RFuture<V> addAndGetAsync(K key, Number value) {
        return runCommand(() -> redissonObject.addAndGetAsync(key, value));
    }

    @Override
    public RFuture<Boolean> containsValueAsync(Object value) {
        return runCommand(() -> redissonObject.containsValueAsync(value));
    }

    @Override
    public RFuture<Boolean> containsKeyAsync(Object key) {
        return runCommand(() -> redissonObject.containsKeyAsync(key));
    }

    @Override
    public RFuture<Integer> sizeAsync() {
        return runCommand(() -> redissonObject.sizeAsync());
    }

    @Override
    public RFuture<Long> fastRemoveAsync(K... keys) {
        return runCommand(() -> redissonObject.fastRemoveAsync(keys));
    }

    @Override
    public RFuture<Boolean> fastPutAsync(K key, V value) {
        return runCommand(() -> redissonObject.fastPutAsync(key, value));
    }

    @Override
    public RFuture<Boolean> fastPutIfAbsentAsync(K key, V value) {
        return runCommand(() -> redissonObject.fastPutIfAbsentAsync(key, value));
    }

    @Override
    public RFuture<Set<K>> readAllKeySetAsync() {
        return runCommand(() -> redissonObject.readAllKeySetAsync());
    }

    @Override
    public RFuture<Collection<V>> readAllValuesAsync() {
        return runCommand(() -> redissonObject.readAllValuesAsync());
    }

    @Override
    public RFuture<Set<java.util.Map.Entry<K, V>>> readAllEntrySetAsync() {
        return runCommand(() -> redissonObject.readAllEntrySetAsync());
    }

    @Override
    public RFuture<Map<K, V>> readAllMapAsync() {
        return runCommand(() -> redissonObject.readAllMapAsync());
    }

    @Override
    public RFuture<V> getAsync(K key) {
        return runCommand(() -> redissonObject.getAsync(key));
    }

    @Override
    public RFuture<V> putAsync(K key, V value) {
        return runCommand(() -> redissonObject.putAsync(key, value));
    }

    @Override
    public RFuture<V> removeAsync(K key) {
        return runCommand(() -> redissonObject.removeAsync(key));
    }

    @Override
    public RFuture<V> replaceAsync(K key, V value) {
        return runCommand(() -> redissonObject.replaceAsync(key, value));
    }

    @Override
    public RFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        return runCommand(() -> redissonObject.replaceAsync(key, oldValue, newValue));
    }

    @Override
    public RFuture<Boolean> removeAsync(Object key, Object value) {
        return runCommand(() -> redissonObject.removeAsync(key, value));
    }

    @Override
    public RFuture<V> putIfAbsentAsync(K key, V value) {
        return runCommand(() -> redissonObject.putIfAbsentAsync(key, value));
    }

    @Override
    public void loadAll(boolean replaceExistingValues, int parallelism) {
        runCommand(() -> { redissonObject.loadAll(replaceExistingValues, parallelism); return null; });

    }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, int parallelism) {
        runCommand(() -> { redissonObject.loadAll(keys, replaceExistingValues, parallelism); return null; });

    }

    @Override
    public <KOut, VOut> RMapReduce<K, V, KOut, VOut> mapReduce() {
        return runCommand(() -> redissonObject.mapReduce());
    }

    @Override
    public RReadWriteLock getReadWriteLock(K key) {
        return runCommand(() -> redissonObject.getReadWriteLock(key));
    }

    @Override
    public RLock getLock(K key) {
        return runCommand(() -> redissonObject.getLock(key));
    }

    @Override
    public int valueSize(K key) {
        return runCommand(() -> redissonObject.valueSize(key));
    }

    @Override
    public V replace(K key, V value) {
        return runCommand(() -> redissonObject.replace(key, value));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return runCommand(() -> redissonObject.replace(key, oldValue, newValue));
    }

    @Override
    public boolean remove(Object key, Object value) {
        return runCommand(() -> redissonObject.remove(key, value));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        runCommand(() -> { redissonObject.putAll(map); return null; });
    }

    @Override
    public Map<K, V> getAll(Set<K> keys) {
        return runCommand(() -> redissonObject.getAll(keys));
    }

    @Override
    public boolean fastPutIfAbsent(K key, V value) {
        return runCommand(() -> redissonObject.fastPutIfAbsent(key, value));
    }

    @Override
    public Set<K> readAllKeySet() {
        return runCommand(() -> redissonObject.readAllKeySet());
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> readAllEntrySet() {
        return runCommand(() -> redissonObject.readAllEntrySet());
    }

    @Override
    public Map<K, V> readAllMap() {
        return runCommand(() -> redissonObject.readAllMap());
    }

    @Override
    public Set<K> keySet() {
        return runCommand(() -> redissonObject.keySet());
    }

    @Override
    public Set<K> keySet(String pattern) {
        return runCommand(() -> redissonObject.keySet(pattern));
    }

    @Override
    public Collection<V> values() {
        return runCommand(() -> redissonObject.values());
    }

    @Override
    public Collection<V> values(String keyPattern) {
        return runCommand(() -> redissonObject.values(keyPattern));
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return runCommand(() -> redissonObject.entrySet());
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet(String keyPattern) {
        return runCommand(() -> redissonObject.entrySet(keyPattern));
    }

    @Override
    public RFuture<Boolean> fastReplaceAsync(K key, V value) {
        return runCommand(() -> redissonObject.fastReplaceAsync(key, value));
    }

    @Override
    public boolean fastReplace(K key, V value) {
        return runCommand(() -> redissonObject.fastReplace(key, value));
    }

    @Override
    public Set<K> keySet(int count) {
        return runCommand(() -> redissonObject.keySet(count));
    }

    @Override
    public Set<K> keySet(String pattern, int count) {
        return runCommand(() -> redissonObject.keySet(pattern, count));
    }

    @Override
    public Collection<V> values(String keyPattern, int count) {
        return runCommand(() -> redissonObject.values(keyPattern, count));
    }

    @Override
    public Collection<V> values(int count) {
        return runCommand(() -> redissonObject.values(count));
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet(String keyPattern, int count) {
        return runCommand(() -> redissonObject.entrySet(keyPattern, count));
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet(int count) {
        return runCommand(() -> redissonObject.entrySet(count));
    }

    @Override
    public RFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, int batchSize) {
        return runCommand(() -> redissonObject.putAllAsync(map, batchSize));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, int batchSize) {
        runCommand(() -> { redissonObject.putAll(map, batchSize); return null; });
    }
}
