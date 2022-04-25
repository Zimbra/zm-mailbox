/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util.memcached;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.CRC32;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.CachedData;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;

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
    public static final String SHA256_HASHING = "SHA-256";
    public static final int BASE_16 = 16;

    private MemcachedClient mMCDClient;
    private int mDefaultExpiry;  // in seconds
    private long mDefaultTimeout;  // in millis
    private String mServerList;  // used for config reporting only
    private String mHashAlgorithm;
    private boolean mBinaryProtocolEnabled;

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

    public synchronized String getServerList()             { return mServerList; }
    public synchronized String getHashAlgorithm()          { return mHashAlgorithm; }
    public synchronized boolean getBinaryProtocolEnabled() { return mBinaryProtocolEnabled; }
    public synchronized int getDefaultExpirySeconds()      { return mDefaultExpiry; }
    public synchronized long getDefaultTimeoutMillis()     { return mDefaultTimeout; }

    /**
     * Connects/reconnects the memcached client with server list, protocol and hashing algorithm.
     * @param servers memcached server list
     * @param useBinaryProtocol if true, use the binary protocol; if false, use the ascii protocol
     * @param hashAlgorithm net.spy.memcached.HashAlgorithm enum
     * @param defaultExpiry in seconds
     * @param defaultTimeout in milliseconds
     * @throws ServiceException
     */
    public void connect(String[] servers, boolean useBinaryProtocol, String hashAlgorithm,
                        int defaultExpiry, long defaultTimeout)
    throws ServiceException {
        // Force spymemcached to use log4j rather than raw stdout/stderr.
        Properties props = System.getProperties();
        props.put("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");

        HashAlgorithm hashAlgo = DefaultHashAlgorithm.KETAMA_HASH;
        if (hashAlgorithm != null && hashAlgorithm.length() > 0) {
            HashAlgorithm ha = DefaultHashAlgorithm.valueOf(hashAlgorithm);
            if (ha != null)
                hashAlgo = ha;
        }
        int qLen = DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN;
        int bufSize = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE;

        MemcachedClient client = null;
        StringBuilder serverList = new StringBuilder();
        if (servers != null && servers.length > 0) {
            // Eliminate duplicates and sort case-insensitively.  This negates operator error
            // configuring server list with inconsistent order on different memcached clients.
            TreeSet<String> tset = new TreeSet<String>();  // TreeSet provides deduping and sorting.
            for (int i = 0; i < servers.length; ++i) {
                tset.add(servers[i].toLowerCase());
            }
            for (String s : tset) {
                if (serverList.length() > 0)
                    serverList.append(", ");
                serverList.append(s);
            }
            List<InetSocketAddress> serverAddrs = parseServerList(tset.toArray(new String[0]));
            ConnectionFactory cf;
            if (useBinaryProtocol)
                cf = new BinaryConnectionFactory(qLen, bufSize, hashAlgo);
            else
                cf = new DefaultConnectionFactory(qLen, bufSize, hashAlgo);
            try {
                client = new MemcachedClient(cf, serverAddrs);
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
            mServerList = serverList.length() > 0 ? serverList.toString() : null;
            mHashAlgorithm = hashAlgo.toString();
            mBinaryProtocolEnabled = useBinaryProtocol;
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
    private static List<InetSocketAddress> parseServerList(String[] servers) {
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
        key = hashMemcacheKey(key);
        if (StringUtil.isNullOrEmpty(key)) return null;
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

        int index = 0;
        for (String key : keys) {
            ((ArrayList<String>) keys).set(index, hashMemcacheKey(key));
            index++;
        }

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
     * @param waitForAck if true, block until ack'd or timeout; if false, return immediately
     * @return
     */
    public boolean put(String key, Object value, boolean waitForAck) {
        return put(key, value, DEFAULT_EXPIRY, DEFAULT_TIMEOUT, waitForAck);
    }

    /**
     * Puts the key/value pair.
     * @param key
     * @param value
     * @param expirySec expiry in seconds
     * @param timeout in millis
     * @param waitForAck if true, block until ack'd or timeout; if false, return immediately
     * @return
     */
    public boolean put(String key, Object value, int expirySec, long timeout, boolean waitForAck) {
        MemcachedClient client;
        key = hashMemcacheKey(key);
        if (StringUtil.isNullOrEmpty(key)) return false;
        synchronized (this) {
            client = mMCDClient;
            if (expirySec == DEFAULT_EXPIRY)
                expirySec = mDefaultExpiry;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null) return false;
        Future<Boolean> future = client.set(key, expirySec, value);
        if (waitForAck) {
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
        } else {
            return true;
        }
    }

    // remove

    /**
     * Removes the value for given key.
     * Default timeout is used.
     * @param key
     * @param waitForAck if true, block until ack'd or timeout; if false, return immediately
     * @return
     */
    public boolean remove(String key, boolean waitForAck) {
        return remove(key, DEFAULT_TIMEOUT, waitForAck);
    }

    /**
     * Removes the value for given key.
     * @param key
     * @param timeout in millis
     * @param waitForAck if true, block until ack'd or timeout; if false, return immediately
     * @return
     */
    public boolean remove(String key, long timeout, boolean waitForAck) {
        Boolean success = null;
        MemcachedClient client;
        key = hashMemcacheKey(key);
        if (StringUtil.isNullOrEmpty(key)) return false;
        synchronized (this) {
            client = mMCDClient;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null) return false;
        Future<Boolean> future = client.delete(key);
        if (waitForAck) {
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
        } else {
            return true;
        }
    }

    // simple wrapper around a byte[]
    private static class ByteArray {
        private byte[] mBytes;
        ByteArray(byte[] bytes) { mBytes = bytes; }
        public byte[] getBytes() { return mBytes; }
    }

    // transcoder for spymemcached API
    private static class ByteArrayTranscoder implements Transcoder<ByteArray> {

        @Override
        public ByteArray decode(CachedData cachedData) {
            return new ByteArray(cachedData.getData());
        }

        @Override
        public CachedData encode(ByteArray byteArray) {
            return new CachedData(0, byteArray.getBytes(), CachedData.MAX_SIZE);
        }

        @Override
        public int getMaxSize() {
            return CachedData.MAX_SIZE;
        }

        @Override
        public boolean asyncDecode(CachedData d) {
            return false;
        }
    }

    // maximum chunk size of big byte array values.  Set to 1MB - 1KB.  We can't use all 1MB because
    // of some overhead in lower layer serialization.
    private static final int MAX_CHUNK_SIZE = 1047552;

    private static final int MAX_VALUE_SIZE = 100 * 1024 * 1024;  // 100MB hard max
    private static final int CKSUM_LEN = 32 * 1024;  // checksum only the first 32KB of each chunk
    private static final byte BBA_PREFIX_VALUE = (byte) 'V';  // indicates single-chunk big byte array
    private static final byte BBA_PREFIX_TOC   = (byte) 'T';  // indicates TOC of multi-chunk big byte array

    // table of contents for a big byte array
    // Contains:
    // - number of chunks
    // - "fingerprint" which is used as part of the key for chunks
    // - length of each chunk
    // - checksum of each chunk
    private static class ByteArrayChunksTOC {
        private int mNumChunks;
        private int[] mLength;  // length of each chunk
        private long[] mChecksum;  // checksum of each chunk
        private long mFingerprint;

        public int getNumChunks() { return mNumChunks; }
        public int getLength(int chunkIndex) { return mLength[chunkIndex]; }
        public long getChecksum(int chunkIndex) { return mChecksum[chunkIndex]; }
        public long getFingerprint() { return mFingerprint; }

        private static final String FN_NUM_CHUNKS = "n";
        private static final String FN_FINGERPRINT = "f";
        private static final String FN_LENGTH = "l";
        private static final String FN_CHECKSUM = "cs";

        public String encode() {
            Map<String, Long> map = new HashMap<String, Long>();
            map.put(FN_NUM_CHUNKS, (long) mNumChunks);
            map.put(FN_FINGERPRINT, mFingerprint);
            for (int i = 0; i < mNumChunks; ++i) {
                map.put(FN_LENGTH + i, (long) mLength[i]);
                map.put(FN_CHECKSUM + i, mChecksum[i]);
            }
            return BEncoding.encode(map);
        }

        @SuppressWarnings("unchecked")
        private static long getFieldLong(Map map, String field) throws ServiceException {
            Object val = map.get(field);
            if (val == null)
                throw ServiceException.FAILURE("Field " + field + " not found", null);
            if (val instanceof Long) {
                return ((Long) val).longValue();
            } else if (val instanceof Integer) {
                return ((Integer) val).longValue();
            } else if (val instanceof String) {
                try {
                    return Long.parseLong((String) val);
                } catch (NumberFormatException e) {
                    throw ServiceException.FAILURE("Invalid number " + val.toString(), null);
                }
            } else {
                throw ServiceException.FAILURE("Invalid number " + val.toString(), null);
            }
        }

        @SuppressWarnings("unchecked")
        public ByteArrayChunksTOC(String encoded) throws ServiceException {
            Map map;
            try {
                map = (Map) BEncoding.decode(encoded);
            } catch (BEncodingException e) {
                throw ServiceException.FAILURE("Invalid ByteArrayChunksTOC value: " + encoded, null);
            }
            mNumChunks = (int) getFieldLong(map, FN_NUM_CHUNKS);
            mFingerprint = getFieldLong(map, FN_FINGERPRINT);
            mLength = new int[mNumChunks];
            mChecksum = new long[mNumChunks];
            for (int i = 0; i < mNumChunks; ++i) {
                mLength[i] = (int) getFieldLong(map, FN_LENGTH + i);
                mChecksum[i] = getFieldLong(map, FN_CHECKSUM + i);
            }
        }

        public ByteArrayChunksTOC(int numChunks, long fingerprint, int[] length, long[] checksum) {
            mNumChunks = numChunks;
            mFingerprint = fingerprint;
            mLength = length;
            mChecksum = checksum;
        }
    }

    // class for dividing a byte array into smaller chunks
    private static class ByteArrayChunks {
        private int mTotalLen;
        private int mNumChunks;
        private ByteArray[] mChunks;
        private long mFingerprint;
        private long[] mChecksum;

        // Splits big byte array into smaller chunks.
        // Checksum is computed for each chunk from the first 32KB.
        // "Fingerprint" of the entire data is computed by checksumming first 32KB of all chunks.
        public ByteArrayChunks(byte[] data) throws ServiceException {
            if (data.length > MAX_VALUE_SIZE)
                throw ServiceException.FAILURE(
                        "Byte array is bigger than max " + MAX_VALUE_SIZE +
                        " bytes; requested " + data.length + " bytes", null);
            mTotalLen = data.length;
            mNumChunks = (data.length + MAX_CHUNK_SIZE - 1) / MAX_CHUNK_SIZE;
            mChunks = new ByteArray[mNumChunks];
            mChecksum = new long[mNumChunks];
            CRC32 ff = new CRC32();
            CRC32 cksumChunk = new CRC32();
            for (int i = 0; i < mNumChunks; ++i) {
                int offset = MAX_CHUNK_SIZE * i;
                int len;
                if (i < mNumChunks - 1)
                    len = MAX_CHUNK_SIZE;
                else
                    len = data.length - offset;
                byte[] bytes = new byte[len];
                System.arraycopy(data, offset, bytes, 0, len);
                mChunks[i] = new ByteArray(bytes);
                cksumChunk.reset();
                int csLen = Math.min(CKSUM_LEN, bytes.length);
                ff.update(bytes, 0, csLen);
                cksumChunk.update(bytes, 0, csLen);
                mChecksum[i] = cksumChunk.getValue();
            }
            mFingerprint = ff.getValue();
        }

        public int getTotalLen() { return mTotalLen; }
        public int getNumChunks() { return mNumChunks; }
        public ByteArray getChunk(int chunkIndex) { return mChunks[chunkIndex]; }

        public ByteArrayChunksTOC makeTOC() {
            int lengths[] = new int[mNumChunks];
            for (int i = 0; i < mNumChunks; ++i) {
                lengths[i] = mChunks[i].getBytes().length;
            }
            return new ByteArrayChunksTOC(mNumChunks, mFingerprint, lengths, mChecksum);
        }

        // Combines the chunks into one big byte array, verifying checksum for each chunk.
        public static byte[] combine(ByteArray[] arrays, ByteArrayChunksTOC toc) throws ServiceException {
            if (arrays.length != toc.getNumChunks())
                return null;
            int len = 0;
            CRC32 cksumChunk = new CRC32();
            // Verify chunks and compute total data length.
            for (int i = 0; i < arrays.length; ++i) {
                // null checks
                ByteArray ba = arrays[i];
                if (ba == null) return null;
                byte[] chunk = ba.getBytes();
                if (chunk == null) return null;
                // verify chunk length
                if (chunk.length != toc.getLength(i))
                    return null;
                // verify chunk checksum
                cksumChunk.reset();
                int csLen = Math.min(CKSUM_LEN, chunk.length);
                cksumChunk.update(chunk, 0, csLen);
                if (cksumChunk.getValue() != toc.getChecksum(i))
                    return null;

                len += chunk.length;
            }
            if (len > MAX_VALUE_SIZE)
                throw ServiceException.FAILURE(
                        "Byte array is bigger than max " + MAX_VALUE_SIZE +
                        " bytes; requested " + len + " bytes", null);
            byte[] data = new byte[len];
            int offset = 0;
            for (int i = 0; i < arrays.length; ++i) {
                byte[] chunk = arrays[i].getBytes();
                System.arraycopy(chunk, 0, data, offset, chunk.length);
                offset += chunk.length;
            }
            return data;
        }
    }

    /**
     * Puts the key/value pair for a big byte array.
     * Default expiry and timeout are used.
     * @param key
     * @param value
     * @param waitForAck if true, block until ack'd or timeout; if false, return immediately
     * @return
     */
    public boolean putBigByteArray(String key, byte[] value, boolean waitForAck) {
        return putBigByteArray(key, value, DEFAULT_EXPIRY, DEFAULT_TIMEOUT, waitForAck);
    }

    /**
     * Puts the key/value pair for a big byte array.
     * 
     * A big byte array value is a byte array whose length can be greater than memcached limit of 1MB.
     * If the data is smaller than MAX_CHUNK_SIZE (1MB - 1KB) it is set in a single key/value pair in
     * memcached, with the value suffixed with a "V". (V for value)  If the data is bigger, the data is
     * split into MAX_CHUNK_SIZE chunks and set as individual cache entries.  The cache keys for the
     * chunks are the combination of the original key, the data "fingerprint" (for some uniqueness),
     * and chunk number.  The cache value for the original key is set to a table of contents containing
     * the number of chunks, the fingerprint, and length and checksum of each chunk, followed by a "T".
     * (T for table of contents)
     * 
     * During retrieval, the value for the main key is examined to see if the last byte is a V or T.  If
     * it's a V, the preceding bytes constitute the entire cache value.  If it's a T, the preceding bytes
     * are interpreted as the table of contents.  The information in the table of contents can be used
     * to then fetch the chunks and reassemble them.  All chunks must exist and must match the length and
     * checksum in the table of contents.  Otherwise the whole get operation is considered a cache miss.
     * 
     * When the main key is updated or removed, the chunk values become orphaned because the table of contents
     * will no longer contain the same fingerprint.  (Some collision risk exists.)  These entries will age
     * out of the cache eventually.
     * 
     * Example of a short value:
     * 
     * key = "foo", value = ['b', 'a', 'r']
     * Memcached will have "foo" = ['b', 'a', 'r', 'V'].
     * 
     * Example of a big value:
     * 
     * key = "foo", value = <1.5MB byte array>
     * Assume the fingerprint computed is 1234567890.
     * Memcached will have:
     *   "foo" = <table of contents> 'T'
     *   "foo:1234567890.0" = <1st chunk of ~1MB>
     *   "foo:1234567890.1" = <2nd chunk of ~0.5MB>
     * 
     * @param key
     * @param value
     * @param expirySec expiry in seconds
     * @param timeout in millis
     * @param waitForAck if true, block until ack'd or timeout; if false, return immediately
     * @return
     */
    public boolean putBigByteArray(String key, byte[] value, int expirySec, long timeout, boolean waitForAck) {
        key = hashMemcacheKey(key);
        if (StringUtil.isNullOrEmpty(key)) return false;
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            if (expirySec == DEFAULT_EXPIRY)
                expirySec = mDefaultExpiry;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null) return false;

        ByteArrayTranscoder bat = new ByteArrayTranscoder();
        ByteArray mainValue;
        if (value.length < MAX_CHUNK_SIZE) {
            // Value is short enough.  Set it directly, with a prefix.  Requires 1 memcached set operation.
            byte[] prefixed = new byte[value.length + 1];
            System.arraycopy(value, 0, prefixed, 0, value.length);
            prefixed[value.length] = BBA_PREFIX_VALUE;
            mainValue = new ByteArray(prefixed);
        } else {
            // Value can't fit in a single memcached entry.  Split into chunks and use table of contents.
            // Requires N+1 memcached set operations.
            ByteArrayChunks chunks;
            try {
                chunks = new ByteArrayChunks(value);
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("Unable to split byte array into chunks", e);
                return false;
            }
            ByteArrayChunksTOC toc = chunks.makeTOC();
    
            // Add chunks to the cache.
            String chunkKeyPrefix = key + ":" + toc.getFingerprint() + ".";
            int numChunks = chunks.getNumChunks();
            for (int i = 0; i < numChunks; ++i) {
                String chunkKey = chunkKeyPrefix + i;
                ByteArray byteArray = chunks.getChunk(i);
                Future<Boolean> future = client.set(chunkKey, expirySec, byteArray, bat);
                if (waitForAck) {
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
                    if (success == null || !success.booleanValue())
                        return false;
                }
            }

            // Put the table of contents as the main value.  Do this after all chunks have been
            // added successfully.
            byte[] tocBytes;
            try {
                tocBytes = toc.encode().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.misc.warn("Unable to get bytes for BBA table of contents", e);
                return false;
            }
            byte[] prefixed = new byte[tocBytes.length + 1];
            System.arraycopy(tocBytes, 0, prefixed, 0, tocBytes.length);
            prefixed[tocBytes.length] = BBA_PREFIX_TOC;
            mainValue = new ByteArray(prefixed);
        }

        // Put the main value.
        Future<Boolean> future = client.set(key, expirySec, mainValue, bat);
        if (waitForAck) {
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
        } else {
            return true;
        }
    }

    /**
     * Retrieves the big byte array value corresponding to the given key.
     * Default timeout is used.
     * @param key
     * @return null if no value is found for the key
     */
    public byte[] getBigByteArray(String key) throws ServiceException {
        return getBigByteArray(key, DEFAULT_TIMEOUT);
    }

    /**
     * Retrieves the big byte array value corresponding to the given key.  See putBigByteArray method
     * for more information.
     * @param key
     * @param timeout in millis
     * @return null if no value is found for the key
     */
    public byte[] getBigByteArray(String key, long timeout) {
        key = hashMemcacheKey(key);
        if (StringUtil.isNullOrEmpty(key)) return null;
        MemcachedClient client;
        synchronized (this) {
            client = mMCDClient;
            if (timeout == DEFAULT_TIMEOUT)
                timeout = mDefaultTimeout;
        }
        if (client == null)
            return null;

        // Get the main value.  This may be the entire value or the table of contents.
        ByteArrayTranscoder bat = new ByteArrayTranscoder();
        ByteArray mainValue = null;
        Future<ByteArray> future = client.asyncGet(key, bat);
        try {
            mainValue = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ZimbraLog.misc.warn("memcached asyncGetBulk timed out after " + timeout + "ms", e);
            future.cancel(false);
        } catch (InterruptedException e) {
            ZimbraLog.misc.warn("InterruptedException during memcached asyncGetBulk operation", e);
        } catch (ExecutionException e) {
            ZimbraLog.misc.warn("ExecutionException during memcached asyncGetBulk operation", e);
        }
        if (mainValue == null)
            return null;
        byte[] prefixed = mainValue.getBytes();
        if (prefixed == null || prefixed.length < 2)
            return null;
        // First byte is value/TOC indicator.  Remove it.
        byte[] value = new byte[prefixed.length - 1];
        System.arraycopy(prefixed, 0, value, 0, prefixed.length - 1);
        // If it's a short value, just return it.
        if (prefixed[prefixed.length - 1] == BBA_PREFIX_VALUE)
            return value;


        // We have a table of contents.
        String tocEncoded = null;
        ByteArrayChunksTOC toc;
        try {
            tocEncoded = new String(value, "utf-8");
            toc = new ByteArrayChunksTOC(tocEncoded);
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.misc.warn("Unable to decode BBA table of contents", e);
            return null;
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Invalid big byte array TOC: " + tocEncoded);
            return null;
        }
        int numChunks = toc.getNumChunks();
        if (numChunks <= 0) {
            // This should never happen.  Just a sanity check.
            ZimbraLog.misc.warn("Big byte array TOC has numChunks=0");
            return null;
        }
        List<String> chunkKeys = new ArrayList<String>(numChunks);
        for (int i = 0; i < numChunks; ++i) {
            String ck = key + ":" + toc.getFingerprint() + "." + i;
            chunkKeys.add(ck);
        }

        // Get the chunks from memcached.
        Map<String, ByteArray> vals = null;
        Future<Map<String, ByteArray>> futureChunks = client.asyncGetBulk(chunkKeys, bat);
        try {
            vals = futureChunks.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            ZimbraLog.misc.warn("memcached asyncGetBulk timed out after " + timeout + "ms", e);
            futureChunks.cancel(false);
        } catch (InterruptedException e) {
            ZimbraLog.misc.warn("InterruptedException during memcached asyncGetBulk operation", e);
        } catch (ExecutionException e) {
            ZimbraLog.misc.warn("ExecutionException during memcached asyncGetBulk operation", e);
        }
        // Make sure all chunks are there.  If not, just return null.
        if (vals == null) return null;
        ByteArray[] byteArrays = new ByteArray[numChunks];
        int index = 0;
        for (String ck : chunkKeys) {
            ByteArray ba = (ByteArray) vals.get(ck);
            if (ba == null)
                return null;
            byteArrays[index] = ba;
            ++index;
        }

        byte[] data = null;
        try {
            data = ByteArrayChunks.combine(byteArrays, toc);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Unable to reassemble byte array from chunks", e);
        }
        return data;
    }

    public static void main(String[] args) throws Exception {
        Test t = new Test();
        t.test();
    }

    public static class Test {
        private static final int KB = 1024;
        private static final int MB = 1024 * 1024;

        public Test() {}

        public void test() throws Exception {
            ZimbraMemcachedClient client = new ZimbraMemcachedClient();
            String servers[] = { "localhost:11211" };
            client.connect(servers, false, null, 3600, 5000);
            try {
                int reps = 100;
                long start = System.currentTimeMillis();
                testInteger(client, reps);
                testString(client, "Str 10", 10, reps);
                testString(client, "Str 1K", 1 * KB, reps);
                testString(client, "Str 50K", 50 * KB, reps);
                testString(client, "Str 100K", 100 * KB, reps);
                testString(client, "Str 500K", 500 * KB, reps);
                testBBA(client, "BBA 10", 10, reps);
                testBBA(client, "BBA 1K", 1 * KB, reps);
                testBBA(client, "BBA 5K", 5 * KB, reps);
                testBBA(client, "BBA 10K", 10 * KB, reps);
                testBBA(client, "BBA 50K", 50 * KB, reps);
                testBBA(client, "BBA 100K", 100 * KB, reps);
                testBBA(client, "BBA 500K", 500 * KB, reps);
                testBBA(client, "BBA 900K", 900 * KB, reps);
                testBBA(client, "BBA 1M--", 1 * MB - 1 * KB - 10, reps);
                testBBA(client, "BBA 1M", 1 * MB, reps);
                testBBA(client, "BBA 5M", 5 * MB, reps);
                testBBA(client, "BBA 10M", 10 * MB, reps);
                testBBA(client, "BBA 20M", 20 * MB, reps);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("Took " + elapsed + "ms");
            } finally {
                client.disconnect(1000);
            }
        }

        private void pause() throws Exception {
            System.gc();
            Thread.sleep(10);
        }

        private void printReport(String testName, long elapsed, int bytes, boolean success) {
            double kbPerSec = ((double) bytes) / 1024.0 / ((double) elapsed) * 1000.0;
            System.out.printf("%-9s: %5dms, %7.2fkb/s, %s\n", testName, elapsed, kbPerSec, success ? "PASS" : "FAIL");
        }

        private static class TestKey implements MemcachedKey {
            private String mKey;
            public TestKey(String k) { mKey = k; }
            public String getKeyPrefix() { return null; }
            public String getKeyValue() { return mKey; }
        }

        private static class IntegerSerializer implements MemcachedSerializer<Integer> {
            public Object serialize(Integer value) throws ServiceException { return value; }        
            public Integer deserialize(Object obj) throws ServiceException { return (Integer) obj; }
        }

        private boolean testInteger(ZimbraMemcachedClient client, int reps) throws Exception {
            MemcachedMap<TestKey, Integer> map =
                new MemcachedMap<TestKey, Integer>(client, new IntegerSerializer());
            TestKey foo = new TestKey("fooInt");
            int val1 = 1234567890;

            boolean success = true;
            map.put(foo, val1);
            pause();
            long start = System.currentTimeMillis();
            for (int i = 0; i < reps; ++i) {
                Integer val2 = map.get(foo);
                boolean same = val2 != null && val2.intValue() == val1;
                if (!same) {
                    success = false;
                    System.out.println("failed on rep " + i);
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            printReport("Integer", elapsed, 10, success);
            return success;
        }

        private static class StringSerializer implements MemcachedSerializer<String> {
            public Object serialize(String value) throws ServiceException { return value; }        
            public String deserialize(Object obj) throws ServiceException { return (String) obj; }
        }

        private boolean testString(ZimbraMemcachedClient client, String testName, int dataLen, int reps)
        throws Exception {
            MemcachedMap<TestKey, String> map =
                new MemcachedMap<TestKey, String>(client, new StringSerializer());
            TestKey foo = new TestKey("fooStr");
            StringBuilder sb = new StringBuilder(dataLen + 20);
            while (sb.length() < dataLen) {
                sb.append("Hello, STR!  ");
            }
            String val1 = sb.substring(0, dataLen);

            boolean success = true;
            map.put(foo, val1);
            pause();
            long start = System.currentTimeMillis();
            for (int i = 0; i < reps; ++i) {
                String val2 = map.get(foo);
                boolean same = val1.equals(val2);
                if (!same) {
                    success = false;
                    System.out.println("failed on rep " + i);
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            printReport(testName, elapsed, dataLen, success);
            return success;
        }

        private static class BBASerializer implements ByteArraySerializer<ByteArray> {
            public byte[] serialize(ByteArray value) throws ServiceException {
                return value.getBytes();
            }
            public ByteArray deserialize(byte[] bytes) throws ServiceException {
                return new ByteArray(bytes);
            }
        }

        private boolean testBBA(ZimbraMemcachedClient client, String testName, int dataLen, int reps)
        throws Exception {
            BigByteArrayMemcachedMap<TestKey, ByteArray> map =
                new BigByteArrayMemcachedMap<TestKey, ByteArray>(client, new BBASerializer());
            TestKey foo = new TestKey("fooBBA");
            StringBuilder sb = new StringBuilder(dataLen + 20);
            while (sb.length() < dataLen) {
                sb.append("Hello, BBA!  ");
            }
            byte[] sbBytes = sb.toString().getBytes("utf-8");
            byte[] data = new byte[dataLen];
            System.arraycopy(sbBytes, 0, data, 0, dataLen);
            ByteArray bytes1 = new ByteArray(data);
            ByteArray bytes2 = null;

            boolean success = true;
            map.put(foo, bytes1);
            pause();
            long start = System.currentTimeMillis();
            for (int i = 0; i < reps; ++i) {
                bytes2 = map.get(foo);
                boolean same = bytes2 != null && bytes1.getBytes().length == bytes2.getBytes().length;
                if (!same) {
                    success = false;
                    System.out.println("failed on rep " + i);
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            byte[] b1 = bytes1 != null ? bytes1.getBytes() : null;
            byte[] b2 = bytes2 != null ? bytes2.getBytes() : null;
            success = success && Arrays.equals(b1, b2);
            printReport(testName, elapsed, dataLen, success);
            return success;
        }
    }

    public String hashMemcacheKey(String key) {
        String sha256Key = "";
        if (StringUtil.isNullOrEmpty(key)) {
            return key;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_HASHING);
            digest.update(key.getBytes(StandardCharsets.UTF_8), 0, key.length());
            StringBuilder tempString = new StringBuilder();
            for (byte byteElement : digest.digest()) {
              tempString.append(String.format("%02x", byteElement));
            }
            sha256Key = tempString.toString();
        } catch (NoSuchAlgorithmException e) {
            ZimbraLog.misc.error("Failed to hash the key", e);
        }

        ZimbraLog.misc.debug("Key: ["+  key + "]");
        ZimbraLog.misc.debug("HashedKey: ["+ sha256Key + "]");

        return sha256Key;
    }
}
