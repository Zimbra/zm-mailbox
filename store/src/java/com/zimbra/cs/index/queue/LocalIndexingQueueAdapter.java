package com.zimbra.cs.index.queue;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.util.concurrent.AtomicLongMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ReIndexStatus;
import com.zimbra.cs.util.ProvisioningUtil;

/**
 * @author Greg Solovyev
 * Default implementation of the indexing queue for
 * non-cluster environment
 */
public class LocalIndexingQueueAdapter extends IndexingQueueAdapter {
    private final ArrayBlockingQueue<AbstractIndexingTasksLocator> itemQueue;
    private final AtomicLongMap<String> totalCounters;
    private final AtomicLongMap<String> succeededCounters;
    private final AtomicLongMap<String> failedCounters;
    private final Map<String, Integer> taskStatus;

    public LocalIndexingQueueAdapter() {
        int queueSize = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueueMaxSize, 10000);
        itemQueue = new ArrayBlockingQueue<AbstractIndexingTasksLocator>(queueSize);
        totalCounters = AtomicLongMap.create();
        succeededCounters = AtomicLongMap.create();
        failedCounters = AtomicLongMap.create();
        taskStatus = new ConcurrentHashMap<>();
    }

    /**
     * Add an item to the tail of the queue. If the underlying queue is full
     * this call blocks until there is space in the queue.
     *
     * @param {@link com.zimbra.cs.index.AbstractIndexingTasksLocator} item
     */
    @Override
    public boolean put(AbstractIndexingTasksLocator item) {
        try {
            ZimbraLog.index.debug("LocalIndexingQueueAdapter - put - before put number of elements %d", itemQueue.size());
            itemQueue.put(item);
        } catch (InterruptedException e) {
            ZimbraLog.index.error("failed to queue items for indexing to mailbox %d", item.getMailboxID(), e);
            return false;
        }
        return true;
    }

    /**
     * Add an item to the tail of the queue. If the underlying queue is full
     * this call returns FALSE.
     *
     * @param {@link com.zimbra.cs.index.AbstractIndexingTasksLocator} item
     * @return TRUE if the item was successfully added/FALSE otherwise.
     */
    @Override
    public boolean add(AbstractIndexingTasksLocator item) throws ServiceException {
        try {
            ZimbraLog.index.debug("LocalIndexingQueueAdapter - add - before add number of elements %d", itemQueue.size());
            return itemQueue.add(item);
        } catch (IllegalStateException e) {
            ZimbraLog.index.debug("unable to add item for account %s to indexing queue", item.getAccountID());
            return false;
        }
    }

    /**
     * Return the next element from the queue and remove it from the queue.
     * If no element is available, return null
     *
     * @return {@link com.zimbra.cs.index.AbstractIndexingTasksLocator}
     * @throws InterruptedException
     */
    @Override
    public AbstractIndexingTasksLocator take()  {
        try {
            ZimbraLog.index.debug("LocalIndexingQueueAdapter - take - before take number of elements %d", itemQueue.size());
            return itemQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    /**
     * Return the next element in the queue and keep it in the queue
     *
     * @return {@link com.zimbra.cs.index.AbstractIndexingTasksLocator}
     */
    @Override
    public AbstractIndexingTasksLocator peek() {
        return itemQueue.peek();
    }

    @Override
    public boolean hasMoreItems() {
        return !itemQueue.isEmpty();
    }

    @Override
    public void drain() {
        itemQueue.clear();
        totalCounters.clear();
        succeededCounters.clear();
        failedCounters.clear();
        taskStatus.clear();
    }

    @Override
    public void incrementSucceededMailboxTaskCount(String accountId, int val) {
        succeededCounters.addAndGet(accountId, val);
        checkStatus(accountId);
    }

    private void checkStatus(String accountId) {
        if (getTaskStatus(accountId) != ReIndexStatus.STATUS_ABORTED
                && (getSucceededMailboxTaskCount(accountId) + getFailedMailboxTaskCount(accountId)) >= getTotalMailboxTaskCount(accountId)) {
            setTaskStatus(accountId, ReIndexStatus.STATUS_DONE);
        }
    }

    @Override
    public int getSucceededMailboxTaskCount(String accountId) {
        return toInt(succeededCounters.get(accountId));
    }

    @Override
    public void deleteMailboxTaskCounts(String accountId) {
        totalCounters.remove(accountId);
        succeededCounters.remove(accountId);
        failedCounters.remove(accountId);
    }

    @Override
    public void clearAllTaskCounts() {
        totalCounters.clear();
        succeededCounters.clear();
        failedCounters.clear();
    }

    @Override
    public void setTotalMailboxTaskCount(String accountId, int val) {
        totalCounters.put(accountId, val);
    }

    @Override
    public void setSucceededMailboxTaskCount(String accountId, int val) {
        succeededCounters.put(accountId, val);
    }

    @Override
    public int getTotalMailboxTaskCount(String accountId) {
        return toInt(totalCounters.get(accountId));
    }

    @Override
    public void incrementFailedMailboxTaskCount(String accountId, int numItems) {
        failedCounters.addAndGet(accountId, numItems);
        checkStatus(accountId);
    }

    @Override
    public int getFailedMailboxTaskCount(String accountId) {
        return toInt(failedCounters.get(accountId));
    }

    @Override
    public int getTaskStatus(String accountId) {
        Integer status = taskStatus.get(accountId);
        return status == null ? ReIndexStatus.STATUS_IDLE : status;
    }

    @Override
    public void setTaskStatus(String accountId, int status) {
        ZimbraLog.index.debug("LocalIndexingQueueAdapter - setTaskStatus - going to update task status for account id %s as %d", accountId, status);
        taskStatus.put(accountId, status);
    }

    @Override
    public void setFailedMailboxTaskCount(String accountId, int val) {
        failedCounters.put(accountId, val);
    }

    public static class Factory implements IndexingQueueAdapter.Factory {

        private static LocalIndexingQueueAdapter instance = null;

        @Override
        public IndexingQueueAdapter getAdapter() {
            synchronized(Factory.class) {
                if (instance == null){
                    instance = new LocalIndexingQueueAdapter();
                }
            }
            return instance;
        }
    }

    private int toInt(Long val) {
        return val.intValue();
    }
}
