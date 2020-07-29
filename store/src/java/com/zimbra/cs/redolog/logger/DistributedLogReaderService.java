package com.zimbra.cs.redolog.logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.StringCodec;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil.PodInfo;
import com.zimbra.cs.redolog.BackupHostManager;
import com.zimbra.cs.redolog.RedoLogBlobStore;
import com.zimbra.cs.redolog.RedoLogManager.LoggableOp;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.RedoStreamSelector;
import com.zimbra.cs.redolog.RedoStreamSelector.RedoStreamSpec;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogReaderService {
    private static RedissonClient client;
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
        blobStore = RedoLogProvider.getInstance().getRedoLogManager().getBlobStore();
        externalBlobStore = RedoLogProvider.getInstance().getRedoLogManager().hasExternalBlobStore();

        PodInfo podInfo = MailboxClusterUtil.getPodInfo();
        RedoStreamSelector selector = BackupHostManager.getInstance().getStreamSelector();

        List<RedoStreamSpec> streamSpecs = selector.getStreamsForPod(podInfo);
        if (streamSpecs.isEmpty()) {
            ZimbraLog.redolog.info("%s is not tasked with processing any redolog streams!", podInfo.getName());
            return;
        }
        List<String> streamNames = streamSpecs.stream().map(s -> s.getStreamName()).collect(Collectors.toList());
        ZimbraLog.redolog.info("%s will listen on %s redolog streams: %s", podInfo.getName(), streamNames.size(), streamNames);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("DistributedLog-Reader-Thread-%d").build();
        executorService = Executors.newFixedThreadPool(streamSpecs.size(), namedThreadFactory);
        for (RedoStreamSpec streamSpec: streamSpecs) {

            RStream<byte[], byte[]> stream = streamSpec.getStream();
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
}

