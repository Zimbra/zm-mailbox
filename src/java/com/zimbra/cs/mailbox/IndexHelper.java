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
import com.zimbra.cs.account.accesscontrol.ZimbraACL;
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
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.index.queryparser.ParseException;


/**
 * Helper class -- basically a dumping ground to move all of the index-oriented things out of Mailbox
 * to try to keep it all in one place
 */
public class IndexHelper {
    private static final Log sLogger = LogFactory.getLog(IndexHelper.class);
    
    private static final long sBatchIndexMaxBytesPerTransaction = LC.zimbra_index_max_transaction_bytes.longValue();
    private static final int sBatchIndexMaxItemsPerTransaction = LC.zimbra_index_max_transaction_items.intValue();

    // how frequently is the mailbox allowed to retry indexing deferred items?  The mailbox will ALWAYS try to index deferred items
    // if a text search is run, this only controls other periodic retries.
    private static final long sIndexDeferredItemsRetryIntervalMs = LC.zimbra_index_deferred_items_delay.longValue() * 1000;
    private static final long sIndexItemDeferredRetryDelayAfterFailureMs = 1000 * LC.zimbra_index_deferred_items_failure_delay.longValue();

    private SyncToken mHighestSubmittedToIndex = null;
    private int mNumIndexingInProgress = 0;
    // the timestamp of the last time we had a failure-to-index.  Not persisted anywhere.
    // Used so the can throttle deferred-index-retries in a situation where an index
    // is corrupt.  '0' means we think the index is good (we've successfully added to it), nonzero
    // means that we've had failures without success.
    private long mLastIndexingFailureTimestamp = 0;

    // special-case if we're doing a 'full reindex' of the mailbox -- don't block text
    // searches while doing a full reindex...
    private boolean mFullReindexInProgress = false;

    /** Status of current reindexing operation for this mailbox, or NULL 
     *  if a re-index is not in progress. */
    private BatchedIndexStatus mReIndexStatus = null;
    private long mLastIndexDeferredTime = 0; // the ENDING time of the last index-deferred-items attempt
    private boolean mIndexingDeferredItems = false; // TRUE if we're in the middle of an index-deferred op.
    private Object mIndexingDeferredItemsLock = new Object(); // the lock protects the mIndexingDeferredItems boolean below
    private Mailbox mMbox;

    static ThreadPool sReIndexThreadPool = new ThreadPool("ReIndex",
        LC.zimbra_index_reindex_pool_size.intValue()); // dont wait for it to complete on shutdown
    static ThreadPool sIndexingCompletedThreadPool = new ThreadPool("IndexingCompleted",
        LC.zimbra_index_completed_pool_size.intValue()); // wait for this to complete on shutdown
    private MailboxIndex   mMailboxIndex = null;

    IndexHelper(Mailbox mbox) { mMbox = mbox; }

    static void startup() {}

    static void shutdown() {
        ZimbraLog.index.info("Shutting down IndexHelper thread pools....");
        // don't wait for reindexing to complete, 
        sReIndexThreadPool.shutdownNow();
        ZimbraLog.index.info("...ReIndexing threadpool shutdown completed.");
        sIndexingCompletedThreadPool.shutdown();
        ZimbraLog.index.info("...IndexingCompleted threadpool shutdown completed.");
    }

    void instantiateMailboxIndex() throws ServiceException {
        mMailboxIndex = new MailboxIndex(getMailbox(), null);
    }

    final private Mailbox getMailbox() { return mMbox; }
    final MailboxIndex getMailboxIndex() { return mMailboxIndex; }

    ZimbraQueryResults search(SoapProtocol proto, OperationContext octxt, SearchParams params) throws IOException, ParseException, ServiceException {
        if (Thread.holdsLock(getMailbox()))
            throw ServiceException.INVALID_REQUEST("Must not call Mailbox.search() while holding Mailbox lock", null);
        if (octxt == null)
            throw ServiceException.INVALID_REQUEST("The OperationContext must not be null", null);

        try {
            boolean textIndexOutOfSync = (getNumNotSubmittedToIndex()>0);
            if (mFullReindexInProgress) // if we're doing a full index rebuild, then don't block us waiting for it to complete
                textIndexOutOfSync = false;
            return MailboxIndex.search(proto, octxt, getMailbox(), params, textIndexOutOfSync);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.TEXT_INDEX_OUT_OF_SYNC) {
                indexDeferredItems();
                // the search itself will implicitly flush
                return MailboxIndex.search(proto, octxt, getMailbox(), params, false);
            } else throw e;
        }
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
    void setIndexImmediatelyMode() {
        if (Thread.holdsLock(getMailbox())) {
            // can't actually flip the bit b/c of deadlock - do it asynchronously
            Thread t = new Thread() {
                public void run() {
                    try {
                        setIndexImmediatelyMode();
                    } catch (OutOfMemoryError e) {
                        Zimbra.halt("Out of memory in AsyncSetIndexImmediatelyMode call to "
                                    +getMailbox(), e);
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
    void clearIndexImmediatelyMode() {
        synchronized(mIndexImmediatelyModeLock) {
            mInIndexImmediatelyMode--;
        }
    }        

    String generateIndexId(int itemId) {
        if (getMailboxIndex() != null)
            return getMailboxIndex().generateIndexId(itemId);
        else
            return null;
    }

    /** Returns the maximum number of items to be batched in a single indexing
     *  pass.  (If a search comes in that requires use of the index, all
     *  pending unindexed items are immediately indexed regardless of batch
     *  size.)  If this number is <tt>0</tt>, all items are indexed immediately
     *  when they are added. */
    int getBatchedIndexingCount() {
        synchronized(mIndexImmediatelyModeLock) {
            if (mInIndexImmediatelyMode>0)
                return 0;

            if (getMailboxIndex() != null)
                return getMailboxIndex().getBatchedIndexingCount();
            return 0;
        }
    }

    void flush() {
        if (mMailboxIndex != null)
            mMailboxIndex.flush();
    }

    void deleteIndex() throws IOException {
        if (mMailboxIndex != null)
            mMailboxIndex.deleteIndex();
    }

    List<String> deleteDocuments(List<String> indexIds) throws IOException {
        if (getMailboxIndex() != null) 
            return getMailboxIndex().deleteDocuments(indexIds);
        else
            return indexIds; // pretend like we have an index, and deleted everything
    }

    /**
     * This API will periodically attempt to re-try deferred index items.
     */
    void maybeIndexDeferredItems() {
        if (Thread.holdsLock(getMailbox())) // don't attempt if we're holding the mailbox lock
            return;

        boolean shouldIndexDeferred = false;
        synchronized (getMailbox()) {
            if (!mIndexingDeferredItems) {
                if (getNumNotSubmittedToIndex() >= getBatchedIndexingCount()) {
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
     * @return Number of items NOT submitted to the index (deferredCount - num in progress)
     */
    int getNumNotSubmittedToIndex() {
        return getMailbox().getIndexDeferredCount() - mNumIndexingInProgress;
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
        assert(!Thread.holdsLock(getMailbox()));
        assert(!Thread.holdsLock(mIndexingDeferredItemsLock));

        synchronized(mIndexingDeferredItemsLock) {
            synchronized(getMailbox()) {
                mLastIndexDeferredTime = System.currentTimeMillis();

                // must sync on 'this' to get correct value.  OK to release the 
                // lock afterwards as we're just checking for 0 and we know the value
                // can't go DOWN since we're holding mIndexingDeferredItemsLock
                if (getNumNotSubmittedToIndex() == 0)
                    return;
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
                synchronized(getMailbox()) {
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
        assert(!Thread.holdsLock(getMailbox()));
        assert(!Thread.holdsLock(mIndexingDeferredItemsLock));
        assert (mIndexingDeferredItems);

        long start = 0;
        if (ZimbraLog.mailbox.isInfoEnabled()) 
            start = System.currentTimeMillis();

        ///////////////////////////////
        // Get the list of deferred items to index 
        List<SearchResult>items = new ArrayList<SearchResult>();
        synchronized(getMailbox()) {
            try {
                boolean success = false;
                try {
                    getMailbox().beginTransaction("IndexDeferredItems_Select", null);
                    DbSearchConstraints c = new DbSearchConstraints();

                    if (mNumIndexingInProgress > 0)
                        assert(mHighestSubmittedToIndex != null);

                    DbSearchConstraints.NumericRange nr = new DbSearchConstraints.NumericRange();
                    if (mHighestSubmittedToIndex == null) 
                        nr.lowest = getMailbox().getHighestFlushedToIndex().getChangeId();
                    else
                        nr.lowest = mHighestSubmittedToIndex.getChangeId();
                    
                    // since mod_metadata >= mod_content (always), and there's an index on mod_metadata
                    // generate a SELECT on both constraints, even though we only really care about 
                    // the mod_content constraint
                    c.modified.add(nr);
                    c.modifiedContent.add(nr);
                    c.hasIndexId = Boolean.TRUE;
                    DbSearch.search(items, getMailbox().getOperationConnection(), c, getMailbox(), SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                    int deferredCount = getMailbox().getIndexDeferredCount();
                    int numInProgress = mNumIndexingInProgress;

                    if ((items.size()+mNumIndexingInProgress) != deferredCount) {
                        if (ZimbraLog.mailbox.isInfoEnabled())
                            ZimbraLog.mailbox.info("IndexDeferredItems("+mHighestSubmittedToIndex
                                                   +", "+nr.lowest
                                                   +"): Deferred count out of sync - found="+items.size()+" " +
                                                   "in progress="+numInProgress+" (deferred count="+getMailbox().getIndexDeferredCount()+")");
                        getMailbox().setCurrentChangeIndexDeferredCount(items.size()+mNumIndexingInProgress);
                    } else {
                        if (ZimbraLog.mailbox.isDebugEnabled())
                            ZimbraLog.mailbox.debug("IndexDeferredItems("+mHighestSubmittedToIndex
                                                    +", "+nr.lowest
                                                    +"): found="+items.size()+" " 
                                                    +"in progress="+numInProgress
                                                    +" (deferred count="+getMailbox().getIndexDeferredCount()+")");
                    }

                    success = true;
                } finally {
                    getMailbox().endTransaction(success);
                }
            } catch (ServiceException e) {
                ZimbraLog.mailbox.info("Unable to index deferred items due to exception in step 1", e);
                return;
            }
        }

        BatchedIndexStatus status = new BatchedIndexStatus();
        status.mNumToProcess = items.size();
        try {
            indexItemList(items, status, false);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.info("Exception from Mailbox.indexItemList", e);
        }

        if (ZimbraLog.mailbox.isInfoEnabled()) {
            long elapsed = System.currentTimeMillis() - start;
            String itemsPerSec = String.format("%.2f", (1000.0*(status.mNumProcessed-status.mNumFailed)) / elapsed);
            int successful = status.mNumProcessed - status.mNumFailed;
            if (ZimbraLog.mailbox.isInfoEnabled())
                ZimbraLog.mailbox.info("Deferred Indexing: submitted "+successful+" items in "+elapsed+"ms ("+itemsPerSec+"/sec). ("+
                                       (status.mNumFailed)+ " items failed to index).  IndexDeferredCount now at "+getMailbox().getIndexDeferredCount()+
                                       " NumNotSubmitted= "+this.getNumNotSubmittedToIndex()); 
        }
    }

    void reIndexInBackgroundThread(OperationContext octxt, Set<Byte> types, Set<Integer> itemIds, boolean skipDelete) throws ServiceException {
        startReIndex(new ReIndexTask(octxt, types, itemIds, skipDelete), true);
    }

    private void startReIndex(ReIndexTask task, boolean globalStatus) throws ServiceException {
        try {
            if (globalStatus) {
                synchronized(getMailbox()) {
                    if (getMailbox().isReIndexInProgress())
                        throw ServiceException.ALREADY_IN_PROGRESS(Long.toString(getMailbox().getId()), mReIndexStatus.toString());
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

        public void run() {
            try {
                ZimbraLog.addMboxToContext(getMailbox().getId());
                ZimbraLog.addAccountNameToContext(getMailbox().getAccount().getName());
                reIndex(mOctxt, mTypesOrNull, mItemIdsOrNull, mSkipDelete);
                onCompletion();
                ZimbraLog.removeMboxFromContext();
                ZimbraLog.removeAccountFromContext();
            } catch (ServiceException e) {
                if (!e.getCode().equals(ServiceException.INTERRUPTED)) { 
                    ZimbraLog.mailbox.warn("Background reindexing failed for Mailbox "+getMailbox().getId()+" reindexing will not be completed.  " +
                                           "The mailbox must be manually reindexed", e);
                }
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.index_add.warn("Caught exception in Async Reindex task", t);
            } finally {
                if (mUseGlobalStatus) {
                    synchronized(getMailbox()) {
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
                    synchronized(getMailbox()) {
                        boolean success = false;
                        try {
                            getMailbox().beginTransaction("reIndex_all", octxt, null);
                            mFullReindexInProgress = true;
                            // reindexing everything, just delete the index
                            if (getMailboxIndex() != null)
                                getMailboxIndex().deleteIndex();

                            // index has been deleted, cancel pending indexes
                            mNumIndexingInProgress = 0;
                            mHighestSubmittedToIndex = new SyncToken(0);

                            // update the idx change tracking
                            getMailbox().setCurrentChangeIndexDeferredCount(100000); // big number
                            getMailbox().setCurrentChangeHighestModContentIndexed(new SyncToken(0));

                            success = true;
                        } catch (IOException e) {
                            throw ServiceException.FAILURE("Error deleting index before re-indexing", e);
                        } finally {
                            getMailbox().endTransaction(success);
                        }
                    }
                    indexDeferredItems();
                } finally {
                    synchronized(getMailbox()) {
                        mFullReindexInProgress = false;
                    }
                }
                if (ZimbraLog.mailbox.isInfoEnabled()) {
                    long end = System.currentTimeMillis();
                    if (getMailboxIndex() != null)
                        getMailboxIndex().flush();
                    ZimbraLog.mailbox.info("Re-Indexing: Mailbox " + getMailbox().getId() + " COMPLETED in "+
                                           (end-start) + "ms");
                }
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

            try {
                //
                // First step, with the mailbox locked: 
                //     -- get a list of all messages in the mailbox
                //     -- delete the index
                //
                synchronized(getMailbox()) {
                    boolean success = false;
                    try {
                        // Don't pass redoRecorder to beginTransaction.  We have already
                        // manually called log() on redoRecorder because this is a long-
                        // running transaction, and we don't want endTransaction to log it
                        // again, resulting in two entries for the same operation in redolog.
                        getMailbox().beginTransaction("reIndex", octxt, null);
                        DbSearchConstraints c = new DbSearchConstraints();
                        if (itemIdsOrNull != null)
                            c.itemIds = itemIdsOrNull; 
                        else if (typesOrNull != null)
                            c.types = typesOrNull;

                        msgs = new ArrayList<SearchResult>();
                        DbSearch.search(msgs, getMailbox().getOperationConnection(), c, getMailbox(), SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                        if (!skipDelete) {
                            // if (!wholeMailbox) {
                            // NOT reindexing everything: delete manually
                            List<String> toDelete = new ArrayList<String>(msgs.size());
                            for (SearchResult s : msgs)
                                toDelete.add(s.indexId);
                            
                            if (getMailboxIndex() != null)
                                getMailboxIndex().deleteDocuments(toDelete);
                        }

                        success = true;
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Error deleting index before re-indexing", e);
                    } finally {
                        getMailbox().endTransaction(success);
                    }
                    mStatus.mNumToProcess = msgs.size();
                }

                indexItemList(msgs, mStatus, true);

                if (ZimbraLog.mailbox.isInfoEnabled()) {
                    long end = System.currentTimeMillis();
                    long avg = 0;
                    long mps = 0;
                    if (mStatus.mNumProcessed>0) {
                        avg = (end - start) / mStatus.mNumProcessed;
                        mps = avg > 0 ? 1000 / avg : 0;                    
                    }
                    if (getMailboxIndex() != null)
                        getMailboxIndex().flush();
                    ZimbraLog.mailbox.info("Re-Indexing: Mailbox " + getMailbox().getId() + " COMPLETED.  Re-indexed "+
                                           mStatus.mNumProcessed
                                           +" items in " + (end-start) + "ms.  (avg "+avg+"ms/item= "+mps+" items/sec)"
                                           +" ("+mStatus.mNumFailed+" failed)");
                }
            } finally {
                if (getMailboxIndex() != null)
                    getMailboxIndex().flush();
            }
        }
    }

    void indexAllDeferredFlagItems() throws ServiceException {
        Set<Integer> itemSet = new HashSet<Integer>();
        synchronized(getMailbox()) {
            boolean success = false;
            try {
                getMailbox().beginTransaction("indexAllDeferredFlagItems", null);
                List<SearchResult> items = new ArrayList<SearchResult>();
                DbSearchConstraints c = new DbSearchConstraints();
                c.tags = new HashSet<Tag>();
                c.tags.add(getMailbox().getFlagById(Flag.ID_FLAG_INDEXING_DEFERRED));
                DbSearch.search(items, getMailbox().getOperationConnection(), c, getMailbox(), 
                                SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                for (SearchResult sr : items) {
                    itemSet.add(sr.id);
                }
                success = true;
            } finally {
                getMailbox().endTransaction(success);
            }
        }

        ReIndexTask task = new ReIndexTask(null, null, itemSet, true) {
            @Override
            protected void onCompletion() {
                try {
                    synchronized(getMailbox()) {
                        boolean success = false;
                        try {
                            getMailbox().beginTransaction("indexAllDeferredFlagItems", null);
                            List<SearchResult> items = new ArrayList<SearchResult>();
                            DbSearchConstraints c = new DbSearchConstraints();
                            c.tags = new HashSet<Tag>();
                            c.tags.add(getMailbox().getFlagById(Flag.ID_FLAG_INDEXING_DEFERRED));
                            DbSearch.search(items, getMailbox().getOperationConnection(), c, getMailbox(),
                                            SortBy.NONE, SearchResult.ExtraData.MODCONTENT);

                            List<Integer> deferredTagsToClear = new ArrayList<Integer>();

                            Flag indexingDeferredFlag = getMailbox().getFlagById(Flag.ID_FLAG_INDEXING_DEFERRED);

                            for (SearchResult sr : items) {
                                MailItem item = getMailbox().getItemById(sr.id, sr.type);
                                deferredTagsToClear.add(sr.id);
                                item.tagChanged(indexingDeferredFlag, false);
                            }
                            getMailbox().getOperationConnection(); // we must call this before DbMailItem.alterTag
                            DbMailItem.alterTag(indexingDeferredFlag, deferredTagsToClear, false);

                            success = true;
                        } finally {
                            getMailbox().endTransaction(success);
                        }

                        if (!getMailbox().getVersion().atLeast(1, 5)) {
                            try {
                                getMailbox().updateVersion(new MailboxVersion((short) 1, (short) 5));
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

    private class IndexingCompletedTask implements Runnable {
        int count;
        SyncToken newHighestModContent;
        boolean succeeded;

        IndexingCompletedTask(int count, SyncToken newHighestModContent, boolean succeeded) {
            this.count = count;
            this.newHighestModContent = newHighestModContent;
            this.succeeded = succeeded;
        }
        public void run() {
            try {
                ZimbraLog.addMboxToContext(getMailbox().getId());
                ZimbraLog.addAccountNameToContext(getMailbox().getAccount().getName());
            } catch (ServiceException e) {}

            synchronized(getMailbox()) {
                try {
                    boolean success = false;
                    try {
                        getMailbox().beginTransaction("indexingCompleted", null);
                        ZimbraLog.index_add.debug("IndexingCompletedTask("+count+","+newHighestModContent+","+succeeded+")"
                                                  +" numInProgress="+mNumIndexingInProgress
                                                  +" curIdxDeferred="+getMailbox().getIndexDeferredCount());
                        if (count > mNumIndexingInProgress) {
                            ZimbraLog.mailbox.warn("IndexingCompleted called with "+count+" but only "+
                                                   mNumIndexingInProgress+"in progress.");
                            count = mNumIndexingInProgress;
                        }

                        getMailbox().getOperationConnection();

                        // update high water mark in DB row
                        SyncToken highestFlushedToIndex = getMailbox().getHighestFlushedToIndex();
                        assert(newHighestModContent.after(highestFlushedToIndex));
                        if (!newHighestModContent.after(highestFlushedToIndex)) {
                            ZimbraLog.mailbox.warn("invalid set for HighestModContentIndex --" +
                                                   " highestFlushedToIndex="+highestFlushedToIndex+
                                                   " requested="+newHighestModContent);
                        } else {
                            // DB index high water mark 
                            getMailbox().setCurrentChangeHighestModContentIndexed(newHighestModContent);
                        }

                        // update indexDeferredCount in DB row
                        int curIdxDeferred = getMailbox().getIndexDeferredCount(); // current count
                        int newCount = curIdxDeferred - count; // new value to set
                        if (newCount < 0) 
                            ZimbraLog.index_add.info("Count out of whack during indexingCompleted " +
                                                     "- completed "+count+" entries but current " +
                                                     "indexDeferred is only "+curIdxDeferred
                                                     +" in progress="+mNumIndexingInProgress);
                        getMailbox().setCurrentChangeIndexDeferredCount(Math.max(0, newCount));

                        // in-memory count of indexing in progress
                        mNumIndexingInProgress -= count;
                        if (mNumIndexingInProgress < 0) {
                            ZimbraLog.index_add.info("IndexingInProgress count out of whack during indexingCompleted"); 
                            mNumIndexingInProgress = 0;
                        }
                        if (mNumIndexingInProgress == 0)
                            mHighestSubmittedToIndex = null;
                        success = true;
                    } finally {
                        getMailbox().endTransaction(success);
                    }
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.info("Caught exception in indexingCompleted: "+e, e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable t) {
                    ZimbraLog.index_add.warn("Caught exception in async IndexingCompletedTask", t);
                }
            }
            ZimbraLog.removeMboxFromContext();
            ZimbraLog.removeAccountFromContext();
        }
    }

    /**
     * Called by the indexing subsystem when indexing has completed for the specified items.
     * 
     * Each call to this API results in one SQL transaction on the mailbox.
     * 
     * The indexing subsystem should attempt to batch the completion callbacks if possible, to
     * lessen the number of SQL transactions.
     *  
     * @param ids
     */
    void indexingCompleted(int count, SyncToken newHighestModContent, boolean succeeded) {
        try {
            sIndexingCompletedThreadPool.execute(new IndexingCompletedTask(count, newHighestModContent, succeeded));
        } catch (RejectedExecutionException e) {
            ZimbraLog.index_add.warn("Unable to submit IndexingCompletedTask("+count+","
                                     +newHighestModContent+","+succeeded+")");
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
        assert(!Thread.holdsLock(getMailbox()));
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("indexItemList("+items.size()+" items, "+
                                    (dontTrackIndexing ? "TRUE" : "FALSE"));
        }
        if (items.size() == 0)
            return;

        // sort by mod_content then by ID
        Collections.sort(items, new Comparator<SearchResult>() {
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
                item = getMailbox().getItemById(null, sr.id, sr.type);
            } catch(ServiceException  e) {
                if (ZimbraLog.index_add.isDebugEnabled())
                    ZimbraLog.index_add.debug("Error fetching deferred item id = " + sr.id + ".  Item will not be indexed.", e);
            } catch(java.lang.RuntimeException e) {
                if (ZimbraLog.index_add.isDebugEnabled())
                    ZimbraLog.index_add.debug("Error fetching deferred item id = " + sr.id + ".  Item will not be indexed.", e);
            }
            if (item != null) {
                chunkSizeBytes += item.getSize();
                try {
                    assert(!Thread.holdsLock(getMailbox()));
                    chunk.add(new Mailbox.IndexItemEntry(false, item, (Integer)sr.extraData, item.generateIndexData(true)));
                } catch (MailItem.TemporaryIndexingException e) {
                    // temporary error
                    if (!dontTrackIndexing) {
                        if (ZimbraLog.index_add.isInfoEnabled())
                            ZimbraLog.index_add.info("Temporary error generating index data for item ID: " + item.getId() + ".  Indexing will be retried", e);
                        mLastIndexingFailureTimestamp = System.currentTimeMillis();
                        throw ServiceException.FAILURE("Temporary indexing exception", e);
                    } else {
                        if (ZimbraLog.index_add.isInfoEnabled())
                            ZimbraLog.index_add.info("Temporary error generating index data for item ID: " + item.getId() + ".  Indexing will be skipped", e);
                    }
                }
            } else {
                if (ZimbraLog.index_add.isDebugEnabled())
                    ZimbraLog.index_add.debug("SKIPPING indexing of item " + sr.id + " ptr=" + item);
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
                        sLogger.debug("Batch Indexing: Mailbox "+ 
                                getMailbox().getId() + "(" + getMailbox().getAccountId() + ")" + 
                                ", batchedIndexingCount=" + getBatchedIndexingCount() +
                                ", indexing " + chunk.size() +" items: " + sb.toString());
                    }
                    
                    synchronized(getMailbox()) {
                        if (status.mCancel) {
                            ZimbraLog.mailbox.warn("CANCELLING batch index of Mailbox "+getMailbox().getId()
                                                   +" before it is complete.  ("+status.mNumProcessed+
                                                   " processed out of "+items.size()+")");                            
                            throw ServiceException.INTERRUPTED("ReIndexing Canceled");
                        }
                        try {
                            boolean success = false;
                            try {
                                getMailbox().beginTransaction("IndexItemList_Chunk", null);
                                for (Mailbox.IndexItemEntry ie : chunk) {
                                    if (dontTrackIndexing)
                                        ie.mModContent = NO_CHANGE;
                                    getMailbox().addIndexItemToCurrentChange(ie);
                                }
                                success = true;
                            } finally {
                                getMailbox().endTransaction(success);
                            }
                        } catch (ServiceException e) {
                            if (ZimbraLog.index_add.isInfoEnabled()) {
                                StringBuilder sb = new StringBuilder();
                                for (Mailbox.IndexItemEntry ie : chunk) {
                                    sb.append(ie.mMailItem.getId()).append('-').append(ie.mModContent).append(',');
                                }
                                ZimbraLog.index_add.info("Error deferred-indexing one chunk: "+sb.toString()+" skipping it (will retry)", e);
                            }
                        }
                        status.mNumProcessed = itemsAttempted;
                    }
                } finally {
                    chunk.clear();
                    chunkSizeBytes = 0;
                }
            }
            if (ZimbraLog.mailbox.isInfoEnabled() && ((itemsAttempted % 2000) == 0) && getMailbox().isReIndexInProgress()) {
                ZimbraLog.mailbox.info("Batch Indexing: Mailbox "+getMailbox().getId()+
                                       " on item "+itemsAttempted+" out of "+items.size());
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
                    synchronized(getMailbox()) {
                        if (!getMailbox().getVersion().atLeast(1, 2)) {
                            try {
                                getMailbox().updateVersion(new MailboxVersion((short) 1, (short) 2));
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

    void redoIndexItem(MailItem item, boolean deleteFirst, int itemId, byte itemType, long timestamp, 
                       boolean noRedo, List<IndexDocument> docList)
    {
        try {
            if (getMailboxIndex() != null) {
                getMailboxIndex().indexMailItem(getMailbox(), deleteFirst, docList, item, NO_CHANGE);
            }
        } catch (Exception e) {
            ZimbraLog.index_add.info("Skipping indexing; Unable to parse message " + itemId + ": " + e.toString(), e);
        }
    }

    void indexingPartOfEndTransaction(List<IndexItemEntry> itemsToIndex, List<String> itemsToDelete) {
        try {
            if (getMailboxIndex() != null && (itemsToDelete != null && !itemsToDelete.isEmpty()))
                getMailboxIndex().deleteDocuments(itemsToDelete);
        } catch (IOException e) {
            if (ZimbraLog.index_add.isDebugEnabled())
                ZimbraLog.index_add.debug("Caught IOException attempting to delete index entries in EndTransaction", e);
        }

        int lastMailItemId = 0;
        try {
            getMailboxIndex().beginWriteOperation();

            for (IndexItemEntry entry : itemsToIndex) {
                MailItem item = entry.mMailItem;
                lastMailItemId = item.getId();
                if (entry.mDocuments == null) {
                    ZimbraLog.index_add.warn("Got NULL index data in endTransaction.  Item "+item.getId()+" will not be indexed.");
                    continue;
                }

                // 2. Index the item before committing the main transaction.
                if (ZimbraLog.mailbox.isDebugEnabled()) {
                    ZimbraLog.mailbox.debug("indexMailItem(changeId="+getMailbox().getLastChangeID()+", "
                                            +"token="+entry.mModContent+"-"
                                            +entry.mMailItem.getId()+")");
                }
                if (getMailboxIndex() != null) {
                    SyncToken old = mHighestSubmittedToIndex;
                    try {
                        if (entry.mModContent != NO_CHANGE) {
                            // update our in-memory structures (but not the ones in SQL)
                            // so that we don't re-submit the same index items over and over
                            // again
                            mNumIndexingInProgress++;
                            mHighestSubmittedToIndex = new SyncToken(entry.mModContent, item.getId());
                        }
                        getMailboxIndex().indexMailItem(getMailbox(), 
                                                        entry.mDeleteFirst, 
                                                        entry.mDocuments, 
                                                        item, 
                                                        entry.mModContent);
                    } catch (ServiceException e) {
                        if (entry.mModContent != NO_CHANGE) {
                            // backout!
                            mHighestSubmittedToIndex = old;
                            mNumIndexingInProgress--;
                            throw e;
                        }
                    }
                }

                // we successfully indexed something!  The index isn't totally corrupt: zero out the 
                // failure timestamp so that indexItemList can use the full transaction size
                mLastIndexingFailureTimestamp = 0;
            }
        } catch (Exception e) {
            ZimbraLog.index_add.warn("Caught exception while indexing message id "+lastMailItemId+" - indexing blocked.  Possibly corrupt index?", e);
            mLastIndexingFailureTimestamp = System.currentTimeMillis();
        } finally {
            try {
                getMailboxIndex().endWriteOperation();
            } catch (Exception e) {
                ZimbraLog.index_add.warn("Caught exception while flushing index - indexing blocked.  Possibly corrupt index?", e);
                mLastIndexingFailureTimestamp = System.currentTimeMillis();
            }
        }
    }

    final BatchedIndexStatus getReIndexStatus() {
        return mReIndexStatus;
    }
}
