/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.DbSearchConstraints;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexPendingDeleteException;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneIndex;
import com.zimbra.cs.index.ReSortingQueryResults;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.index.ZimbraIndexReader.TermFieldEnumeration;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.util.Zimbra;

/**
 * Index related mailbox operations.
 *
 * @author tim
 * @author ysasaki
 */
public final class MailboxIndex {
    private static final long MAX_TX_BYTES = LC.zimbra_index_max_transaction_bytes.longValue();
    private static final int MAX_TX_ITEMS = LC.zimbra_index_max_transaction_items.intValue();
    private static final long FAILURE_DELAY = LC.zimbra_index_deferred_items_failure_delay.intValue() * 1000;

    private static final ThreadPoolExecutor INDEX_EXECUTOR = new ThreadPoolExecutor(
            LC.zimbra_index_threads.intValue(), LC.zimbra_index_threads.intValue(),
            Long.MAX_VALUE, TimeUnit.NANOSECONDS, new SynchronousQueue<Runnable>(),
            new ThreadFactoryBuilder().setNameFormat("Index-%d").setDaemon(true).build());
    // Re-index threads are created on demand basis. The number of threads are capped.
    private static final ExecutorService REINDEX_EXECUTOR = new ThreadPoolExecutor(
            0, LC.zimbra_reindex_threads.intValue(), 0L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            new ThreadFactoryBuilder().setNameFormat("ReIndex-%d").setDaemon(true).build());

    private volatile long lastFailedTime = -1;
    // Only one thread may run index at a time.
    private final Semaphore indexLock = new Semaphore(1);
    private final Mailbox mailbox;
    private final Analyzer analyzer;
    private IndexStore indexStore;
    // current re-indexing operation for this mailbox, or NULL if a re-index is not in progress.
    private volatile ReIndexTask reIndex;
    // current compact-indexing operation for this mailbox, or NULL if a compact-index is not in progress.
    private volatile CompactIndexTask compactIndex;
    private volatile SetMultimap<MailItem.Type, Integer> deferredIds; // guarded by IndexHelper
    boolean indexingSuspended = false;
    int numMaybeIndexDeferredItemsCalls = 0;

    MailboxIndex(Mailbox mbox) {
        mailbox = mbox;
        String analyzerName;
        try {
            analyzerName = mbox.getAccount().getTextAnalyzer();
        } catch (ServiceException e) {
            analyzerName = null;
        }
        analyzer = ZimbraAnalyzer.getAnalyzer(analyzerName);
    }

    /**
     * Starts all index threads.
     */
    public static void startup() {
        INDEX_EXECUTOR.prestartAllCoreThreads();
    }

    public static void shutdown() {
        IndexStore.getFactory().destroy();
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    void open() throws ServiceException {
        indexStore = IndexStore.getFactory().getIndexStore(mailbox);
    }

    public final IndexStore getIndexStore() {
        assert(indexStore != null);
        return indexStore;
    }

    /**
     * This is the preferred form of the API call.
     *
     * In order to avoid deadlock, callers MUST NOT be holding the Mailbox lock when calling this API.
     *
     * You MUST call {@link ZimbraQueryResults#doneWithSearchResults()} when you are done with the search results,
     * otherwise resources will be leaked.
     *
     * @param octxt Operation Context
     * @param params Search Parameters
     * @return search result
     */
    public ZimbraQueryResults search(SoapProtocol proto, OperationContext octx, SearchParams params)
            throws ServiceException {
        assert(mailbox.lock.isUnlocked());
        assert(octx != null);

        ZimbraQuery query = new ZimbraQuery(octx, proto, mailbox, params);
        Set<MailItem.Type> types = toIndexTypes(params.getTypes());
        // no need to index if the search doesn't involve Lucene
        if (!params.isQuick() && query.hasTextOperation() && getDeferredCount(types) > 0) {
            try {
                // don't wait if an indexing is in progress by other thread
                indexDeferredItems(types, new BatchStatus(), false);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Failed to index deferred items", e);
            }
        }
        return search(query);
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, Set<MailItem.Type> types,
            SortBy sortBy, int chunkSize, boolean inDumpster) throws ServiceException {
        SearchParams params = new SearchParams();
        params.setQueryString(queryString);
        params.setTimeZone(null);
        params.setLocale(null);
        params.setTypes(types);
        params.setSortBy(sortBy);
        params.setChunkSize(chunkSize);
        params.setPrefetch(true);
        params.setFetchMode(SearchParams.Fetch.NORMAL);
        params.setInDumpster(inDumpster);
        return search(SoapProtocol.Soap12, octxt, params);
    }

    public ZimbraQueryResults search(OperationContext octxt, SearchParams params) throws ServiceException {
    	return search(SoapProtocol.Soap12, octxt, params);
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, Set<MailItem.Type> types,
            SortBy sortBy, int chunkSize) throws ServiceException {
        return search(octxt, queryString, types, sortBy, chunkSize, false);
    }

    private ZimbraQueryResults search(ZimbraQuery zq) throws ServiceException {
        SearchParams params = zq.getParams();
        ZimbraLog.search.debug("query: %s", params.getQueryString());
        ZimbraLog.searchstat.debug("query: %s", zq.toSanitizedtring());

        // handle special-case Task-only sorts: convert them to a "normal sort" and then re-sort them at the end
        // TODO: this hack (converting the sort) should be able to go away w/ the new SortBy implementation, if the
        // lower-level code was modified to use the SortBy.Criterion and SortBy.Direction data (instead of switching on
        // the SortBy itself). We still will need this switch so that we can wrap the results in ReSortingQueryResults.
        boolean isTaskSort = false;
        boolean isLocalizedSort = false;
        boolean isReadSort = false;
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
            case READ_ASC:
                isReadSort = true;
                params.setSortBy(SortBy.READ_ASC);
                break;
            case READ_DESC:
                isReadSort = true;
                params.setSortBy(SortBy.DATE_DESC);
                break;
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                isLocalizedSort = true;
                break;
        }

        ZimbraQueryResults results = zq.execute();
        if (isTaskSort || isReadSort) {
            results = new ReSortingQueryResults(results, originalSort, null);
        }
        if (isLocalizedSort) {
            results = new ReSortingQueryResults(results, originalSort, params);
        }
        return results;
    }

    /**
     * Returns true if any of the specified email addresses exists in contacts, otherwise false.
     */
    public boolean existsInContacts(Collection<InternetAddress> addrs) throws IOException {
        Set<MailItem.Type> types = EnumSet.of(MailItem.Type.CONTACT);
        if (getDeferredCount(types) > 0) {
            try {
                indexDeferredItems(types, new BatchStatus(), false);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Failed to index deferred items", e);
            }
        }

        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            for (InternetAddress addr : addrs) {
                if (!Strings.isNullOrEmpty(addr.getAddress())) {
                    String lcAddr = addr.getAddress().toLowerCase();
                    try (TermFieldEnumeration values = searcher.getIndexReader()
                        .getTermsForField(LuceneFields.L_CONTACT_DATA, lcAddr)) {
                        if (values.hasMoreElements()) {
                            BrowseTerm term = values.nextElement();
                            if (term != null && lcAddr.equals(term.getText())) {
                                ZimbraLog.index.debug("Contact = %s present in indexed items",
                                    lcAddr);
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }


    /**
     * Returns the maximum number of items to be batched in a single indexing pass. If a search comes in that requires
     * use of the index, all deferred unindexed items are immediately indexed regardless of batch size. If this number
     * is {@code 0}, all items are indexed immediately when they are added.
     */
    public int getBatchThreshold() {
        if (indexStore instanceof LuceneIndex) {
            try {
                return mailbox.getAccount().getBatchedIndexingSize();
            } catch (ServiceException e) {
                ZimbraLog.index.warn("Failed to get %s",Provisioning.A_zimbraBatchedIndexingSize, e);
            }
        }
        return 0; // disable batch indexing for non Lucene index stores
    }

    void evict() {
        indexStore.evict();
    }

    public void deleteIndex() throws IOException {
        if (isReIndexInProgress()) {
            cancelReIndex();
        }
        indexStore.deleteIndex();
    }

    /**
     * Submits a task to {@link #INDEX_EXECUTOR}.
     *
     * @param task index task
     * @throws RejectedExecutionException if all index threads are busy
     */
    public void submit(IndexTask task) {
        INDEX_EXECUTOR.submit(task);
    }

    void setIndexingSuspended( boolean suspended) {
        ZimbraLog.index.info("indexSuspended set to %s.  Current deferred count %s", suspended,
                getDeferredCount(EnumSet.noneOf(MailItem.Type.class)));
        indexingSuspended = suspended;
        if (! suspended) {
            numMaybeIndexDeferredItemsCalls = 0;
        }
    }

    /**
     * Attempts to index deferred items.
     */
    void maybeIndexDeferredItems() {
        if ((indexStore != null) && indexStore.isPendingDelete()) {
            ZimbraLog.index.debug("index delete is in progress by other thread, skipping");
            return;  // No point in indexing if we are going to delete the index
        }
        if (indexingSuspended) {
            if ((numMaybeIndexDeferredItemsCalls % 1000) == 0) {
                ZimbraLog.index.debug("Indexing suspended. maybeIndexDeferredItems called %s times whilst suspended",
                        numMaybeIndexDeferredItemsCalls);
                numMaybeIndexDeferredItemsCalls = 0;
            }
            numMaybeIndexDeferredItemsCalls++;
            return;
        }

        // If there was a failure, we trigger indexing even if the deferred count is still low.
        if ((lastFailedTime >= 0 && System.currentTimeMillis() - lastFailedTime > FAILURE_DELAY) ||
                getDeferredCount(EnumSet.noneOf(MailItem.Type.class)) >= getBatchThreshold()) {
            try {
                INDEX_EXECUTOR.submit(new BatchIndexTask());
            } catch (RejectedExecutionException e) {
                ZimbraLog.index.warn("Skipping batch index because all index threads are busy");
            }
        }
    }

    void resumeIndexing() {
        setIndexingSuspended(false);
    }

    void resumeIndexingAndDrainDeferred() {
        resumeIndexing();
        maybeIndexDeferredItems();
        ZimbraLog.index.info("resumeIndexingAndDrainDeferred deferred count=%s",
                getDeferredCount(EnumSet.noneOf(MailItem.Type.class)));
    }

    /**
     * Index deferred items.
     *
     * @param types item types to index, empty set means all types
     * @param wait if an indexing is in progress by other threads, true to wait for them to complete, false to skip
     * indexing
     */
    private void indexDeferredItems(Set<MailItem.Type> types, BatchStatus status, boolean wait)
            throws ServiceException {
        assert(mailbox.lock.isUnlocked());
        if ((indexStore != null) && indexStore.isPendingDelete()) {
            ZimbraLog.index.debug("index delete is in progress by other thread, skipping");
            return;  // No point in indexing if we are going to delete the index
        }

        if (wait) {
            indexLock.acquireUninterruptibly();
        } else if (!indexLock.tryAcquire()) {
            ZimbraLog.index.debug("index is in progress by other thread, skipping");
            return;
        }
        lastFailedTime = -1; // reset
        try {
            long start = System.currentTimeMillis();
            Collection<Integer> ids = getDeferredIds(types);
            indexItemList(ids, status);

            long elapsed = System.currentTimeMillis() - start;
            ZimbraLog.index.info("Batch complete processed=%d,failed=%d,elapsed=%d (%.2f items/sec)",
                    status.getProcessed(), status.getFailed(), elapsed,
                    1000.0 * (status.getProcessed() - status.getFailed()) / elapsed);
        } finally {
            indexLock.release();
        }
    }

    @VisibleForTesting
    public void indexDeferredItems() throws ServiceException {
        indexDeferredItems(EnumSet.noneOf(MailItem.Type.class), new BatchStatus(), true);
    }

    /**
     * Kick off the requested re-index in a background thread. The re-index is run on a best-effort basis, if it fails
     * a WARN message is logged, but it won't be retried.
     */
    public void startReIndex() throws ServiceException {
        startReIndex(new ReIndexTask(mailbox, null));
    }

    public void startReIndexById(Collection<Integer> ids) throws ServiceException {
        startReIndex(new ReIndexTask(mailbox, ids));
    }

    public void startReIndexByType(Set<MailItem.Type> types) throws ServiceException {
        List<Integer> ids;
        DbConnection conn = DbPool.getConnection(mailbox);
        try {
            ids = DbMailItem.getReIndexIds(conn, mailbox, types);
        } finally {
            conn.closeQuietly();
        }
        startReIndexById(ids);
    }

    private synchronized void startReIndex(ReIndexTask task) throws ServiceException {
        if ((indexStore != null) && indexStore.isPendingDelete()) {
            throw ServiceException.FAILURE("Unable to submit reindex request. Index is pending delete", null);
        }
        try {
            if (reIndex != null) {
                throw ServiceException.ALREADY_IN_PROGRESS(
                        Integer.toString(mailbox.getId()), reIndex.status.toString());
            }
            // reIndex and compactIndex cannot interleave
            if (isCompactIndexInProgress()) {
                throw ServiceException.ALREADY_IN_PROGRESS(
                        Integer.toString(mailbox.getId()), "Compact Index");
            }
            REINDEX_EXECUTOR.submit(reIndex = task);
        } catch (RejectedExecutionException e) {
            throw ServiceException.FAILURE("Unable to submit reindex request. Try again later", e);
        }
    }

    public synchronized ReIndexStatus cancelReIndex() {
        if (reIndex == null) {
            return null;
        }
        reIndex.status.cancel();
        return reIndex.status;
    }

    public void startCompactIndex() throws ServiceException {
        startCompactIndex(new CompactIndexTask(mailbox));
    }

    private synchronized void startCompactIndex(CompactIndexTask task) throws ServiceException {
        if ((indexStore != null) && indexStore.isPendingDelete()) {
            throw ServiceException.FAILURE("Unable to submit compact index request. Index is pending delete", null);
        }
        try {
            if (compactIndex != null) {
                throw ServiceException.ALREADY_IN_PROGRESS(
                        Integer.toString(mailbox.getId()), "Compact Index");
            }
            // reIndex and compactIndex cannot interleave
            if (isReIndexInProgress()) {
                throw ServiceException.ALREADY_IN_PROGRESS(
                        Integer.toString(mailbox.getId()), reIndex.status.toString());
            }
            REINDEX_EXECUTOR.submit(compactIndex = task);
        } catch (RejectedExecutionException e) {
            throw ServiceException.FAILURE("Unable to submit compact index request. Try again later", e);
        }
    }

    public boolean verify(PrintStream out) throws ServiceException {
        indexLock.acquireUninterruptibly(); // make sure no writers are opened
        try {
            return indexStore.verify(out);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to verify index", e);
        } finally {
            indexLock.release();
        }
    }

    private class ReIndexTask extends IndexTask {
        private final Collection<Integer> ids;
        private final ReIndexStatus status = new ReIndexStatus();

        ReIndexTask(Mailbox mbox, Collection<Integer> ids) {
            super(mbox);
            this.ids = ids;
        }

        @Override
        public void exec() {
            try {
                ZimbraLog.index.info("Re-index start");

                long start = System.currentTimeMillis();
                reIndex();
                long elapsed = System.currentTimeMillis() - start;
                long avg = 0;
                long mps = 0;

                if (status.getProcessed() > 0) {
                    avg = elapsed / status.getProcessed();
                    mps = avg > 0 ? 1000 / avg : 0;
                }
                ZimbraLog.index.info("Re-index completed items=%d,failed=%d,elapsed=%d (avg %d ms/item, %d items/sec)",
                        status.getTotal(), status.getFailed(), elapsed, avg, mps);
                onCompletion();
            } catch (ServiceException e) {
                if (e.getCode() == ServiceException.INTERRUPTED) {
                    ZimbraLog.index.info("Re-index cancelled %s", status);
                } else {
                    ZimbraLog.index.error("Re-index failed. This mailbox must be manually re-indexed.", e);
                }
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.index.error("Re-index failed. This mailbox must be manually re-indexed.", t);
            } finally {
                synchronized (MailboxIndex.this) {
                    reIndex = null;
                }
            }
        }

        /**
         * Subclass may override to trigger something at end of indexing.
         *
         * @throws ServiceException error
         */
        protected void onCompletion() throws ServiceException {
        }

        /**
         * Re-Index some or all items in this mailbox. This can be a *very* expensive operation (upwards of an hour to
         * run on a large mailbox on slow hardware). We are careful to unlock the mailbox periodically so that the
         * mailbox can still be accessed while the re-index is running, albeit at a slower rate.
         */
        void reIndex() throws ServiceException {
            if (ids == null) { // full re-index
                mailbox.lock.lock();
                try {
                    ZimbraLog.index.info("Resetting DB index data");
                    mailbox.resetIndex();
                    ZimbraLog.index.info("Deleting index store data");
                    try {
                        indexStore.deleteIndex();
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Failed to delete index before re-index", e);
                    }
                    clearDeferredIds();
                } finally {
                    mailbox.lock.release();
                }
                ZimbraLog.index.info("Re-indexing all items");
                indexDeferredItems(EnumSet.noneOf(MailItem.Type.class), status, true);
                // skipping the optimize!!
                // Note: Lucene 3.5.0 highly discourage optimizing the index as
                // it is horribly inefficient and very rarely justified. Please check the API doc for more details.
            } else { // partial re-index
                indexLock.acquireUninterruptibly();
                try {
                    indexItemList(ids, status);
                } finally {
                    indexLock.release();
                }
            }
        }
    }

    private class CompactIndexTask extends IndexTask {

        public CompactIndexTask(Mailbox mbox) {
            super(mbox);
        }

        @Override
        protected void exec() throws Exception {
            try {
                ZimbraLog.index.info("Compact-index start");

                long start = System.currentTimeMillis();
                compact();
                long elapsed = System.currentTimeMillis() - start;
                ZimbraLog.index.info("Compact-index completed elapsed=%d", elapsed);
            } catch (ServiceException e) {
                if (e.getCode() == ServiceException.INTERRUPTED) {
                    ZimbraLog.index.info("Compact-index cancelled");
                } else {
                    ZimbraLog.index.error("Compact-index failed. This mailbox must be re-indexed.", e);
                }
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.index.error("Compact-index failed. This mailbox must be manually re-indexed.", t);
            } finally {
                synchronized (MailboxIndex.this) {
                    compactIndex = null;
                }
            }
        }
    }

    /**
     * Migrate to mailbox version 1.5.
     */
    @SuppressWarnings("deprecation")
    void indexAllDeferredFlagItems() throws ServiceException {
        Set<Integer> ids = new HashSet<Integer>();
        boolean success = false;
        try {
            mailbox.beginTransaction("indexAllDeferredFlagItems", null);
            DbSearchConstraints.Leaf c = new DbSearchConstraints.Leaf();
            c.tags.add(mailbox.getFlagById(Flag.ID_INDEXING_DEFERRED));
            List<DbSearch.Result> list = new DbSearch(mailbox).search(mailbox.getOperationConnection(),
                    c, SortBy.NONE, -1, -1, DbSearch.FetchMode.ID);
            for (DbSearch.Result sr : list) {
                ids.add(sr.getId());
            }
            success = true;
        } finally {
            mailbox.endTransaction(success);
        }

        ReIndexTask task = new ReIndexTask(mailbox, ids) {
            @Override
            protected void onCompletion() {
                try {
                    mailbox.lock.lock();
                    try {
                        boolean success = false;
                        try {
                            mailbox.beginTransaction("indexAllDeferredFlagItems", null);
                            DbSearchConstraints.Leaf c = new DbSearchConstraints.Leaf();
                            c.tags.add(mailbox.getFlagById(Flag.ID_INDEXING_DEFERRED));
                            List<DbSearch.Result> list = new DbSearch(mailbox).search(mailbox.getOperationConnection(),
                                    c, SortBy.NONE, -1, -1, DbSearch.FetchMode.MODCONTENT);

                            List<Integer> deferredTagsToClear = new ArrayList<Integer>();

                            Flag indexingDeferredFlag = mailbox.getFlagById(Flag.ID_INDEXING_DEFERRED);

                            for (DbSearch.Result sr : list) {
                                MailItem item = mailbox.getItemById(sr.getId(), sr.getType());
                                deferredTagsToClear.add(sr.getId());
                                item.tagChanged(indexingDeferredFlag, false);
                            }
                            mailbox.getOperationConnection(); // we must call this before DbMailItem.alterTag
                            DbTag.alterTag(indexingDeferredFlag, deferredTagsToClear, false);
                            success = true;
                        } finally {
                            mailbox.endTransaction(success);
                        }

                        if (!mailbox.getVersion().atLeast(1, 5)) {
                            try {
                                mailbox.updateVersion(new MailboxVersion((short) 1, (short) 5));
                            } catch (ServiceException se) {
                                ZimbraLog.mailbox.warn("Failed to update mbox version after " +
                                        "reindex all deferred items during mailbox upgrade initialization.", se);
                            }
                        }
                    } finally {
                        mailbox.lock.release();
                    }
                } catch (ServiceException se) {
                    ZimbraLog.mailbox.warn("Failed to clear deferred flag after " +
                            "reindex all deferred items during mailbox upgrade initialization.", se);
                }
            }
        };

        try {
            if (ids.isEmpty()) {
                task.onCompletion();
            } else {
                startReIndex(task);
            }
        } catch (RejectedExecutionException e) {
            ZimbraLog.mailbox.warn("Failed to reindex deferred items on mailbox upgrade initialization." +
                    " Skipping (you will have to manually reindex this mailbox)");
        }
    }

    /**
     * Index a potentially very large list of {@link MailItem}s. Iterate through the list of items, fetch each one and
     * call generateIndexData(). Buffer the items, IndexData into a chunk and when the chunk gets sufficiently large,
     * run a Mailbox transaction to actually do the indexing
     *
     * @param ids item IDs to index
     * @param status progress will be written to the status
     * @throws ServiceException {@link ServiceException#INTERRUPTED} if {@link #cancelReIndex()} is called
     */
    private void indexItemList(Collection<Integer> ids, BatchStatus status) throws ServiceException {
        assert(mailbox.lock.isUnlocked());

        status.setTotal(ids.size());
        if (ids.isEmpty()) {
            return;
        }

        // we re-index 'chunks' of items -- up to a certain size or count
        List<Mailbox.IndexItemEntry> chunk = new ArrayList<Mailbox.IndexItemEntry>();
        long chunkByteSize = 0;
        int i = 0;
        for (int id : ids) {
            i++;
            status.addProcessed(1);

            // Fetch the item and generate the list of Lucene documents to index. Do this without holding the Mailbox
            // lock. Once we've accumulated a "chunk" of items, do a mailbox transaction to actually add them to the
            // index.
            ZimbraLog.index.debug("Tokenizing id=%d", id);
            MailItem item = null;
            try {
                mailbox.beginReadTransaction("IndexItemList-Fetch", null);
                item = mailbox.getItemById(id, MailItem.Type.UNKNOWN, false);
            } catch (MailServiceException.NoSuchItemException e) { // fallback to dumpster
                try {
                    item = mailbox.getItemById(id, MailItem.Type.UNKNOWN, true);
                } catch (MailServiceException.NoSuchItemException again) { // The item has just been deleted.
                    ZimbraLog.index.debug("deferred item no longer exist id=%d", id);
                    removeDeferredId(id);
                    continue;
                }
            } catch (MailServiceException e) {
                // fetch without metadata because reindex will regenerate metadata
                if (MailServiceException.INVALID_METADATA.equals(e.getCode()) && isReIndexInProgress()) {
                    UnderlyingData ud = DbMailItem.getById(mailbox, id, MailItem.Type.UNKNOWN, false);
                    ud.metadata = null; // ignore corrupted metadata
                    item = mailbox.getItem(ud);
                } else {
                    throw e;
                }
            } catch (Exception e) {
                ZimbraLog.index.warn("Failed to fetch deferred item id=%d", id, e);
                status.addFailed(1);
                continue;
            } finally {
                mailbox.endTransaction(item != null);
            }
            try {
                chunk.add(new Mailbox.IndexItemEntry(item, item.generateIndexData()));
            } catch (MailItem.TemporaryIndexingException e) {
                ZimbraLog.index.warn("Temporary index failure id=%d", id, e);
                lastFailedTime = System.currentTimeMillis();
                status.addFailed(1);
                continue;
            }
            chunkByteSize += item.getSize();

            if (i == ids.size() || chunkByteSize > MAX_TX_BYTES || chunk.size() >= MAX_TX_ITEMS) {
                // we have a chunk of items and their corresponding index data -- add them to the index
                try {
                    ZimbraLog.index.debug("Batch progress %d/%d", i, ids.size());

                    if (status.isCancelled()) {
                        throw ServiceException.INTERRUPTED("cancelled");
                    }

                    try {
                        boolean success = false;
                        try {
                            mailbox.beginTransaction("IndexItemList-Commit", null);
                            for (Mailbox.IndexItemEntry entry : chunk) {
                                mailbox.addIndexItemToCurrentChange(entry);
                            }
                            success = true;
                        } finally {
                            mailbox.endTransaction(success);
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.index.warn("Failed to index chunk=%s", chunk, e);
                        status.addFailed(chunk.size());
                    }
                } finally {
                    chunk.clear();
                    chunkByteSize = 0;
                }
            }
        }
    }

    /**
     * Mailbox version (1.0,1.1)->1.2 Re-Index all contacts.
     */
    void upgradeMailboxTo1_2() throws ServiceException {
        DbConnection conn = DbPool.getConnection(mailbox);
        try {
            List<Integer> ids = DbMailItem.getReIndexIds(conn, mailbox, EnumSet.of(MailItem.Type.CONTACT));
            if (ids.isEmpty()) {
                return;
            }
            ReIndexTask task = new ReIndexTask(mailbox, ids) {
                @Override
                protected void onCompletion() throws ServiceException {
                    mailbox.lock.lock();
                    try {
                        if (!mailbox.getVersion().atLeast(1, 2)) {
                            try {
                                mailbox.updateVersion(new MailboxVersion((short) 1, (short) 2));
                            } catch (ServiceException e) {
                                ZimbraLog.mailbox.warn("Failed to update mbox version after " +
                                        "reindex contacts on mailbox upgrade initialization.", e);
                            }
                        }
                    } finally {
                        mailbox.lock.release();
                    }
                }
            };
            startReIndex(task);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Failed to reindex contacts on mailbox upgrade initialization." +
                " Skipping (you will have to manually reindex contacts for this mailbox)");
        } finally {
            conn.closeQuietly();
        }
    }

    /**
     * Entry point for Redo-logging system only. Everybody else should use queueItemForIndexing inside a transaction.
     */
    public void redoIndexItem(MailItem item, int itemId, List<IndexDocument> docs) {
        mailbox.lock.lock();
        try {
            Indexer indexer = indexStore.openIndexer();
            try {
                indexer.addDocument(item.getFolder(), item, docs);
            } finally {
                indexer.close();
            }
        } catch (Exception e) {
            ZimbraLog.index.warn("Skipping indexing; Unable to parse message %d", itemId, e);
        } finally {
            mailbox.lock.release();
        }
    }

    /**
     * Deletes index documents. The caller doesn't necessarily hold the mailbox lock.
     */
    synchronized void delete(List<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }

        Indexer indexer;
        try {
            indexer = indexStore.openIndexer();
        } catch (IndexPendingDeleteException e) {
            ZimbraLog.index.debug("delete of ids from index aborted as it is pending delete");
            lastFailedTime = System.currentTimeMillis();
            return;
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to open Indexer", e);
            lastFailedTime = System.currentTimeMillis();
            return;
        }

        try {
            indexer.deleteDocument(ids);
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to delete index documents", e);
        } finally {
            try {
                indexer.close();
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to close Indexer", e);
                return;
            }
        }
        removeDeferredId(ids);
    }

    /**
     * Adds index documents. The caller must hold the mailbox lock.
     */
    synchronized void add(List<IndexItemEntry> entries) throws ServiceException {
        assert(mailbox.lock.isWriteLockedByCurrentThread());
        if (entries.isEmpty()) {
            return;
        }

        Indexer indexer;
        try {
            indexer = indexStore.openIndexer();
        } catch (IndexPendingDeleteException e) {
            ZimbraLog.index.debug("add of entries to index aborted as index is pending delete");
            lastFailedTime = System.currentTimeMillis();
            return;
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to open Indexer", e);
            lastFailedTime = System.currentTimeMillis();
            return;
        }

        List<MailItem> indexed = new ArrayList<MailItem>(entries.size());
        try {
            for (IndexItemEntry entry : entries) {
                if ((indexStore != null) && indexStore.isPendingDelete()) {
                    ZimbraLog.index.debug("add of list of entries to index aborted as index is pending delete");
                    lastFailedTime = System.currentTimeMillis();
                    return;  // No point in indexing if we are going to delete the index
                }
                if (entry.documents == null) {
                    ZimbraLog.index.warn("NULL index data item=%s", entry);
                    continue;
                }

                ZimbraLog.index.debug("Indexing id=%d", entry.item.getId());

                try {
                    indexer.addDocument(entry.item.getFolder(), entry.item, entry.documents);
                } catch (IOException e) {
                    ZimbraLog.index.warn("Failed to index item=%s", entry, e);
                    lastFailedTime = System.currentTimeMillis();
                    continue;
                }
                indexed.add(entry.item);
            }
        } finally {
            try {
                indexer.close();
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to close Indexer", e);
                return;
            }
        }

        List<Integer> ids = new ArrayList<Integer>(indexed.size());
        for (MailItem item : indexed) {
            ids.add(item.getId());
        }
        DbMailItem.setIndexIds(mailbox.getOperationConnection(), mailbox, ids);
        for (MailItem item : indexed) {
            item.mData.indexId = item.getId();
            removeDeferredId(item.getId());
        }
    }

    /**
     * Primes the index for the fastest available search if useful to the underlying IndexStore.
     * This is a very expensive operation especially on large index.
     */
    public void optimize() throws ServiceException {
        indexDeferredItems(EnumSet.noneOf(MailItem.Type.class), new BatchStatus(), true); // index all pending items
        indexStore.optimize();
    }

    /**
     * Compacts the index data by expunging deletes
     * @throws ServiceException
     */
    public void compact() throws ServiceException {
        try {
            Indexer indexer = indexStore.openIndexer();
            try {
                indexer.compact();
            } finally {
                indexer.close();
            }
        } catch (IndexPendingDeleteException e) {
            ZimbraLog.index.debug("Compaction of index aborted as it is pending delete");
        } catch (IOException e) {
            ZimbraLog.index.error("Failed to compact index", e);
        }
    }

    public static final class IndexStats {
        private final int maxDocs;
        private final int numDeletedDocs;

        public IndexStats(int maxDocs, int numDeletedDocs) {
            super();
            this.maxDocs = maxDocs;
            this.numDeletedDocs = numDeletedDocs;
        }

        public int getMaxDocs() {
            return maxDocs;
        }

        public int getNumDeletedDocs() {
            return numDeletedDocs;
        }
    }

    public IndexStats getIndexStats() throws ServiceException {
        int maxDocs = 0;
        int numDeletedDocs = 0;
        try {
            Indexer indexer = indexStore.openIndexer();
            try {
                maxDocs = indexer.maxDocs();
            } finally {
                indexer.close();
            }
            numDeletedDocs = numDeletedDocs();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to open Indexer", e);
        }

        return new IndexStats(maxDocs, numDeletedDocs);
    }

    /**
     * Returns the number of deleted documents.
     * @return number of deleted docs for this index
     * @throws ServiceException
     */
    public int numDeletedDocs() throws ServiceException {
        try {
            try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
                return searcher.getIndexReader().numDeletedDocs();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to open Searcher", e);
        }
    }

    public synchronized ReIndexStatus getReIndexStatus() {
        return reIndex != null ? reIndex.status : null;
    }

    public boolean isReIndexInProgress() {
        return reIndex != null;
    }

    public boolean isCompactIndexInProgress() {
        return compactIndex != null;
    }

    public List<DbSearch.Result> search(DbSearchConstraints constraints, 
            DbSearch.FetchMode fetch, SortBy sort, int offset, int size, boolean inDumpster) throws ServiceException {
            return search(constraints, fetch, sort, offset, size, inDumpster, null);
    }
    
    /**
     * Executes a DB search in a mailbox transaction.
     */
    public List<DbSearch.Result> search(DbSearchConstraints constraints,
            DbSearch.FetchMode fetch, SortBy sort, int offset, int size, boolean inDumpster, Mailbox authMailbox) throws ServiceException {
        List<DbSearch.Result> result;
        boolean success = false;
        try {
            mailbox.beginReadTransaction("search", null);
            result = new DbSearch(mailbox, inDumpster, authMailbox).search(mailbox.getOperationConnection(),
                    constraints, sort, offset, size, fetch);
            if (fetch == DbSearch.FetchMode.MAIL_ITEM) {
                // Convert UnderlyingData to MailItem
                ListIterator<DbSearch.Result> itr = result.listIterator();
                while (itr.hasNext()) {
                    DbSearch.Result sr = itr.next();
                    try {
                        MailItem item = mailbox.getItem(sr.getItemData());
                        itr.set(new ItemSearchResult(item, sr.getSortValue()));
                    } catch (ServiceException se) {
                        ZimbraLog.index.info(String.format(
                            "Problem constructing Result for folder=%s item=%s from UnderlyingData - dropping item",
                                    sr.getItemData().folderId, sr.getItemData().id, sr.getId()), se);
                        itr.remove();
                    }
                }
            }
            success = true;
        } finally {
            mailbox.endTransaction(success);
        }
        return result;
    }

    /* These regexes really shouldn't be complicated - so this value should be way more than enough.
     * Leaving hard coded.  This is the number of accesses allowed to the underlying CharSequence before
     * deciding that too much resource has been used.
     */
    final private static int MAX_REGEX_ACCESSES = 100000;
    /**
     * Returns all domain names from the index.
     *
     * @param field Lucene field name (e.g. LuceneFields.L_H_CC)
     * @param regex matching pattern or null to match everything
     * @return {@link BrowseTerm}s which correspond to all of the domain terms stored in a given field
     */
    public List<BrowseTerm> getDomains(String field, String regex) throws IOException, ServiceException {
        Pattern pattern = Strings.isNullOrEmpty(regex) ? null : Pattern.compile(
                regex.startsWith("@") ? regex : "@" + regex);
        List<BrowseTerm> result = new ArrayList<BrowseTerm>();
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            try (TermFieldEnumeration values = searcher.getIndexReader().getTermsForField(field,
                "")) {
                while (values.hasMoreElements()) {
                    BrowseTerm term = values.nextElement();
                    if (term == null) {
                        break;
                    }
                    String text = term.getText();
                    // Domains are tokenized with '@' prefix. Exclude partial domain tokens.
                    if ((text.startsWith("@") && text.contains(".")) && (pattern == null
                        || AccessBoundedRegex.matches(text, pattern, MAX_REGEX_ACCESSES))) {
                        result.add(new BrowseTerm(text.substring(1), term.getFreq()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns all attachment types from the index.
     *
     * @param regex matching pattern or null to match everything
     * @return {@link BrowseTerm}s which correspond to all of the attachment types in the index
     */
    public List<BrowseTerm> getAttachmentTypes(String regex) throws IOException, ServiceException {
        Pattern pattern = Strings.isNullOrEmpty(regex) ? null : Pattern.compile(regex);
        List<BrowseTerm> result = new ArrayList<BrowseTerm>();
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            try (TermFieldEnumeration values = searcher.getIndexReader()
                .getTermsForField(LuceneFields.L_ATTACHMENTS, "")) {
                while (values.hasMoreElements()) {
                    BrowseTerm term = values.nextElement();
                    if (pattern == null || AccessBoundedRegex.matches(term.getText(), pattern,
                        MAX_REGEX_ACCESSES)) {
                        result.add(term);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns all objects (e.g. PO, etc) from the index.
     *
     * @param regex matching pattern or null to match everything
     * @return {@link BrowseTerm}s which correspond to all of the objects in the index
     */
    public List<BrowseTerm> getObjects(String regex) throws IOException, ServiceException {
        Pattern pattern = Strings.isNullOrEmpty(regex) ? null : Pattern.compile(regex);
        List<BrowseTerm> result = new ArrayList<BrowseTerm>();
        try (ZimbraIndexSearcher searcher = indexStore.openSearcher()) {
            try (TermFieldEnumeration values = searcher.getIndexReader()
                .getTermsForField(LuceneFields.L_OBJECTS, "")) {
                while (values.hasMoreElements()) {
                    BrowseTerm term = values.nextElement();
                    if (term == null) {
                        break;
                    }
                    if (pattern == null || AccessBoundedRegex.matches(term.getText(), pattern,
                        MAX_REGEX_ACCESSES)) {
                        result.add(term);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the index deferred item count for the types.
     *
     * @param types item types, empty set means all types
     * @return index deferred count
     */
    private int getDeferredCount(Set<MailItem.Type> types) {
        SetMultimap<MailItem.Type, Integer> ids;
        try {
            ids = Multimaps.synchronizedSetMultimap(getDeferredIds());
        } catch (ServiceException e) {
            ZimbraLog.index.error("Failed to query deferred IDs", e);
            return 0;
        }

        if (ids == null || ids.isEmpty()) {
            return 0;
        } else if (types.isEmpty()) {
            return ids.size();
        } else {
            int total = 0;
            for (MailItem.Type type : types) {
                total += ids.get(type).size();
            }
            return total;
        }
    }

    private SetMultimap<MailItem.Type, Integer> getDeferredIds() throws ServiceException {
        if (deferredIds == null) {
            DbConnection conn = DbPool.getConnection(mailbox);
            try {
                deferredIds = DbMailItem.getIndexDeferredIds(conn, mailbox);
            } finally {
                conn.closeQuietly();
            }
        }
        return deferredIds;
    }

    private synchronized Collection<Integer> getDeferredIds(Set<MailItem.Type> types) throws ServiceException {
        SetMultimap<MailItem.Type, Integer> ids = getDeferredIds();
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        } else if (types.isEmpty()) {
            return ImmutableSet.copyOf(ids.values());
        } else {
            ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
            for (MailItem.Type type : types) {
                Set<Integer> set = ids.get(type);
                if (set != null) {
                    builder.addAll(set);
                }
            }
            return builder.build();
        }
    }

    /**
     * Adds the item to the deferred queue.
     *
     * @param item item to index
     */
    synchronized void add(MailItem item) {
        switch (item.getIndexStatus()) {
            case NO:
                return;
            case DONE:
                item.mData.indexId = MailItem.IndexStatus.STALE.id();
                break;
            default:
                break;
        }

        if (deferredIds == null) {
            return;
        }

        deferredIds.put(item.getType(), item.getId());
        ZimbraLog.index.debug("deferredIds=%s", deferredIds);
    }

    synchronized void removeDeferredId(int id) {
        assert id > 0 : id;
        if (deferredIds == null) {
            return;
        }
        deferredIds.values().remove(id);
    }

    synchronized void removeDeferredId(Collection<Integer> ids) {
        if (deferredIds == null) {
            return;
        }
        deferredIds.values().removeAll(ids);
    }

    synchronized void clearDeferredIds() {
        deferredIds = null;
    }

    /**
     * Converts conversation type to message type if the type set contains it. We need to index message items when
     * a conversation search is requested.
     */
    private Set<MailItem.Type> toIndexTypes(Set<MailItem.Type> types) {
        if (types.contains(MailItem.Type.CONVERSATION)) {
            types = EnumSet.copyOf(types); // copy
            types.remove(MailItem.Type.CONVERSATION);
            types.add(MailItem.Type.MESSAGE);
        }
        return types;
    }

    /**
     * Batch index progress information. The counters are not thread safe.
     */
    private static class BatchStatus {
        private int total = -1;
        private int processed = 0;
        private int failed = 0;

        void setTotal(int value) {
            total = value;
        }

        void addProcessed(int delta) {
            processed += delta;
        }

        void addFailed(int delta) {
            failed += delta;
        }

        public int getTotal() {
            return total;
        }

        public int getProcessed() {
            return processed;
        }

        public int getFailed() {
            return failed;
        }

        boolean isCancelled() {
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("total", getTotal())
                .add("processed", getProcessed())
                .add("failed", getFailed())
                .toString();
        }
    }

    /**
     * Re-index progress information. The counters are thread safe.
     */
    public static final class ReIndexStatus extends BatchStatus {
        private volatile int total = -1;
        private volatile int processed = 0;
        private volatile int failed = 0;
        private volatile boolean cancel = false;

        private ReIndexStatus() {
        }

        @Override
        void setTotal(int value) {
            total = value;
        }

        @Override
        void addProcessed(int delta) {
            processed += delta;
            if (processed % 2000 == 0) {
                ZimbraLog.index.info("Re-index progress %d/%d", processed, total);
            }
        }

        @Override
        void addFailed(int delta) {
            failed += delta;
        }

        @Override
        public int getTotal() {
            return total;
        }

        @Override
        public int getProcessed() {
            return processed;
        }

        @Override
        public int getFailed() {
            return failed;
        }

        void cancel() {
            cancel = true;
        }

        @Override
        boolean isCancelled() {
            return cancel;
        }
    }

    public static abstract class IndexTask implements Runnable {
        private final Mailbox mailbox;

        public IndexTask(Mailbox mbox) {
            mailbox = mbox;
        }

        @Override
        public final void run() {
            try {
                ZimbraLog.addMboxToContext(mailbox.getId());
                ZimbraLog.addAccountNameToContext(mailbox.getAccount().getName());
                exec();
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.index.error(t.getMessage(), t);
            } finally {
                ZimbraLog.clearContext();
            }
        }

        protected abstract void exec() throws Exception;
    }

    private final class BatchIndexTask extends IndexTask {

        BatchIndexTask() {
            super(mailbox);
        }

        @Override
        protected void exec() throws Exception {
            indexDeferredItems(EnumSet.noneOf(MailItem.Type.class), new BatchStatus(), false);
        }

    }

    private static final class ItemSearchResult extends DbSearch.Result {
        private final MailItem item;

        ItemSearchResult(MailItem item, Object sortkey) {
            super(sortkey);
            this.item = item;
        }

        @Override
        public int getId() {
            return item.getId();
        }

        @Override
        public int getIndexId() {
            return item.getIndexId();
        }

        @Override
        public Type getType() {
            return item.getType();
        }

        @Override
        public MailItem getItem() {
            return item;
        }
    }

}
