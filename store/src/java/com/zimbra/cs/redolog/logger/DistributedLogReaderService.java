package com.zimbra.cs.redolog.logger;

import java.io.ByteArrayInputStream;
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
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.redolog.RedoLogManager.LoggableOp;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogReaderService {
    private static RedissonClient client;
    private static int streamsCount;
    private List<RStream<byte[], byte[]>> streams;
    private ExecutorService executorService;
    private static String group;
    private static String consumer;
    private volatile boolean running = false;
    private static DistributedLogReaderService instance = null;
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
        streamsCount = LC.redis_num_streams.intValue();
        streams = new ArrayList<RStream<byte[], byte[]>>(streamsCount);

        ZimbraLog.redolog.info("Reader service streamsCount %d", streamsCount);

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("DistributedLog-Reader-Thread-%d").build();
        executorService = Executors.newFixedThreadPool(streamsCount, namedThreadFactory);

        for (int i=0; i<streamsCount ; i++) {
            streams.add(client.getStream(LC.redis_streams_redo_log_stream_prefix.value()+i, ByteArrayCodec.INSTANCE));
            if (!streams.get(i).isExists()) {
                streams.get(i).createGroup(group, StreamMessageId.ALL); // stream will be auto-created
                ZimbraLog.redolog.info("created consumer group %s and stream %s", group, streams.get(i).getName());
            } else {
                // create group if it doesn't exist on the stream
                StreamInfo<byte[], byte[]> info = streams.get(i).getInfo();
                if (info.getGroups() == 0) {
                    streams.get(i).createGroup(group, StreamMessageId.ALL);
                    ZimbraLog.redolog.info("created consumer group %s for existing stream %s", group, streams.get(i).getName());
                }
            }
            executorService.execute(new LogMonitor(streams.get(i)));
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

        private void handleOp(StreamMessageId messageId, Map<byte[], byte[]> fields) {
            InputStream payload = null;
            long timestamp = 0;
            int mboxId = 0;
            int opType = 0;
            TransactionId txnId = null;
            long submitTime = 0;
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
                }
            }

            MailboxOperation mailboxOperation = MailboxOperation.fromInt(opType);
            RedoableOp op = new LoggableOp(mailboxOperation, txnId, payload, timestamp, mboxId);
            long queued = System.currentTimeMillis() - submitTime;
            op.log(true);
            ZimbraLog.redolog.debug("processed streamId=%s, opTimestamp=%s, mboxId=%s, op=%s, txnId=%s, queued=%sms", messageId, timestamp,
                    mboxId, mailboxOperation, txnId, queued);
            ackAndDelete(messageId, stream.getName());
            fields.clear();
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

