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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;
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

    protected abstract void storeBlobData(InputStream in, long size, String digest) throws ServiceException, IOException;

    protected abstract void deleteBlobData(String digest) throws ServiceException;

    public void logBlob(InputStream in, long size, String digest, int mboxId) throws ServiceException, IOException {
        logBlob(in, size, digest, Arrays.asList(new Integer(mboxId)));
    }

    public void logBlob(InputStream in, long size, String digest, List<Integer> mboxIds) throws ServiceException, IOException {
        if (!refManager.addRefs(digest, mboxIds)) {
            try {
                storeBlobData(in, size, digest);
            } catch (IOException | ServiceException e) {
                ZimbraLog.redolog.error("unable to store redolog blob! digest=%s", digest, e);
            } finally {
                ByteUtil.closeStream(in);
            }
            ZimbraLog.redolog.debug("uploaded blob with digest=%s and mapped to mailboxes %s", digest, mboxIds);
        } else {
            ZimbraLog.redolog.debug("added mailboxes %s to existing redolog blob digest=%s", mboxIds, digest);
        }
    }

    public void derefRedoLogFile(File file) throws ServiceException, IOException {
        for (BlobReferences refs: refManager.getBlobRefs(file)) {
            String digest = refs.getDigest();
            Collection<Integer> mboxIds = refs.getMailboxIds();
            if (refManager.removeRefs(digest, mboxIds)) {
                deleteBlobData(digest);
                ZimbraLog.redolog.debug("deleted redolog blob with digest=%s (no more references)", digest);
            } else {
                ZimbraLog.redolog.debug("unlinked redolog blob with digest=%s (more references remain)", digest);
            }
        }
    }

    protected static abstract class BlobReferenceManager {

        /**
         * Link the mailbox IDs to the specified digest.
         * @return true if the blob already exists in the backend; false otherwise
         * @throws ServiceException
         */
        public abstract boolean addRefs(String digest, Collection<Integer> mboxIds) throws ServiceException, IOException;

        /**
         * Unlink the mailbox IDs from the specified digest.
         * @return true if the blob has no more references, false otherwise
         * @throws ServiceException
         */
        public abstract boolean removeRefs(String digest, Collection<Integer> mboxIds) throws ServiceException, IOException;

        /**
         * Subclasses can override, in case we track mappings of redo files to their blobs externally
         */
        public Iterable<BlobReferences> getBlobRefs(File file) throws IOException {
            Map<String, BlobReferences> mappings = new HashMap<>();
            FileLogReader reader = new FileLogReader(file);
            reader.open();
            long seq = reader.getHeader().getSequence();
            ZimbraLog.backup.debug("finding blob references for redolog sequence %s (%s)", seq, file.getName());
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
            ZimbraLog.backup.debug("found mappings for %s digests", mappings.size(), file.getName());
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

    public static interface Factory {
        public RedoLogBlobStore getRedoLogBlobStore() throws ServiceException;
    }
}
