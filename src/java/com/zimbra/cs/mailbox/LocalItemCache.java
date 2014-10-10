package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.zimbra.common.localconfig.LC;

public class LocalItemCache {
    static final int MAX_ITEM_CACHE_WITH_LISTENERS = LC.zimbra_mailbox_active_cache.intValue();
    static final int MAX_ITEM_CACHE_WITHOUT_LISTENERS = LC.zimbra_mailbox_inactive_cache.intValue();
    static final int MAX_ITEM_CACHE_FOR_GALSYNC_MAILBOX = LC.zimbra_mailbox_galsync_cache.intValue();
    private final Map<Integer, MailItem> itemsById;
    private final Map<String, Integer> idsByUuid;

    public LocalItemCache() {
        itemsById = new ConcurrentLinkedHashMap.Builder<Integer, MailItem>().maximumWeightedCapacity(MAX_ITEM_CACHE_WITH_LISTENERS).build();
        idsByUuid = new ConcurrentHashMap<String, Integer>(MAX_ITEM_CACHE_WITH_LISTENERS);
    }

    public void put(MailItem item) {
        int id = item.getId();
        itemsById.put(id, item);
        String uuid = item.getUuid();
        if (uuid != null) {
            idsByUuid.put(uuid, id);
        }
    }

    public MailItem get(int id) {
        return itemsById.get(id);
    }

    public MailItem get(String uuid) {
        // Always fetch item from mapById map to preserve LRU's access time ordering.
        Integer id = idsByUuid.get(uuid);
        return id != null ? itemsById.get(id) : null;
    }

    public MailItem remove(MailItem item) {
        return remove(item.getId());
    }

    public MailItem remove(int id) {
        MailItem removed = itemsById.remove(id);
        if (removed != null) {
            String uuid = removed.getUuid();
            if (uuid != null) {
                idsByUuid.remove(uuid);
            }
        }
        return removed;
    }

    public boolean contains(MailItem item) {
        return itemsById.containsKey(item.getId());
    }

    public Collection<MailItem> values() {
        return itemsById.values();
    }

    public int size() {
        return itemsById.size();
    }

    public void clear() {
        itemsById.clear();
        idsByUuid.clear();
    }
}