package com.zimbra.common.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.LoadingCache;

public class LoadingCacheUtil {

    /**
     * Get all values for given key set
     * @param loadingCache
     * @param keySet set of keys to get values for
     */
    public static <K, V> Map<K, List<V>>
        getAll(LoadingCache<K, List<V>> loadingCache, Set<K> keySet) {
        try {
            return loadingCache.getAll(keySet);
        } catch (ExecutionException e) {
            ZimbraLog.cache.warn("Unable to load values: %s", e.getMessage());
        }
        return loadingCache.asMap();
    }

    /**
     * Get values for given key
     * @param loadingCache
     * @param key key to get value for
     */
    public static <K, V> List<V> get(LoadingCache<K, List<V>> loadingCache, K key) {
        try {
            return loadingCache.get(key);
        } catch (ExecutionException e) {
            ZimbraLog.cache.warn("Unable to load value for %s: %s", key, e.getMessage());
        }
        return null;
    }
}
