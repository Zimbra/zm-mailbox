package com.zimbra.cs.mailbox.redis;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.redisson.Redisson;
import org.redisson.api.BatchOptions;
import org.redisson.api.ClusterNodesGroup;
import org.redisson.api.ExecutorOptions;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.MapOptions;
import org.redisson.api.Node;
import org.redisson.api.NodesGroup;
import org.redisson.api.RAtomicDouble;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBatch;
import org.redisson.api.RBinaryStream;
import org.redisson.api.RBitSet;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBoundedBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RBuckets;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RDeque;
import org.redisson.api.RDoubleAdder;
import org.redisson.api.RGeo;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RKeys;
import org.redisson.api.RLexSortedSet;
import org.redisson.api.RList;
import org.redisson.api.RListMultimap;
import org.redisson.api.RListMultimapCache;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RLock;
import org.redisson.api.RLongAdder;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RPriorityBlockingDeque;
import org.redisson.api.RPriorityBlockingQueue;
import org.redisson.api.RPriorityDeque;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RRemoteService;
import org.redisson.api.RScheduledExecutorService;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RSemaphore;
import org.redisson.api.RSet;
import org.redisson.api.RSetCache;
import org.redisson.api.RSetMultimap;
import org.redisson.api.RSetMultimapCache;
import org.redisson.api.RSortedSet;
import org.redisson.api.RStream;
import org.redisson.api.RTopic;
import org.redisson.api.RTransaction;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class RedissonRetryClient implements RedissonClient {

    private RedissonClient client;
    private int clientVersion;
    private ReadWriteLock clientLock = new ReentrantReadWriteLock();

    public RedissonRetryClient(RedissonClient client) {
        this.client = client;
        clientVersion = 1;
    }

    public int getClientVersion() {
        return clientVersion;
    }

    private boolean waitForCluster(Config redissonConfig, int maxWaitMillis) {
        int waitMillis = 250;
        int waited = 0;
        while (waited <= maxWaitMillis) {
            try {
                client = Redisson.create(redissonConfig);
                clientVersion++;
                return true;
            } catch (RedisException e) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e1) {}
                waited += waitMillis;
            }
        }
        return false;
    }

    public synchronized void restart() {
        clientLock.writeLock().lock();
        try {
            ZimbraLog.mailbox.info("restarting redisson client (version %d)", clientVersion);
            Config config = client.getConfig();
            client.shutdown();
            int maxWaitMillis = LC.redis_cluster_reconnect_timeout.intValue();
            boolean success = waitForCluster(config, maxWaitMillis);
            if (!success) {
                ZimbraLog.mailbox.warn("unable to restart redisson client after %d ms", maxWaitMillis);
            } else {
                ZimbraLog.mailbox.info("new redisson client created (version=%d)", clientVersion);
            }
        } finally {
            clientLock.writeLock().unlock();
        }
    }

    public <R> R runInitializer(RedissonRetryDecorator.RedissonInitializer<R> initializer) {
        clientLock.readLock().lock();
        try {
            return initializer.init(client);
        } finally {
            clientLock.readLock().unlock();
        }
    }

    @Override
    public <K, V> RMap<K, V> getMap(String name) {
        return new RedissonRetryMap<K, V>(client -> client.getMap(name), this);
    }

    @Override
    public <K, V> RMap<K, V> getMap(String name, MapOptions<K, V> options) {
        return new RedissonRetryMap<K, V>(client -> client.getMap(name, options), this);
    }

    @Override
    public <K, V> RMap<K, V> getMap(String name, Codec codec) {
        return new RedissonRetryMap<K, V>(client -> client.getMap(name, codec), this);
    }

    @Override
    public <K, V> RMap<K, V> getMap(String name, Codec codec, MapOptions<K, V> options) {
        return new RedissonRetryMap<K, V>(client -> client.getMap(name, codec, options), this);
    }

    @Override
    public <V> RList<V> getList(String name) {
        return new RedissonRetryList<V>(client -> client.getList(name), this);
    }

    @Override
    public <V> RList<V> getList(String name, Codec codec) {
        return new RedissonRetryList<V>(client -> client.getList(name, codec), this);
    }

    @Override
    public <M> RTopic<M> getTopic(String name) {
        return new RedissonRetryTopic<M>(client -> client.getTopic(name), this);
    }

    @Override
    public <M> RTopic<M> getTopic(String name, Codec codec) {
        return new RedissonRetryTopic<M>(client -> client.getTopic(name, codec), this);
    }

    @Override
    public <V> RBucket<V> getBucket(String name) {
        return new RedissonRetryBucket<V>(client -> client.getBucket(name), this);
    }

    @Override
    public <V> RBucket<V> getBucket(String name, Codec codec) {
        return new RedissonRetryBucket<V>(client -> client.getBucket(name, codec), this);
    }

    @Override
    public RReadWriteLock getReadWriteLock(String name) {
        return new RedissonRetryReadWriteLock(client -> client.getReadWriteLock(name), this);
    }

    @Override
    public RLock getLock(String name) {
        return new RedissonRetryLock(client -> client.getLock(name), this);
    }

    @Override
    public RAtomicLong getAtomicLong(String name) {
        return new RedissonRetryAtomicLong(client -> client.getAtomicLong(name), this);
    }

    @Override
    public void shutdown() {
        client.shutdown();
    }

    @Override
    public void shutdown(long quietPeriod, long timeout, TimeUnit unit) {
        client.shutdown(quietPeriod, timeout, unit);

    }

    @Override
    public Config getConfig() {
        return client.getConfig();
    }

    @Override
    public NodesGroup<Node> getNodesGroup() {
        return client.getNodesGroup();
    }

    @Override
    public ClusterNodesGroup getClusterNodesGroup() {
        return client.getClusterNodesGroup();
    }

    @Override
    public boolean isShutdown() {
        return client.isShutdown();
    }

    @Override
    public boolean isShuttingDown() {
        return client.isShuttingDown();
    }

    @Override
    public RBatch createBatch() {
        return new RedissonRetryBatch(client -> client.createBatch(), this);
    }

    @Override
    public RBatch createBatch(BatchOptions options) {
        return new RedissonRetryBatch(client -> client.createBatch(options), this);
    }

    //RedissonClient interface methods below are not currently used anywhere in the codebase,
    //and therefore do not have corresponding retry-capable implementations subclassing {@link RedissonRetryDecorator}.

    @Override
    public RBinaryStream getBinaryStream(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RGeo<V> getGeo(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RGeo<V> getGeo(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSetCache<V> getSetCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSetCache<V> getSetCache(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec,
            MapOptions<K, V> options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name,
            MapOptions<K, V> options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RBuckets getBuckets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RBuckets getBuckets(Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RHyperLogLog<V> getHyperLogLog(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RHyperLogLog<V> getHyperLogLog(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name,
            LocalCachedMapOptions<K, V> options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name,
            Codec codec, LocalCachedMapOptions<K, V> options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RSemaphore getSemaphore(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLock getFairLock(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSet<V> getSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSet<V> getSet(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSortedSet<V> getSortedSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSortedSet<V> getSortedSet(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLexSortedSet getLexSortedSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M> RPatternTopic<M> getPatternTopic(String pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <M> RPatternTopic<M> getPatternTopic(String pattern, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RQueue<V> getQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RDelayedQueue<V> getDelayedQueue(RQueue<V> destinationQueue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RQueue<V> getQueue(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityQueue<V> getPriorityQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityQueue<V> getPriorityQueue(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityBlockingQueue<V> getPriorityBlockingQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityBlockingQueue<V> getPriorityBlockingQueue(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityBlockingDeque<V> getPriorityBlockingDeque(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityBlockingDeque<V> getPriorityBlockingDeque(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityDeque<V> getPriorityDeque(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RPriorityDeque<V> getPriorityDeque(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingQueue<V> getBlockingQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingQueue<V> getBlockingQueue(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name,
            Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RDeque<V> getDeque(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RDeque<V> getDeque(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingDeque<V> getBlockingDeque(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBlockingDeque<V> getBlockingDeque(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RAtomicDouble getAtomicDouble(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLongAdder getLongAdder(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RDoubleAdder getDoubleAdder(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RCountDownLatch getCountDownLatch(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RBitSet getBitSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBloomFilter<V> getBloomFilter(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RBloomFilter<V> getBloomFilter(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScript getScript() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScheduledExecutorService getExecutorService(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScheduledExecutorService getExecutorService(Codec codec, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScheduledExecutorService getExecutorService(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RRemoteService getRemoteService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RRemoteService getRemoteService(Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RRemoteService getRemoteService(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RRemoteService getRemoteService(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RKeys getKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLiveObjectService getLiveObjectService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RTransaction createTransaction(TransactionOptions arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScheduledExecutorService getExecutorService(String arg0, ExecutorOptions arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RScheduledExecutorService getExecutorService(String arg0, Codec arg1, ExecutorOptions arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RRateLimiter getRateLimiter(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RStream<K, V> getStream(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> RStream<K, V> getStream(String arg0, Codec arg1) {
        throw new UnsupportedOperationException();
    }
}
