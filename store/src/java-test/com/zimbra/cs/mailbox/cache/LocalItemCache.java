package com.zimbra.cs.mailbox.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public class LocalItemCache extends MapItemCache<MailItem> {

    public LocalItemCache(Mailbox mbox, Map<Integer, MailItem> itemMap, Map<String, Integer> uuidMap) {
        super(mbox, itemMap, uuidMap);
    }

    @Override
    protected MailItem toCacheValue(MailItem item) {
        return item;
    }

    @Override
    protected MailItem fromCacheValue(MailItem value) {
        return value;
    }

    public static class Factory implements ItemCache.Factory {

        @Override
        public ItemCache getItemCache(Mailbox mbox) {
            Map<Integer, MailItem> itemMap = new ConcurrentLinkedHashMap.Builder<Integer, MailItem>().maximumWeightedCapacity(
                    Mailbox.MAX_ITEM_CACHE_WITH_LISTENERS).build();
            Map<String, Integer> uuidMap = new ConcurrentHashMap<>(Mailbox.MAX_ITEM_CACHE_WITH_LISTENERS);
            return new LocalItemCache(mbox, itemMap, uuidMap);
        }
    }
}