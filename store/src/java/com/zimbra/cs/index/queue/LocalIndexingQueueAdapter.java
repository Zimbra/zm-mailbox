package com.zimbra.cs.index.queue;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

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
    private final HashMap<String, Integer> totalCounters;
    private final HashMap<String, Integer> succeededCounters;
    private final HashMap<String, Integer> failedCounters;
    private final HashMap<String, Integer> taskStatus;

    public LocalIndexingQueueAdapter() {
        int queueSize = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexingQueueMaxSize, 10000);
        itemQueue = new ArrayBlockingQueue<AbstractIndexingTasksLocator>(queueSize);
        totalCounters = new HashMap<String, Integer>();
        succeededCounters = new HashMap<String, Integer>();
        failedCounters = new HashMap<String, Integer>();
        taskStatus = new HashMap<String, Integer>();
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
    public synchronized void incrementSucceededMailboxTaskCount(String accountId, int val) {
        Integer currentCount = succeededCounters.get(accountId);
        if (currentCount == null) {
            currentCount = 0;
        }
        succeededCounters.put(accountId, currentCount + val);
        checkStatus(accountId);
    }

    private void checkStatus(String accountId) {
        if (getTaskStatus(accountId) != ReIndexStatus.STATUS_ABORTED
                && (getSucceededMailboxTaskCount(accountId) + getFailedMailboxTaskCount(accountId)) >= getTotalMailboxTaskCount(accountId)) {
            setTaskStatus(accountId, ReIndexStatus.STATUS_DONE);
        }
    }

    @Override
    public synchronized int getSucceededMailboxTaskCount(String accountId) {
        Integer currentCount = succeededCounters.get(accountId);
        return currentCount == null ? 0 : currentCount;
    }

    @Override
    public synchronized void deleteMailboxTaskCounts(String accountId) {
        totalCounters.remove(accountId);
        succeededCounters.remove(accountId);
        failedCounters.remove(accountId);
    }

    @Override
    public synchronized void clearAllTaskCounts() {
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
        Integer val = totalCounters.get(accountId);
        return val == null ? 0 : val;
    }

    @Override
    public void incrementFailedMailboxTaskCount(String accountId, int numItems) {
        Integer currentCount = failedCounters.get(accountId);
        if (currentCount == null) {
            currentCount = 0;
        }
        failedCounters.put(accountId, currentCount + numItems);
        checkStatus(accountId);
    }

    @Override
    public int getFailedMailboxTaskCount(String accountId) {
        Integer currentCount = failedCounters.get(accountId);
        return currentCount == null ? 0 : currentCount;
    }

    @Override
    public int getTaskStatus(String accountId) {
        Integer status = taskStatus.get(accountId);
        return status == null ? ReIndexStatus.STATUS_IDLE : status;
    }

    @Override
    public void setTaskStatus(String accountId, int status) {
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
}
