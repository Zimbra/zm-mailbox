package com.zimbra.cs.mailbox.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

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
        public ItemCache getItemCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            int cacheCapacity = LC.zimbra_mailbox_active_cache.intValue();
            Map<String, Integer> uuidMap = new ConcurrentHashMap<>(cacheCapacity);
            Map<Integer, MailItem> itemMap = new ConcurrentLinkedHashMap.Builder<Integer, MailItem>()
                    .maximumWeightedCapacity(cacheCapacity)
                    .listener(new EvictionListener<Integer, MailItem>() {
                        @Override
                        public void onEviction(Integer id, MailItem item) {
                            if (ZimbraLog.cache.isTraceEnabled() ) {
                                ZimbraLog.cache.trace("mailbox %s: evicting %s id=%s from item cache", item.getMailboxId(), item.getType(), id);
                            }
                            String uuid = item.getUuid();
                            if (uuid != null) {
                                uuidMap.remove(uuid);
                            }
                        }
                    }).build();
            return new LocalItemCache(mbox, itemMap, uuidMap);
        }

        @Override
        public FolderCache getFolderCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            return new LocalFolderCache();
        }

        @Override
        public TagCache getTagCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            return new LocalTagCache();
        }

        @Override
        public TransactionCacheTracker getTransactionCacheTracker(Mailbox mbox) {
            //no need for transaction tracker for local case
            return null;
        }

        @Override
        public FolderTagSnapshotCache getFolderTagSnapshotCache(Mailbox mbox) {
            return new LocalFolderTagSnapshotCache(mbox);
        }
    }

    @Override
    public void trim(int numItems) {
        int excess = size() - numItems;
        if (excess <= 0) {
            return;
        }
        MailItem[] overflow = new MailItem[excess];
        int i = 0;
        for (MailItem item : values()) {
            overflow[i++] = item;
            if (i >= excess) {
                break;
            }
        }
        while (--i >= 0) {
            if (size() <= numItems) {
                return;
            }

            remove(overflow[i]);
            uncacheChildren(overflow[i]);
        }
    }

    @Override
    public void uncacheChildren(MailItem parent) {
        List<MailItem> children = new ArrayList<>();
        for (MailItem cached: values()) {
            if (cached.getParentId() == parent.getId()) {
                children.add(cached);
            }
        }
        if (!children.isEmpty()) {
            for (MailItem child: children) {
                remove(child);
                uncacheChildren(child);
            }
        }
    }
}