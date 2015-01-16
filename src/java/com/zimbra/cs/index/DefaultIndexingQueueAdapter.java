/**
 *
 */
package com.zimbra.cs.index;

import java.util.concurrent.ArrayBlockingQueue;

import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * @author Greg Solovyev
 * Default implementation of the indexing queue for non-cluster environment
 */
public class DefaultIndexingQueueAdapter implements IndexingQueueAdapter {
    private final ArrayBlockingQueue<IndexingQueueItemLocator> itemQueue;
    private static IndexingQueueAdapter instance = null;

    public static synchronized IndexingQueueAdapter getInstance() {
        if(instance == null) {
            instance = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        }
        return instance;
    }
    /**
     *
     */
    public DefaultIndexingQueueAdapter() {
        itemQueue = new ArrayBlockingQueue<IndexingQueueItemLocator>(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexingQueueMaxSize, 10000));
    }

    /**
     * Add an item to the tail of the queue
     * @param {@link com.zimbra.cs.index.IndexingQueueItemLocator} item
     */
    @Override
    public boolean put(IndexingQueueItemLocator item) {
        try {
            itemQueue.put(item);
        } catch (InterruptedException e) {
            ZimbraLog.index.error("failed to queue item %d for indexing to mailbox %d", item.getMailItemID(), item.getMailboxID(), e);
            return false;
        }
        return true;
    }

    /**
     * Return the next element from the queue and remove it from the queue
      * @return {@link com.zimbra.cs.index.IndexingQueueItemLocator}
     */
    @Override
    public IndexingQueueItemLocator take() {
        try {
            return itemQueue.take();
        } catch (InterruptedException e) {
            ZimbraLog.index.error("failed to retrieve next item from indexing queue", e);
            return null;
        }
    }

    /**
     * Return the next element in the queue and keep it in the queue
     * @return {@link com.zimbra.cs.index.IndexingQueueItemLocator}
     */
    @Override
    public IndexingQueueItemLocator peek() {
        return itemQueue.peek();
    }

    @Override
    public boolean hasMoreItems() {
        return !itemQueue.isEmpty();
    }
}
