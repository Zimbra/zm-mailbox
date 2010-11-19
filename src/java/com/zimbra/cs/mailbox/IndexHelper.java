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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ThreadPool;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Mailbox.BatchedIndexStatus;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.util.Zimbra;

/**
 * Helper class -- basically a dumping ground to move all of the index-oriented things out of Mailbox
 * to try to keep it all in one place
 */
public final class IndexHelper {
    private static final Log sLogger = LogFactory.getLog(IndexHelper.class);

    private static final long sBatchIndexMaxBytesPerTransaction = LC.zimbra_index_max_transaction_bytes.longValue();
    private static final int sBatchIndexMaxItemsPerTransaction = LC.zimbra_index_max_transaction_items.intValue();

    // how frequently is the mailbox allowed to retry indexing deferred items?  The mailbox will ALWAYS try to index deferred items
    // if a text search is run, this only controls other periodic retries.
    private static final long sIndexDeferredItemsRetryIntervalMs = LC.zimbra_index_deferred_items_delay.longValue() * 1000;
    private static final long sIndexItemDeferredRetryDelayAfterFailureMs = 1000 * LC.zimbra_index_deferred_items_failure_delay.longValue();

    private SyncToken mHighestSubmittedToIndex = null;
    // the timestamp of the last time we had a failure-to-index.  Not persisted anywhere.
    // Used so the can throttle deferred-index-retries in a situation where an index
    // is corrupt.  '0' means we think the index is good (we've successfully added to it), nonzero
    // means that we've had failures without success.
    private long mLastIndexingFailureTimestamp = 0;

    // special-case if we're doing a 'full reindex' of the mailbox -- don't block text
    // searches while doing a full reindex...
    private boolean mFullReindexInProgress = false;

    /**
     * Status of current reindexing operation for this mailbox,
     * or NULL if a re-index is not in progress.
     */
    private BatchedIndexStatus mReIndexStatus;
    private long mLastIndexDeferredTime = 0; // the ENDING time of the last index-deferred-items attempt
    private boolean mIndexingDeferredItems = false; // TRUE if we're in the middle of an index-deferred op.
    private Object mIndexingDeferredItemsLock = new Object(); // the lock protects the mIndexingDeferredItems boolean below
    private final Mailbox mailbox;

    static ThreadPool sReIndexThreadPool = new ThreadPool("ReIndex",
        LC.zimbra_index_reindex_pool_size.intValue()); // dont wait for it to complete on shutdown
    private MailboxIndex mailboxIndex;

    IndexHelper(Mailbox mbox) {
        mailbox = mbox;
    }

    static void startup() {}

    static void shutdown() {
        ZimbraLog.index.info("Shutting down IndexHelper thread pools....");
        // don't wait for reindexing to complete,
        sReIndexThreadPool.shutdownNow();
        ZimbraLog.index.info("...ReIndexing threadpool shutdown completed.");
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
    public ZimbraQueryResults search(SoapProtocol proto, OperationContext octxt, SearchParams params)
            throws IOException, ServiceException {
        if (Thread.holdsLock(mailbox))
            throw ServiceException.INVALID_REQUEST("Must not call Mailbox.search() while holding Mailbox lock", null);
        if (octxt == null)
            throw ServiceException.INVALID_REQUEST("The OperationContext must not be null", null);

        try {
            boolean textIndexOutOfSync = mailbox.getIndexDeferredCount() > 0;
            if (mFullReindexInProgress) {
                // if we're doing a full index rebuild, then don't block us waiting for it to complete
                textIndexOutOfSync = false;
            }
            return MailboxIndex.search(proto, octxt, mailbox, params, textIndexOutOfSync);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.TEXT_INDEX_OUT_OF_SYNC) {
                indexDeferredItems();
                // the search itself will implicitly flush
                return MailboxIndex.search(proto, octxt, mailbox, params, false);
            } else throw e;
        }
    }

    public ZimbraQueryResults search(OperationContext octxt, String queryString, byte[] types, SortBy sortBy,
            int chunkSize, boolean inDumpster) throws IOException, ServiceException {
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

    public ZimbraQueryResults search(OperationContext octxt, String queryString, byte[] types, SortBy sortBy,
            int chunkSize) throws IOException, ServiceException {
        return search(octxt, queryString, types, sortBy, chunkSize, false);
    }

    private Object mIndexImmediatelyModeLock = new Object();
    private int mInIndexImmediatelyMode = 0;

    /**
     * Put this mailbox into a temporary (until cleared, or until mailbox reload)
     * "index immediately" mode.  This is useful for message import performance where we are
     * adding a large number of items sequentially to a mailbox.
     *
     * This setting is intentionally stored only in memory -- if the server restarts or the mailbox
     * is somehow reloaded, we revert to the LDAP-set batch index value
     */
    public void setIndexImmediatelyMode() {
        if (Thread.holdsLock(mailbox)) {
            // can't actually flip the bit b/c of deadlock - do it asynchronously
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        setIndexImmediatelyMode();
                    } catch (OutOfMemoryError e) {
                        Zimbra.halt("Out of memory in AsyncSetIndexImmediatelyMode call to " + mailbox, e);
                    }
                }
            };
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        } else {
            synchronized(mIndexImmediatelyModeLock) {
                mInIndexImmediatelyMode++;
            }
            // we have to force the index to completely catch up here as we cannot
            // index immediately if the index is behind
            indexDeferredItems();
        }
    }

    /**
     * Clear the "index immediately" mode setting, return to LDAP-set batched index settings
     */
    public void clearIndexImmediatelyMode() {
        synchronized(mIndexImmediatelyModeLock) {
            mInIndexImmediatelyMode--;
        }
    }

    /** Returns the maximum number of items to be batched in a single indexing
     *  pass.  (If a search comes in that requires use of the index, all
     *  pending unindexed items are immediately indexed regardless of batch
     *  size.)  If this number is <tt>0</tt>, all items are indexed immediately
     *  when they are added. */
    int getBatchedIndexingCount() {
        synchronized (mIndexImmediatelyModeLock) {
            if (mInIndexImmediatelyMode > 0) {
                return 0;
            }
            return getMailboxIndex().getBatchedIndexingCount();
        }
    }

    void evict() {
        getMailboxIndex().evict();
    }

    void deleteIndex() throws IOException {
        getMailboxIndex().deleteIndex();
    }

    List<Integer> deleteDocuments(List<Integer> indexIds) throws IOException {
        return getMailboxIndex().deleteDocuments(indexIds);
    }

    /**
     * This API will periodically attempt to re-try deferred index items.
     */
    void maybeIndexDeferredItems() {
        if (Thread.holdsLock(mailbox)) // don't attempt if we're holding the mailbox lock
            return;

        boolean shouldIndexDeferred = false;
        synchronized (mailbox) {
            if (!mIndexingDeferredItems) {
                if (mailbox.getIndexDeferredCount() >= getBatchedIndexingCount()) {
                    long now = System.currentTimeMillis();

                    if (((now - sIndexItemDeferredRetryDelayAfterFailureMs) > mLastIndexingFailureTimestamp) &&
                                    ((now - sIndexDeferredItemsRetryIntervalMs) > mLastIndexDeferredTime))
                        shouldIndexDeferred = true;
                }
            }
        }
        if (shouldIndexDeferred)
            indexDeferredItems();
    }

    /**
     * "Catch-up" the text index by indexing all the items up to the most recent changeID
     * function makes a "Best attempt" to index all items: it is not guaranteed that the index is
     * completely caught-up when this function returns.
     *
     * This outer function is responsible for synchronization -- it guarantees that only one
     * thread can be in indexDeferredItemsInternal at a time, but does not require us to
     * hold the Mailbox lock during the whole operation.  Threads can block
     * waiting for other indexDeferredItems threads to complete -- they will be run
     * once the operation completes.
     */
    private void indexDeferredItems() {
        assert(!Thread.holdsLock(mailbox));
        assert(!Thread.holdsLock(mIndexingDeferredItemsLock));

        synchronized (mIndexingDeferredItemsLock) {
            synchronized (mailbox) {
                mLastIndexDeferredTime = System.currentTimeMillis();

                // must sync on 'this' to get correct value.  OK to release the
                // lock afterwards as we're just checking for 0 and we know the value
                // can't go DOWN since we're holding mIndexingDeferredItemsLock
                if (mailbox.getIndexDeferredCount() == 0) {
                    return;
                }
            }
            while (mIndexingDeferredItems) {
                try {
                    mIndexingDeferredItemsLock.wait();
                } catch (InterruptedException e) {}
            }
            mIndexingDeferredItems = true;
        }
        try {
            // at this point we know we're the only one in here
            indexDeferredItemsInternal();
        } finally {
            synchronized(mIndexingDeferredItemsLock) {
                synchronized (mailbox) {
                    mLastIndexDeferredTime = System.currentTimeMillis();
                }
                mIndexingDeferredItems = false;
                mIndexingDeferredItemsLock.notify();
            }
        }
    }

    /**
     * Do the actual work, index all the deferred items
     */
    private void indexDeferredItemsInternal() {
        assert(!Thread.holdsLock(mailbox));
        assert(!Thread.holdsLock(mIndexingDeferredItemsLock));
        assert (mIndexingDeferredItems);

        long start = 0;
        if (ZimbraLog.mailbox.isInfoEnabled())
            start = System.currentTimeMillis();

        // Get the list of deferred items to index
        List<SearchResult>items = new ArrayList<SearchResult>();
        synchronized (mailbox) {
            try {
                boolean success = false;
                try {
                    mailbox.beginTransaction("IndexDeferredItems_Select", null);
                    DbSearchConstraints c = new DbSearchConstraints();
                    DbSearchConstraints.NumericRange nr = new DbSearchConstraints.NumericRange();
                    if (mHighestSubmittedToIndex == null)
                        nr.lowest = mailbox.getHighestFlushedToIndex().getChangeId();
                    else
                        nr.lowest = mHighestSubmittedToIndex.getChangeId();

                    // since mod_metadata >= mod_content (always), and there's an index on mod_metadata
                    // generate a SELECT on both constraints, even though we only really care about
                    // the mod_content constraint
                    c.modified.add(nr);
                    c.modifiedContent.add(nr);
                    c.hasIndexId = Boolean.TRUE;
                    DbSearch.search(items, mailbox.getOperationConnection(), c, mailbox, SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                    int deferredCount = mailbox.getIndexDeferredCount();

                    if (items.size() != deferredCount) {
                        ZimbraLog.mailbox.warn("IndexDeferredItems(%s,%d): Deferred count out of sync - found=%d deferred=%d",
                                mHighestSubmittedToIndex, nr.lowest, items.size(), mailbox.getIndexDeferredCount());
                        mailbox.setCurrentChangeIndexDeferredCount(items.size());
                    } else {
                        ZimbraLog.mailbox.debug("IndexDeferredItems(%s,%d): found=%d deferred=%d",
                                mHighestSubmittedToIndex, nr.lowest, items.size(), mailbox.getIndexDeferredCount());
                    }

                    success = true;
                } finally {
                    mailbox.endTransaction(success);
                }
            } catch (ServiceException e) {
                ZimbraLog.mailbox.info("Unable to index deferred items due to exception in step 1", e);
                return;
            }
        }

        BatchedIndexStatus status = mFullReindexInProgress ? mReIndexStatus : new BatchedIndexStatus();
        status.mNumToProcess = items.size();
        try {
            indexItemList(items, status, false);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.info("Exception from Mailbox.indexItemList", e);
        }

        long elapsed = System.currentTimeMillis() - start;
        int success = status.mNumProcessed - status.mNumFailed;
        ZimbraLog.indexing.info("Indexing success=%d elapsed=%d (%.2f items/sec) failed=%d deferred=%d",
                success, elapsed, 1000.0 * success / elapsed, status.mNumFailed, mailbox.getIndexDeferredCount());
    }

    /**
     * Kick off the requested reindexing in a background thread. The reindexing is run on a best-effort basis, if it
     * fails a WARN message is logged but it is not retried.
     */
    public void reIndexInBackgroundThread(OperationContext octxt, Set<Byte> types, Set<Integer> itemIds,
            boolean skipDelete) throws ServiceException {
        startReIndex(new ReIndexTask(octxt, types, itemIds, skipDelete), true);
    }

    private void startReIndex(ReIndexTask task, boolean globalStatus) throws ServiceException {
        try {
            if (globalStatus) {
                synchronized (mailbox) {
                    if (mailbox.index.isReIndexInProgress()) {
                        throw ServiceException.ALREADY_IN_PROGRESS(
                                Integer.toString(mailbox.getId()), mReIndexStatus.toString());
                    }
                    mReIndexStatus = task.mStatus;
                    task.setUseGlobalStatus(true);
                }
            }
            sReIndexThreadPool.execute(task);
        } catch (RejectedExecutionException e) {
            throw ServiceException.FAILURE("Unable to submit reindex request.  Try again later", e);
        }
    }

    private class ReIndexTask implements Runnable {

        protected OperationContext mOctxt;
        protected Set<Byte> mTypesOrNull;
        protected Set<Integer> mItemIdsOrNull;
        protected boolean mSkipDelete;
        protected BatchedIndexStatus mStatus = new BatchedIndexStatus();
        protected boolean mUseGlobalStatus = false;

        ReIndexTask(OperationContext octxt, Set<Byte> typesOrNull, Set<Integer> itemIdsOrNull, boolean skipDelete) {
            mOctxt = octxt;
            mTypesOrNull = typesOrNull;
            mItemIdsOrNull = itemIdsOrNull;
            mSkipDelete = skipDelete;
        }

        void setUseGlobalStatus(boolean value) { mUseGlobalStatus = value; }

        @Override
        public void run() {
            try {
                ZimbraLog.addMboxToContext(mailbox.getId());
                ZimbraLog.addAccountNameToContext(mailbox.getAccount().getName());
                reIndex(mOctxt, mTypesOrNull, mItemIdsOrNull, mSkipDelete);
                onCompletion();
                ZimbraLog.removeMboxFromContext();
                ZimbraLog.removeAccountFromContext();
            } catch (ServiceException e) {
                if (!e.getCode().equals(ServiceException.INTERRUPTED)) {
                    ZimbraLog.indexing.warn("Background reindexing failed for Mailbox %d reindexing will not be completed. " +
                            "The mailbox must be manually reindexed", mailbox.getId(), e);
                }
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.indexing.warn("Failed Async Reindex", t);
            } finally {
                if (mUseGlobalStatus) {
                    synchronized (mailbox) {
                        mReIndexStatus = null;
                    }
                }
            }
        }

        protected void onCompletion() {
            // override me in a subclass to trigger something at end of indexing
        }

        /**
         * Re-Index some or all items in this mailbox.  This can be a *very* expensive operation (upwards of an hour to run
         * on a large mailbox on slow hardware).  We are careful to unlock the mailbox periodically so that the
         * mailbox can still be accessed while the reindex is running, albeit at a slower rate.
         *
         * @param typesOrNull  If NULL then all items are re-indexed, otherwise only the specified types are reindexed.
         * @param itemIdsOrNull List of ItemIDs to reindex.  If this is specified, typesOrNull MUST be null
         * @param skipDelete if TRUE then don't bother deleting the passed-in item IDs
         * @throws ServiceException
         */
        void reIndex(OperationContext octxt, Set<Byte> typesOrNull, Set<Integer> itemIdsOrNull, boolean skipDelete) throws ServiceException {

            if (typesOrNull==null && itemIdsOrNull==null) {
                // special case for reindexing WHOLE mailbox.  We do this differently so that we lean
                // on the existing high-water-mark system (allows us to restart where we left off even
                // if server restarts)
                long start = 0;
                if (ZimbraLog.mailbox.isInfoEnabled())
                    start = System.currentTimeMillis();
                try {
                    synchronized (mailbox) {
                        boolean success = false;
                        try {
                            mailbox.beginTransaction("reIndex_all", octxt, null);
                            mFullReindexInProgress = true;
                            // reindexing everything, just delete the index
                            getMailboxIndex().deleteIndex();

                            // index has been deleted, cancel pending indexes
                            mHighestSubmittedToIndex = new SyncToken(0);

                            // update the idx change tracking
                            mailbox.setCurrentChangeIndexDeferredCount(100000); // big number
                            mailbox.setCurrentChangeHighestModContentIndexed(new SyncToken(0));

                            success = true;
                        } catch (IOException e) {
                            throw ServiceException.FAILURE("Error deleting index before re-indexing", e);
                        } finally {
                            mailbox.endTransaction(success);
                        }
                    }
                    indexDeferredItems();
                } finally {
                    synchronized (mailbox) {
                        mFullReindexInProgress = false;
                    }
                }
                ZimbraLog.mailbox.info("Re-Indexing: Mailbox %d COMPLETED in %d ms",
                        mailbox.getId(), System.currentTimeMillis() - start);
                return;
            }

            if (typesOrNull != null && typesOrNull.isEmpty())
                // passed empty set.  do nothing
                return;
            if (itemIdsOrNull != null && itemIdsOrNull.isEmpty())
                // passed empty set.  do nothing
                return;

            long start = 0;
            if (ZimbraLog.mailbox.isInfoEnabled())
                start = System.currentTimeMillis();

            if (typesOrNull != null && itemIdsOrNull != null)
                throw ServiceException.INVALID_REQUEST("Must only specify one of Types, ItemIds to Mailbox.reIndex", null);

            List<SearchResult> msgs = null;

            //
            // First step, with the mailbox locked:
            //     -- get a list of all messages in the mailbox
            //     -- delete the index
            //
            synchronized (mailbox) {
                boolean success = false;
                try {
                    // Don't pass redoRecorder to beginTransaction.  We have already
                    // manually called log() on redoRecorder because this is a long-
                    // running transaction, and we don't want endTransaction to log it
                    // again, resulting in two entries for the same operation in redolog.
                    mailbox.beginTransaction("reIndex", octxt, null);
                    DbSearchConstraints c = new DbSearchConstraints();
                    if (itemIdsOrNull != null)
                        c.itemIds = itemIdsOrNull;
                    else if (typesOrNull != null)
                        c.types = typesOrNull;

                    msgs = new ArrayList<SearchResult>();
                    DbSearch.search(msgs, mailbox.getOperationConnection(), c, mailbox, SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                    if (!skipDelete) {
                        // if (!wholeMailbox) {
                        // NOT reindexing everything: delete manually
                        List<Integer> toDelete = new ArrayList<Integer>(msgs.size());
                        for (SearchResult s : msgs) {
                            toDelete.add(s.indexId);
                        }
                        try {
                            getMailboxIndex().deleteDocuments(toDelete);
                        } catch (IOException e) {
                            throw ServiceException.FAILURE("Error deleting index before re-indexing", e);
                        }
                    }
                    success = true;
                } finally {
                    mailbox.endTransaction(success);
                }
                mStatus.mNumToProcess = msgs.size();
            }

            indexItemList(msgs, mStatus, true);

            if (ZimbraLog.mailbox.isInfoEnabled()) {
                long end = System.currentTimeMillis();
                long avg = 0;
                long mps = 0;
                if (mStatus.mNumProcessed > 0) {
                    avg = (end - start) / mStatus.mNumProcessed;
                    mps = avg > 0 ? 1000 / avg : 0;
                }
                ZimbraLog.mailbox.info(
                        "Re-Indexing COMPLETED mid=%d,items=%d,failed=%s,elapsed=%d (avg %d ms/item, %d items/sec)",
                        mailbox.getId(), mStatus.mNumToProcess, mStatus.mNumFailed, end - start, avg, mps);
            }
        }
    }

    void indexAllDeferredFlagItems() throws ServiceException {
        Set<Integer> itemSet = new HashSet<Integer>();
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

                for (SearchResult sr : items) {
                    itemSet.add(sr.id);
                }
                success = true;
            } finally {
                mailbox.endTransaction(success);
            }
        }

        ReIndexTask task = new ReIndexTask(null, null, itemSet, true) {
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
                                MailItem item = mailbox.getItemById(sr.id, sr.type);
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
            if (itemSet.isEmpty()) {
                task.onCompletion();
            } else {
                startReIndex(task, false);
            }
        } catch (RejectedExecutionException e) {
            ZimbraLog.mailbox.warn("Failed to reindex deferred items on mailbox upgrade initialization." +
            "  Skipping (you will have to manually reindex this mailbox)");
        }
    }

    /**
     * Called by the indexing subsystem when indexing has completed for the specified items.
     *
     * Each call to this API results in one SQL transaction on the mailbox.
     *
     * The indexing subsystem should attempt to batch the completion callbacks if possible, to
     * lessen the number of SQL transactions.
     */
    public void indexingCompleted(int count, SyncToken newHighestModContent, boolean succeeded) {
        synchronized (mailbox) {
            try {
                boolean success = false;
                try {
                    mailbox.beginTransaction("indexingCompleted", null);
                    ZimbraLog.indexing.debug("IndexingCompletedTask(%d,%d,%b) deferred=%d",
                            count, newHighestModContent, succeeded, mailbox.getIndexDeferredCount());

                    mailbox.getOperationConnection();

                    // update high water mark in DB row
                    SyncToken highestFlushedToIndex = mailbox.getHighestFlushedToIndex();
                    assert(newHighestModContent.after(highestFlushedToIndex));
                    if (!newHighestModContent.after(highestFlushedToIndex)) {
                        ZimbraLog.mailbox.warn("invalid set for HighestModContentIndex " +
                                "-highestFlushedToIndex=%d requested=%d", highestFlushedToIndex, newHighestModContent);
                    } else {
                        // DB index high water mark
                        mailbox.setCurrentChangeHighestModContentIndexed(newHighestModContent);
                    }

                    // update indexDeferredCount in DB row
                    int curIdxDeferred = mailbox.getIndexDeferredCount(); // current count
                    int newCount = curIdxDeferred - count; // new value to set
                    if (newCount < 0) {
                        ZimbraLog.indexing.warn("Count out of whack during indexingCompleted " +
                                "- completed %d entries but current indexDeferred is only %d", count, curIdxDeferred);
                    }
                    mailbox.setCurrentChangeIndexDeferredCount(Math.max(0, newCount));
                    mHighestSubmittedToIndex = null;
                    success = true;
                } finally {
                    mailbox.endTransaction(success);
                }
            } catch (ServiceException e) {
                ZimbraLog.indexing.error("Failed to complete indexing", e);
            }
        }
    }

    /**
     * Index a (protentially very large) list of MailItems.  Extract the indexable data outside of
     * the mailbox lock and then index a chunk of items at a time into the mailbox.
     *
     * @param items
     * @param dontTrackIndexing if TRUE then we're re-indexing data - don't use the
     *                          mod_content highwater mark to track these indexes
     *
     * @throws ServiceException.INTERRUPTED if status.mCancel is set to TRUE (by some other thread, synchronized on the Mailbox)
     *
     */
    private void indexItemList(List<SearchResult> items, BatchedIndexStatus status,
                               boolean dontTrackIndexing) throws ServiceException {
        assert(!Thread.holdsLock(mailbox));
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("indexItemList("+items.size()+" items, "+
                                    (dontTrackIndexing ? "TRUE" : "FALSE"));
        }
        if (items.size() == 0)
            return;

        // sort by mod_content then by ID
        Collections.sort(items, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult lhs, SearchResult rhs) {
                int diff = ((Integer)lhs.extraData) - ((Integer)rhs.extraData);
                if (diff == 0)
                    return lhs.id - rhs.id;
                else
                    return diff;
            }
            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }
        }
        );

        ///////////////////////////////////
        // Do the actual indexing: iterate through the list of items, fetch each one
        // and call generateIndexData().  Buffer the items,IndexData into a chunk
        // and when the chunk gets sufficiently large, run a Mailbox transaction
        // to actually do the indexing

        // we reindex 'chunks' of items -- up to a certain size or count
        List<Mailbox.IndexItemEntry> chunk = new ArrayList<Mailbox.IndexItemEntry>();
        long chunkSizeBytes = 0;

        // track the total number of deferred items which are indexed by looking at the
        // deferred count before and after each chunk transaction
        int itemsAttempted = 0;

        for (Iterator<SearchResult> iter = items.iterator(); iter.hasNext(); ) {
            SearchResult sr = iter.next();
            itemsAttempted++;

            //
            // First step: fetch the MailItem and generate the list Lucene documents to index.
            // Do this without holding the Mailbox lock.  Once we've accumulated a "chunk"
            // of items, do a mailbox transaction to actually add them to the index
            //

            MailItem item = null;
            try {
                item = mailbox.getItemById(null, sr.id, sr.type);
            } catch (ServiceException  e) {
                ZimbraLog.indexing.debug("Failed to fetch deferred item id=%d. Item will not be indexed.", sr.id, e);
            } catch (RuntimeException e) {
                ZimbraLog.indexing.debug("Failed to fetch deferred item id=%d. Item will not be indexed.", sr.id, e);
            }
            if (item != null) {
                chunkSizeBytes += item.getSize();
                try {
                    assert(!Thread.holdsLock(mailbox));
                    chunk.add(new Mailbox.IndexItemEntry(false, item, (Integer)sr.extraData, item.generateIndexData(true)));
                } catch (MailItem.TemporaryIndexingException e) {
                    // temporary error
                    if (!dontTrackIndexing) {
                        ZimbraLog.indexing.info("Temporary error generating index data for item ID: %d. Indexing will be retried",
                                item.getId(), e);
                        mLastIndexingFailureTimestamp = System.currentTimeMillis();
                        throw ServiceException.FAILURE("Temporary indexing exception", e);
                    } else {
                        ZimbraLog.indexing.info("Temporary error generating index data for item ID: %d. Indexing will be skipped",
                                item.getId(), e);
                    }
                }
            } else {
                ZimbraLog.indexing.debug("SKIPPING indexing of item %d %s", sr.id, item);
            }

            int chunkSizeToUse = sBatchIndexMaxItemsPerTransaction;
            if (mLastIndexingFailureTimestamp > 0) {
                // Our most recent index attempts have all failed.  Lets NOT try a big full-size chunk
                // since they are expensive.  Instead we'll try a small number of items and see if
                // we can make any of them index correctly...
                chunkSizeToUse = 5;
            }

            if (!iter.hasNext() || chunkSizeBytes > sBatchIndexMaxBytesPerTransaction || chunk.size() >= chunkSizeToUse) {
                //
                // Second step: we have a chunk of items and their corresponding index data -- add them to the index
                //
                try {
                    if (sLogger.isDebugEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        for (Mailbox.IndexItemEntry ie : chunk) {
                            sb.append(ie.mMailItem.getId()).append('-').append(ie.mModContent).append('-').append(ie.mMailItem.getType()).append(',');
                        }
                        sLogger.debug("Batch Indexing: Mailbox "+ mailbox.getId() + "(" + mailbox.getAccountId() + ")" +
                                ", batchedIndexingCount=" + getBatchedIndexingCount() +
                                ", indexing " + chunk.size() +" items: " + sb.toString());
                    }

                    synchronized (mailbox) {
                        if (status.mCancel) {
                            ZimbraLog.mailbox.warn("CANCELLING batch index of Mailbox " + mailbox.getId()
                                    +" before it is complete.  (" + status.mNumProcessed +
                                    " processed out of " + items.size() + ")");
                            throw ServiceException.INTERRUPTED("ReIndexing Canceled");
                        }
                        try {
                            boolean success = false;
                            try {
                                mailbox.beginTransaction("IndexItemList_Chunk", null);
                                for (Mailbox.IndexItemEntry ie : chunk) {
                                    if (dontTrackIndexing) {
                                        ie.mModContent = NO_CHANGE;
                                    }
                                    mailbox.addIndexItemToCurrentChange(ie);
                                }
                                success = true;
                            } finally {
                                mailbox.endTransaction(success);
                            }
                        } catch (ServiceException e) {
                            if (ZimbraLog.indexing.isInfoEnabled()) {
                                StringBuilder sb = new StringBuilder();
                                for (Mailbox.IndexItemEntry ie : chunk) {
                                    sb.append(ie.mMailItem.getId()).append('-').append(ie.mModContent).append(',');
                                }
                                ZimbraLog.indexing.info("Error deferred-indexing one chunk: %s skipping it (will retry)",
                                        sb, e);
                            }
                        }
                        status.mNumProcessed = itemsAttempted;
                    }
                } finally {
                    chunk.clear();
                    chunkSizeBytes = 0;
                }
            }
            if (ZimbraLog.mailbox.isInfoEnabled() && ((itemsAttempted % 2000) == 0) &&
                    mailbox.index.isReIndexInProgress()) {
                ZimbraLog.mailbox.info("Batch Indexing: Mailbox " + mailbox.getId() +
                        " on item " + itemsAttempted + " out of " + items.size());
            }
        }
    }

    void upgradeMailboxTo1_2() {
        // Version (1.0,1.1)->1.2 Re-Index all contacts
        Set<Byte> types = new HashSet<Byte>();
        types.add(MailItem.TYPE_CONTACT);
        if (!types.isEmpty()) {
            ReIndexTask task = new ReIndexTask(null, types, null, false) {
                @Override
                protected void onCompletion() {
                    synchronized (mailbox) {
                        if (!mailbox.getVersion().atLeast(1, 2)) {
                            try {
                                mailbox.updateVersion(new MailboxVersion((short) 1, (short) 2));
                            } catch (ServiceException se) {
                                ZimbraLog.mailbox.warn("Failed to update mbox version after " +
                                                       "reindex contacts on mailbox upgrade initialization.", se);
                            }
                        }
                    }
                }
            };
            try {
                startReIndex(task, false);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("Failed to reindex contacts on mailbox upgrade initialization." +
                "  Skipping (you will have to manually reindex contacts for this mailbox)");
            }
        }
    }

    private static final int NO_CHANGE = -1;

    /**
     * Entry point for Redo-logging system only. Everybody else should use queueItemForIndexing inside a transaction.
     */
    public void redoIndexItem(MailItem item, boolean deleteFirst, int itemId, List<IndexDocument> docList) {
        synchronized (mailbox) {
            try {
                getMailboxIndex().indexMailItem(mailbox, deleteFirst, docList, item, NO_CHANGE);
            } catch (Exception e) {
                ZimbraLog.indexing.info("Skipping indexing; Unable to parse message %d", itemId, e);
            }
        }
    }

    void indexingPartOfEndTransaction(List<IndexItemEntry> itemsToIndex, List<Integer> itemsToDelete) {
        if (itemsToDelete != null && !itemsToDelete.isEmpty()) {
            try {
                getMailboxIndex().deleteDocuments(itemsToDelete);
            } catch (IOException e) {
                ZimbraLog.indexing.warn("Failed to delete index entries", e);
            }
        }

        try {
            getMailboxIndex().beginWrite();
        } catch (IOException e) {
            ZimbraLog.indexing.warn("Failed to open IndexWriter", e);
            mLastIndexingFailureTimestamp = System.currentTimeMillis();
            return;
        }

        int lastMailItemId = 0;
        try {
            for (IndexItemEntry entry : itemsToIndex) {
                MailItem item = entry.mMailItem;
                lastMailItemId = item.getId();
                if (entry.mDocuments == null) {
                    ZimbraLog.indexing.warn("Got NULL index data. Item %d will not be indexed.", item.getId());
                    continue;
                }

                // 2. Index the item before committing the main transaction.
                if (ZimbraLog.mailbox.isDebugEnabled()) {
                    ZimbraLog.mailbox.debug("indexMailItem(changeId=" + mailbox.getLastChangeID() + ", "
                            + "token=" + entry.mModContent + "-" + entry.mMailItem.getId() + ")");
                }
                SyncToken old = mHighestSubmittedToIndex;
                try {
                    if (entry.mModContent != NO_CHANGE) {
                        // update our in-memory structures (but not the ones in SQL)
                        // so that we don't re-submit the same index items over and over
                        // again
                        mHighestSubmittedToIndex = new SyncToken(entry.mModContent, item.getId());
                    }
                    getMailboxIndex().indexMailItem(mailbox,
                            entry.mDeleteFirst, entry.mDocuments, item, entry.mModContent);
                } catch (ServiceException e) {
                    if (entry.mModContent != NO_CHANGE) {
                        // backout!
                        mHighestSubmittedToIndex = old;
                        throw e;
                    }
                }

                // we successfully indexed something!  The index isn't totally corrupt: zero out the
                // failure timestamp so that indexItemList can use the full transaction size
                mLastIndexingFailureTimestamp = 0;
            }
        } catch (ServiceException e) {
            ZimbraLog.indexing.warn("Failed to index message-id %d - indexing blocked. Possibly corrupt index?",
                    lastMailItemId, e);
            mLastIndexingFailureTimestamp = System.currentTimeMillis();
        } finally {
            getMailboxIndex().endWrite();
        }
    }

    public BatchedIndexStatus getReIndexStatus() {
        synchronized (mailbox) {
            return mReIndexStatus;
        }
    }

    public boolean isReIndexInProgress() {
        return getReIndexStatus() != null;
    }

}
