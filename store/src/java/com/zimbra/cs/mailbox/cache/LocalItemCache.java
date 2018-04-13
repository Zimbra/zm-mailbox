package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public class LocalItemCache extends ItemCache {
    private final Map<Integer /* id */, MailItem> mapById;
    private final Map<String /* uuid */, Integer /* id */> uuid2id;

    public LocalItemCache(Mailbox mbox) {
        super(mbox);
        mapById = new ConcurrentLinkedHashMap.Builder<Integer, MailItem>().maximumWeightedCapacity(
                        Mailbox.MAX_ITEM_CACHE_WITH_LISTENERS).build();
        uuid2id = new ConcurrentHashMap<String, Integer>(Mailbox.MAX_ITEM_CACHE_WITH_LISTENERS);
    }

    @Override
    public void put(MailItem item) {
        int id = item.getId();
        mapById.put(id, item);
        String uuid = item.getUuid();
        if (uuid != null) {
            uuid2id.put(uuid, id);
        }
    }

    @Override
    public MailItem get(int id) {
        return mapById.get(id);
    }

    @Override
    public MailItem get(String uuid) {
        // Always fetch item from mapById map to preserve LRU's access time ordering.
        Integer id = uuid2id.get(uuid);
        return id != null ? mapById.get(id) : null;
    }

    @Override
    public MailItem remove(MailItem item) {
        return remove(item.getId());
    }

    @Override
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

    @Override
    public boolean contains(MailItem item) {
        return mapById.containsKey(item.getId());
    }

    @Override
    public Collection<MailItem> values() {
        return mapById.values();
    }

    @Override
    public int size() {
        return mapById.size();
    }

    @Override
    public void clear() {
        mapById.clear();
        uuid2id.clear();
    }

    public static class Factory implements ItemCache.Factory {

        @Override
        public ItemCache getItemCache(Mailbox mbox) {
            return new LocalItemCache(mbox);
        }
    }
}