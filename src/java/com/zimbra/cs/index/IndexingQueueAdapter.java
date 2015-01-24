/**
 *
 */
package com.zimbra.cs.index;


/**
 * @author Greg Solovyev
 * Describes access to the indexing queue
 */
public interface IndexingQueueAdapter {

    /**
     * Add an item to the tail of the queue
     * @param {@link com.zimbra.cs.index.IndexingQueueItemLocator} item
     */
    public boolean put(IndexingQueueItemLocator item);
    /**
     * Return the next element from the queue and remove it from the queue. Blocks until an element is available in the queue.
      * @return {@link com.zimbra.cs.index.IndexingQueueItemLocator}
      * @throws InterruptedException
     */
    public IndexingQueueItemLocator take() throws InterruptedException;

    /**
     * Return the next element in the queue and keep it in the queue. Returns null if the queue is empty.
     * @return {@link com.zimbra.cs.index.IndexingQueueItemLocator}
     */
    public IndexingQueueItemLocator peek();
    public boolean hasMoreItems();
}
