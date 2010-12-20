/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Version;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.io.NullOutputStream;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.IndexHelper;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Lucene index.
 *
 * @author tim
 * @author ysasaki
 */
public final class LuceneIndex {

    /**
     * We don't want to enable StopFilter preserving position increments, which is enabled on or after 2.9, because we
     * want phrases to match across removed stop words.
     */
    public static final Version VERSION = Version.LUCENE_24;

    private static final Semaphore READER_THROTTLE = new Semaphore(LC.zimbra_index_max_readers.intValue());
    private static final Semaphore WRITER_THROTTLE = new Semaphore(LC.zimbra_index_max_writers.intValue());

    private static IndexReadersCache readersCache;

    private final LuceneDirectory luceneDirectory;
    private MailboxIndex mailboxIndex;
    private IndexWriterRef writerRef;

    static void startup() {
        if (DebugConfig.disableIndexing) {
            ZimbraLog.index.info("Indexing is disabled by the localconfig 'debug_disable_indexing' flag");
            return;
        }
        BooleanQuery.setMaxClauseCount(LC.zimbra_index_lucene_max_terms_per_query.intValue());
        readersCache = new IndexReadersCache(
                LC.zimbra_index_reader_cache_size.intValue(),
                LC.zimbra_index_reader_cache_ttl.longValue() * 1000,
                LC.zimbra_index_reader_cache_sweep_frequency.longValue() * 1000);
    }

    static void shutdown() {
        if (DebugConfig.disableIndexing) {
            return;
        }
        readersCache.shutdown();
    }

    /**
     * Returns total bytes written to the filesystem by Lucene - for stats.
     * logging.
     *
     * @return bytes count
     */
    long getBytesWritten() {
        return luceneDirectory.getBytesWritten();
    }

    /**
     * Returns total bytes read from the filesystem by Lucene - for stats
     * logging.
     *
     * @return bytes count
     */
    long getBytesRead() {
        return luceneDirectory.getBytesRead();
    }

    LuceneIndex(MailboxIndex mbidx, String idxParentDir, int mailboxId) throws ServiceException {
        mailboxIndex = mbidx;

        // this must be different from the idxParentDir (see the IMPORTANT comment below)
        String idxPath = idxParentDir + File.separatorChar + '0';

        File parentDirFile = new File(idxParentDir);

        // IMPORTANT!  Don't make the actual index directory (mIdxDirectory) yet!
        //
        // The runtime open-index code checks the existance of the actual index directory:
        // if it does exist but we cannot open the index, we do *NOT* create it under the
        // assumption that the index was somehow corrupted and shouldn't be messed-with....on the
        // other hand if the index dir does NOT exist, then we assume it has never existed (or
        // was deleted intentionally) and therefore we should just create an index.
        if (!parentDirFile.exists()) {
            parentDirFile.mkdirs();
        }

        if (!parentDirFile.canRead()) {
            throw ServiceException.FAILURE("Cannot READ index directory (mailbox=" + mailboxId + " idxPath=" + idxPath + ")", null);
        }
        if (!parentDirFile.canWrite()) {
            throw ServiceException.FAILURE("Cannot WRITE index directory (mailbox=" + mailboxId + " idxPath=" + idxPath + ")", null);
        }

        // the Lucene code does not atomically swap the "segments" and "segments.new"
        // files...so it is possible that a previous run of the server crashed exactly in such
        // a way that we have a "segments.new" file but not a "segments" file.  We we will check here
        // for the special situation that we have a segments.new
        // file but not a segments file...
        File segments = new File(idxPath, "segments");
        if (!segments.exists()) {
            File segments_new = new File(idxPath, "segments.new");
            if (segments_new.exists()) {
                segments_new.renameTo(segments);
            }
        }

        try {
            luceneDirectory = LuceneDirectory.open(new File(idxPath), new SingleInstanceLockFactory());
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to create LuceneDirectory: " + idxPath, e);
        }
    }

    /**
     * Adds the list of documents to the index.
     * <p>
     * If {@code deleteFirst} is false, then we are sure that this item is not
     * already in the index, and so we can skip the check-update step.
     */
    void addDocument(MailItem item, List<IndexDocument> docs, boolean deleteFirst) throws IOException {
        for (IndexDocument doc : docs) {
            // doc can be shared by multiple threads if multiple mailboxes
            // are referenced in a single email
            synchronized (doc) {
                doc.removeSortSubject();
                doc.removeSortName();

                doc.addSortSubject(item.getSortSubject());
                doc.addSortName(item.getSortSender());

                doc.removeMailboxBlobId();
                doc.addMailboxBlobId(item.getId());

                // If this doc is shared by multi threads, then the date might just be wrong,
                // so remove and re-add the date here to make sure the right one gets written!
                doc.removeSortDate();
                doc.addSortDate(item.getDate());

                doc.removeSortSize();
                doc.addSortSize(item.getSize());
                doc.addAll();

                if (deleteFirst) {
                    Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(item.getId()));
                    writerRef.get().updateDocument(term, doc.toDocument());
                } else {
                    writerRef.get().addDocument(doc.toDocument());
                }
            }
        }
    }

    /**
     * Deletes documents.
     * <p>
     * The document count may be more than you expect here, the document may already be deleted and just not be
     * optimized out yet -- some Lucene APIs (e.g. docFreq) will still return the old count until the indexes are
     * optimized.
     */
    void deleteDocuments(List<Integer> ids) throws IOException {
        for (Integer id : ids) {
            Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, id.toString());
            writerRef.get().deleteDocuments(term);
            ZimbraLog.index.debug("Deleted documents id=%d", id);
        }
    }

    /**
     * Deletes this index completely.
     */
    synchronized void deleteIndex() throws IOException {
        assert(writerRef == null);
        ZimbraLog.index.debug("Deleting index %s", luceneDirectory);
        readersCache.remove(this);

        String[] files;
        try {
            files = luceneDirectory.listAll();
        } catch (NoSuchDirectoryException ignore) {
            return;
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to delete index: %s", luceneDirectory, e);
            return;
        }

        for (String file : files) {
            luceneDirectory.deleteFile(file);
        }
    }

    private void enumerateTermsForField(String regex, Term firstTerm, TermEnumInterface callback) throws IOException {
        IndexReaderRef ref = getIndexReaderRef();
        try {
            TermEnum terms = ref.get().terms(firstTerm);
            boolean hasDeletions = ref.get().hasDeletions();

            // HACK!
            boolean stripAtBeforeRegex = false;
            if (callback instanceof DomainEnumCallback)
                stripAtBeforeRegex = true;

            Pattern p = null;
            if (regex != null && regex.length() > 0) {
                p = Pattern.compile(regex);
            }

            do {
                Term cur = terms.term();
                if (cur != null) {
                    if (!cur.field().equals(firstTerm.field())) {
                        break;
                    }

                    boolean skipIt = false;
                    if (p != null) {
                        String compareTo = cur.text();
                        if (stripAtBeforeRegex)
                            if (compareTo.length() > 1 && compareTo.charAt(0) == '@') {
                                compareTo = compareTo.substring(1);
                            }
                        if (!p.matcher(compareTo).matches()) {
                            skipIt = true;
                        }
                    }

                    if (!skipIt) {
                        // NOTE: the term could exist in docs, but they might all be deleted. Unfortunately this means
                        // that we need to actually walk the TermDocs enumeration for this document to see if it is
                        // non-empty
                        if (!hasDeletions || ref.get().termDocs(cur).next()) {
                            callback.onTerm(cur, terms.docFreq());
                        }
                    }
                }
            } while (terms.next());
        } finally {
            ref.dec();
        }
    }

    /**
     * Returns {@code true} if all tokens were expanded or {@code false} if more
     * tokens were available but we hit the specified maximum.
     */
    boolean expandWildcardToken(Collection<String> toRet, String field, String token, int maxToReturn)
            throws IOException {
        // all lucene text should be in lowercase...
        token = token.toLowerCase();

        IndexReaderRef ref = getIndexReaderRef();
        try {
            Term firstTerm = new Term(field, token);
            TermEnum terms = ref.get().terms(firstTerm);

            do {
                Term cur = terms.term();
                if (cur != null) {
                    if (!cur.field().equals(firstTerm.field())) {
                        break;
                    }

                    String curText = cur.text();

                    if (curText.startsWith(token)) {
                        if (toRet.size() >= maxToReturn) {
                            return false;
                        }
                        // we don't care about deletions, they will be filtered later
                        toRet.add(cur.text());
                    } else {
                        if (curText.compareTo(token) > 0) {
                            break;
                        }
                    }
                }
            } while (terms.next());

            return true;
        } finally {
            ref.dec();
        }
    }

    /**
     * Removes from cache.
     */
    void evict() {
        readersCache.remove(this);
    }

    /**
     * @param fieldName Lucene field (e.g. LuceneFields.L_H_CC)
     * @param collection Strings which correspond to all of the domain terms stored in a given field
     */
    void getDomainsForField(String fieldName, String regex, Collection<BrowseTerm> collection) throws IOException {
        regex = Strings.nullToEmpty(regex);
        enumerateTermsForField(regex, new Term(fieldName,""), new DomainEnumCallback(collection));
    }

    /**
     * @param collection Strings which correspond to all of the attachment types in the index
     */
    void getAttachments(String regex, Collection<BrowseTerm> collection) throws IOException {
        regex = Strings.nullToEmpty(regex);
        enumerateTermsForField(regex, new Term(LuceneFields.L_ATTACHMENTS, ""), new TermEnumCallback(collection));
    }

    /**
     * Return the list of objects (e.g. PO, etc) from the index, for SearchBuilder browsing.
     */
    void getObjects(String regex, Collection<BrowseTerm> collection) throws IOException {
        regex = Strings.nullToEmpty(regex);
        enumerateTermsForField(regex, new Term(LuceneFields.L_OBJECTS, ""), new TermEnumCallback(collection));
    }

    /**
     * Caller is responsible for calling {@link IndexSearcherRef#dec()} before allowing it to go out of scope
     * (otherwise a RuntimeException will occur).
     *
     * @return A {@link IndexSearcherRef} for this index.
     * @throws IOException if opening an {@link IndexReader} failed
     */
    IndexSearcherRef getIndexSearcherRef() throws IOException {
        return new IndexSearcherRef(getIndexReaderRef());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("mbox", mailboxIndex.getMailboxId())
            .add("dir", luceneDirectory)
            .toString();
    }

    private IndexReader openIndexReader(boolean tryRepair) throws IOException {
        try {
            return IndexReader.open(luceneDirectory, null, true,
                    LC.zimbra_index_lucene_term_index_divisor.intValue());
        } catch (CorruptIndexException e) {
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexReader(false);
        } catch (AssertionError e) {
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexReader(false);
        }
    }

    private IndexWriter openIndexWriter(boolean create, boolean tryRepair) throws IOException {
        try {
            IndexWriter writer = new IndexWriter(luceneDirectory, mailboxIndex.getAnalyzer(),
                    create, IndexWriter.MaxFieldLength.LIMITED) {
                /**
                 * Redirect Lucene's logging to ZimbraLog.
                 */
                @Override
                public void message(String message) {
                    ZimbraLog.index.debug("IW: %s", message);
                }
            };
            if (ZimbraLog.index.isDebugEnabled()) {
                // Set a dummy PrintStream, otherwise Lucene suppresses logging.
                writer.setInfoStream(new PrintStream(new NullOutputStream()));
            }
            return writer;
        } catch (AssertionError e) {
            unlockIndexWriter();
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexWriter(false, false);
        } catch (CorruptIndexException e) {
            unlockIndexWriter();
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexWriter(false, false);
        }
    }

    private synchronized <T extends Throwable> void repair(T ex) throws T {
        ZimbraLog.index.error("Index corrupted", ex);
        LuceneIndexRepair repair = new LuceneIndexRepair(luceneDirectory);
        try {
            if (repair.repair() > 0) {
                ZimbraLog.index.info("Index repaired, re-indexing is recommended.");
            } else {
                ZimbraLog.index.warn("Unable to repair, re-indexing is required.");
                throw ex;
            }
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to repair, re-indexing is required.", e);
            throw ex;
        }
    }

    private void unlockIndexWriter() {
        try {
            IndexWriter.unlock(luceneDirectory);
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to unlock IndexWriter %s", this, e);
        }
    }

    /**
     * Caller is responsible for calling {@link IndexReaderRef#dec()} before
     * allowing it to go out of scope (otherwise a RuntimeException will occur).
     *
     * @return A {@link IndexReaderRef} for this index.
     * @throws IOException
     */
    private synchronized IndexReaderRef getIndexReaderRef() throws IOException {
        IndexReaderRef ref = readersCache.get(this);
        if (ref != null) {
            if (ref.isStale()) {
                IndexReader oldReader = ref.get();
                IndexReader newReader;
                try {
                    newReader = oldReader.reopen();
                    if (oldReader != newReader) { // reader changed, must close old one
                        ZimbraLog.search.debug("Reopened IndexReader");
                        readersCache.remove(this); // ref--
                        ref.dec();
                        READER_THROTTLE.acquireUninterruptibly();
                        ref = new IndexReaderRef(this, newReader); // ref = 1
                        readersCache.put(this, ref); // ref++
                    }
                } catch (IOException e) {
                    ZimbraLog.search.debug("Failed to reopen IndexReader", e);
                    readersCache.remove(this);
                    ref.dec();
                    ref = null;
                }
            }
            return ref;
        }

        READER_THROTTLE.acquireUninterruptibly();
        IndexReader reader = null;
        try {
            reader = openIndexReader(true);
        } catch (IOException e) {
            // Handle the special case of trying to open a not-yet-created index, by opening for write and immediately
            // closing. Index directory should get initialized as a result.
            File indexDir = luceneDirectory.getFile();
            if (isEmptyDirectory(indexDir)) {
                // create an empty index
                IndexWriter writer = new IndexWriter(luceneDirectory, null, true, IndexWriter.MaxFieldLength.LIMITED);
                writer.close();
                reader = openIndexReader(false);
            } else {
                throw e;
            }
        } finally {
            if (reader == null) {
                READER_THROTTLE.release();
            }
        }

        ref = new IndexReaderRef(this, reader); // ref = 1
        readersCache.put(this, ref); // ref++
        return ref;
    }

    /**
     * Check to see if it is OK for us to create an index in the specified directory.
     *
     * @param dir index directory
     * @return TRUE if the index directory is empty or doesn't exist,
     *         FALSE if the index directory exists and has files in it or if we cannot list files in the directory
     */
    private boolean isEmptyDirectory(File dir) {
        if (!dir.exists()) { // dir doesn't even exist yet.  Create the parents and return true
            dir.mkdirs();
            return true;
        }

        // Empty directory is okay, but a directory with any files implies index corruption.
        File[] files = dir.listFiles();

        // if files is null here, we are likely running into file permission issue
        if (files == null) {
            ZimbraLog.index.warn("Could not list files in directory %s", dir.getAbsolutePath());
            return false;
        }

        int num = 0;
        for (File file : files) {
            String fname = file.getName();
            if (file.isDirectory() && (fname.equals(".") || fname.equals(".."))) {
                continue;
            }
            num++;
        }
        return (num <= 0);
    }

    synchronized void beginWrite() throws IOException {
        if (writerRef != null) {
            writerRef.inc();
        } else {
            WRITER_THROTTLE.acquireUninterruptibly();
            try {
                writerRef = openWriter();
            } finally {
                if (writerRef == null) {
                    WRITER_THROTTLE.release();
                }
            }
        }
    }

    synchronized void endWrite() throws IOException {
        commitWriter();
        readersCache.stale(this); // let the reader reopen
    }

    private IndexWriterRef openWriter() throws IOException {
        assert(Thread.holdsLock(this));

        LuceneConfig config = new LuceneConfig(mailboxIndex.mailbox.index.getBatchThreshold() > 0);

        IndexWriter writer;
        try {
            writer = openIndexWriter(false, true);
        } catch (IOException e) {
            // the index (the segments* file in particular) probably didn't exist
            // when new IndexWriter was called in the try block, we would get a
            // FileNotFoundException for that case. If the directory is empty,
            // this is the very first index write for this this mailbox (or the
            // index might be deleted), the FileNotFoundException is benign.
            if (isEmptyDirectory(luceneDirectory.getFile())) {
                writer = openIndexWriter(true, false);
            } else {
                throw e;
            }
        }

        writer.setMergeScheduler(new MergeScheduler());
        writer.setMaxBufferedDocs(config.maxBufferedDocs);
        writer.setRAMBufferSizeMB(config.ramBufferSizeKB / 1024.0);
        writer.setMergeFactor(config.mergeFactor);

        if (config.mergePolicy) {
            LogDocMergePolicy policy = new LogDocMergePolicy(writer);
            writer.setMergePolicy(policy);
            policy.setUseCompoundDocStore(config.useCompoundFile);
            policy.setUseCompoundFile(config.useCompoundFile);
            policy.setMergeFactor(config.mergeFactor);
            policy.setMinMergeDocs((int) config.minMerge);
            if (config.maxMerge != Integer.MAX_VALUE) {
                policy.setMaxMergeDocs((int) config.maxMerge);
            }
        } else {
            LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy(writer);
            writer.setMergePolicy(policy);
            policy.setUseCompoundDocStore(config.useCompoundFile);
            policy.setUseCompoundFile(config.useCompoundFile);
            policy.setMergeFactor(config.mergeFactor);
            policy.setMinMergeMB(config.minMerge / 1024.0);
            if (config.maxMerge != Integer.MAX_VALUE) {
                policy.setMaxMergeMB(config.maxMerge / 1024.0);
            }
        }

        return new IndexWriterRef(this, writer);
    }

    private void commitWriter() throws IOException {
        assert(Thread.holdsLock(this));
        assert(writerRef != null);

        ZimbraLog.index.debug("Commit IndexWriter");

        MergeTask task = new MergeTask(writerRef);
        if (IndexHelper.isIndexThread()) { // batch index running in background
            task.exec();
        } else { // catch-up index running in foreground
            boolean success = false;
            try {
                writerRef.get().commit();
                mailboxIndex.mailbox.index.submit(task); // merge must run in background
                success = true;
            } catch (RejectedExecutionException e) {
                ZimbraLog.index.warn("Skipping merge because all index threads are busy");
            } finally {
                if (!success) {
                    writerRef.dec();
                }
            }
        }
    }

    /**
     * Called by {@link IndexWriterRef#dec()}. Can be called by the thread that opened the writer or the merge thread.
     */
    synchronized void closeWriter() {
        assert(writerRef != null);

        ZimbraLog.index.debug("Close IndexWriter");

        try {
            writerRef.get().close(false); // ignore phantom pending merges
        } catch (IOException e) {
            ZimbraLog.index.error("Failed to close IndexWriter", e);
        } finally {
            unlockIndexWriter();
            WRITER_THROTTLE.release();
            writerRef = null;
        }
    }

    /**
     * Called by {@link IndexReaderRef#dec()}. Can be called by the thread that opened the reader or the cache sweeper
     * thread.
     */
    void closeReader(IndexReader reader) {
        ZimbraLog.search.debug("Close IndexReader");

        try {
            reader.close();
        } catch (IOException e) {
            ZimbraLog.search.warn("Failed to close IndexReader", e);
        } finally {
            READER_THROTTLE.release();
        }
    }

    interface TermEnumInterface {
        void onTerm(Term term, int docFreq);
    }

    static class DomainEnumCallback implements TermEnumInterface {
        private Collection<BrowseTerm> mCollection;

        DomainEnumCallback(Collection<BrowseTerm> collection) {
            mCollection = collection;
        }

        @Override
        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1 && text.charAt(0) == '@') {
                mCollection.add(new BrowseTerm(text.substring(1), docFreq));
            }
        }
    }

    static class TermEnumCallback implements TermEnumInterface {
        private Collection<BrowseTerm> mCollection;

        TermEnumCallback(Collection<BrowseTerm> collection) {
            mCollection = collection;
        }

        @Override
        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1) {
                mCollection.add(new BrowseTerm(text, docFreq));
            }
        }
    }

    /**
     * Only one background thread that holds the lock may process a merge for the given writer. Other concurrent
     * attempts simply skip the merge.
     */
    private static final class MergeScheduler extends SerialMergeScheduler {
        private AtomicReference<Thread> lock = new AtomicReference<Thread>();

        /**
         * Try to hold the lock.
         *
         * @return true if the lock is held, false the lock is currently held by the other thread.
         */
        boolean tryLock() {
            return lock.compareAndSet(null, Thread.currentThread());
        }

        void release() {
            lock.compareAndSet(Thread.currentThread(), null);
        }

        /**
         * Skip the merge unless the lock is held.
         */
        @Override
        public void merge(IndexWriter writer) throws CorruptIndexException, IOException {
            if (lock.get() == Thread.currentThread()) {
                super.merge(writer);
            }
        }

        /**
         * Removes the Thread-to-IndexWriter reference.
         */
        @Override
        public void close() {
            super.close();
            lock.set(null);
        }
    }

    /**
     * In order to minimize delay caused by merges, merges are processed only in background threads. Writers triggered
     * by batch threshold or search commit the changes before processing merges, so that the changes are available to
     * readers without long delay that merges likely cause. Merge threads don't block other writer threads running in
     * foreground. Another indexing using the same writer may start even while the merge is in progress.
     */
    private static final class MergeTask extends IndexHelper.IndexTask {
        private final IndexWriterRef ref;

        MergeTask(IndexWriterRef ref) {
            super(ref.getIndex().mailboxIndex.mailbox);
            this.ref = ref;
        }

        @Override
        public void exec() {
            try {
                if (((MergeScheduler) ref.get().getMergeScheduler()).tryLock()) {
                    ref.get().maybeMerge();
                } else {
                    ZimbraLog.index.debug("Merge is in progress by other thread");
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to merge IndexWriter", e);
            } finally {
                ((MergeScheduler) ref.get().getMergeScheduler()).release();
                ref.dec();
            }
        }
    }

    private static final class LuceneConfig {

        final boolean mergePolicy;
        final long minMerge;
        final long maxMerge;
        final int mergeFactor;
        final boolean useCompoundFile;
        final int maxBufferedDocs;
        final int ramBufferSizeKB;

        LuceneConfig(boolean batchIndexing) {
            if (batchIndexing) {
                mergePolicy = LC.zimbra_index_lucene_batch_merge_policy.booleanValue();
                minMerge = LC.zimbra_index_lucene_batch_min_merge.longValue();
                maxMerge = LC.zimbra_index_lucene_batch_max_merge.longValue();
                mergeFactor = LC.zimbra_index_lucene_batch_merge_factor.intValue();
                useCompoundFile = LC.zimbra_index_lucene_batch_use_compound_file.booleanValue();
                maxBufferedDocs = LC.zimbra_index_lucene_batch_max_buffered_docs.intValue();
                ramBufferSizeKB = LC.zimbra_index_lucene_batch_ram_buffer_size_kb.intValue();
            } else {
                mergePolicy = LC.zimbra_index_lucene_nobatch_merge_policy.booleanValue();
                minMerge = LC.zimbra_index_lucene_nobatch_min_merge.longValue();
                maxMerge = LC.zimbra_index_lucene_nobatch_max_merge.longValue();
                mergeFactor = LC.zimbra_index_lucene_nobatch_merge_factor.intValue();
                useCompoundFile = LC.zimbra_index_lucene_nobatch_use_compound_file.booleanValue();
                maxBufferedDocs = LC.zimbra_index_lucene_nobatch_max_buffered_docs.intValue();
                ramBufferSizeKB = LC.zimbra_index_lucene_nobatch_ram_buffer_size_kb.intValue();
            }
        }

    }

}
