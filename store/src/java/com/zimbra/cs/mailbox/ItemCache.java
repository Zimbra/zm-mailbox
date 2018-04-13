package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public class ItemCache {
    private final Map<Integer /* id */, MailItem> mapById;
    private final Map<String /* uuid */, Integer /* id */> uuid2id;
    private final Mailbox mbox;

    public ItemCache(Mailbox mbox) {
        mapById = new ConcurrentLinkedHashMap.Builder<Integer, MailItem>().maximumWeightedCapacity(
                        Mailbox.MAX_ITEM_CACHE_WITH_LISTENERS).build();
        uuid2id = new ConcurrentHashMap<String, Integer>(Mailbox.MAX_ITEM_CACHE_WITH_LISTENERS);
        this.mbox = mbox;
    }

    public void put(MailItem item) {
        int id = item.getId();
        mapById.put(id, item);
        String uuid = item.getUuid();
        if (uuid != null) {
            uuid2id.put(uuid, id);
        }
    }

    public MailItem get(int id) {
        return mapById.get(id);
    }

    public MailItem get(String uuid) {
        // Always fetch item from mapById map to preserve LRU's access time ordering.
        Integer id = uuid2id.get(uuid);
        return id != null ? mapById.get(id) : null;
    }

    public MailItem remove(MailItem item) {
        return remove(item.getId());
    }

    public MailItem remove(int id) {
        MailItem removed = mapById.remove(id);
        if (removed != null) {
            String uuid = removed.getUuid();
            if (uuid != null) {
                uuid2id.remove(uuid);
            }
        }
        return removed;
    }

    public boolean contains(MailItem item) {
        return mapById.containsKey(item.getId());
    }

    public Collection<MailItem> values() {
        return mapById.values();
    }

    public int size() {
        return mapById.size();
    }

    public void clear() {
        mapById.clear();
        uuid2id.clear();
    }
}