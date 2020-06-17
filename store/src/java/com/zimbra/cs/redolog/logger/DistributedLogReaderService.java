package com.zimbra.cs.redolog.logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RScript;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RScript.ReturnType;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamInfo;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.StringCodec;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.PodInfo;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.WorkerType;
import com.zimbra.cs.redolog.RedoLogBlobStore;
import com.zimbra.cs.redolog.RedoLogManager.LoggableOp;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.BlobRecorder;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogReaderService {
    private static RedissonClient client;
    private Map<Integer, RStream<byte[], byte[]>> streams;
    private ExecutorService executorService;
    private static String group;
    private static String consumer;
    private volatile boolean running = false;
    private static DistributedLogReaderService instance = null;
    private RedoLogBlobStore blobStore;
    private boolean externalBlobStore;
    private static Splitter splitter = Splitter.on(",");
    private RScript script;
    private static final String ACK_DEL_SCRIPT =
            "redis.call('xack', KEYS[1], ARGV[1], ARGV[2]); " +
            "return redis.call('xdel', KEYS[1], ARGV[2]);";


    private DistributedLogReaderService() {}

    public static DistributedLogReaderService getInstance() {
        if (instance == null) {
            synchronized(DistributedLogReaderService.class) {
                if (instance == null) {
                    instance = new DistributedLogReaderService();
                }
            }
        }
        return instance;
    }

    public synchronized void startUp() throws ServiceException {
        running = true;
        group = LC.redis_streams_redo_log_group.value();
        consumer = MailboxClusterUtil.getMailboxWorkerName();
        client = RedissonClientHolder.getInstance().getRedissonClient();
        script = client.getScript(StringCodec.INSTANCE);
        streams = new HashMap<>();
        blobStore = RedoLogProvider.getInstance().getRedoLogManager().getBlobStore();
        externalBlobStore = RedoLogProvider.getInstance().getRedoLogManager().hasExternalBlobStore();

        PodInfo podInfo = MailboxClusterUtil.getPodInfo();
        RedoStreamSelector selector = new RedoStreamSelector();
        List<Integer> streamIndexes = selector.getStreamIndexes(podInfo.getIndex());
        if (streamIndexes.isEmpty()) {
            ZimbraLog.redolog.info("%s is not tasked with processing any redolog streams!", podInfo.getName());
            return;
        }
        ZimbraLog.redolog.info("%s will listen on redolog streams %s", podInfo.getName(), streamIndexes);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("DistributedLog-Reader-Thread-%d").build();
        executorService = Executors.newFixedThreadPool(streamIndexes.size(), namedThreadFactory);
        for (int i: streamIndexes) {
            RStream<byte[], byte[]> stream = client.getStream(LC.redis_streams_redo_log_stream_prefix.value()+i, ByteArrayCodec.INSTANCE);
            streams.put(i, stream);
            if (!stream.isExists()) {
                stream.createGroup(group, StreamMessageId.ALL); // stream will be auto-created
                ZimbraLog.redolog.info("created consumer group %s and stream %s", group, stream.getName());
            } else {
                // create group if it doesn't exist on the stream
                StreamInfo<byte[], byte[]> info = stream.getInfo();
                if (info.getGroups() == 0) {
                    stream.createGroup(group, StreamMessageId.ALL);
                    ZimbraLog.redolog.info("created consumer group %s for existing stream %s", group, stream.getName());
                }
            }
            executorService.execute(new LogMonitor(stream));
        }
    }

    class LogMonitor implements Runnable {
        private RStream<byte[], byte[]> stream;

        public LogMonitor(RStream<byte[], byte[]> stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            ZimbraLog.redolog.info("Started Log queue monitoring thread %s for %s", Thread.currentThread().getName(), stream.getName());
            while (running) {
                int blockSecs = LC.redis_redolog_stream_read_timeout_secs.intValue();
                int count = LC.redis_redolog_stream_max_items_per_read.intValue();
                Map<StreamMessageId, Map<byte[], byte[]>> logs = stream.readGroup(group, consumer, count, blockSecs, TimeUnit.SECONDS, StreamMessageId.NEVER_DELIVERED);
                if (logs == null || logs.isEmpty()) {
                    ZimbraLog.redolog.debug("no redo entries found after waiting on stream for %s seconds", blockSecs);
                    continue;
                }
                ZimbraLog.redolog.debug("received %s stream entries: %s", logs.size(), logs.keySet());

                if (logs.size() == 1) {
                    // only one entry, so process it directly
                    StreamMessageId messageId = logs.keySet().iterator().next();
                    Map<byte[], byte[]> data = logs.remove(messageId);
                    handleOp(messageId, data);
                } else {
                    // process in StreamMessageId order to ensure proper redolog ordering
                    ArrayList<StreamMessageId> messageIds = new ArrayList<>(logs.keySet());
                    messageIds.sort(SORT_BY_STREAM_ID);
                    for (StreamMessageId messageId: messageIds) {
                        Map<byte[], byte[]> data = logs.remove(messageId);
                        handleOp(messageId, data);
                    }
                }
            }
        }

        private boolean shouldLogOp(MailboxOperation op, MailboxOperation parentOp) {
            if (!externalBlobStore) {
                return true; //log everything if not using external redolog blob store
            }
            switch (op) {
            case StoreIncomingBlob:
                // StoreIncomingBlob operations shouldn't be logged with external blob stores,
                // but we guard for it here anyways
                return false;
            case AbortTxn:
            case CommitTxn:
                if (parentOp == null) {
                    return true;
                } else {
                    return parentOp != MailboxOperation.StoreIncomingBlob;
                }
            default:
                return true;
            }
        }

        private void handleOp(StreamMessageId messageId, Map<byte[], byte[]> fields) {
            InputStream payload = null;
            long timestamp = 0;
            int mboxId = 0;
            int opType = 0;
            int parentOpType = 0;
            TransactionId txnId = null;
            long submitTime = 0;

            InputStream blobPayload = null;
            String blobDigest = null;
            long blobSize = 0;
            List<Integer> mboxIds = null;

            /*
             * We can't use map.get() here because keys are byte arrays, which are compared by reference (not equality).
             * Instead we iterate over the map and compare to the known keys using Arrays.equals().
             *
             * Tried using redisson's CompositeCodec to specify different codecs for keys vs values (StringCodec and ByteArrayCodec, respectively),
             * but while XADD respects the key/value codecs, XREADGROUP doesn't, leading to a cast error on the stream consumer.
             */
            for (Map.Entry<byte[], byte[]> entry: fields.entrySet()) {
                byte[] key = entry.getKey();
                byte[] val = entry.getValue();
                if (Arrays.equals(key, DistributedLogWriter.F_DATA)) {
                    payload = new ByteArrayInputStream(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_TIMESTAMP)) {
                    timestamp = Longs.fromByteArray(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_MAILBOX_ID)) {
                    mboxId = Ints.fromByteArray(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_OP_TYPE)) {
                    opType = Ints.fromByteArray(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_PARENT_OP_TYPE)) {
                    parentOpType = Ints.fromByteArray(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_SUBMIT_TIME)) {
                    submitTime = Longs.fromByteArray(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_TXN_ID)) {
                    String txnStr = new String(val, Charsets.UTF_8);
                    try {
                        txnId = TransactionId.decodeFromString(txnStr);
                    } catch (ServiceException e) {
                        ZimbraLog.redolog.error("unable to decode transaction ID '%s' from redis stream", txnStr, e);
                        ackAndDelete(messageId, stream.getName());
                        return;
                    }
                } else if (Arrays.equals(key, DistributedLogWriter.F_BLOB_DATA)) {
                    blobPayload = new ByteArrayInputStream(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_BLOB_DIGEST)) {
                    blobDigest = new String(val, Charsets.UTF_8);
                } else if (Arrays.equals(key, DistributedLogWriter.F_BLOB_SIZE)) {
                    blobSize = Longs.fromByteArray(val);
                } else if (Arrays.equals(key, DistributedLogWriter.F_BLOB_REFS)) {
                    String mboxIsStr = new String(val, Charsets.UTF_8);
                    mboxIds = splitter.splitToList(mboxIsStr).stream().mapToInt(n -> Integer.parseInt(n)).boxed().collect(Collectors.toList());
                }
            }

            MailboxOperation mailboxOperation = MailboxOperation.fromInt(opType);
            MailboxOperation parentMailboxOperation = null;
            if (parentOpType > 0) {
                parentMailboxOperation = MailboxOperation.fromInt(parentOpType);
            }
            RedoableOp op = new LoggableOp(mailboxOperation, txnId, payload, timestamp, mboxId);
            long queued = System.currentTimeMillis() - submitTime;
            boolean logOp = shouldLogOp(mailboxOperation, parentMailboxOperation);
            long blobStoreDuration = -1;
            try {
                if (logOp) {
                    op.log(true);
                }
                if (blobPayload != null) {
                    long start = System.currentTimeMillis();
                    blobStore.logBlob(blobPayload, blobSize, blobDigest, mboxIds);
                    blobStoreDuration = System.currentTimeMillis() - start;
                }
            } catch (ServiceException | IOException e) {
                ZimbraLog.redolog.error("error processing redolog entry %s (txnId=%s)!", mailboxOperation, txnId, e);
            } finally {
                ackAndDelete(messageId, stream.getName());
                if (ZimbraLog.redolog.isDebugEnabled()) {
                    if (blobPayload != null) {
                        ZimbraLog.redolog.debug("processed streamId=%s, opTimestamp=%s, mboxId=%s, op=%s, txnId=%s, queued=%sms, blobStore=%sms", messageId, timestamp,
                                mboxId, mailboxOperation, txnId, queued, blobStoreDuration);
                    } else {
                        ZimbraLog.redolog.debug("processed streamId=%s, opTimestamp=%s, mboxId=%s, op=%s, txnId=%s, queued=%sms", messageId, timestamp,
                                mboxId, mailboxOperation, txnId, queued);
                    }
                }
                fields.clear();
            }
        }

        private void ackAndDelete(StreamMessageId id, String streamName) {
            List<Object> keys = Arrays.<Object>asList(streamName);
            script.eval(Mode.READ_WRITE, ACK_DEL_SCRIPT, ReturnType.INTEGER, keys, group, id.toString());
        }
    }

    private static final Comparator<StreamMessageId> SORT_BY_STREAM_ID = new Comparator<StreamMessageId>() {

        @Override
        public int compare(StreamMessageId id1, StreamMessageId id2) {
            return id1.getId0() == id2.getId0() ?
                    Long.compare(id1.getId1(), id2.getId1()) :
                        Long.compare(id1.getId0(), id2.getId0());
        }
    };

    public static class RedoStreamSelector {

        private int numStreams;
        private int numWorkers;
        private Map<Integer, Integer> streamPodMap = new HashMap<>();
        private Map<Integer, List<Integer>> streamsByPod = new HashMap<>();
        private PodInfo currentPod;
        private HashFunction hasher;
        private boolean hasExternalBlobStore;
        private static final int DEFAULT_STREAM_INDEX = 0;

        public RedoStreamSelector() {
            this(LC.redis_num_redolog_streams.intValue(), LC.num_backup_restore_workers.intValue(),
                    RedoLogProvider.getInstance().getRedoLogManager().hasExternalBlobStore());
        }

        public RedoStreamSelector(int numStreams, int numWorkers) {
            this(numStreams, numWorkers, true);
        }

        public RedoStreamSelector(int numStreams, int numWorkers, boolean hasExternalBlobStore) {
            this.numStreams = numStreams;
            this.numWorkers = numWorkers;
            this.currentPod = MailboxClusterUtil.getPodInfo();
            hasher = Hashing.murmur3_128();
            initStreamMapping();
            this.hasExternalBlobStore = hasExternalBlobStore;
        }

        private void initStreamMapping() {
            if (numWorkers > numStreams) {
                ZimbraLog.redolog.warn("numWorkers > numStreams (%s>%s), not all zmc-backup-restore workers will be used!", numWorkers, numStreams);
            }
            int workerNum=0;
            for (int streamNum = 0; streamNum < numStreams; streamNum++) {
                streamPodMap.put(streamNum, workerNum);
                streamsByPod.computeIfAbsent(workerNum, k -> new ArrayList<>()).add(streamNum);
                workerNum++;
                if (workerNum >= numWorkers) {
                    workerNum = 0;
                }
            }
        }

        public List<Integer> getStreamIndexes(int podIndex) {
            return streamsByPod.getOrDefault(podIndex, Collections.emptyList());
        }

        public List<Integer> getActivePodIndexes() {
            return new ArrayList<>(streamsByPod.keySet());
        }

        public int getStreamIndex(RedoableOp op) {
            String acctId = op.getAccountId();
            if (acctId != null) {
                return getStreamIndex(acctId);
            } else if (hasExternalBlobStore && op instanceof CommitTxn) {
                // StoreIncomingBlob  commit ops for a given digest should for a go to the same stream, so that they are
                // processed serially on the consumer side. This is to avoid a race condition in updating blob references,
                // in case the BlobReferenceManager implementation is not atomic.
                CommitTxn commit = (CommitTxn) op;
                BlobRecorder blobData = commit.getBlobRecorder();
                if (blobData != null && blobData.getBlobDigest() != null) {
                    return getStreamIndex(blobData.getBlobDigest());
                } else {
                    // temp warnings
                    ZimbraLog.redolog.warn("CommitTxn for %s doesn't have blob data! sending to stream 0", op.getTxnOpCode());
                    return DEFAULT_STREAM_INDEX;
                }
            } else {
                ZimbraLog.redolog.warn("%s doesn't have an accountId! sending to stream 0", op.getOperation());
                return DEFAULT_STREAM_INDEX;
            }
        }

        private int getStreamIndex(String str) {
            return Math.abs(hasher.hashString(str, Charsets.UTF_8).asInt()) % numStreams;
        }

        public int getBackupPodForAccount(NamedEntry acct) {
            int streamIndex = getStreamIndex(acct.getId());
            return streamPodMap.get(streamIndex);
        }

        public boolean isLocal(NamedEntry entry) {
            if (currentPod.getType() == WorkerType.BACKUP_RESTORE) {
                return getBackupPodForAccount(entry) == currentPod.getIndex();
            } else {
                return false;
            }
        }
    }
}

