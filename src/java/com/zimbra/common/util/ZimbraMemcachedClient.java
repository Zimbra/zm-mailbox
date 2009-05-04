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

package com.zimbra.common.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.zimbra.common.service.ServiceException;

import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;

public class ZimbraMemcachedClient {

    public static class KeyPrefix {
        private String mValue;
        public KeyPrefix(String val) { mValue = val; }
        public String toString() { return mValue; }
        public int length() { return mValue.length(); }
    }

    private MemcachedClient mMCDClient;
    private final Object mMCDClientGuard = new Object();
    private int mDefaultExpiry;  // in seconds
    private long mDefaultTimeout;  // in millis

    /**
     * Constructs a memcached client with a list of servers, default expiry (in seconds) and timeout
     * (in milliseconds).  Ascii protocol is used.  NATIVE_HASH hashing algorithm is used.
     * @param servers
     * @param defaultExpiry
     * @param defaultTimeout
     * @throws ServiceException
     */
    public ZimbraMemcachedClient(List<InetSocketAddress> servers, int defaultExpiry, long defaultTimeout)
    throws ServiceException {
        this(servers, false, null, defaultExpiry, defaultTimeout);
    }

    /**
     * Constructs a memcached client.
     * @param servers memcached server list
     * @param useBinaryProtocol if true, use the binary protocol; if false, use the ascii protocol
     * @param hashAlgorithm net.spy.memcached.HashAlgorithm enum
     * @param defaultExpiry in seconds
     * @param defaultTimeout in milliseconds
     * @throws ServiceException
     */
    public ZimbraMemcachedClient(List<InetSocketAddress> servers, boolean useBinaryProtocol, String hashAlgorithm,
                                 int defaultExpiry, long defaultTimeout)
    throws ServiceException {
        mDefaultExpiry = defaultExpiry;
        mDefaultTimeout = defaultTimeout;
        reconnect(servers, useBinaryProtocol, hashAlgorithm);
    }

    /**
     * Reset/reconnect the memcached client with new server list, protocol and hashing algorithm.
     * @param servers memcached server list
     * @param useBinaryProtocol if true, use the binary protocol; if false, use the ascii protocol
     * @param hashAlgorithm net.spy.memcached.HashAlgorithm enum
     * @throws ServiceException
     */
    public void reconnect(List<InetSocketAddress> servers, boolean useBinaryProtocol, String hashAlgorithm)
    throws ServiceException {
        HashAlgorithm hashAlgo = HashAlgorithm.NATIVE_HASH;
        if (hashAlgorithm != null && hashAlgorithm.length() > 0) {
            HashAlgorithm ha = HashAlgorithm.valueOf(hashAlgorithm);
            if (ha != null)
                hashAlgo = ha;
        }
        int qLen = DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN;
        int bufSize = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE;

        MemcachedClient client;
        ConnectionFactory cf;
        if (useBinaryProtocol)
            cf = new BinaryConnectionFactory(qLen, bufSize, hashAlgo);
        else
            cf = new DefaultConnectionFactory(qLen, bufSize, hashAlgo);
        try {
            client = new MemcachedClient(cf, servers);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to initialize memcached client", e);
        }
        MemcachedClient oldClient = setTheClient(client);
        // New client is ready for use by other threads at this point.
        if (oldClient != null)
            shutdown(oldClient, 30000);
    }

    private MemcachedClient getTheClient() {
        synchronized (mMCDClientGuard) {
            return mMCDClient;
        }
    }

    private MemcachedClient setTheClient(MemcachedClient client) {
        MemcachedClient oldClient;
        synchronized (mMCDClientGuard) {
            oldClient = mMCDClient;
            mMCDClient = client;
        }
        return oldClient;
    }

    /**
     * Shutdown the memcached client.  Drain the queue and do a normal shutdown if possible.
     * Both drain and normal shutdown are attempted with timeout.  When unsuccessful, shutdown
     * the client immediately.
     * @param timeout in millis
     */
    public void shutdown(long timeout) {
        MemcachedClient client = getTheClient();
        if (client != null) {
            shutdown(client, timeout);
        }
    }

    private void shutdown(MemcachedClient client, long timeout) {
        boolean drained = client.waitForQueues(timeout, TimeUnit.MILLISECONDS);
        if (!drained)
            ZimbraLog.misc.warn("Memcached client did not drain queue in " + timeout + "ms");
        boolean success = client.shutdown(timeout, TimeUnit.MILLISECONDS);
        if (!success) {
            ZimbraLog.misc.warn("Memcached client did not shutdown gracefully in " + timeout +
                                "ms; forcing immediate shutdowb");
            client.shutdown();
        }
    }

    private static final char KEY_DELIMITER = ':';

    private String addPrefix(KeyPrefix prefix, String keyval) {
        StringBuilder sb = new StringBuilder(prefix.length() + 1 + keyval.length());
        sb.append(prefix.toString()).append(KEY_DELIMITER).append(keyval);
        return sb.toString();
    }

    private String removePrefix(KeyPrefix prefix, String keyval) {
        String prefixStr = prefix.toString() + KEY_DELIMITER;
        if (keyval.startsWith(prefixStr))
            return keyval.substring(prefixStr.length());
        else
            return keyval;
    }

    private static final int DEFAULT_PORT = 11211;

    /**
     * Parse a server list string.  Values are delimited by a sequence of commas and/or whitespace chars.
     * Each server value is hostname:port or just hostname.  Default port is 11211.
     * @param serverList
     * @return
     */
    public static List<InetSocketAddress> parseServerList(String serverList) {
        List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();
        if (serverList != null) {
            String[] servers = serverList.split("[\\s,]+");
            if (servers != null) {
                for (String server : servers) {
                    if (server.length() == 0)
                        continue;
                    String[] parts = server.split(":");
                    if (parts != null) {
                        String host;
                        int port = DEFAULT_PORT;
                        if (parts.length == 1) {
                            host = parts[0];
                        } else if (parts.length == 2) {
                            host = parts[0];
                            try {
                                port = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                ZimbraLog.misc.warn("Invalid server " + server);
                                break;
                            }
                        } else {
                            ZimbraLog.misc.warn("Invalid server " + server);
                            break;
                        }
                        InetSocketAddress addr = new InetSocketAddress(host, port);
                        addrs.add(addr);
                    } else {
                        ZimbraLog.misc.warn("Invalid server " + server);
                        break;
                    }
                }
            }
        }
        return addrs;
    }

    // get

    /**
     * Retrieves the value corresponding to the given key prefix and key.
     * Default timeout is used.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @return null if no value is found for the key
     */
    public Object get(KeyPrefix prefix, String key) {
        return getWithRawKey(addPrefix(prefix, key), mDefaultTimeout);
    }

    /**
     * Retrieves the value corresponding to the given key.  The key is "raw", meaning
     * any prefix is already contained in the key.
     * Default timeout is used.
     * @param rawkey
     * @return null if no value is found for the key
     */
    public Object getWithRawKey(String rawkey) {
        return getWithRawKey(rawkey, mDefaultTimeout);
    }

    /**
     * Retrieves the value corresponding to the given key prefix and key.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @param timeout in millis
     * @return null if no value is found for the key
     */
    public Object get(KeyPrefix prefix, String key, long timeout) {
        return getWithRawKey(addPrefix(prefix, key), timeout);
    }

    /**
     * Retrieves the value corresponding to the given key.  The key is "raw", meaning
     * any prefix is already contained in the key.
     * @param rawkey
     * @param timeout in millis
     * @return null if no value is found for the key
     */
    public Object getWithRawKey(String rawkey, long timeout) {
        Object value = null;
        Future<Object> future = getTheClient().asyncGet(rawkey);
        try {
            value = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ZimbraLog.misc.warn("memcached asyncGet timed out after " + timeout + "ms", e);
            future.cancel(false);
        } catch (InterruptedException e) {
            ZimbraLog.misc.warn("InterruptedException during memcached asyncGet operation", e);
        } catch (ExecutionException e) {
            ZimbraLog.misc.warn("ExecutionException during memcached asyncGet operation", e);
        }
        return value;
    }

    // getMulti

    /**
     * Retrieves the values corresponding to the given keys, using a common prefix.
     * Default timeout is used.
     * @param prefix Rawkey = prefix + ":" + key
     * @param keys
     * @return map of (key, value); key is without prefix; missing keys have null value
     */
    public Map<String, Object> getMulti(KeyPrefix prefix, List<String> keys) {
        return getMulti(prefix, keys, mDefaultTimeout);
    }

    /**
     * Retrieves the values corresponding to the given keys.  The keys are "raw", meaning
     * any prefix is already contained in the keys.
     * Default timeout is used.
     * @param rawkeys
     * @return map of (key, value); missing keys have null value
     */
    public Map<String, Object> getMultiWithRawKeys(List<String> rawkeys) {
        return getMultiWithRawKeys(rawkeys, mDefaultTimeout);
    }

    /**
     * Retrieves the values corresponding to the given keys, using a common prefix.
     * @param prefix Rawkey = prefix + ":" + key
     * @param keys
     * @param timeout in millis
     * @return map of (key, value); key is without prefix; missing keys have null value
     */
    public Map<String, Object> getMulti(KeyPrefix prefix, List<String> keys, long timeout) {
        List<String> rawkeys = new ArrayList<String>(keys.size());
        for (String key : keys) {
            rawkeys.add(addPrefix(prefix, key));
        }
        Map<String, Object> rawValues = getMultiWithRawKeys(rawkeys, timeout);
        Map<String, Object> result = new HashMap<String, Object>(rawValues.size());
        for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
            String rawkey = entry.getKey();
            String key = removePrefix(prefix, rawkey);
            result.put(key, entry.getValue());
        }
        return result;
    }

    /**
     * Retrieves the values corresponding to the given keys.  The keys are "raw", meaning
     * any prefix is already contained in the keys.
     * @param rawkeys
     * @param timeout in millis
     * @return map of (key, value); missing keys have null value
     */
    public Map<String, Object> getMultiWithRawKeys(List<String> rawkeys, long timeout) {
        Map<String, Object> value = null;
        Future<Map<String, Object>> future = getTheClient().asyncGetBulk(rawkeys);
        try {
            value = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ZimbraLog.misc.warn("memcached asyncGetBulk timed out after " + timeout + "ms", e);
            future.cancel(false);
        } catch (InterruptedException e) {
            ZimbraLog.misc.warn("InterruptedException during memcached asyncGetBulk operation", e);
        } catch (ExecutionException e) {
            ZimbraLog.misc.warn("ExecutionException during memcached asyncGetBulk operation", e);
        }
        return value;
    }

    // contains

    /**
     * Tests if the cache has an entry for the given prefix and key.
     * Default timeout is used.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @return true if cache contains an entry for the key
     */
    public boolean contains(KeyPrefix prefix, String key) {
        return containsWithRawKey(addPrefix(prefix, key), mDefaultTimeout);
    }

    /**
     * Tests if the cache has an entry for the given key.
     * The key is "raw", meaning any prefix is already contained in the key.
     * Default timeout is used.
     * @param rawkey
     * @return true if cache contains an entry for the key
     */
    public boolean containsWithRawKey(String rawkey) {
        return containsWithRawKey(rawkey, mDefaultTimeout);
    }

    /**
     * Tests if the cache has an entry for the given prefix and key.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @param timeout in millis
     * @return true if cache contains an entry for the key
     */
    public boolean contains(KeyPrefix prefix, String key, long timeout) {
        return containsWithRawKey(addPrefix(prefix, key), timeout);
    }

    /**
     * Tests if the cache has an entry for the given key.
     * The key is "raw", meaning any prefix is already contained in the key.
     * @param rawkey
     * @param timeout in millis
     * @return true if cache contains an entry for the key
     */
    public boolean containsWithRawKey(String rawkey, long timeout) {
        Object value = getWithRawKey(rawkey, timeout);
        return value != null;
    }

    // put

    /**
     * Puts the prefix+key/value pair.  Default expiry and timeout are used.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @param value
     * @return
     */
    public boolean put(KeyPrefix prefix, String key, Object value) {
        return putWithRawKey(addPrefix(prefix, key), value, mDefaultExpiry, mDefaultTimeout);
    }

    /**
     * Puts the key/value pair.  The key is "raw", meaning any prefix is already contained in the key.
     * Default expiry and timeout are used.
     * @param rawkey
     * @param value
     * @return
     */
    public boolean putWithRawKey(String rawkey, Object value) {
        return putWithRawKey(rawkey, value, mDefaultExpiry, mDefaultTimeout);
    }

    /**
     * Puts the prefix+key/value pair.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @param value
     * @param expirySec expiry in seconds
     * @param timeout in millis
     * @return
     */
    public boolean put(KeyPrefix prefix, String key, Object value, int expirySec, long timeout) {
        return putWithRawKey(addPrefix(prefix, key), value, expirySec, timeout);
    }

    /**
     * Puts the key/value pair.  The key is "raw", meaning any prefix is already contained in the key.
     * @param rawkey
     * @param value
     * @param expirySec expiry in seconds
     * @param timeout in millis
     * @return
     */
    public boolean putWithRawKey(String rawkey, Object value, int expirySec, long timeout) {
        Future<Boolean> future = getTheClient().set(rawkey, expirySec, value);
        Boolean success = null;
        try {
            success = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ZimbraLog.misc.warn("memcached set timed out after " + timeout + "ms", e);
            future.cancel(false);
        } catch (InterruptedException e) {
            ZimbraLog.misc.warn("InterruptedException during memcached set operation", e);
        } catch (ExecutionException e) {
            ZimbraLog.misc.warn("ExecutionException during memcached set operation", e);
        }
        return success != null && success.booleanValue();
    }

    // remove

    /**
     * Removes the value for given prefix and key.
     * Default timeout is used.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @param timeout in millis
     * @return
     */
    public boolean remove(KeyPrefix prefix, String key) {
        return removeWithRawKey(addPrefix(prefix, key), mDefaultTimeout);
    }

    /**
     * Removes the value for given key.  The key is "raw", meaning any prefix is already contained in the key.
     * Default timeout is used.
     * @param rawkey
     * @return
     */
    public boolean removeWithRawKey(String rawkey) {
        return removeWithRawKey(rawkey, mDefaultTimeout);
    }

    /**
     * Removes the value for given prefix and key.
     * @param prefix Rawkey = prefix + ":" + key
     * @param key
     * @param timeout in millis
     * @return
     */
    public boolean remove(KeyPrefix prefix, String key, long timeout) {
        return removeWithRawKey(addPrefix(prefix, key), timeout);
    }

    /**
     * Removes the value for given key.  The key is "raw", meaning any prefix is already contained in the key.
     * @param rawkey
     * @param timeout in millis
     * @return
     */
    public boolean removeWithRawKey(String rawkey, long timeout) {
        Boolean success = null;
        Future<Boolean> future = getTheClient().delete(rawkey);
        try {
            success = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ZimbraLog.misc.warn("memcached delete timed out after " + timeout + "ms", e);
            future.cancel(false);
        } catch (InterruptedException e) {
            ZimbraLog.misc.warn("InterruptedException during memcached delete operation", e);
        } catch (ExecutionException e) {
            ZimbraLog.misc.warn("ExecutionException during memcached delete operation", e);
        }
        return success != null && success.booleanValue();
    }
}
