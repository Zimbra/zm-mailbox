/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util.memcached;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

/**
 * A key/value lookup map backed by memcached.  This class does not implement java.util.Map
 * because memcached doesn't allow iteration.
 * 
 * Example:
 * 
 *     class MyKey implements MemcachedKey {
 *         public String getKeyPrefix() { return "myApp:"; }
 *         public String getKeyValue() { ... }
 *     }
 * 
 *     class MyValue { ... }
 * 
 *     class MySerializer implements Serializer<MyValue> {
 *         public String serialize(MyValue value) { return value.toString(); }
 *         public MyValue deserialize(String str) { return new MyValue(str); }
 *     }
 * 
 *     ZimbraMemcachedClient mcdClient = new ZimbraMemcachedClient(...);
 *     MySerializer serializer = new MySerializer();
 *     MemcachedMap<MyKey, MyValue> mcdMap = new MemcachedMap(mcdClient, serializer);
 * 
 *     MyKey foo = new MyKey("foo");
 *     MyValue bar = new MyValue("bar");
 *     mcdMap.put(foo, bar);
 *     MyValue bar2 = mcdMap.get(foo);
 *     mcdMap.remove(foo);
 *     boolean hasFoo = mcdMap.containsKey(foo);
 * 
 *     MyKey k1 = new MyKey("k1");
 *     MyKey k2 = new MyKey("k2");
 *     List<MyKey> keys = new ArrayList<MyKey>();
 *     keys.add(k1);
 *     keys.add(k2);
 *     Map<MyKey, MyValue> values = mcdMap.getMulti(keys);
 *
 * @param <K> key implements the MemcachedKey interface
 * @param <V> value must have a Serializer<V> implementation
 */
public class MemcachedMap<K extends MemcachedKey, V> {

    private ZimbraMemcachedClient mClient;
    private MemcachedSerializer<V> mSerializer;

    /**
     * Creates a map using a memcached client and serializer.
     * @param client
     * @param serializer
     */
    public MemcachedMap(ZimbraMemcachedClient client, MemcachedSerializer<V> serializer) {
        mClient = client;
        mSerializer = serializer;
    }

    /**
     * Returns true if key has a value in memcached.
     * @param key
     * @return
     * @throws ServiceException
     */
    public boolean containsKey(K key) throws ServiceException {
        return get(key) != null;
    }

    /**
     * Returns the value for a key.  Null is returned if key is not found in memcached.
     * @param key
     * @return
     * @throws ServiceException
     */
    public V get(K key) throws ServiceException {
        String prefix = key.getKeyPrefix();
        String kval = prefix != null ? prefix + key.getKeyValue() : key.getKeyValue();
        String valstr = (String) mClient.get(kval);
        V value = null;
        if (valstr != null)
            value = mSerializer.deserialize(valstr);
        return value;
    }

    /**
     * Returns values for given keys.  The returned java.util.Map is never null and has an
     * entry for every key.  Entry value will be null if key was not found in memcached.
     * This operation is batched and parallelized in the memcached client layer.  Use this
     * method rather than calling get() in a loop.
     * @param keys
     * @return
     * @throws ServiceException
     */
    public Map<K, V> getMulti(Collection<K> keys) throws ServiceException {
        Map<String, K> keyMap = new HashMap<String, K>(keys.size());
        for (K key : keys) {
            String prefix = key.getKeyPrefix();
            String kval = prefix != null ? prefix + key.getKeyValue() : key.getKeyValue();
            keyMap.put(kval, key);
        }
        Map<String, Object> valueMap = mClient.getMulti(keyMap.keySet());
        Map<K, V> result = new HashMap<K, V>(keys.size());
        // Put the values in a map keyed by the K objects.
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            K key = keyMap.get(entry.getKey());
            if (key != null) {
                String valstr = (String) entry.getValue();
                V value = null;
                if (valstr != null)
                    value = mSerializer.deserialize(valstr);
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Sets the key/value pair in memcached.
     * @param key
     * @param value
     * @throws ServiceException
     */
    public void put(K key, V value) throws ServiceException {
        String prefix = key.getKeyPrefix();
        String kval = prefix != null ? prefix + key.getKeyValue() : key.getKeyValue();
        String valstr = mSerializer.serialize(value);
        mClient.put(kval, valstr);
    }

    /**
     * Sets multiple key/value pairs in memcached.  This operation is done serially in a loop.
     * @param map
     * @throws ServiceException
     */
    public void putMulti(Map<K, V> map) throws ServiceException {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Remove the key from memcached.
     * @param key
     * @throws ServiceException
     */
    public void remove(K key) throws ServiceException {
        String prefix = key.getKeyPrefix();
        String kval = prefix != null ? prefix + key.getKeyValue() : key.getKeyValue();
        mClient.remove(kval);
    }

    /**
     * Remove multiple keys from memcached.  This operation is done serially in a loop.
     * @param keys
     * @throws ServiceException
     */
    public void removeMulti(Collection<K> keys) throws ServiceException {
        for (K key : keys) {
            String prefix = key.getKeyPrefix();
            String kval = prefix != null ? prefix + key.getKeyValue() : key.getKeyValue();
            mClient.remove(kval);
        }
    }
}
