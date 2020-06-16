package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.zimbra.cs.redolog.logger.DistributedLogReaderService.RedoStreamSelector;
import com.zimbra.cs.redolog.op.BlobRecorder;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;

public class DistributedLogWriter implements LogWriter {

    private static RedissonClient client;
    private static int streamsCount;
    private List<RStream<byte[], byte[]>> streams;

    public static final byte[] F_DATA = "d".getBytes();
    public static final byte[] F_TIMESTAMP = "t".getBytes();
    public static final byte[] F_MAILBOX_ID = "m".getBytes();
    public static final byte[] F_OP_TYPE = "o".getBytes();
    public static final byte[] F_TXN_ID = "i".getBytes();
    public static final byte[] F_PARENT_OP_TYPE = "p".getBytes();
    public static final byte[] F_SUBMIT_TIME = "s".getBytes();
    public static final byte[] F_BLOB_DATA = "b".getBytes();
    public static final byte[] F_BLOB_DIGEST = "g".getBytes();
    public static final byte[] F_BLOB_SIZE = "l".getBytes();
    public static final byte[] F_BLOB_REFS = "r".getBytes();

    private Map<TransactionId, CountDownLatch> pendingOps = new HashMap<>();
    private boolean externalBlobStore;
    private Joiner joiner = Joiner.on(",");
    private RedoStreamSelector streamSelector;

    public DistributedLogWriter() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
        streamsCount = LC.redis_num_redolog_streams.intValue();
        streams = new ArrayList<RStream<byte[], byte[]>>(streamsCount);

        ZimbraLog.redolog.info("DistributedLogWriter streamsCount: %d", streamsCount);

        for (int i=0; i<streamsCount ; i++) {
            streams.add(client.getStream(LC.redis_streams_redo_log_stream_prefix.value()+i, ByteArrayCodec.INSTANCE));
        }

        externalBlobStore = RedoLogProvider.getInstance().getRedoLogManager().hasExternalBlobStore();
        streamSelector = new RedoStreamSelector();
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    private void addBlobDataFields(Map<byte[], byte[]> fields, CommitTxn commit) throws IOException {
        BlobRecorder operationBlobData = commit.getBlobRecorder();
        if (operationBlobData == null) {
            return;
        }
        InputStream blobStream = operationBlobData.getBlobInputStream();
        if (blobStream == null) {
            return;
        }
        long blobSize = operationBlobData.getBlobSize();
        String digest = operationBlobData.getBlobDigest();
        Set<Integer> mboxIds = operationBlobData.getReferencedMailboxIds();
        if (ZimbraLog.redolog.isDebugEnabled()) {
            ZimbraLog.redolog.debug("adding blob data to stream during commit of %s txnId=%s, digest=%s, size=%s, ids=%s", commit.getTxnOpCode(), commit.getTransactionId(), digest, blobSize, mboxIds);
        }
        String mboxIdsCsv = joiner.join(operationBlobData.getReferencedMailboxIds());
        fields.put(F_BLOB_DATA, ByteStreams.toByteArray(blobStream));
        fields.put(F_BLOB_DIGEST, digest.getBytes(Charsets.UTF_8));
        fields.put(F_BLOB_REFS, mboxIdsCsv.getBytes(Charsets.UTF_8));
        fields.put(F_BLOB_SIZE, Longs.toByteArray(blobSize));
    }

    @Override
    public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException {
        if (externalBlobStore && op.getOperation() == MailboxOperation.StoreIncomingBlob) {
            // we don't actually send StoreIncomingBlob operations to the backup pods if an external redolog blob store
            // is configured. Its CommitTxn does get logged, which contains the actual blob data to be
            // stored in the blob store.
            // We are essentially hijacking the StoreIncomingBlob mechanism to log shared blobs to the blob store
            // only if the transaction is successful.
            return;
        }

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

        // If logging a commit/abort transaction, also log the parent operation type.
        // This is because, when using an external redolog blob store, some operations aren't actually logged to the redolog file.
        if (op.isEndMarker()) {
            fields.put(F_PARENT_OP_TYPE, Ints.toByteArray(op.getTxnOpCode().getCode()));
        }

        if (externalBlobStore && op instanceof CommitTxn) {
            // Blob data for redolog operations involving blobs is sent as part of the CommitTxn, so that
            // aborted operations don't have to then revert storing the blob.
            CommitTxn commit = (CommitTxn) op;
            addBlobDataFields(fields, commit);
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

        int streamIndex = streamSelector.getStreamIndex(op);
        ZimbraLog.redolog.info("sending %s txnId=%s mboxId=%s to stream %s", op.getOperation(), op.getTransactionId(), op.getMailboxId(), streamIndex);
        RFuture<StreamMessageId> future = streams.get(streamIndex).addAllAsync(fields);
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
