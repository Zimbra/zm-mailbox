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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;

public class ZimbraMemcachedClient {

    private static class ConnObserver implements ConnectionObserver {
        public void connectionEstablished(SocketAddress sa, int reconnectCount) {
            if (sa instanceof InetSocketAddress) {
                InetSocketAddress isa = (InetSocketAddress) sa;
                String hostPort = isa.getHostName() + ":" + isa.getPort();
                ZimbraLog.misc.info("Reconnected to memcached at " + hostPort);
            } else {
                ZimbraLog.misc.info("Reconnected to memcached at " + sa.toString());
            }
        }

        public void connectionLost(SocketAddress sa) {
            if (sa instanceof InetSocketAddress) {
                InetSocketAddress isa = (InetSocketAddress) sa;
                String hostPort = isa.getHostName() + ":" + isa.getPort();
                ZimbraLog.misc.warn("Lost connection to memcached at " + hostPort);
            } else {
                ZimbraLog.misc.warn("Lost connection to memcached at " + sa.toString());
            }
        }        
    }

    public static final int DEFAULT_EXPIRY = -1;
    public static final long DEFAULT_TIMEOUT = -1;

    private MemcachedClient mMCDClient;
    private int mDefaultExpiry;  // in seconds
    private long mDefaultTimeout;  // in millis

    /**
     * Constructs a memcached client.  Call connect() before using this.
     */
    public ZimbraMemcachedClient() {
        mMCDClient = null;
        mDefaultExpiry = 86400;
        mDefaultTimeout = 10000;
    }

    public boolean isConnected() {
        synchronized (this) {
            return mMCDClient != null;
        }
    }

    /**
     * Connects/reconnects the memcached client with server list, protocol and hashing algorithm.
     * @param servers memcached server list
     * @param useBinaryProtocol if true, use the binary protocol; if false, use the ascii protocol
     * @param hashAlgorithm net.spy.memcached.HashAlgorithm enum
     * @param defaultExpiry in seconds
     * @param defaultTimeout in milliseconds
     * @throws ServiceException
     */
    public void connect(List<InetSocketAddress> servers, boolean useBinaryProtocol, String hashAlgorithm,
                          int defaultExpiry, long defaultTimeout)
    throws ServiceException {
        HashAlgorithm hashAlgo = HashAlgorithm.KETAMA_HASH;
        if (hashAlgorithm != null && hashAlgorithm.length() > 0) {
            HashAlgorithm ha = HashAlgorithm.valueOf(hashAlgorithm);
            if (ha != null)
                hashAlgo = ha;
        }
        int qLen = DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN;
        int bufSize = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE;

        MemcachedClient client = null;
        if (servers != null && servers.size() > 0) {
            ConnectionFactory cf;
            if (useBinaryProtocol)
                cf = new BinaryConnectionFactory(qLen, bufSize, hashAlgo);
            else
                cf = new DefaultConnectionFactory(qLen, bufSize, hashAlgo);
            try {
                client = new MemcachedClient(cf, servers);
                boolean added = client.addObserver(new ConnObserver());
                if (!added)
                    ZimbraLog.misc.error("Unable to add connection observer to memcached client");
            } catch (IOException e) {
                throw ServiceException.FAILURE("Unable to initialize memcached client", e);
            }
        }
        MemcachedClient oldClient = null;
        synchronized (this) {
            oldClient = mMCDClient;
            mMCDClient = client;
            mDefaultExpiry = defaultExpiry;
            mDefaultTimeout = defaultTimeout;
        }
        // New client is ready for use by other threads at this point.
        if (oldClient != null)
            disconnect(oldClient, 30000);
    }

    /**
     * Shutdown the memcached client.  Drain the queue and do a normal shutdown if possible.
     * Both drain and normal shutdown are attempted with timeout.  When unsuccessful, shutdown
     * the client immediately.
     * @param timeout in millis
     */
    public void disconnect(long timeout) {
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            mMCDClient = null;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client != null) {
            disconnect(client, timeout);
        }
    }

    private void disconnect(MemcachedClient client, long timeout) {
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

    private static final int DEFAULT_PORT = 11211;

    /**
     * Parse a server list.
     * Each server value is hostname:port or just hostname.  Default port is 11211.
     * @param serverList
     * @return
     */
    public static List<InetSocketAddress> parseServerList(String[] servers) {
        if (servers != null) {
            List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>(servers.length);
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
                            continue;
                        }
                    } else {
                        ZimbraLog.misc.warn("Invalid server " + server);
                        continue;
                    }
                    InetSocketAddress addr = new InetSocketAddress(host, port);
                    addrs.add(addr);
                } else {
                    ZimbraLog.misc.warn("Invalid server " + server);
                    continue;
                }
            }
            return addrs;
        } else {
            return new ArrayList<InetSocketAddress>(0);
        }
    }

    /**
     * Parse a server list string.  Values are delimited by a sequence of commas and/or whitespace chars.
     * Each server value is hostname:port or just hostname.  Default port is 11211.
     * @param serverList
     * @return
     */
    public static List<InetSocketAddress> parseServerList(String serverList) {
        if (serverList != null) {
            String[] servers = serverList.split("[\\s,]+");
            return parseServerList(servers);
        } else {
            return new ArrayList<InetSocketAddress>(0);
        }
    }

    // get

    /**
     * Retrieves the value corresponding to the given key.
     * Default timeout is used.
     * @param key
     * @return null if no value is found for the key
     */
    public Object get(String key) {
        return get(key, DEFAULT_TIMEOUT);
    }

    /**
     * Retrieves the value corresponding to the given key.
     * @param key
     * @param timeout in millis
     * @return null if no value is found for the key
     */
    public Object get(String key, long timeout) {
        Object value = null;
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null) return null;
        Future<Object> future = client.asyncGet(key);
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
     * Retrieves the values corresponding to the given keys.
     * Default timeout is used.
     * The returned map is never null and always contains an entry for every key.
     * Null value is used for any key not found in memcached.
     * @param keys
     * @return map of (key, value); missing keys have null value
     */
    public Map<String, Object> getMulti(Collection<String> keys) {
        return getMulti(keys, DEFAULT_TIMEOUT);
    }

    /**
     * Retrieves the values corresponding to the given keys.
     * The returned map is never null and always contains an entry for every key.
     * Null value is used for any key not found in memcached.
     * @param keys
     * @param timeout in millis
     * @return map of (key, value); missing keys have null value
     */
    public Map<String, Object> getMulti(Collection<String> keys, long timeout) {
        Map<String, Object> value = null;
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client != null) {
            Future<Map<String, Object>> future = client.asyncGetBulk(keys);
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
        }
        // Make sure the returned map contains an entry for every key passed in.  Add null value
        // for any keys missing from memcached response.
        if (value == null)
            value = new HashMap<String, Object>(keys.size());
        for (String key : keys) {
            if (!value.containsKey(key))
                value.put(key, null);
        }
        return value;
    }

    // put

    /**
     * Puts the key/value pair.
     * Default expiry and timeout are used.
     * @param key
     * @param value
     * @return
     */
    public boolean put(String key, Object value) {
        return put(key, value, DEFAULT_EXPIRY, DEFAULT_TIMEOUT);
    }

    /**
     * Puts the key/value pair.
     * @param key
     * @param value
     * @param expirySec expiry in seconds
     * @param timeout in millis
     * @return
     */
    public boolean put(String key, Object value, int expirySec, long timeout) {
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            if (expirySec == DEFAULT_EXPIRY)
                expirySec = mDefaultExpiry;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null) return true;
        Future<Boolean> future = client.set(key, expirySec, value);
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
     * Removes the value for given key.
     * Default timeout is used.
     * @param key
     * @return
     */
    public boolean remove(String key) {
        return remove(key, DEFAULT_TIMEOUT);
    }

    /**
     * Removes the value for given key.
     * @param key
     * @param timeout in millis
     * @return
     */
    public boolean remove(String key, long timeout) {
        Boolean success = null;
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null) return true;
        Future<Boolean> future = client.delete(key);
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
