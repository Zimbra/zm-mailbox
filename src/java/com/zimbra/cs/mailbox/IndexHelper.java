/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.cs.util.Zimbra;

/**
 * Index related mailbox operations.
 *
 * @author tim
 * @author ysasaki
 */
public final class IndexHelper {

    private static final long MAX_TX_BYTES = LC.zimbra_index_max_transaction_bytes.longValue();
    private static final int MAX_TX_ITEMS = LC.zimbra_index_max_transaction_items.intValue();
    private static final long FAILURE_DELAY = LC.zimbra_index_deferred_items_failure_delay.intValue() * 1000;

    private static final ThreadPoolExecutor INDEX_EXECUTOR = new ThreadPoolExecutor(
            LC.zimbra_index_threads.intValue(), LC.zimbra_index_threads.intValue(),
            Long.MAX_VALUE, TimeUnit.NANOSECONDS, new SynchronousQueue<Runnable>(), new IndexThreadFactory("Index"));
    // Re-index threads are created on demand basis. The number of threads are capped.
    private static final ExecutorService REINDEX_EXECUTOR = new ThreadPoolExecutor(
            0, LC.zimbra_reindex_threads.intValue(), 0L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(), new IndexThreadFactory("ReIndex"));
    private volatile long lastFailedTime = -1;
    // Only one thread may run index at a time.
    private Semaphore indexLock = new Semaphore(1);
    private final Mailbox mailbox;
    private MailboxIndex mailboxIndex;
    // current re-indexing operation for this mailbox, or NULL if a re-index is not in progress.
    private volatile ReIndexTask reIndex;
    private SetMultimap<MailItem.Type, Integer> deferredIds; // guarded by IndexHelper


    IndexHelper(Mailbox mbox) {
        mailbox = mbox;
    }

    /**
     * Starts all index threads.
     */
    static void startup() {
        INDEX_EXECUTOR.prestartAllCoreThreads();
    }

    /**
     * Returns true if the current thread was spawned by {@link #INDEX_EXECUTOR}.
     */
    public static boolean isIndexThread() {
        return Thread.currentThread().getThreadGroup() == IndexThreadFactory.GROUP;
    }

    void instantiateMailboxIndex() throws ServiceException {
        mailboxIndex = new MailboxIndex(mailbox);
    }

    public final MailboxIndex getMailboxIndex() {
        assert(mailboxIndex != null);
        return mailboxIndex;
    }

    /**
     * This is the preferred form of the API call.
     *
     * In order to avoid deadlock, callers MUST NOT be holding the Mailbox lock when calling this API.
     *
     * You MUST call {@link ZimbraQueryResults#doneWithSearchResults()} when you are done with the search results,
     * otherwise resources will be leaked.
     *
     * @param proto soap protocol the request is coming from. Determines the type of Element we create for proxied results.
     * @param octxt Operation Context
     * @param params Search Parameters
     * @return search result
     */
    public ZimbraQueryResults search(SoapProtocol proto, OperationContext octx, SearchParams params)
            throws ServiceException {
        assert(!Thread.holdsLock(mailbox));
        assert(octx != null);

        ZimbraQuery query = MailboxIndex.compileQuery(proto, octx, mailbox, params);
        Set<MailItem.Type> types = toIndexTypes(params.getTypes());
        // no need to index if the search doesn't involve Lucene
        if (query.countTextOperations() > 0 && getDeferredCount(types) > 0) {
            try {
                // don't wait if an indexing is in progress by other thread
                indexDeferredItems(types, new BatchStatus(), false);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Failed to index deferred items", e);
            }
        }
        return MailboxIndex.search(query);
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, Set<MailItem.Type> types,
            SortBy sortBy, int chunkSize, boolean inDumpster) throws ServiceException {
        SearchParams params = new SearchParams();
        params.setQueryStr(queryString);
        params.setTimeZone(null);
        params.setLocale(null);
        params.setTypes(types);
        params.setSortBy(sortBy);
        params.setChunkSize(chunkSize);
        params.setPrefetch(true);
        params.setMode(SearchResultMode.NORMAL);
        params.setInDumpster(inDumpster);
        return search(SoapProtocol.Soap12, octxt, params);
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, Set<MailItem.Type> types,
            SortBy sortBy, int chunkSize) throws ServiceException {
        return search(octxt, queryString, types, sortBy, chunkSize, false);
    }

    /**
     * Returns the maximum number of items to be batched in a single indexing pass. If a search comes in that requires
     * use of the index, all deferred unindexed items are immediately indexed regardless of batch size. If this number
     * is {@code 0}, all items are indexed immediately when they are added.
     */
    public int getBatchThreshold() {
        try {
            return mailbox.getAccount().getIntAttr(Provisioning.A_zimbraBatchedIndexingSize, 0);
        } catch (ServiceException e) {
            ZimbraLog.index.warn("Failed to get " + Provisioning.A_zimbraBatchedIndexingSize, e);
            return 0;
        }
    }

    void evict() {
        getMailboxIndex().evict();
    }

    void deleteIndex() throws IOException {
        getMailboxIndex().deleteIndex();
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

    /**
     * Attempts to index deferred items.
     */
    void maybeIndexDeferredItems() {
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

    /**
     * Index deferred items.
     *
     * @param types item types to index, empty set means all types
     * @param wait if an indexing is in progress by other threads, true to wait for them to complete, false to skip
     * indexing
     */
    private void indexDeferredItems(Set<MailItem.Type> types, BatchStatus status, boolean wait)
            throws ServiceException {
        assert(!Thread.holdsLock(mailbox));

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

    /**
     * Kick off the requested re-index in a background thread. The re-index is run on a best-effort basis, if it fails
     * a WARN message is logged, but it won't be retried.
     */
    public void startReIndex(OperationContext octx) throws ServiceException {
        startReIndex(new ReIndexTask(octx, mailbox, null));
    }

    public void startReIndexById(OperationContext octx, Collection<Integer> ids) throws ServiceException {
        startReIndex(new ReIndexTask(octx, mailbox, ids));
    }

    public void startReIndexByType(OperationContext octx, Set<MailItem.Type> types) throws ServiceException {
        startReIndexById(octx, DbMailItem.getReIndexIds(mailbox, types));
    }

    private synchronized void startReIndex(ReIndexTask task) throws ServiceException {
        try {
            if (reIndex != null) {
                throw ServiceException.ALREADY_IN_PROGRESS(
                        Integer.toString(mailbox.getId()), reIndex.status.toString());
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

    public boolean verify(PrintStream out) throws ServiceException {
        indexLock.acquireUninterruptibly(); // make sure no writers are opened
        try {
            return mailboxIndex.verify(out);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to verify index", e);
        } finally {
            indexLock.release();
        }
    }

    private class ReIndexTask extends IndexTask {
        private final OperationContext octx;
        private final Collection<Integer> ids;
        private final ReIndexStatus status = new ReIndexStatus();

        ReIndexTask(OperationContext octx, Mailbox mbox, Collection<Integer> ids) {
            super(mbox);
            assert(ids == null || !ids.isEmpty());
            this.octx = octx;
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
                ZimbraLog.index.info("Re-index completed items=%d,failed=%s,elapsed=%d (avg %d ms/item, %d items/sec)",
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
                synchronized (IndexHelper.this) {
                    reIndex = null;
                }
            }
        }

        /**
         * Subclass may override to trigger something at end of indexing.
         */
        protected void onCompletion() {
        }

        /**
         * Re-Index some or all items in this mailbox. This can be a *very* expensive operation (upwards of an hour to
         * run on a large mailbox on slow hardware). We are careful to unlock the mailbox periodically so that the
         * mailbox can still be accessed while the re-index is running, albeit at a slower rate.
         */
        void reIndex() throws ServiceException {
            if (ids == null) { // full re-index
                synchronized (mailbox) {
                    boolean success = false;
                    try {
                        mailbox.beginTransaction("re-index-fully", octx, null);
                        DbMailItem.resetIndexId(mailbox);
                        getMailboxIndex().deleteIndex();
                        success = true;
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Failed to delete index before re-index", e);
                    } finally {
                        mailbox.endTransaction(success);
                    }
                }
                clearDeferredIds();
                indexDeferredItems(EnumSet.noneOf(MailItem.Type.class), status, true);
            } else { // partial re-index
                synchronized (mailbox) {
                    boolean success = false;
                    try {
                        // Don't pass redoRecorder to beginTransaction.  We have already manually called log() on
                        // redoRecorder because this is a long running transaction, and we don't want endTransaction to
                        // log it again, resulting in two entries for the same operation in redolog.
                        mailbox.beginTransaction("re-index-partially", octx, null);
                        mailbox.addIndexDeleteToCurrentChange(ids);
                        success = true;
                    } finally {
                        mailbox.endTransaction(success);
                    }
                }
                indexLock.acquireUninterruptibly();
                try {
                    indexItemList(ids, status);
                } finally {
                    indexLock.release();
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
        synchronized (mailbox) {
            boolean success = false;
            try {
                mailbox.beginTransaction("indexAllDeferredFlagItems", null);
                List<SearchResult> items = new ArrayList<SearchResult>();
                DbSearchConstraints c = new DbSearchConstraints();
                c.tags = new HashSet<Tag>();
                c.tags.add(mailbox.getFlagById(Flag.ID_FLAG_INDEXING_DEFERRED));
                DbSearch.search(items, mailbox.getOperationConnection(), c, mailbox,
                        SortBy.NONE, SearchResult.ExtraData.NONE);

                for (SearchResult sr : items) {
                    ids.add(sr.id);
                }
                success = true;
            } finally {
                mailbox.endTransaction(success);
            }
        }

        ReIndexTask task = new ReIndexTask(null, mailbox, ids) {
            @Override
            protected void onCompletion() {
                try {
                    synchronized (mailbox) {
                        boolean success = false;
                        try {
                            mailbox.beginTransaction("indexAllDeferredFlagItems", null);
                            List<SearchResult> items = new ArrayList<SearchResult>();
                            DbSearchConstraints c = new DbSearchConstraints();
                            c.tags = new HashSet<Tag>();
                            c.tags.add(mailbox.getFlagById(Flag.ID_FLAG_INDEXING_DEFERRED));
                            DbSearch.search(items, mailbox.getOperationConnection(), c, mailbox,
                                            SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                            List<Integer> deferredTagsToClear = new ArrayList<Integer>();

                            Flag indexingDeferredFlag = mailbox.getFlagById(Flag.ID_FLAG_INDEXING_DEFERRED);

                            for (SearchResult sr : items) {
                                MailItem item = mailbox.getItemById(sr.id, MailItem.Type.of(sr.type));
                                deferredTagsToClear.add(sr.id);
                                item.tagChanged(indexingDeferredFlag, false);
                            }
                            mailbox.getOperationConnection(); // we must call this before DbMailItem.alterTag
                            DbMailItem.alterTag(indexingDeferredFlag, deferredTagsToClear, false);

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
        assert(!Thread.holdsLock(mailbox));

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

            MailItem item = null;
            try {
                item = mailbox.getItemById(null, id, MailItem.Type.UNKNOWN);
            } catch (Exception  e) {
                ZimbraLog.index.warn("Failed to fetch deferred item id=%d", id, e);
                status.addFailed(1);
                continue;
            }
            try {
                chunk.add(new Mailbox.IndexItemEntry(false, item, item.generateIndexData(true)));
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

                    synchronized (mailbox) {
                        try {
                            boolean success = false;
                            try {
                                mailbox.beginTransaction("IndexItemList_Chunk", null);
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
    void upgradeMailboxTo1_2() {
        try {
            List<Integer> ids = DbMailItem.getReIndexIds(mailbox, EnumSet.of(MailItem.Type.CONTACT));
            if (ids.isEmpty()) {
                return;
            }
            ReIndexTask task = new ReIndexTask(null, mailbox, ids) {
                @Override
                protected void onCompletion() {
                    synchronized (mailbox) {
                        if (!mailbox.getVersion().atLeast(1, 2)) {
                            try {
                                mailbox.updateVersion(new MailboxVersion((short) 1, (short) 2));
                            } catch (ServiceException e) {
                                ZimbraLog.mailbox.warn("Failed to update mbox version after " +
                                        "reindex contacts on mailbox upgrade initialization.", e);
                            }
                        }
                    }
                }
            };
            startReIndex(task);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Failed to reindex contacts on mailbox upgrade initialization." +
                " Skipping (you will have to manually reindex contacts for this mailbox)");
        }
    }

    /**
     * Entry point for Redo-logging system only. Everybody else should use queueItemForIndexing inside a transaction.
     */
    public void redoIndexItem(MailItem item, boolean deleteFirst, int itemId, List<IndexDocument> docList) {
        synchronized (mailbox) {
            try {
                mailboxIndex.beginWrite();
                try {
                    mailboxIndex.indexMailItem(mailbox, deleteFirst, docList, item);
                } finally {
                    mailboxIndex.endWrite();
                }
            } catch (Exception e) {
                ZimbraLog.index.warn("Skipping indexing; Unable to parse message %d", itemId, e);
            }
        }
    }

    /**
     * Adds and deletes index documents. Called by {@link Mailbox#endTransaction(boolean)}.
     *
     * @param add items to add
     * @param del item IDs to delete
     * @return list of items that have successfully been indexed
     * @throws ServiceException DB error
     */
    synchronized void update(List<IndexItemEntry> add, List<Integer> del) throws ServiceException {
        assert(Thread.holdsLock(mailbox));

        try {
            mailboxIndex.beginWrite();
        } catch (IOException e) {
            ZimbraLog.index.warn("Failed to open IndexWriter", e);
            lastFailedTime = System.currentTimeMillis();
            return;
        }

        List<MailItem> indexed = new ArrayList<MailItem>(add.size());

        try {
            if (!del.isEmpty()) {
                try {
                    mailboxIndex.deleteDocuments(del);
                } catch (IOException e) {
                    ZimbraLog.index.warn("Failed to delete index documents", e);
                }
            }

            for (IndexItemEntry entry : add) {
                if (entry.documents == null) {
                    ZimbraLog.index.warn("NULL index data item=%s", entry);
                    continue;
                }

                ZimbraLog.mailbox.debug("index item=%d", entry);

                try {
                    mailboxIndex.indexMailItem(mailbox, entry.deleteFirst, entry.documents, entry.item);
                } catch (ServiceException e) {
                    ZimbraLog.index.warn("Failed to index item=%d", entry, e);
                    lastFailedTime = System.currentTimeMillis();
                    continue;
                }
                indexed.add(entry.item);
            }
        } finally {
            try {
                mailboxIndex.endWrite();
            } catch (IOException e) {
                ZimbraLog.index.error("Failed to commit IndexWriter", e);
                return;
            }
        }

        DbMailItem.setIndexIds(mailbox, indexed);
        for (MailItem item : indexed) {
            item.indexIdChanged(item.getId());
            removeDeferredId(item.getId());
        }
    }

    public synchronized ReIndexStatus getReIndexStatus() {
        return reIndex != null ? reIndex.status : null;
    }

    public boolean isReIndexInProgress() {
        return reIndex != null;
    }

    /**
     * Returns the index deferred item count for the types.
     *
     * @param types item types, empty set means all types
     * @return index deferred count
     */
    private synchronized int getDeferredCount(Set<MailItem.Type> types) {
        SetMultimap<MailItem.Type, Integer> ids;
        try {
            ids = getDeferredIds();
        } catch (ServiceException e) {
            ZimbraLog.index.error("Failed to query deferred IDs", e);
            return 0;
        }

        if (ids.isEmpty()) {
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

    private synchronized SetMultimap<MailItem.Type, Integer> getDeferredIds() throws ServiceException {
        if (deferredIds == null) {
            deferredIds = DbMailItem.getIndexDeferredIds(mailbox);
        }
        return deferredIds;
    }

    private synchronized Collection<Integer> getDeferredIds(Set<MailItem.Type> types) throws ServiceException {
        SetMultimap<MailItem.Type, Integer> ids = getDeferredIds();
        if (ids.isEmpty()) {
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

    synchronized void addDeferredId(MailItem.Type type, int id) {
        assert id > 0 : id;
        if (deferredIds == null) {
            return;
        }
        deferredIds.put(type, id);
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
            return Objects.toStringHelper(this)
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

    private static final class IndexThreadFactory implements ThreadFactory {
        private static final ThreadGroup GROUP = new ThreadGroup("Index");
        static {
            GROUP.setDaemon(true);
        }
        private final String name;
        private final AtomicInteger count = new AtomicInteger(1);

        IndexThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(GROUP, runnable, name + '-' + count.getAndIncrement());
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

}
