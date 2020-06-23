package com.zimbra.cs.redolog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.op.StoreIncomingBlob;
import com.zimbra.cs.store.Blob;

public class RedoOpBlobStore extends RedoLogBlobStore {

    public RedoOpBlobStore() {
        super(new NoOpReferenceManager());
    }

    private static final Map<String, Blob> replayedBlobs = new HashMap<String, Blob>();

    @Override
    public Blob fetchBlob(String identifier) throws ServiceException {
        return replayedBlobs.get(identifier);
    }

    public void registerBlob(String identifier, Blob blob) {
        replayedBlobs.put(identifier, blob);
    }

    @Override
    protected void storeBlobData(InputStream in, long size, String digest) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deleteBlobData(String digest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingRedoBlobOperation logBlob(Mailbox mbox, Blob blob, long size, List<Integer> mboxIds) throws ServiceException, IOException {
        StoreIncomingBlob storeRedoRecorder = new StoreIncomingBlob(blob.getDigest(), (int) size, mboxIds);
        storeRedoRecorder.start(mbox.getOperationTimestampMillis());
        storeRedoRecorder.setBlobBodyInfo(blob.getFile());
        storeRedoRecorder.log();
        return new StoreIncomingBlobOperation(storeRedoRecorder);
    }


    protected static class NoOpReferenceManager extends RedoLogBlobStore.BlobReferenceManager {

        @Override
        public boolean addRefs(String digest, Collection<Integer> mboxIds) {
            return false;
        }

        @Override
        public boolean removeRefs(String digest, Collection<Integer> mboxIds) {
            return false;
        }
    }

    public static class Factory implements RedoLogBlobStore.Factory {

        @Override
        public RedoLogBlobStore getRedoLogBlobStore() {
            return new RedoOpBlobStore();
        }
    }

    public class StoreIncomingBlobOperation extends PendingRedoBlobOperation {

        private StoreIncomingBlob op;

        public StoreIncomingBlobOperation(StoreIncomingBlob op) {
            this.op = op;
        }

        @Override
        public void commit() throws ServiceException {
            op.commit();

        }

        @Override
        public void abort() {
            op.abort();
        }
    }
}
