package com.zimbra.cs.redolog.logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamInfo;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.ByteArrayCodec;

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
import com.zimbra.cs.redolog.RedoLogManager.DistributedRedoOpContext;
import com.zimbra.cs.redolog.RedoLogManager.RedoOpContext;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.TransactionId;

public class DistributedLogReaderService {
    private static RedissonClient client;
    private RStream<byte[], byte[]> stream;
    private ExecutorService executorService;
    private static String group;
    private static String consumer;
    private volatile boolean running = false;
    private static DistributedLogReaderService instance = null;
    private static LogWriter fileWriter = null;

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

    public synchronized void startUp() {
        running = true;
        group = LC.redis_streams_redo_log_group.value();
        consumer = MailboxClusterUtil.getMailboxWorkerName();
        client = RedissonClientHolder.getInstance().getRedissonClient();
        stream = client.getStream(LC.redis_streams_redo_log_stream.value(), ByteArrayCodec.INSTANCE);
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
        fileWriter = RedoLogProvider.getInstance().getRedoLogManager().getCurrentLogWriter();
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("DistributedLog-Reader-Thread-%d").build();
        executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
        executorService.execute(new LogMonitor());
    }

    class LogMonitor implements Runnable {
        @Override
        public void run() {
            ZimbraLog.redolog.info("Started Log queue monitoring thread %s", Thread.currentThread().getName());
            READ_STREAM: while (running) {
                ZimbraLog.redolog.debug("Iterating %s", Thread.currentThread().getName());
                int blockSecs = LC.redis_redolog_stream_read_timeout_secs.intValue();
                int count = LC.redis_redolog_stream_max_items_per_read.intValue();
                Map<StreamMessageId, Map<byte[], byte[]>> logs = stream.readGroup(group, consumer, count, blockSecs, TimeUnit.SECONDS, StreamMessageId.NEVER_DELIVERED);
                if (logs == null || logs.isEmpty()) {
                    ZimbraLog.redolog.debug("no redo entries found after waiting on stream for %s seconds", blockSecs);
                    continue;
                }
                for(Map.Entry<StreamMessageId, Map<byte[], byte[]>> m:logs.entrySet()){
                    StreamMessageId messageId = m.getKey();
                    Map<byte[], byte[]> fields = m.getValue();
                    InputStream payload = null;
                    long timestamp = 0;
                    int mboxId = 0;
                    int opType = 0;
                    TransactionId txnId = null;
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
                        } else if (Arrays.equals(key, DistributedLogWriter.F_TXN_ID)) {
                            String txnStr = new String(val, Charsets.UTF_8);
                            try {
                                txnId = TransactionId.decodeFromString(txnStr);
                            } catch (ServiceException e) {
                                ZimbraLog.redolog.error("unable to decode transaction ID '%s' from redis stream", txnStr, e);
                                continue READ_STREAM;
                            }
                        }
                    }
                    MailboxOperation op = MailboxOperation.fromInt(opType);
                    RedoOpContext context = new DistributedRedoOpContext(timestamp, mboxId, op, txnId);
                    ZimbraLog.redolog.debug("received streamId=%s, opTimestamp=%s, mboxId=%s, op=%s, txnId=%s", messageId, timestamp, mboxId, op, txnId);
                    try {
                        fileWriter.log(context, payload, true);
                        stream.ack(group, messageId);
                    } catch (IOException e) {
                        ZimbraLog.redolog.error("Failed to log data using filewriter", e);
                    }
                }
            }
        }

    }
}
