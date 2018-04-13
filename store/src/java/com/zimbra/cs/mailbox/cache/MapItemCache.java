package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public abstract class MapItemCache<T> extends ItemCache {
    private final Map<Integer /* id */, T> mapById;
    private final Map<String /* uuid */, Integer /* id */> uuid2id;

    public MapItemCache(Mailbox mbox, Map<Integer, T> itemMap, Map<String, Integer> uuidMap) {
        super(mbox);
        mapById = itemMap;
        uuid2id = uuidMap;
    }

    protected abstract T toCacheValue(MailItem item);

    protected abstract MailItem fromCacheValue(T value);

    @Override
    public void put(MailItem item) {
        int id = item.getId();
        mapById.put(id, toCacheValue(item));
        String uuid = item.getUuid();
        if (uuid != null) {
            uuid2id.put(uuid, id);
        }
    }

    @Override
    public MailItem get(int id) {
        return fromCacheValue(mapById.get(id));
    }

    @Override
    public MailItem get(String uuid) {
        Integer id = uuid2id.get(uuid);
        return id != null ? get(id) : null;
    }

    @Override
    public MailItem remove(MailItem item) {
        return remove(item.getId());
    }

    @Override
    public MailItem remove(int id) {
        MailItem removed = fromCacheValue(mapById.remove(id));
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
        return mapById.values().stream().map(v -> fromCacheValue(v)).filter(mi -> Objects.nonNull(mi)).collect(Collectors.toList());
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
}