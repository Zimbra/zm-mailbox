package com.zimbra.cs.redolog.logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamInfo;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.StringCodec;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.redolog.RedoLogManager.RedoOpContext;
import com.zimbra.cs.redolog.RedoLogManager.DistributedRedoOpContext;;
import com.zimbra.cs.redolog.RedoLogProvider;

public class DistributedLogReaderService {
    private static RedissonClient client;
    private RStream<String, String> stream;
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
        stream = client.getStream(LC.redis_streams_redo_log_stream.value(), StringCodec.INSTANCE);
        if (!stream.isExists()) {
            stream.createGroup(group, StreamMessageId.ALL); // stream will be auto-created
            ZimbraLog.redolog.info("created consumer group %s and stream %s", group, stream.getName());
        } else {
            // create group if it doesn't exist on the stream
            StreamInfo<String, String> info = stream.getInfo();
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
            while (running) {
                ZimbraLog.redolog.debug("Iterating %s", Thread.currentThread().getName());
                int blockSecs = LC.redis_redolog_stream_read_timeout_secs.intValue();
                int count = LC.redis_redolog_stream_max_items_per_read.intValue();
                Map<StreamMessageId, Map<String, String>> logs = stream.readGroup(group, consumer, count, blockSecs, TimeUnit.SECONDS, StreamMessageId.NEVER_DELIVERED);
                if (logs == null || logs.isEmpty()) {
                    ZimbraLog.redolog.debug("no redo entries found after waiting on stream for %s seconds", blockSecs);
                    continue;
                }
                for(Map.Entry<StreamMessageId, Map<String, String>> m:logs.entrySet()){
                    StreamMessageId messageId = m.getKey();
                    Map<String, String> fields = m.getValue();
                    String dataStr = fields.get(DistributedLogWriter.F_DATA);
                    String timestampStr = fields.get(DistributedLogWriter.F_TIMESTAMP);
                    String mboxIdStr = fields.get(DistributedLogWriter.F_MAILBOX_ID);
                    String typeStr = fields.get(DistributedLogWriter.F_OP_TYPE);
                    InputStream targetStream = new ByteArrayInputStream(dataStr.getBytes());
                    long timestamp = Long.valueOf(timestampStr);
                    int mboxId = Integer.valueOf(mboxIdStr);
                    MailboxOperation opType = MailboxOperation.fromInt(Integer.valueOf(typeStr));
                    RedoOpContext context = new DistributedRedoOpContext(timestamp, mboxId, opType);
                    ZimbraLog.redolog.debug("received streamId=%s, timestamp=%s, mboxId=%s, op=%s", messageId, timestamp, mboxId, opType);
                    try {
                        fileWriter.log(context, targetStream, true);
                        stream.ack(group, messageId);
                    } catch (IOException e) {
                        ZimbraLog.redolog.error("Failed to log data using filewriter", e);
                    }
                }
            }
        }

    }
}
