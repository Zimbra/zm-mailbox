package com.zimbra.cs.pubsub.message;

import com.zimbra.soap.admin.type.CacheSelector;

/**
 * Contains the CacheSelector payload of a FlushCacheRequest (SOAP).
 * Allows for resubmitting the request over the PubSub channel.
 */
public class FlushCacheMsg extends PubSubMsg {
    private CacheSelector selector;

    // Default for serialization - don't remove
    public FlushCacheMsg() { }

    public FlushCacheMsg(CacheSelector selector) {
        this.selector = selector;
    }

    public void setSelector(CacheSelector selector) {
        this.selector = selector;
    }

    public CacheSelector getSelector() {
        return this.selector;
    }

    public String toString() {
        return String.format("FlushCacheMsg[targetService=%s, selector=%s]",  this.getTargetService(), this.selector);
    }

}
