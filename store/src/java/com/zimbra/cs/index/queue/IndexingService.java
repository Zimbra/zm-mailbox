package com.zimbra.cs.index.queue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TemporaryIndexingException;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.mailbox.MailboxManager.FetchMode;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ReIndexStatus;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * listens on a shared indexing queue and submits indexing tasks to a executor
 *
 * @author Greg Solovyev
 *
 */
public class IndexingService {
    private ThreadPoolExecutor INDEX_EXECUTOR;
    private ThreadPoolExecutor INDEX_RETRY_EXECUTOR;
    private IndexingQueueAdapter queueAdapter;
    private volatile boolean running = false;
    private Thread queueMonitor;
    private Thread retryQueueMonitor;

    private static IndexingService instance = null;

    private ArrayBlockingQueue<AbstractIndexingTasksLocator> retryQueue = null;
    private Set<Integer> indexVerifiedMailboxes = null;

    private IndexingService() {}

    public static IndexingService getInstance() {
        if (instance == null) {
            synchronized(IndexingService.class) {
                if (instance == null) {
                    instance = new IndexingService();
                }
            }
        }
        return instance;
    }

    public synchronized void startUp() {
        queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        running = true;
        int numThreads = 1;
        String indexURL = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexURL, "solr:http://localhost:7983/solr");
        String[] urlParts = indexURL.split(":", 2);
        String indexURLPrefix = urlParts[0];
        if (indexURLPrefix.equalsIgnoreCase("solrcloud")) {
            numThreads = ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexThreads, 10);
        }

        INDEX_EXECUTOR = new ThreadPoolExecutor(numThreads, numThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new ArrayBlockingQueue<Runnable>(10000), new ThreadFactoryBuilder().setNameFormat("IndexExecutor-%d")
                        .setDaemon(true).build());
        INDEX_EXECUTOR.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        INDEX_EXECUTOR.prestartAllCoreThreads();
        queueMonitor = new Thread(new IndexQueueMonitor());
        queueMonitor.start();

        int retryQueueSize = LC.zimbra_index_retry_message_queue_size.intValue();
        retryQueue = new ArrayBlockingQueue<AbstractIndexingTasksLocator>(retryQueueSize);
        indexVerifiedMailboxes = new HashSet<>();
        INDEX_RETRY_EXECUTOR = new ThreadPoolExecutor(numThreads, numThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new ArrayBlockingQueue<Runnable>(LC.zimbra_index_retry_executor_work_queue_size.intValue()), new ThreadFactoryBuilder().setNameFormat("IndexRetryExecutor-%d")
                        .setDaemon(true).build());
        INDEX_RETRY_EXECUTOR.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        INDEX_RETRY_EXECUTOR.prestartAllCoreThreads();
        retryQueueMonitor = new Thread(new IndexRetryQueueMonitor());
        retryQueueMonitor.start();
    }

    /**
     * orderly shutdown the service
     */
    @PreDestroy
    public synchronized void shutDown() {
        running = false;
        if (queueMonitor != null && queueMonitor.isAlive()) {
            queueMonitor.interrupt();
        }
        if (INDEX_EXECUTOR != null) {
            INDEX_EXECUTOR.purge();
            INDEX_EXECUTOR.shutdownNow(); // terminate any executing tasks
            INDEX_EXECUTOR = null;
        }
        if (retryQueueMonitor != null && retryQueueMonitor.isAlive()) {
            retryQueueMonitor.interrupt();
        }
        if (INDEX_RETRY_EXECUTOR != null) {
            INDEX_RETRY_EXECUTOR.purge();
            INDEX_RETRY_EXECUTOR.shutdownNow(); // terminate any executing tasks
            INDEX_RETRY_EXECUTOR = null;
        }
    }

    public int getNumActiveTasks() {
        return INDEX_EXECUTOR.getActiveCount();
    }

    class IndexQueueMonitor implements Runnable {

        /**
         * Function responsible for processing the non-indexed items from database.
         * @param queueItem
         */
        private void processIndexForMailbox(AbstractIndexingTasksLocator queueItem) {
            DbConnection conn = null;
            try {
                conn = DbPool.getConnection(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID());
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(queueItem.accountID,
                        FetchMode.AUTOCREATE, false);

                if (indexVerifiedMailboxes.contains(mbox.getId())) {
                    ZimbraLog.index.debug("Mailbox id %d is already checked for index consistency", mbox.getId());
                    return;
                }

                List<Integer> nonIndexedItemsFromDB = DbMailItem.getNonIndexedItemsForMailbox(mbox, conn);
                int nonIndexedItemsFromDbCount = nonIndexedItemsFromDB.size();
                ZimbraLog.index.info("%d non-indexed items found for mailbox id %d from database",
                        nonIndexedItemsFromDbCount, mbox.getId());
                if (nonIndexedItemsFromDbCount == 0) {
                    ZimbraLog.index.debug("No non-indexed item found for mailbox id %d from database", mbox.getId());
                    return;
                }

                List<MailItem> itemsToIndex = new ArrayList<MailItem>();
                for (Integer itemId : nonIndexedItemsFromDB) {
                    itemsToIndex.add(mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN));
                    // Too much verbose but is needed for monitoring
                    ZimbraLog.index.info("Added mailitem %d for index retry", itemId);
                }
                int nonIndexedItemsCount = itemsToIndex.size();
                ZimbraLog.index.info("Going to process %d non-indexed mailItems", nonIndexedItemsCount);

                if (nonIndexedItemsCount != 0) {
                    AbstractIndexingTasksLocator itemLocator = new AddMailItemToIndexTask(itemsToIndex,
                            mbox.getAccountId(), mbox.getId(), mbox.getSchemaGroupId(),
                            mbox.attachmentsIndexingEnabled(), false);
                    retryQueue.put(itemLocator);
                    indexVerifiedMailboxes.add(mbox.getId());
                }
            } catch (ServiceException | InterruptedException e) {
                // Exception is being swallowed because it should not effect the indexing for
                // the mailitem for which the current check is being performed
                ZimbraLog.index.error("IndexQueueMonitor - handleIndex - exception", e);
            } finally {
                DbPool.quietClose(conn);
            }
        }

        @Override
        public void run() {
            ZimbraLog.index.info("Started index queue monitoring thread %s", Thread.currentThread().getName());
            while (running) {
                try {
                    if (!Zimbra.started()) {
                        Thread.sleep(100); // avoid a tight loop
                        continue;
                    }

                    AbstractIndexingTasksLocator queueItem = queueAdapter.take();

                    if(queueItem == null) {
                        Thread.sleep(ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueuePollingInterval, 500)); // avoid a tight loop
                        continue;
                    }

                    processIndexForMailbox(queueItem);

                    if (INDEX_EXECUTOR.isTerminating() || INDEX_EXECUTOR.isShutdown()) {
                        // this thread will not process this item, so put it
                        // back in the queue for other threads to process
                        queueAdapter.put(queueItem);
                        break;
                    }

                    ZimbraLog.index.debug("IndexQueueMonitor - %s - Actively task executing threads: %d, Queue size: %d",
                            Thread.currentThread().getName(), INDEX_EXECUTOR.getActiveCount(), INDEX_EXECUTOR.getQueue().size());

                    try {
                        if (queueItem instanceof AddMailItemToIndexTask) {
                            if (((AddMailItemToIndexTask) queueItem).isReindex()) {
                                if (queueAdapter.getTaskStatus(queueItem.getAccountID()) != ReIndexStatus.STATUS_ABORTED) {
                                    ZimbraLog.index.debug("%s submitting an re-indexing task MailItemIndexTask for account %s", Thread
                                            .currentThread().getName(), queueItem.getAccountID());
                                    INDEX_EXECUTOR.submit(new MailItemIndexTask((AddMailItemToIndexTask) queueItem));
                                } else {
                                    // skip the tasks and they will
                                    // automatically get drained
                                    queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(), ((AddMailItemToIndexTask) queueItem).getMailItemsToAdd().size() );
                                    ZimbraLog.index
                                            .debug("%s ignoring re-indexing task MailItemIndexTask for account %s. Re-indexing has been aborted.",
                                                    Thread.currentThread().getName(), queueItem.getAccountID());
                                }
                            } else {
                                ZimbraLog.index.debug("%s submitting an indexing task MailItemIndexTask for account %s", Thread
                                        .currentThread().getName(), queueItem.getAccountID());
                                INDEX_EXECUTOR.submit(new MailItemIndexTask((AddMailItemToIndexTask) queueItem));
                            }
                        } else if (queueItem instanceof AddToIndexTaskLocator) {
                            if (((AddToIndexTaskLocator) queueItem).isReindex()) {
                                if (queueAdapter.getTaskStatus(queueItem.getAccountID()) != ReIndexStatus.STATUS_ABORTED) {
                                    ZimbraLog.index.debug("%s submitting an re-indexing task IndexingTask for account %s", Thread
                                            .currentThread().getName(), queueItem.getAccountID());
                                    INDEX_EXECUTOR.submit(new IndexingTask((AddToIndexTaskLocator) queueItem));
                                } else {
                                    // skip the tasks and they will
                                    // automatically get drained
                                    queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(), 1 );
                                    ZimbraLog.index
                                            .debug("%s ignoring re-indexing task IndexingTask for account %s. Re-indexing has been aborted.",
                                                    Thread.currentThread().getName(), queueItem.getAccountID());
                                }
                            } else {
                                ZimbraLog.index.debug("%s submitting an indexing task IndexingTask for account %s", Thread
                                        .currentThread().getName(), queueItem.getAccountID());
                                INDEX_EXECUTOR.submit(new IndexingTask((AddToIndexTaskLocator) queueItem));
                            }
                        } else if (queueItem instanceof DeleteFromIndexTaskLocator) {
                            ZimbraLog.index.debug("%s submitting a delete-from-index task for account %s", Thread
                                    .currentThread().getName(), queueItem.getAccountID());
                            INDEX_EXECUTOR.submit(new DeleteFromIndexTask((DeleteFromIndexTaskLocator) queueItem));
                        }
                    } catch (RejectedExecutionException e) {
                        ZimbraLog.index.error("Indexing task is rejected", e);
                        queueAdapter.put(queueItem);
                    }
                } catch (InterruptedException e) {
                    // must be shutting down, if !running will break
                    // automatically, otherwise will continue
                }
            }
            ZimbraLog.index.info("Stopping indexing thread %s", Thread.currentThread().getName());
        }
    }

    class IndexRetryQueueMonitor implements Runnable {
        @Override
        public void run() {
            ZimbraLog.index.info("Started index retry queue monitoring thread %s", Thread.currentThread().getName());
            while (running) {
                try {
                    if (!Zimbra.started()) {
                        Thread.sleep(100); // avoid a tight loop
                        continue;
                    }

                    AbstractIndexingTasksLocator queueItem = retryQueue.take();

                    if(queueItem == null) {
                        Thread.sleep(ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueuePollingInterval, 500)); // avoid a tight loop
                        continue;
                    }

                    if (INDEX_RETRY_EXECUTOR.isTerminating() || INDEX_RETRY_EXECUTOR.isShutdown()) {
                        // this thread will not process this item, so put it
                        // back in the queue for other threads to process
                        retryQueue.put(queueItem);
                        break;
                    }

                    ZimbraLog.index.debug("IndexRetryQueueMonitor - %s - Actively task executing threads: %d, Queue size: %d",
                            Thread.currentThread().getName(), INDEX_RETRY_EXECUTOR.getActiveCount(), INDEX_RETRY_EXECUTOR.getQueue().size());

                    try {
                        if (queueItem instanceof AddMailItemToIndexTask) {
                            ZimbraLog.index.debug("%s submitting an indexing task MailItemIndexTask for account %s",
                                    Thread.currentThread().getName(), queueItem.getAccountID());
                            INDEX_RETRY_EXECUTOR.submit(new MailItemIndexTask((AddMailItemToIndexTask) queueItem));
                        } else {
                            // The flow should never be reaching here as we are retrying only on
                            // AddMailItemToIndexTask
                            ZimbraLog.index.error("%s Not supported type for account %s",
                                    Thread.currentThread().getName(), queueItem.getAccountID());
                        }
                    } catch (RejectedExecutionException e) {
                        ZimbraLog.index.error("Indexing retry task is rejected", e);
                        retryQueue.put(queueItem);
                    }
                } catch (InterruptedException e) {
                    // must be shutting down, if !running will break
                    // automatically, otherwise will continue
                }
            }
            ZimbraLog.index.info("Stopping indexing retry thread %s", Thread.currentThread().getName());
        }
    }

    public boolean isRunning() {
        return running;
    }

    class DeleteFromIndexTask implements Runnable {
        private DeleteFromIndexTaskLocator queuedTask;

        public DeleteFromIndexTask(DeleteFromIndexTaskLocator task) {
            queuedTask = task;
        }

        @Override
        public void run() {
            ZimbraLog.index.debug("Started DeleteFromIndexTask %s", Thread.currentThread().getName());
            int maxRetries = 0;
            try {
                maxRetries = Provisioning.getInstance().getLocalServer().getMaxIndexingRetries();
                IndexStore indexStore = IndexStore.getFactory().getIndexStore(queuedTask.getAccountID());
                indexStore.openIndexer().deleteDocument(queuedTask.getItemIds());
                ZimbraLog.index.debug("Finished delete-from-index task " + Thread.currentThread().getName());
            } catch (Exception e) {
                if(queuedTask.getRetries() < maxRetries) {
                    ZimbraLog.index.warn("An attempt to delete %d items from index of account %s has failed. Will retry.", queuedTask.getItemIds().size(), queuedTask.getAccountID(), e);
                    queuedTask.addRetry();
                    queueAdapter.put(queuedTask);
                } else {
                    ZimbraLog.index.error("Permanently failed to delete %d items from index of account %s after %d attempts", queuedTask.getItemIds().size(), queuedTask.getAccountID(), queuedTask.getRetries(), e);
                }
            } finally {
                ZimbraLog.clearContext();
            }
        }
    }

    class MailItemIndexTask implements Runnable {

        private AddMailItemToIndexTask queueItem;

        public MailItemIndexTask(AddMailItemToIndexTask item) {
            queueItem = item;
        }

        private boolean handleAbortForReindexIfSet()
        {
            if (queueItem.isReindex() && queueAdapter.getTaskStatus(queueItem.getAccountID()) == ReIndexStatus.STATUS_ABORTED) {
                queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(), queueItem.getMailItemsToAdd().size() );
                ZimbraLog.index.debug("IndexingService - MailItemIndexTask - %s ignoring reindexing task for account %s.",
                                Thread.currentThread().getName(), queueItem.getAccountID());
                return true;
            }
            ZimbraLog.index.debug("Task Status for account %s is %d",queueItem.getAccountID() , queueAdapter.getTaskStatus(queueItem.getAccountID()));
            return false;
        }

        /** Function responsible to handle adding to retry queue based on number of retries.
         *  It increases the sleep duration exponentially based upon the number of times retries is done.
         */
        private void handleRetryForMailItems() {
            try {
                int maxRetries = Provisioning.getInstance().getLocalServer().getMaxIndexingRetries();
                if (queueItem.getRetries() < maxRetries) {
                    ZimbraLog.index.debug(
                            "MailItemIndexTask - handleRetryForMailItems - received to index %d mail items for account %s after %d attempts. Adding in retry queue.",
                            queueItem.getMailItemsToAdd().size(), queueItem.getAccountID(), queueItem.getRetries());
                    // Exponentially increase the sleep duration based on the number of retries
                    Thread.sleep((long) (1000 * 30 * Math.pow(2, queueItem.getRetries())));
                    queueItem.addRetry();
                    retryQueue.add(queueItem);
                    return;
                }
            } catch (ServiceException | InterruptedException e) {
                ZimbraLog.index.error("MailItemIndexTask - handleRetryForMailItems - ", e);
                // Flow should never reach here in normal circumstances hence not increasing the
                // retry counter and giving object one more chance to get indexed
                retryQueue.add(queueItem);
                return;
            }
            ZimbraLog.index.error(
                    "MailItemIndexTask - handleRetryForMailItems - permanently failed to index %d mail items for account %s after %d attempts.",
                    queueItem.getMailItemsToAdd().size(), queueItem.getAccountID(), queueItem.getRetries());
        }

        /**
         * Function responsible to set the DB entries for an IndexItemEntry 
         * @param indexItemEntries
         * @throws ServiceException
         * @throws IOException
         */
        private void setIndexIds(List<IndexItemEntry> indexItemEntries) throws ServiceException, IOException {
            DbConnection conn = null;
            try {
                ZimbraLog.index.debug("MailItemIndexTask - setIndexIds called with %d IndexItemEntry size.",
                        indexItemEntries.size());
                conn = DbPool.getConnection(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID());
                IndexStore indexStore = IndexStore.getFactory().getIndexStore(queueItem.getAccountID());
                if (indexItemEntries.size() > 0) {
                    indexStore.openIndexer().add(indexItemEntries);
                    List<Integer> indexedIds = new ArrayList<Integer>();
                    for (IndexItemEntry entry : indexItemEntries) {
                        indexedIds.add(entry.getItem().getId());
                        //itemsDone.add(e)
                    }
                    if (indexedIds.size() > 0) {
                        DbMailItem.setIndexIds(conn, queueItem.getMailboxSchemaGroupID(), queueItem.getMailboxID(),
                                indexedIds, Provisioning.getInstance().getAccountById(queueItem.getAccountID())
                                        .isDumpsterEnabled());
                    }
                }
                conn.commit();
            } finally {
                DbPool.quietClose(conn);
            }
        }

        @Override
        public void run() {
            ZimbraLog.index.debug("MailItemIndexTask - Started indexing task %s with %d mailItems",
                    Thread.currentThread().getName(), queueItem.getMailItemsToAdd().size());
            List<IndexItemEntry> indexItemEntries = new ArrayList<IndexItemEntry>();
            List<MailItem> itemsDone = new ArrayList<MailItem>();
            try {
                if (!handleAbortForReindexIfSet()) {
                    for (MailItem mailItem : queueItem.getMailItemsToAdd()) {
                        List<IndexDocument> docs = mailItem
                                .generateIndexDataAsync(queueItem.attachmentIndexingEnabled());
                        if (docs.size() > 0) {
                            indexItemEntries.add(new IndexItemEntry(mailItem, docs));
                        }
                        itemsDone.add(mailItem);
                    }

                    setIndexIds(indexItemEntries);

                    // status reporting
                    if (queueItem.isReindex()) {
                        queueAdapter.incrementSucceededMailboxTaskCount(queueItem.getAccountID(),
                                queueItem.getMailItemsToAdd().size());
                    }
                }
                ZimbraLog.index.debug("%s processed %d items", Thread.currentThread().getName(),
                        queueItem.getMailItemsToAdd().size());
            } catch (TemporaryIndexingException e) {
                ZimbraLog.index.warn("MailItemIndexTask - TemporaryIndexingException. Going to add in retry queue.", e);
                try {
                    if(indexItemEntries.size() > 0) {
                        setIndexIds(indexItemEntries);
                        queueItem.removeMailItems(itemsDone);
                    }
                    handleRetryForMailItems();
                } catch (Exception ei) {
                    ZimbraLog.index.errorQuietly("MailItemIndexTask - exception - ", ei);
                }

            } catch (Exception e) {
                ZimbraLog.index.errorQuietly("MailItemIndexTask - exception - ", e);
                if (queueItem.isReindex()) {
                    queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(),
                            queueItem.getMailItemsToAdd().size());
                }

            } finally {
                ZimbraLog.clearContext();
            }
            ZimbraLog.index.debug("MailItemIndexTask - Finished indexing task %s", Thread.currentThread().getName());
        }
    }

    class IndexingTask implements Runnable {
        private AddToIndexTaskLocator queueItem;

        public IndexingTask(AddToIndexTaskLocator item) {
            queueItem = item;
        }

        @Override
        public void run() {
            // list of items that were sent to Indexer
            List<MailItem> indexedItems = new ArrayList<MailItem>();
            ZimbraLog.index.debug("Started indexing task %s", Thread.currentThread().getName());
            int maxRetries = 0;
            try {
                maxRetries = Provisioning.getInstance().getLocalServer().getMaxIndexingRetries();
                IndexStore indexStore = IndexStore.getFactory().getIndexStore(queueItem.getAccountID());
                MailItem.UnderlyingData ud = null;
                List<MailItemIdentifier> itemsToIndex = queueItem.getMailItemsToAdd();
                DbConnection conn = DbPool.getConnection(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID());
                try {
                    List<IndexItemEntry> indexItemEntries = new ArrayList<IndexItemEntry>();
                    for (MailItemIdentifier itemID : itemsToIndex) {
                        try {
                            ud = DbMailItem.getById(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID(),
                                    itemID.getId(), itemID.getType(), itemID.isInDumpster(), conn);
                        } catch (NoSuchItemException ex) {// item may have been moved to/from Dumpster after being queued for indexing
                            try {
                                ud = DbMailItem.getById(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID(),
                                        itemID.getId(), itemID.getType(), !itemID.isInDumpster(), conn);
                            } catch (NoSuchItemException nex) {// could not find this item in Dumpster either.
                                // Log an error.
                                ZimbraLog.index.error("Could not find item %d in mailbox %d account %s",
                                        itemID.getId(), queueItem.getMailboxID(), queueItem.getAccountID(), nex);
                                if (queueItem.isReindex()) {
                                    // Log a failed item for re-index batch status reporting.
                                    // Do not add this item to indexedItems and do not put it back into the queue.
                                    // Move on.
                                    queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(), 1);
                                }
                                continue;
                            }
                        }

                        if (ud != null) {
                            // get the mail item's body
                            MailItem item = MailItem.constructItem(
                                    Provisioning.getInstance().getAccountById(queueItem.getAccountID()), ud,
                                    queueItem.getMailboxID());
                            indexItemEntries.add(new IndexItemEntry(item, item.generateIndexDataAsync(queueItem
                                    .attachmentIndexingEnabled())));
                            indexedItems.add(item);
                        } else {
                            // either something is seriously messed up or this
                            // item was deleted and purged before it could be
                            // indexed
                            ZimbraLog.index.warn("Could not find underlying data for item %d account %s mailbox %d",
                                    itemID.getId(), queueItem.getAccountID(), queueItem.getMailboxID());
                        }
                    }
                    /*
                     * do this at the end of the loop for two reasons: 1) run a
                     * single SQL UPDATE statement with multiple IDs instead of
                     * multiple statements 2) send multiple documents to Solr in
                     * a single request instead of multiple requests in the
                     * event that the preceding loop throws an exception,
                     * neither DB nor Solr will not be updated and the queueItem
                     * will be pushed back into the queue in 'finally' block
                     */
                    if (indexItemEntries.size() > 0) {
                        indexStore.openIndexer().add(indexItemEntries);
                        List<Integer> indexedIds = new ArrayList<Integer>();
                        for (IndexItemEntry entry : indexItemEntries) {
                            indexedIds.add(entry.getItem().getId());
                        }
                        if (indexedIds.size() > 0) {
                            DbMailItem.setIndexIds(conn, queueItem.getMailboxSchemaGroupID(), queueItem.getMailboxID(),
                                    indexedIds, Provisioning.getInstance().getAccountById(queueItem.getAccountID())
                                            .isDumpsterEnabled());
                        }
                    }
                    conn.commit();

                    // status reporting
                    if (queueItem.isReindex()) {
                        queueAdapter.incrementSucceededMailboxTaskCount(queueItem.getAccountID(), indexedItems.size());
                    }
                    ZimbraLog.index.debug("%s processed %d items", Thread.currentThread().getName(),
                            itemsToIndex.size());
                } finally {
                    DbPool.quietClose(conn);
                }
                ZimbraLog.index.debug("Finished indexing task %s", Thread.currentThread().getName());
            } catch (Exception e) {
                /*
                 * If we caught an exception and we still have retries put the item back in the queue.
                 * sending an item to Solr index twice does not skew or corrupt
                 * the index even if the item was previously indexed.
                 * If we are out of retries - report a permanent failure.
                 */
                if(queueItem.getRetries() < maxRetries) {
                    ZimbraLog.index.warn("An attempt to index %d mail items for account %s failed. Will retry.", queueItem.getMailItemsToAdd().size(), queueItem.getAccountID(), e);
                    queueItem.addRetry();
                    queueAdapter.put(queueItem);
                } else {
                    ZimbraLog.index.error("Permanently failed to index %d mail items for account %s after %d attempts.", queueItem.getMailItemsToAdd().size(),queueItem.getAccountID(), queueItem.getRetries(), e);
                    // status reporting
                    if (queueItem.isReindex()) {
                        queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(), indexedItems.size());
                    }
                }
            } finally {
                ZimbraLog.clearContext();
            }
        }
    }
}
