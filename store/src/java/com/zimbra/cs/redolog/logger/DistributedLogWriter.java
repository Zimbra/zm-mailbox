package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import com.google.common.io.ByteStreams;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.redolog.RedoLogManager.LocalRedoOpContext;
import com.zimbra.cs.redolog.RedoLogManager.RedoOpContext;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogWriter implements LogWriter {

    private static RedissonClient client;
    private RStream<String, String> stream;

    public static final String F_DATA = "d";
    public static final String F_TIMESTAMP = "t";
    public static final String F_MAILBOX_ID = "m";
    public static final String F_OP_TYPE = "p";

    public DistributedLogWriter() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
        stream = client.getStream(LC.redis_streams_redo_log_stream.value(), StringCodec.INSTANCE);
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void log(RedoOpContext context, InputStream data, boolean synchronous) throws IOException {
        byte[] payload = ByteStreams.toByteArray(data);
        Map<String, String> fields = new HashMap<>();
        fields.put(F_DATA, new String(payload));
        fields.put(F_TIMESTAMP, String.valueOf(context.getOpTimestamp()));
        fields.put(F_MAILBOX_ID, String.valueOf(context.getOpMailboxId()));
        fields.put(F_OP_TYPE, String.valueOf(context.getOperationType().getCode()));
        stream.addAll(fields);
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException {
        log(new LocalRedoOpContext(op), data, synchronous);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public long getSize() throws IOException {
        return stream.sizeInMemory();
    }

    @Override
    public long getCreateTime() {
        return 0;
    }

    @Override
    public long getLastLogTime() {
        return 0;
    }

    @Override
    public boolean isEmpty() throws IOException {
        if (stream.sizeInMemory() == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean exists() {
        return stream.isExists();
    }

    @Override
    public String getAbsolutePath() {
        return null;
    }

    @Override
    public boolean renameTo(File dest) {
        return false;
    }

    @Override
    public boolean delete() throws IOException {
        return stream.delete();
    }

    @Override
    public File rollover(LinkedHashMap activeOps) throws IOException {
        return null;
    }

    @Override
    public long getSequence() {
        return 0;
    }
}
