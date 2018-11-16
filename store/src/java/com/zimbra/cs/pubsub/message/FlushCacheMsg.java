package com.zimbra.cs.pubsub.message;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.admin.type.CacheSelector;
import com.zimbra.soap.json.JacksonUtil;

/**
 * Contains the CacheSelector payload of a FlushCacheRequest (SOAP).
 * Allows for resubmitting the request over the PubSub channel.
 */
public class FlushCacheMsg extends PubSubMsg {
    private String selector;

    // Default for serialization - don't remove
    public FlushCacheMsg() { }

    public FlushCacheMsg(CacheSelector selector) {
        setSelector(selector);
    }

    public void setSelector(CacheSelector cacheSelector) {
        try {
            selector = JacksonUtil.jaxbToJsonNonSoapApi(cacheSelector);
        } catch (ServiceException e) {
            ZimbraLog.cache.info("FlushCacheMsg.setSelector(%s) - problem serializing, so using null", cacheSelector);
            selector = null;
        }
    }

    public CacheSelector getSelector() {
        return JacksonUtil.jsonToJaxbNonSoapApi(selector, CacheSelector.class);
    }

    public String toString() {
        return String.format("FlushCacheMsg[targetService=%s, selector=%s]",  getTargetService(), selector);
    }
}
