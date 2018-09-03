/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.NoSuchDirectoryException;
import org.apache.lucene.util.Version;

import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.io.ByteStreams;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.util.IOUtil;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

/**
 * {@link IndexStore} implementation using Apache Lucene.
 *
 * @author tim
 * @author ysasaki
 */
public final class LuceneIndex extends IndexStore {

    /**
     * We don't want to enable StopFilter preserving position increments, which is enabled on or after 2.9, because we
     * want phrases to match across removed stop words.
     * TODO: LUCENE_2* are obsolete as of Lucene 3.1.
     */
    @SuppressWarnings("deprecation")
    public static final Version VERSION = Version.LUCENE_24;

    private static final Semaphore READER_THROTTLE = new Semaphore(LC.zimbra_index_max_readers.intValue());
    private static final Semaphore WRITER_THROTTLE = new Semaphore(LC.zimbra_index_max_writers.intValue());

    private static final Cache<Integer, IndexSearcherImpl> SEARCHER_CACHE =
        CacheBuilder.newBuilder()
        .maximumSize(LC.zimbra_index_reader_cache_size.intValue())
        .expireAfterAccess(LC.zimbra_index_reader_cache_ttl.intValue(), TimeUnit.SECONDS)
        .removalListener(new RemovalListener<Integer, IndexSearcherImpl>() {
            @Override
            public void onRemoval(RemovalNotification<Integer, IndexSearcherImpl> notification) {
                IOUtil.closeQuietly(notification.getValue());
            }
        })
        .build();

    // Bug: 60631
    // cache lucene index of GAL sync account separately with no automatic eviction
    private static final ConcurrentMap<Integer, IndexSearcherImpl> GAL_SEARCHER_CACHE =
        new ConcurrentLinkedHashMap.Builder<Integer, IndexSearcherImpl>()
        .maximumWeightedCapacity(LC.zimbra_galsync_index_reader_cache_size.intValue())
        .listener(new EvictionListener<Integer, IndexSearcherImpl>() {
            @Override
            public void onEviction(Integer mboxId, IndexSearcherImpl searcher) {
                IOUtil.closeQuietly(searcher);
            }
        })
        .build();

    private final Mailbox mailbox;
    private final LuceneDirectory luceneDirectory;
    private final AtomicBoolean pendingDelete = new AtomicBoolean(false);
    private final WriterInfo writerInfo = new WriterInfo();

    /**
     * Holds information related to writers to the index.
     * Deletion of the index is only allowed when there are no writers, hence it is important to know
     * when there are no more writers.
     */
    private final class WriterInfo {
        private IndexWriterRef writerRef;
        private final Lock lock = new ReentrantLock();
        private final Condition hasNoWriters  = lock.newCondition();

        private WriterInfo() {
            writerRef = null;
        }

        public IndexWriterRef getWriterRef() {
            return writerRef;
        }

        /**
         * @param newRef index writer reference.  If null, there are no writers
         */
        public void setWriterRef(IndexWriterRef newRef)
        throws IOException {
            if ((newRef != null) && isPendingDelete()) {
                throw new IndexPendingDeleteException();
            }
            lock.lock();
            try {
                writerRef= newRef;
                if (writerRef == null) {
                    hasNoWriters.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        public Condition getHasNoWritersCondition() {
            return hasNoWriters;
        }

        public Lock getHasNoWritersLock() {
            return lock;
        }
    }

    private LuceneIndex(Mailbox mbox) throws ServiceException {
        mailbox = mbox;
        Volume vol = VolumeManager.getInstance().getVolume(mbox.getIndexVolume());
        String dir = vol.getMailboxDir(mailbox.getId(), Volume.TYPE_INDEX);

        // this must be different from the root dir (see the IMPORTANT comment below)
        File root = new File(dir + File.separatorChar + '0');

        // IMPORTANT!  Don't make the actual index directory (mIdxDirectory) yet!
        //
        // The runtime open-index code checks the existance of the actual index directory:
        // if it does exist but we cannot open the index, we do *NOT* create it under the
        // assumption that the index was somehow corrupted and shouldn't be messed-with....on the
        // other hand if the index dir does NOT exist, then we assume it has never existed (or
        // was deleted intentionally) and therefore we should just create an index.
        if (!root.exists()) {
            root.mkdirs();
        }

        if (!root.canRead()) {
            throw ServiceException.FAILURE("LuceneDirectory not readable mbox=" + mbox.getId() + ",dir=" + root, null);
        }
        if (!root.canWrite()) {
            throw ServiceException.FAILURE("LuceneDirectory not writable mbox=" + mbox.getId() + ",dir=" + root, null);
        }

        // the Lucene code does not atomically swap the "segments" and "segments.new" files...so it is possible that a
        // previous run of the server crashed exactly in such a way that we have a "segments.new" file but not a
        // "segments" file. We we will check here for the special situation that we have a segments.new/ file but not a
        // segments file...
        File segments = new File(root, "segments");
        if (!segments.exists()) {
            File newSegments = new File(root, "segments.new");
            if (newSegments.exists()) {
                newSegments.renameTo(segments);
            }
        }

        try {
            luceneDirectory = LuceneDirectory.open(root);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to create LuceneDirectory: " + root, e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("mbox", mailbox.getId()).add("dir", luceneDirectory).toString();
    }

    private synchronized void doDeleteIndex() throws IOException {
        assert(writerInfo.getWriterRef() == null);
        ZimbraLog.index.debug("Deleting index %s", luceneDirectory);
        if (mailbox.isGalSyncMailbox()) {
            IOUtil.closeQuietly(GAL_SEARCHER_CACHE.remove(mailbox.getId()));
        } else {
            SEARCHER_CACHE.asMap().remove(mailbox.getId());
        }

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

    /**
     * Deletes this index completely.
     */
    @Override
    public void deleteIndex() throws IOException {
        pendingDelete.set(true);
        writerInfo.getHasNoWritersLock().lock();
        try {
            if (writerInfo.getWriterRef() != null) {
                writerInfo.getHasNoWritersCondition().awaitUninterruptibly();
            }
            doDeleteIndex();
        } finally {
            writerInfo.getHasNoWritersLock().unlock();
        }
        pendingDelete.set(false);
    }

    /**
     * Runs a common search query + common sort order (and throw away the result) to warm up the Lucene cache and OS
     * file system cache.
     */
    @Override
    public synchronized void warmup() {
        if (SEARCHER_CACHE.asMap().containsKey(mailbox.getId()) ||
                GAL_SEARCHER_CACHE.containsKey(mailbox.getId())) {
            return; // already warmed up
        }
        long start = System.currentTimeMillis();
        try {
            try (IndexSearcher searcher = (IndexSearcher) openSearcher()) {
                searcher.search(new TermQuery(new Term(LuceneFields.L_CONTENT, "zimbra")), 1,
                    new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true)));
            }
        } catch (IOException e) {
            ZimbraLog.search.warn("Failed to warm up", e);
        }
        ZimbraLog.search.debug("WarmUpLuceneSearcher elapsed=%d", System.currentTimeMillis() - start);
    }

    /**
     * Removes IndexSearcher used for this index from cache.
     */
    @Override
    public void evict() {
        if (mailbox.isGalSyncMailbox()) {
            IOUtil.closeQuietly(GAL_SEARCHER_CACHE.remove(mailbox.getId()));
        } else {
            SEARCHER_CACHE.asMap().remove(mailbox.getId());
        }
    }

    private IndexReader openIndexReader(boolean tryRepair) throws IOException {
        try {
            return IndexReader.open(luceneDirectory, null, true, LC.zimbra_index_lucene_term_index_divisor.intValue());
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

    private IndexWriter openIndexWriter(IndexWriterConfig.OpenMode mode, boolean tryRepair) throws IOException {
        try {
            IndexWriter writer = new IndexWriter(luceneDirectory, getWriterConfig().setOpenMode(mode)) {
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
                writer.setInfoStream(new PrintStream(ByteStreams.nullOutputStream()));
            }
            return writer;
        } catch (AssertionError e) {
            unlockIndexWriter();
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexWriter(mode, false);
        } catch (CorruptIndexException e) {
            unlockIndexWriter();
            if (!tryRepair) {
                throw e;
            }
            repair(e);
            return openIndexWriter(mode, false);
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
     * Caller is responsible for calling {@link IndexReader#close()} to release system resources associated with it.
     *
     * @return A {@link IndexReader} for this index.
     * @throws IOException if opening an {@link IndexReader} failed
     */
    @Override
    public synchronized ZimbraIndexSearcher openSearcher() throws IOException {
        IndexSearcherImpl searcher = null;
        if (mailbox.isGalSyncMailbox()) {
            searcher = GAL_SEARCHER_CACHE.get(mailbox.getId());
        } else {
            searcher = SEARCHER_CACHE.getIfPresent(mailbox.getId());
        }
        if (searcher != null) {
            ZimbraLog.search.debug("CacheHitLuceneSearcher %s", searcher);
            searcher.inc();
            return searcher;
        }

        READER_THROTTLE.acquireUninterruptibly();
        long start = System.currentTimeMillis();
        try {
            searcher = new IndexSearcherImpl(openIndexReader(true));
        } catch (IOException e) {
            // Handle the special case of trying to open a not-yet-created index, by opening for write and immediately
            // closing. Index directory should get initialized as a result.
            if (isEmptyDirectory(luceneDirectory.getDirectory())) {
                // create an empty index
                IndexWriter writer = new IndexWriter(luceneDirectory,
                        getWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE));
                IOUtil.closeQuietly(writer);
                searcher = new IndexSearcherImpl(openIndexReader(false));
            } else {
                throw e;
            }
        } finally {
            if (searcher == null) {
                READER_THROTTLE.release();
            }
        }

        ZimbraLog.search.debug("OpenLuceneSearcher %s,elapsed=%d", searcher, System.currentTimeMillis() - start);
        searcher.inc();
        if (mailbox.isGalSyncMailbox()) {
            IOUtil.closeQuietly(GAL_SEARCHER_CACHE.put(mailbox.getId(), searcher));
        } else {
            SEARCHER_CACHE.asMap().put(mailbox.getId(), searcher);
        }
        return searcher;
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

    @Override
    public synchronized Indexer openIndexer() throws IOException {
        if (writerInfo.getWriterRef() != null) {
            writerInfo.getWriterRef().inc();
        } else {
            WRITER_THROTTLE.acquireUninterruptibly();
            try {
                writerInfo.setWriterRef(openWriter());
            } finally {
                if (writerInfo.getWriterRef() == null) {
                    WRITER_THROTTLE.release();
                }
            }
        }
        return new LuceneIndexerImpl(writerInfo.getWriterRef());
    }

    private IndexWriterRef openWriter() throws IOException {
        assert(Thread.holdsLock(this));

        IndexWriter writer;
        try {
            writer = openIndexWriter(IndexWriterConfig.OpenMode.APPEND, true);
        } catch (IOException e) {
            // the index (the segments* file in particular) probably didn't exist
            // when new IndexWriter was called in the try block, we would get a
            // FileNotFoundException for that case. If the directory is empty,
            // this is the very first index write for this this mailbox (or the
            // index might be deleted), the FileNotFoundException is benign.
            if (isEmptyDirectory(luceneDirectory.getDirectory())) {
                writer = openIndexWriter(IndexWriterConfig.OpenMode.CREATE, false);
            } else {
                throw e;
            }
        }
        return new IndexWriterRef(this, writer);
    }

    private synchronized void commitWriter() throws IOException {
        assert(writerInfo.getWriterRef() != null);

        ZimbraLog.index.debug("Commit IndexWriter");

        MergeTask task = new MergeTask(writerInfo.getWriterRef());

        boolean success = false;
        try {
            try {
                writerInfo.getWriterRef().get().commit();
            } catch (CorruptIndexException e) {
                try {
                    writerInfo.getWriterRef().get().close(false);
                } catch (Throwable ignore) {
                }
                repair(e);
                throw e; // fail to commit regardless of the repair
            } catch (AssertionError e) {
                try {
                    writerInfo.getWriterRef().get().close(false);
                } catch (Throwable ignore) {
                }
                writerInfo.getWriterRef().get().close(false);
                repair(e);
                throw e; // fail to commit regardless of the repair
            }
            mailbox.index.submit(task); // merge must run in background
            success = true;
        } catch (RejectedExecutionException e) {
            ZimbraLog.index.warn("Skipping merge because all index threads are busy");
        } finally {
            if (!success) {
                writerInfo.getWriterRef().dec();
            }
        }
    }

    /**
     * Called by {@link IndexWriterRef#dec()}. Can be called by the thread that opened the writer or the merge thread.
     */
    private synchronized void closeWriter() {
        if (writerInfo.getWriterRef() == null) {
            return;
        }

        ZimbraLog.index.debug("Close IndexWriter");

        try {
            writerInfo.getWriterRef().get().close(false); // ignore phantom pending merges
        } catch (CorruptIndexException e) {
            try {
                repair(e);
            } catch (CorruptIndexException ignore) {
            }
        } catch (AssertionError e) {
            repair(e);
        } catch (IOException e) {
            ZimbraLog.index.error("Failed to close IndexWriter", e);
        } finally {
            unlockIndexWriter();
            WRITER_THROTTLE.release();
            try {
                writerInfo.setWriterRef(null);
            } catch (IOException e) {
            }
        }
    }

    /**
     * Run a sanity check for the index. Callers are responsible to make sure the index is not opened by any writer.
     *
     * @param out info stream where messages should go. If null, no messages are printed.
     * @return true if no problems were found, otherwise false
     * @throws IOException failed to verify, but it doesn't necessarily mean the index is corrupted.
     */
    @Override
    public boolean verify(PrintStream out) throws IOException {
        if (!IndexReader.indexExists(luceneDirectory)) {
            out.println("index does not exist or no segments file found: " + luceneDirectory.getDirectory());
            return true;
        }
        CheckIndex check = new CheckIndex(luceneDirectory);
        if (out != null) {
            check.setInfoStream(out);
        }
        CheckIndex.Status status = check.checkIndex();
        return status.clean;
    }

    /**
     * Only one background thread that holds the lock may process a merge for the given writer. Other concurrent
     * attempts simply skip the merge.
     */
    private static final class MergeScheduler extends SerialMergeScheduler {
        private final ReentrantLock lock = new ReentrantLock();

        /**
         * Acquires the lock.
         */
        void lock() {
            lock.lock();
        }

        /**
         * Try to hold the lock.
         *
         * @return true if the lock is held, false the lock is currently held by the other thread.
         */
        boolean tryLock() {
            return lock.tryLock();
        }

        void release() {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException ignore) {
            }
        }

        /**
         * Skip the merge unless the lock is held.
         */
        @Override
        public void merge(IndexWriter writer) throws CorruptIndexException, IOException {
            if (lock.isHeldByCurrentThread()) {
                super.merge(writer);
            }
        }

        /**
         * Removes the Thread-to-IndexWriter reference.
         */
        @Override
        public void close() {
            super.close();
            release();
        }
    }

    /**
     * In order to minimize delay caused by merges, merges are processed only in background threads. Writers triggered
     * by batch threshold or search commit the changes before processing merges, so that the changes are available to
     * readers without long delay that merges likely cause. Merge threads don't block other writer threads running in
     * foreground. Another indexing using the same writer may start even while the merge is in progress.
     */
    private final class MergeTask extends MailboxIndex.IndexTask {
        private final IndexWriterRef ref;

        MergeTask(IndexWriterRef ref) {
            super(ref.getIndex().mailbox);
            this.ref = ref;
        }

        @Override
        public void exec() throws IOException {
            IndexWriter writer = ref.get();
            MergeScheduler scheduler = (MergeScheduler) writer.getConfig().getMergeScheduler();
            try {
                if (scheduler.tryLock()) {
                    writer.maybeMerge();
                } else {
                    ZimbraLog.index.debug("Merge is in progress by other thread");
                }
            } catch (CorruptIndexException e) {
                try {
                    writer.close(false);
                } catch (Throwable ignore) {
                }
                repair(e);
            } catch (AssertionError e) {
                try {
                    writer.close(false);
                } catch (Throwable ignore) {
                }
                repair(e);
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to merge IndexWriter", e);
            } finally {
                scheduler.release();
                ref.dec();
            }
        }
    }

    private IndexWriterConfig getWriterConfig() {
        IndexWriterConfig config = new IndexWriterConfig(VERSION, mailbox.index.getAnalyzer());
        config.setMergeScheduler(new MergeScheduler());
        config.setMaxBufferedDocs(LC.zimbra_index_lucene_max_buffered_docs.intValue());
        config.setRAMBufferSizeMB(LC.zimbra_index_lucene_ram_buffer_size_kb.intValue() / 1024.0);
        if (LC.zimbra_index_lucene_merge_policy.booleanValue()) {
            LogDocMergePolicy policy = new LogDocMergePolicy();
            config.setMergePolicy(policy);
            policy.setUseCompoundFile(LC.zimbra_index_lucene_use_compound_file.booleanValue());
            policy.setMergeFactor(LC.zimbra_index_lucene_merge_factor.intValue());
            policy.setMinMergeDocs(LC.zimbra_index_lucene_min_merge.intValue());
            if (LC.zimbra_index_lucene_max_merge.intValue() != Integer.MAX_VALUE) {
                policy.setMaxMergeDocs(LC.zimbra_index_lucene_max_merge.intValue());
            }
        } else {
            LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
            config.setMergePolicy(policy);
            policy.setUseCompoundFile(LC.zimbra_index_lucene_use_compound_file.booleanValue());
            policy.setMergeFactor(LC.zimbra_index_lucene_merge_factor.intValue());
            policy.setMinMergeMB(LC.zimbra_index_lucene_min_merge.intValue() / 1024.0);
            if (LC.zimbra_index_lucene_max_merge.intValue() != Integer.MAX_VALUE) {
                policy.setMaxMergeMB(LC.zimbra_index_lucene_max_merge.intValue() / 1024.0);
            }
        }
        return config;
    }

    public static ZimbraQueryResults search(ZimbraQuery zq) throws ServiceException {
        SearchParams params = zq.getParams();
        ZimbraLog.search.debug("query: %s", params.getQueryString());

        // handle special-case Task-only sorts: convert them to a "normal sort"
        //     and then re-sort them at the end
        // FIXME - this hack (converting the sort) should be able to go away w/ the new SortBy
        //         implementation, if the lower-level code was modified to use the SortBy.Criterion
        //         and SortBy.Direction data (instead of switching on the SortBy itself)
        //         We still will need this switch so that we can wrap the
        //         results in the ReSortingQueryResults
        boolean isTaskSort = false;
        boolean isLocalizedSort = false;
        SortBy originalSort = params.getSortBy();
        switch (originalSort) {
            case TASK_DUE_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_DUE_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_STATUS_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_STATUS_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_PERCENT_COMPLETE_ASC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case TASK_PERCENT_COMPLETE_DESC:
                isTaskSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                isLocalizedSort = true;
                break;
        }

        ZimbraQueryResults results = zq.execute();
        if (isTaskSort) {
            results = new ReSortingQueryResults(results, originalSort, null);
        }
        if (isLocalizedSort) {
            results = new ReSortingQueryResults(results, originalSort, params);
        }
        return results;
    }

    @Override
    public boolean isPendingDelete() {
        return pendingDelete.get();
    }

    @Override
    public void setPendingDelete(boolean pendingDelete) {
        this.pendingDelete.set(pendingDelete);
    }

    public static final class Factory implements IndexStore.Factory {
        public Factory() {
            BooleanQuery.setMaxClauseCount(LC.zimbra_index_lucene_max_terms_per_query.intValue());
        }

        @Override
        public LuceneIndex getIndexStore(Mailbox mbox) throws ServiceException {
            return new LuceneIndex(mbox);
        }

        @Override
        public void destroy() {
            SEARCHER_CACHE.asMap().clear();

            for (IndexSearcherImpl searcher : GAL_SEARCHER_CACHE.values()) {
                IOUtil.closeQuietly(searcher);
            }
            GAL_SEARCHER_CACHE.clear();
        }
    }

    private static final class LuceneIndexerImpl implements Indexer {
        private final IndexWriterRef writer;

        LuceneIndexerImpl(IndexWriterRef writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            writer.index.commitWriter();
            ZimbraIndexSearcher searcher = null;
            if (writer.getIndex().mailbox.isGalSyncMailbox()) {
                searcher = GAL_SEARCHER_CACHE.get(writer.getIndex().mailbox.getId());
            } else {
                searcher = SEARCHER_CACHE.getIfPresent(writer.getIndex().mailbox.getId());
            }
            if (searcher != null) {
                ZimbraLuceneIndexReader ndxReader = (ZimbraLuceneIndexReader)searcher.getIndexReader();
                IndexReader newReader = IndexReader.openIfChanged(ndxReader.getLuceneReader(), true);
                if (newReader != null) {
                    if (writer.getIndex().mailbox.isGalSyncMailbox()) {
                        //make sure that we close the previous value associated with the key
                        IOUtil.closeQuietly(GAL_SEARCHER_CACHE.put(writer.getIndex().mailbox.getId(),
                                new IndexSearcherImpl(newReader)));
                    } else {
                        // Bug: 69870
                        // No need to close the previous value associated with the key here.
                        // CacheBuilder sends a callback using removalListener onRemoval(..)
                        // which eventually closes IndexSearcher
                        SEARCHER_CACHE.asMap().put(writer.getIndex().mailbox.getId(),
                            new IndexSearcherImpl(newReader));
                    }
                }
            }
        }

        @Override
        public void compact() {
            MergeScheduler scheduler = (MergeScheduler) writer.get().getConfig().getMergeScheduler();
            scheduler.lock();
            try {
                ZimbraLog.index.info("Force merge deletes %d", writer.get().maxDoc() - writer.get().numDocs());
                writer.get().forceMergeDeletes(true);
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to optimize index", e);
            } finally {
                scheduler.release();
            }
        }

        @Override
        public synchronized int maxDocs() {
            // IndexWriter.maxDoc() - Returns total number of docs in this index, including docs not yet flushed
            //                        (still in the RAM buffer), not counting deletions.
            return writer.get().maxDoc();
        }

        /**
         * Adds the list of documents to the index.
         * <p>
         * If the index status is stale, delete the stale documents first, then add new documents. If the index status
         * is deferred, we are sure that this item is not already in the index, and so we can skip the check-update step.
         */
        @Override
        public synchronized void addDocument(Folder folder, MailItem item, List<IndexDocument> docs)
                throws IOException {
            if (docs == null || docs.isEmpty()) {
                return;
            }

            // handle the partial re-index case here by simply deleting all the documents matching the index_id
            // so that we can simply add the documents to the index later!!
            switch (item.getIndexStatus()) {
                case STALE:
                case DONE: // for partial re-index
                    Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(item.getId()));
                    writer.get().deleteDocuments(term);
                    break;
                case DEFERRED:
                    break;
                default:
                    assert false : item.getIndexId();
            }

            for (IndexDocument doc : docs) {
                // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                synchronized (doc) {
                    setFields(item, doc);
                    Document luceneDoc = doc.toDocument();
                    if (ZimbraLog.index.isTraceEnabled()) {
                        ZimbraLog.index.trace("Adding lucene document %s", luceneDoc.toString());
                    }
                    writer.get().addDocument(luceneDoc);
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
        @Override
        public void deleteDocument(List<Integer> ids) throws IOException {
            for (Integer id : ids) {
                Term term = new Term(LuceneFields.L_MAILBOX_BLOB_ID, id.toString());
                writer.get().deleteDocuments(term);
                ZimbraLog.index.debug("Deleted documents id=%d", id);
            }
        }
    }

    /**
     * {@link IndexWriter} wrapper that supports a reference counter.
     */
    private final class IndexWriterRef {
        private final LuceneIndex index;
        private final IndexWriter writer;
        private final AtomicInteger count = new AtomicInteger(1); // ref counter

        IndexWriterRef(LuceneIndex index, IndexWriter writer) {
            this.index = index;
            this.writer = writer;
        }

        IndexWriter get() {
            return writer;
        }

        LuceneIndex getIndex() {
            return index;
        }

        void inc() {
            count.incrementAndGet();
        }

        void dec() {
            synchronized (index) {
                if (count.decrementAndGet() <= 0) {
                    index.closeWriter();
                }
            }
        }

    }

    /**
     * Custom {@link IndexSearcher} that supports a reference counter.
     */
    private static final class IndexSearcherImpl implements ZimbraIndexSearcher {
        private final AtomicInteger count = new AtomicInteger(1);
        private final IndexSearcher luceneSearcher;
        private final ZimbraIndexReader luceneReader;

        IndexSearcherImpl(IndexReader reader) {
            luceneSearcher = new IndexSearcher(reader);
            luceneReader = new ZimbraLuceneIndexReader(luceneSearcher.getIndexReader());
        }

        void inc() {
            count.incrementAndGet();
        }

        @Override
        public void close() throws IOException {
            if (count.decrementAndGet() == 0) {
                ZimbraLog.search.debug("Close IndexSearcher");
                try {
                    IOUtil.closeQuietly(luceneSearcher);
                } finally {
                    IOUtil.closeQuietly(getIndexReader());
                    READER_THROTTLE.release();
                }
            }
        }

        @Override
        public Document doc(ZimbraIndexDocumentID docID) throws IOException {
            if (docID instanceof ZimbraLuceneDocumentID) {
                ZimbraLuceneDocumentID zlDocID = (ZimbraLuceneDocumentID)docID;
                return luceneSearcher.doc(zlDocID.getLuceneDocID());
            }
            throw new IllegalArgumentException("Expected a ZimbraLuceneDocumentID");
        }

        @Override
        public int docFreq(Term term) throws IOException {
            return luceneSearcher.docFreq(term);
        }

        @Override
        public ZimbraIndexReader getIndexReader() {
            return luceneReader;
        }

        @Override
        public ZimbraTopDocs search(Query query, int n) throws IOException {
            return ZimbraTopDocs.create(luceneSearcher.search(query, n));
        }

        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n) throws IOException {
            TermsFilter luceneFilter = (filter == null) ? null : new TermsFilter(filter.getTerms());
            return ZimbraTopDocs.create(luceneSearcher.search(query, luceneFilter, n));
        }

        @Override
        public ZimbraTopFieldDocs search(Query query, ZimbraTermsFilter filter, int n, Sort sort) throws IOException {
            TermsFilter luceneFilter = (filter == null) ? null : new TermsFilter(filter.getTerms());
            return ZimbraTopFieldDocs.create(luceneSearcher.search(query, luceneFilter, n, sort));
        }
    }

    public static final class ZimbraLuceneIndexReader implements ZimbraIndexReader {

        private final IndexReader luceneReader;

        private ZimbraLuceneIndexReader(IndexReader indexReader) {
            luceneReader = indexReader;
        }

        @Override
        public void close() throws IOException {
            IOUtil.closeQuietly(getLuceneReader());
        }

        @Override
        public int numDocs() {
            return getLuceneReader().numDocs();
        }

        @Override
        public int numDeletedDocs() {
            return getLuceneReader().numDeletedDocs();
        }

        /**
         * Returns an enumeration of the String representations for values of terms with {@code field}
         * positioned to start at the first term with a value greater than {@code firstTermValue}.
         * The enumeration is ordered by String.compareTo().
         */
        @Override
        public TermFieldEnumeration getTermsForField(String field, String firstTermValue) throws IOException {
            return new LuceneTermValueEnumeration(field, firstTermValue);
        }

        private final class LuceneTermValueEnumeration implements TermFieldEnumeration {
            private TermEnum termEnumeration;
            private final String field;

            private LuceneTermValueEnumeration(String field, String firstTermValue) throws IOException {
                termEnumeration = getLuceneReader().terms(new Term(field, firstTermValue));
                this.field = field;
            }

            @Override
            public boolean hasMoreElements() {
                if (termEnumeration == null) {
                    return false;
                }
                Term term = termEnumeration.term();
                return ((term != null) && field.equals(term.field()));
            }

            @Override
            public BrowseTerm nextElement() {
                if (termEnumeration == null) {
                    throw new NoSuchElementException("No more values");
                }
                Term term = termEnumeration.term();
                if ((term != null) && field.equals(term.field())) {
                    BrowseTerm nextVal = new BrowseTerm(term.text(), termEnumeration.docFreq());
                    try {
                        termEnumeration.next();
                    } catch (IOException e) {
                        IOUtil.closeQuietly(termEnumeration);
                        termEnumeration = null;
                    }
                    return nextVal;
                } else {
                    IOUtil.closeQuietly(termEnumeration);
                    throw new NoSuchElementException("No more values");
                }
            }

            @Override
            public void close() throws IOException {
                if (termEnumeration != null) {
                    IOUtil.closeQuietly(termEnumeration);
                }
                termEnumeration = null;
            }
        }

        public IndexReader getLuceneReader() {
            return luceneReader;
        }
    }

    /**
     * Note: Lucene 3.5.0 highly discourages optimizing the index as it is horribly inefficient and very rarely
     *       justified. Please check {@code IndexWriter.forceMerge} API documentation for more details.
     *       Code removed which used to use forceMerge.
     */
    @Override
    public void optimize() {
    }
}
