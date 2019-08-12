package com.zimbra.cs.mailbox.redis;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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
import org.redisson.api.RRingBuffer;
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
import org.redisson.client.protocol.RedisCommand;
import org.redisson.command.CommandExecutor;
import org.redisson.config.Config;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.LivenessProbeOverride;

public class RedissonRetryClient implements RedissonClient {

    private RedissonClient client;
    private int clientVersion;
    private ReadWriteLock clientLock = new ReentrantReadWriteLock();
    private final Set<WeakReference<RedissonRetryTopic>> channels;

    public RedissonRetryClient(RedissonClient client) {
        this.client = client;
        clientVersion = 1;
        channels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public int getClientVersion() {
        return clientVersion;
    }

    public CommandExecutor getCommandExecutor() {
        return new RedissonRetryCommandExecutor(client -> ((Redisson) client).getCommandExecutor(), this);
    }

    private boolean waitForCluster(Config redissonConfig, int maxWaitMillis) {
        /*
         * We use a 1s delay here as opposed to a tighter loop because a client retry is likely caused
         * by one or more redis hosts rebooting - which, in the worst case, can take a bit of time.
         */
        int waitMillis = 1000;
        int waited = 0;
        int attempt = 0;
        while (waited <= maxWaitMillis) {
            try {
                ZimbraLog.mailbox.info("waiting for redis cluster to become available (attempt %s)", ++attempt);
                client = Redisson.create(redissonConfig);
                clientVersion++;
                return true;
            } catch (RedisException e) {
                ZimbraLog.mailbox.info("redis cluster not ready after %s attempts: %s", ++attempt, e.getMessage());
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e1) {}
                waited += waitMillis;
            }
        }
        return false;
    }

    private void initializePubSubChannels() {
        if (!channels.isEmpty()) {
            ZimbraLog.mailbox.debug("re-initializing %s existing pubsub channels", channels.size());
            Iterator<WeakReference<RedissonRetryTopic>> iter = channels.iterator();
            while (iter.hasNext()) {
                RedissonRetryTopic channel = iter.next().get();
                if (channel == null) {
                    iter.remove();
                } else {
                    channel.initialize();
                }
            }
        }
    }

    public synchronized void waitForClusterOK() {
        int maxWaitMillis = LC.redis_cluster_reconnect_timeout_millis.intValue();
        int waited = 0;
        int waitMillis = 1000;
        Collection<Node> nodes = getNodesGroup().getNodes();
        long start = System.currentTimeMillis();
        // for now, loop over currently-known nodes until we get cluster_state:ok from one of them
        clientLock.writeLock().lock();
        try {
            ZimbraLog.mailbox.info("waiting until redis cluster_state is OK...");
            while (waited <= maxWaitMillis) {
                for (Node node: nodes) {
                    try {
                        Map<String, String> info = node.clusterInfoAsync().get();
                        String clusterState = info.get("cluster_state");
                        if (clusterState != null && clusterState.equals("ok")) {
                            ZimbraLog.mailbox.info("redis cluster is OK again after %sms", System.currentTimeMillis() - start);
                            return;
                        } else {
                            try {
                                Thread.sleep(waitMillis);
                                waited += waitMillis;
                            } catch (InterruptedException e1) {}
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        try {
                            Thread.sleep(waitMillis);
                            waited += waitMillis;
                        } catch (InterruptedException e1) {}
                    }
                }
            }
            ZimbraLog.mailbox.warn("redis cluster is not OK after %sms!", maxWaitMillis);
        } finally {
            clientLock.writeLock().unlock();
        }

    }

    public synchronized int restart(int clientVersionAtFailure) {
        if (clientVersionAtFailure < clientVersion) {
            ZimbraLog.mailbox.debug("another thread already re-initialized the redisson client (failed at version %s, current version=%s)",
                    clientVersionAtFailure, clientVersion);
        } else {
            clientLock.writeLock().lock();
            try (LivenessProbeOverride override = new LivenessProbeOverride()) {
                try {
                    ZimbraLog.mailbox.info("restarting redisson client (version %d)", clientVersion);
                    Config config = client.getConfig();
                    client.shutdown();
                    int maxWaitMillis = LC.redis_cluster_reconnect_timeout_millis.intValue();
                    boolean success = waitForCluster(config, maxWaitMillis);
                    if (!success) {
                        ZimbraLog.mailbox.warn("unable to restart redisson client after %d ms", maxWaitMillis);
                    } else {
                        ZimbraLog.mailbox.info("new redisson client created (version=%d)", clientVersion);
                        //re-initialize pubsub channels up front, other redisson objects get re-initialized lazily
                        initializePubSubChannels();
                    }
                } finally {
                    clientLock.writeLock().unlock();
                }
            }
        }
        return clientVersion;
    }

    public <R> R runInitializer(RedissonRetryDecorator.RedissonInitializer<R> initializer) {
        clientLock.readLock().lock();
        try {
            return initializer.init(client);
        } finally {
            clientLock.readLock().unlock();
        }
    }

    ReadWriteLock getClientLock() {
        return clientLock;
    }

    public <T> T evalWrite(String key, Codec codec, RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
        return getCommandExecutor().evalWrite(key, codec, evalCommandType, script, keys, params);
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
    public RTopic getTopic(String name) {
        RedissonRetryTopic topic = new RedissonRetryTopic(client -> client.getTopic(name), this);
        channels.add(new WeakReference<>(topic));
        return topic;
    }

    @Override
    public RTopic getTopic(String name, Codec codec) {
        RedissonRetryTopic topic = new RedissonRetryTopic(client -> client.getTopic(name, codec), this);
        channels.add(new WeakReference<>(topic));
        return topic;
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
        client.shutdown(0, 2, TimeUnit.SECONDS);
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

    @Override
    public RLock getFairLock(String name) {
        return new RedissonRetryLock(client -> client.getFairLock(name), this);
    }

    @Override
    public <V> RSet<V> getSet(String name) {
        return new RedissonRetrySet<V>(client -> client.getSet(name), this);
    }

    @Override
    public <V> RSet<V> getSet(String name, Codec codec) {
        return new RedissonRetrySet<V>(client -> client.getSet(name, codec), this);
    }

    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name) {
        return new RedissonRetryScoredSortedSet<V>(client -> client.getScoredSortedSet(name), this);
    }

    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name, Codec codec) {
        return new RedissonRetryScoredSortedSet<V>(client -> client.getScoredSortedSet(name, codec), this);
    }

    @Override
    public RScript getScript() {
        return new RedissonRetryScript(client -> client.getScript(), this);
    }


    @Override
    public RScript getScript(Codec arg0) {
        return new RedissonRetryScript(client -> client.getScript(arg0), this);
    }

    @Override
    public String getId() {
        return client.getId();
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
    public <V> RSortedSet<V> getSortedSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RSortedSet<V> getSortedSet(String name, Codec codec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLexSortedSet getLexSortedSet(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RPatternTopic getPatternTopic(String pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RPatternTopic getPatternTopic(String pattern, Codec codec) {
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

    @Override
    public RLock getMultiLock(RLock... arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RLock getRedLock(RLock... arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RRingBuffer<V> getRingBuffer(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> RRingBuffer<V> getRingBuffer(String arg0, Codec arg1) {
        throw new UnsupportedOperationException();
    }
}
