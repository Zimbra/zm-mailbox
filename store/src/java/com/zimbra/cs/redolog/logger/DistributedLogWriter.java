package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.redisson.api.RFuture;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.ByteArrayCodec;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.BlobRecorder;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogWriter implements LogWriter {

    private static RedissonClient client;
    private static int streamsCount;
    private List<RStream<byte[], byte[]>> streams;

    public static final byte[] F_DATA = "d".getBytes();
    public static final byte[] F_TIMESTAMP = "t".getBytes();
    public static final byte[] F_MAILBOX_ID = "m".getBytes();
    public static final byte[] F_OP_TYPE = "p".getBytes();
    public static final byte[] F_TXN_ID = "i".getBytes();
    public static final byte[] F_SUBMIT_TIME = "s".getBytes();
    public static final byte[] F_BLOB_DATA = "b".getBytes();
    public static final byte[] F_BLOB_DIGEST = "g".getBytes();
    public static final byte[] F_BLOB_SIZE = "l".getBytes();
    public static final byte[] F_BLOB_REFS = "r".getBytes();

    private Map<TransactionId, CountDownLatch> pendingOps = new HashMap<>();
    private boolean externalBlobStore;
    private Joiner joiner = Joiner.on(",");

    public DistributedLogWriter() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
        streamsCount = LC.redis_num_streams.intValue();
        streams = new ArrayList<RStream<byte[], byte[]>>(streamsCount);

        ZimbraLog.redolog.info("DistributedLogWriter streamsCount: %d", streamsCount);

        for (int i=0; i<streamsCount ; i++) {
            streams.add(client.getStream(LC.redis_streams_redo_log_stream_prefix.value()+i, ByteArrayCodec.INSTANCE));
        }

        externalBlobStore = RedoLogProvider.getInstance().getRedoLogManager().hasExternalBlobStore();
    }

    /*
     * Returns the index of the stream that could be used to send data to the
     * Backup Restore Pod.
     */
    private int getStreamIndex(int mboxId) {
        if (mboxId == RedoableOp.MAILBOX_ID_ALL || mboxId == RedoableOp.UNKNOWN_ID) {
            // if the mailbox ID isn't specified, send it to the first stream
            return 0;
        }
        int streamIndex = mboxId % streamsCount;
        ZimbraLog.redolog.debug("DistributedLogWriter - getStreamIndex mboxId:%d, streamsCount:%d, streamIndex:%d", mboxId, streamsCount, streamIndex);
        return streamIndex;
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException {
        byte[] payload = ByteStreams.toByteArray(data);
        Map<byte[], byte[]> fields = new HashMap<>();
        int mailboxId = op.getMailboxId();
        TransactionId txnId = op.getTransactionId();
        MailboxOperation opType = op.getOperation();
        fields.put(F_DATA, payload);
        fields.put(F_TIMESTAMP, Longs.toByteArray(op.getTimestamp()));
        fields.put(F_MAILBOX_ID, Ints.toByteArray(mailboxId));
        fields.put(F_OP_TYPE, Ints.toByteArray(opType.getCode()));
        fields.put(F_TXN_ID, txnId.encodeToString().getBytes(Charsets.UTF_8));
        fields.put(F_SUBMIT_TIME, Longs.toByteArray(System.currentTimeMillis()));

        if (externalBlobStore && op instanceof BlobRecorder) {
            // log the blob data in a separate field, so that the zmc-backup-restore pods can upload it to the redolog blob store
            BlobRecorder blobRecorder = (BlobRecorder) op;
            InputStream blobStream = blobRecorder.getBlobInputStream();
            if (blobStream != null) {
                long blobSize = blobRecorder.getBlobSize();
                String digest = blobRecorder.getBlobDigest();
                String mboxIds = joiner.join(blobRecorder.getReferencedMailboxIds());
                ZimbraLog.redolog.info("adding blob data to stream for %s txnId=%s, digest=%s, size=%s, ids=%s", opType, txnId, digest, blobSize, mboxIds);
                fields.put(F_BLOB_DATA, ByteStreams.toByteArray(blobStream));
                fields.put(F_BLOB_DIGEST, digest.getBytes(Charsets.UTF_8));
                fields.put(F_BLOB_REFS, mboxIds.getBytes(Charsets.UTF_8));
                fields.put(F_BLOB_SIZE, Longs.toByteArray(blobSize));
            }
        }

        boolean isStartMarker = op.isStartMarker();
        boolean isEndMarker = op.isEndMarker();
        if (isStartMarker) {
            pendingOps.put(txnId, new CountDownLatch(1));
        }
        if (isEndMarker) {
            // We need to make sure that the parent op has been successfully sent to the stream before submitting
            // a commitTxn or abortTxn.
            CountDownLatch latch = pendingOps.get(txnId);
            if (latch != null) {
                try {
                    long waitStart = System.currentTimeMillis();
                    latch.await();
                    if (ZimbraLog.redolog.isDebugEnabled()) {
                        long waited = System.currentTimeMillis() - waitStart;
                        ZimbraLog.redolog.debug("waited %sms for %s to be submitted to the stream before submitting %s (txnId=%s)", waited, op.getTxnOpCode(), opType, txnId);
                    }
                } catch (InterruptedException e1) {}
            }
        }
        long start = System.currentTimeMillis();

        RFuture<StreamMessageId> future = streams.get(getStreamIndex(mailboxId)).addAllAsync(fields);
        future.onComplete((streamId, e) -> {
            long elapsed = System.currentTimeMillis() - start;
            if (isStartMarker) {
                CountDownLatch latch = pendingOps.remove(txnId);
                if (latch != null) {
                    latch.countDown();
                }
            }
            if (e == null) {
                if (ZimbraLog.redolog.isDebugEnabled()) {
                    ZimbraLog.redolog.debug("submitted op %s txnId=%s to redis stream (stream_id=%s) (elapsed=%s)",
                            opType, txnId, streamId, elapsed);
                }
            } else {
                ZimbraLog.redolog.error("error writing op %s txnId=%s to redis stream",
                        opType, txnId, e);
            }
        });
    }

    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCreateTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastLogTime() {
        throw new UnsupportedOperationException();
    }

    /*
     * Returns true if all the configured streams has size < 1,
     * else false.
     * @throws IOException
     */
    @Override
    public boolean isEmpty() throws IOException {
        for (int i=0; i<streamsCount ; i++) {
            if (streams.get(i).size() > 0)
                return false;
        }
        return true;
    }

    /*
     * Returns true if at least one stream exists else return false.
     */
    @Override
    public boolean exists() {
        for (int i=0; i<streamsCount ; i++) {
            if (streams.get(i).isExists())
                return true;
        }
        return false;
    }

    @Override
    public String getAbsolutePath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameTo(File dest) {
        throw new UnsupportedOperationException();
    }

    /*
     * Returns true if all the configured streams gets deleted else false.
     */
    @Override
    public boolean delete() throws IOException {
        boolean returnCode = false;
        for (int i=0; i<streamsCount ; i++) {
            returnCode = streams.get(i).delete();
        }
        return returnCode;
    }

    @Override
    public File rollover(LinkedHashMap activeOps) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSequence() {
        throw new UnsupportedOperationException();
    }
}
