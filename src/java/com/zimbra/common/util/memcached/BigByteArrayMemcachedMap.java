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

import com.zimbra.common.service.ServiceException;

/**
 * A key/value lookup map backed by memcached, with support for values larger than 1MB.
 * This class does not implement java.util.Map because memcached doesn't allow iteration.
 * To use this map, the value object must support serialization to byte array. (rather than String)
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
 *     class MySerializer implements ByteArraySerializer<MyValue> {
 *         public byte[] serialize(MyValue value) { serialize to byte array }
 *         public MyValue deserialize(byte[] data) { deserialize from byte array }
 *     }
 * 
 *     ZimbraMemcachedClient mcdClient = new ZimbraMemcachedClient(...);
 *     MySerializer serializer = new MySerializer();
 *     BigByteArrayMemcachedMap<MyKey, MyValue> mcdMap = new BigByteArrayMemcachedMap(mcdClient, serializer);
 * 
 *     MyKey foo = new MyKey("foo");
 *     MyValue bar = new MyValue("bar");
 *     mcdMap.put(foo, bar);
 *     MyValue bar2 = mcdMap.get(foo);
 *     mcdMap.remove(foo);
 *
 * @param <K> key implements the MemcachedKey interface
 * @param <V> value must have a ByteArraySerializer<V> implementation
 */
public class BigByteArrayMemcachedMap<K extends MemcachedKey, V> {

    private ZimbraMemcachedClient mClient;
    private ByteArraySerializer<V> mSerializer;
    private boolean mAckWrites;

    /**
     * Creates a map using a memcached client and serializer.
     * @param client
     * @param serializer
     * @param ackWrites if false, put and remove operations return immediately, without waiting for an ack
     *                  if true, put and remove operations block until ack or timeout
     */
    public BigByteArrayMemcachedMap(ZimbraMemcachedClient client, ByteArraySerializer<V> serializer, boolean ackWrites) {
        mClient = client;
        mSerializer = serializer;
        mAckWrites = ackWrites;
    }

    public BigByteArrayMemcachedMap(ZimbraMemcachedClient client, ByteArraySerializer<V> serializer) {
        this(client, serializer, true);
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
        byte[] data = mClient.getBigByteArray(kval);
        V value = null;
        if (data != null)
            value = mSerializer.deserialize(data);
        return value;
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
        byte[] data = mSerializer.serialize(value);
        mClient.putBigByteArray(kval, data, mAckWrites);
    }

    /**
     * Remove the key from memcached.
     * @param key
     * @throws ServiceException
     */
    public void remove(K key) throws ServiceException {
        String prefix = key.getKeyPrefix();
        String kval = prefix != null ? prefix + key.getKeyValue() : key.getKeyValue();
        mClient.remove(kval, mAckWrites);
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
            mClient.remove(kval, mAckWrites);
        }
    }
}
