package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * listens on a shared indexing queue and submits indexing tasks to a executor
 * @author Greg Solovyev
 *
 */
public class IndexingService {
    private ThreadPoolExecutor INDEX_EXECUTOR;
    private IndexingQueueAdapter queueAdapter;
    private volatile boolean running = false;
    private Thread queueMonitor;

    public IndexingService() {

    }

    public synchronized void startUp() {
        queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        running = true;
        INDEX_EXECUTOR = new ThreadPoolExecutor(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexThreads, 10),
                ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexThreads, 10),
                    Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(10000),
                        new ThreadFactoryBuilder().setNameFormat("IndexExecutor-%d").setDaemon(true).build());
        INDEX_EXECUTOR.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        INDEX_EXECUTOR.prestartAllCoreThreads();
        queueMonitor = new Thread(new IndexQueueMonitor());
        queueMonitor.start();
    }

    /**
     * orderly shutdown the service
     */
    @PreDestroy
    public synchronized void shutDown() {
        running = false;
        if(queueMonitor != null && queueMonitor.isAlive()) {
            queueMonitor.interrupt();
        }
        if(INDEX_EXECUTOR != null) {
            INDEX_EXECUTOR.purge();
            INDEX_EXECUTOR.shutdownNow(); //terminate any executing tasks
            INDEX_EXECUTOR = null;
        }
    }


    public int getNumActiveTasks() {
        return INDEX_EXECUTOR.getActiveCount();
    }

    class IndexQueueMonitor implements Runnable {
        @Override
        public void run() {
            ZimbraLog.index.info("Started indexing thread " + Thread.currentThread().getName());
            while(running) {
                try {
                    IndexingQueueItemLocator queueItem = queueAdapter.take();

                    if(INDEX_EXECUTOR.isTerminating() || INDEX_EXECUTOR.isShutdown()) {
                        //this thread will not process this item, so put it back in the queue for other threads to process
                        queueAdapter.put(queueItem);
                        break;
                    }

                    //negative number indicates a cancelled task
                    if(queueAdapter.getTotalMailboxTaskCount(queueItem.getAccountID()) > 0) {
                        try {
                            ZimbraLog.index.debug("%s found an indexing task for account %s", Thread.currentThread().getName(), queueItem.getAccountID());
                            INDEX_EXECUTOR.submit(new IndexingTask(queueItem));
                        } catch (RejectedExecutionException e) {
                            ZimbraLog.index.error("Indexing task is rejected",e);
                            queueAdapter.put(queueItem);
                        }
                    }
                } catch (InterruptedException e) {
                    //must be shutting down, if !running will break automatically, otherwise will continue
                }
            }
            ZimbraLog.index.info("Stopping indexing thread " + Thread.currentThread().getName());
        }
    }

    public boolean isRunning() {
        return running;
    }

    class IndexingTask implements Runnable {
        private IndexingQueueItemLocator queueItem;
        public IndexingTask(IndexingQueueItemLocator item) {
            queueItem = item;
        }

        @Override
        public void run() {
            //list of items that were sent to Solr
            List<MailItem> indexedItems = new ArrayList<MailItem>();
            try {
                ZimbraLog.index.info("Started indexing task " + Thread.currentThread().getName());
                IndexStore indexStore = IndexStore.getFactory().getIndexStore(queueItem.getAccountID());
                MailItem.UnderlyingData ud = null;
                List<IndexingQueueItemLocator.MailItemIdentifier> itemsToIndex = queueItem.getMailItems();
                DbConnection conn = DbPool.getConnection(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID());
                try {
                    List<IndexItemEntry> indexItemEntries = new ArrayList<IndexItemEntry>();
                    for(IndexingQueueItemLocator.MailItemIdentifier itemID : itemsToIndex) {
                        try {
                            ud = DbMailItem.getById(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID(), itemID.getId(), itemID.getType(), itemID.isInDumpster(), conn);
                        } catch (NoSuchItemException ex) {//item may have been moved to/from Dumpster after being queued for indexing
                            try {
                                ud = DbMailItem.getById(queueItem.getMailboxID(), queueItem.getMailboxSchemaGroupID(), itemID.getId(), itemID.getType(), !itemID.isInDumpster(), conn);
                            } catch (NoSuchItemException nex) {//could not find this item in Dumpster either.
                                 //Log an error.
                                ZimbraLog.index.error("Could not find item %d in mailbox %d account %s", itemID.getId(), queueItem.getMailboxID(), queueItem.getAccountID(), nex);
                                //Log a failed item for status reporting
                                queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(),1);
                                //Do not add this item to indexedItems. Move on.
                                continue;
                            }
                        }

                        if(ud != null) {
                            //get the mail item's body and send it to indexing
                            MailItem item = MailItem.constructItem(Provisioning.getInstance().getAccountById(queueItem.getAccountID()),ud,queueItem.getMailboxID());
                            indexItemEntries.add(new IndexItemEntry(item, item.generateIndexDataAsync(queueItem.attachmentIndexingEnabled())));
                            indexedItems.add(item);
                        } else {
                            //either something is seriously messed up or this item was deleted and purged before it could be indexed
                            ZimbraLog.index.warn("Could not find underlying data for item %d account %s mailbox %d", itemID.getId(), queueItem.getAccountID(), queueItem.getMailboxID());
                        }
                    }
                    /* do this at the end of the loop for two reasons:
                     *  1) run a single SQL UPDATE statement with multiple IDs instead of multiple statements
                     *  2) send multiple documents to Solr in a single request instead of multiple requests
                     * in the event that the preceding loop throws an exception, neither DB nor Solr will not be updated and the queueItem will be pushed back into the queue in 'finally' block
                     */
                    if(indexItemEntries.size() > 0) {
                        indexStore.openIndexer().add(indexItemEntries);
                        List<Integer> indexedIds = new ArrayList<Integer>();
                        for(IndexItemEntry entry : indexItemEntries) {
                            indexedIds.add(entry.item.getId());
                        }
                        if(indexedIds.size() > 0) {
                            DbMailItem.setIndexIds(conn, queueItem.getMailboxSchemaGroupID(), queueItem.getMailboxID(), indexedIds, Provisioning.getInstance().getAccountById(queueItem.getAccountID()).isDumpsterEnabled());
                        }
                    }
                    conn.commit();

                    /* These MailItems may already be cached on this server with indexId==0, so we should kick them from cache now.
                     * When they are returned by new DB search, they will have indexIds.
                     */
                    if(MailboxManager.getInstance().isMailboxLoadedAndAvailable(queueItem.getMailboxID())) {
                        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(queueItem.getMailboxID());
                        mailbox.batchUncache(indexedItems);
                    }

                    //status reporting
                    queueAdapter.incrementSucceededMailboxTaskCount(queueItem.getAccountID(),indexedItems.size());
                    ZimbraLog.index.debug("%s processed %d items", Thread.currentThread().getName(), itemsToIndex.size());
                } finally {
                    DbPool.quietClose(conn);
                }
            } catch (Exception e) {
                ZimbraLog.index.error(e.getMessage(), e);
                /* If we caught an exception put the item back in the queue.
                * sending an item to Solr index twice does not skew or corrupt the index even if the item was previously indexed
                */
                queueAdapter.put(queueItem);

                //status reporting
                queueAdapter.incrementFailedMailboxTaskCount(queueItem.getAccountID(),indexedItems.size());
            } finally {
                ZimbraLog.clearContext();
            }
            ZimbraLog.index.info("Finished indexing task " + Thread.currentThread().getName());
        }
    }
}
