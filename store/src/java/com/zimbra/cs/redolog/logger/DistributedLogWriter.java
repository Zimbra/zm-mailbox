package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import org.redisson.api.RFuture;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogWriter implements LogWriter {

    private static RedissonClient client;
    private RStream<String, String> stream;
    private RFuture<Void> groupFuture;

    public DistributedLogWriter() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
        stream = client.getStream(LC.redis_streams_redo_log_stream.value(), StringCodec.INSTANCE);
        groupFuture = stream.createGroupAsync(LC.redis_streams_redo_log_group.value());
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void log(InputStream data, boolean synchronous) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while((ch = data.read()) != -1) {
            sb.append((char)ch);
        }
        String str = sb.toString();
        stream.add(LC.redis_streams_redo_log_field.value(), str);
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException {
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
