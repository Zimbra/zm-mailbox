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
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.StringCodec;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.RedissonClientHolder;
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
        consumer = "log-reader-c1";
        client = RedissonClientHolder.getInstance().getRedissonClient();
        stream = client.getStream(LC.redis_streams_redo_log_stream.value(), StringCodec.INSTANCE);
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
                Map<StreamMessageId, Map<String, String>> logs = stream.readGroup(group, consumer,1, 0, TimeUnit.SECONDS, StreamMessageId.NEVER_DELIVERED);
                for(Map.Entry<StreamMessageId, Map<String, String>> m:logs.entrySet()){  
                    Map<String, String> fields = (Map<String, String>) m.getValue();
                    for(Map.Entry<String, String> m1:fields.entrySet()){ 
                        InputStream targetStream = new ByteArrayInputStream(m1.getValue().getBytes());
                        try {
                            fileWriter.log(targetStream, true);
                            stream.ack(group, (StreamMessageId) m.getKey());
                        } catch (IOException e) {
                            ZimbraLog.redolog.error("Failed to log data using filewriter", e);
                        }
                        ZimbraLog.redolog.debug("Key:%s Value:%s", m1.getKey(), m1.getValue());
                    }
                }
            }
        }
        
    }
}
