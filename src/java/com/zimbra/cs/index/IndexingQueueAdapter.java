/**
 *
 */
package com.zimbra.cs.index;


/**
 * @author Greg Solovyev
 * Describes access to the indexing queue
 */
public interface IndexingQueueAdapter {
    public boolean put(IndexingQueueItemLocator item);
    public IndexingQueueItemLocator take();
    public IndexingQueueItemLocator peek();
    public boolean hasMoreItems();
}
