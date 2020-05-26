package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataSource;
import javax.activation.FileDataSource;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.op.BlobRecorder;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.store.Blob;

public abstract class RedoLogBlobStore {

    private BlobReferenceManager refManager;
    private static Factory factory;

    public RedoLogBlobStore(BlobReferenceManager refManager) {
        this.refManager = refManager;
    }

    public static synchronized Factory getFactory() {
        if (factory == null) {
            setFactory(LC.zimbra_class_redolog_blob_store_factory.value());
        }
        return factory;
    }

    public static final void setFactory(String factoryClassName) {
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = ExtensionUtil.findClass(factoryClassName)
                            .asSubclass(Factory.class);
                } catch (ClassNotFoundException cnfe) {
                    ZimbraLog.redolog.error("cannot instantiate specified RedoLogBlobStore factory class %s! Defaulting to RedoOpBlobStore", factoryClassName);
                    factoryClass = RedoOpBlobStore.Factory.class;
                }
            }
        } catch (ClassCastException cce) {
            ZimbraLog.redolog.error("cannot instantiate specified RedoLogBlobStore factory class %s!", factoryClassName);
        }
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ZimbraLog.redolog.error("cannot instantiate specified RedoLogBlobStore factory class %s!", factoryClassName);
        }
        ZimbraLog.redolog.info("Using RedoLogBlobStore factory %s", factory.getClass().getDeclaringClass().getSimpleName());
    }

    public abstract Blob fetchBlob(String identifier) throws ServiceException, IOException;

    protected abstract void storeBlobData(InputStream in, long size, String digest) throws IOException;

    protected abstract void deleteBlobData(String digest);

    public PendingRedoBlobOperation logBlob(Mailbox mbox, Blob blob, long size) throws ServiceException, IOException {
     // wrap in FileDataSource so that we can read the same InputStream twice
        return logBlob(mbox, new FileDataSource(blob.getFile()).getInputStream(), size, blob.getDigest(), Arrays.asList(mbox.getId()));
    }

    public PendingRedoBlobOperation logBlob(Mailbox mbox, Blob blob, long size, List<Integer> mboxIds) throws ServiceException, IOException {
        // wrap in FileDataSource so that we can read the same InputStream twice
        return logBlob(mbox, new FileDataSource(blob.getFile()).getInputStream(), size, blob.getDigest(), mboxIds);
    }

    public PendingRedoBlobOperation logBlob(Mailbox mbox, InputStream in, long size, String digest) throws ServiceException, IOException {
        return logBlob(mbox, in, size, digest, Arrays.asList(mbox.getId()));
    }

    public PendingRedoBlobOperation logBlob(Mailbox mbox, DataSource in, long size, String digest) throws ServiceException, IOException {
        return logBlob(mbox, in.getInputStream(), size, digest);
    }

    protected PendingRedoBlobOperation logBlob(Mailbox mbox, InputStream in, long size, String digest, List<Integer> mboxIds) throws ServiceException, IOException {
        return new ExternalRedoBlobOperation(in, size, digest, mboxIds);
    }

    public void derefRedoLogFile(File file) throws IOException {
        for (BlobReferences refs: refManager.getBlobRefs(file)) {
            String digest = refs.getDigest();
            Collection<Integer> mboxIds = refs.getMailboxIds();
            if (refManager.removeRefs(digest, mboxIds)) {
                deleteBlobData(digest);
                ZimbraLog.redolog.info("deleted redolog blob with digest=%s (no more references)", digest);
            } else {
                ZimbraLog.redolog.info("unlinked redolog blob with digest=%s (more references remain)", digest);
            }
        }
    }

    protected static abstract class BlobReferenceManager {

        /**
         * Link the mailbox IDs to the specified digest.
         * @return true if the blob already exists in the backend; false otherwise
         */
        public abstract boolean addRefs(String digest, Collection<Integer> mboxIds);

        /**
         * Unlink the mailbox IDs from the specified digest.
         * @return true if the blob has no more references, false otherwise
         */
        public abstract boolean removeRefs(String digest, Collection<Integer> mboxIds);

        /**
         * Subclasses can override, in case we track mappings of redo files to their blobs externally
         */
        public Iterable<BlobReferences> getBlobRefs(File file) throws IOException {
            Map<String, BlobReferences> mappings = new HashMap<>();
            FileLogReader reader = new FileLogReader(file);
            reader.open();
            long seq = reader.getHeader().getSequence();
            ZimbraLog.backup.info("finding blob references for redolog sequence %s (%s)", seq, file.getName());
            RedoableOp op = null;
            while ((op = reader.getNextOp()) != null) {
                if (op instanceof BlobRecorder) {
                    String digest = ((BlobRecorder) op).getBlobDigest();
                    if (digest != null) {
                        int mboxId = op.getMailboxId();
                        mappings.computeIfAbsent(digest, k -> new BlobReferences(k)).addMapping(mboxId);
                    }
                }
            }
            ZimbraLog.backup.info("found mappings for %s digests", mappings.size(), file.getName());
            return mappings.values();
        }
    }

    /**
     * Represents a mapping of one or more mailbox IDs to a given blob digest
     */
    protected static class BlobReferences {

        private String digest;
        private Set<Integer> mboxIds;

        public BlobReferences(String digest) {
            this.digest = digest;
            this.mboxIds = new HashSet<>();
        }

        public String getDigest() {
            return digest;
        }

        public Collection<Integer> getMailboxIds() {
            return mboxIds;
        }

        public void addMapping(int mboxId) {
            this.mboxIds.add(mboxId);
        }
    }

    public abstract class PendingRedoBlobOperation {

        public abstract void commit();

        public abstract void abort();
    }

    public class ExternalRedoBlobOperation extends PendingRedoBlobOperation {

        private String digest;
        private List<Integer> mboxIds;
        private InputStream in;
        private long size;

        public ExternalRedoBlobOperation(InputStream in, long size, String digest, List<Integer> mboxIds) {
            this.in = in;
            this.digest = digest;
            this.mboxIds = mboxIds;
        }

        @Override
        public void commit() {
            // for external redolog blob stores, the actual execution logic happens on during a commit
            if (!refManager.addRefs(digest, mboxIds)) {
                try {
                    storeBlobData(in, size, digest);
                } catch (IOException e) {
                    ZimbraLog.redolog.error("unable to store redolog blob! digest=%s", digest, e);
                } finally {
                    ByteUtil.closeStream(in);
                }
                ZimbraLog.redolog.info("uploaded blob with digest=%s and mapped to mailboxes %s", digest, mboxIds);
            } else {
                ZimbraLog.redolog.info("added mailboxes %s to existing redolog blob digest=%s", mboxIds, digest);
            }
        }

        @Override
        public void abort() {
            // do nothing by default; subclasses can override if necessary
        }
    }

    public static interface Factory {
        public RedoLogBlobStore getRedoLogBlobStore();
    }
}
