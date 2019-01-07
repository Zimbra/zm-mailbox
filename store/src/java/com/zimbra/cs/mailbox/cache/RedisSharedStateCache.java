package com.zimbra.cs.mailbox.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailItemState;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionAware.CachePolicy;
import com.zimbra.cs.mailbox.TransactionAwareMap.GreedyMapGetter;
import com.zimbra.cs.mailbox.TransactionAwareMap.MapLoader;
import com.zimbra.cs.mailbox.TransactionCacheTracker;
import com.zimbra.cs.mailbox.TransactionListener;
import com.zimbra.cs.mailbox.cache.CachedObjectRegistry.CachedObjectKey;
import com.zimbra.cs.mailbox.cache.CachedObjectRegistry.CachedObjectKeyType;
import com.zimbra.cs.mailbox.redis.RedisBackedMap;

public abstract class RedisSharedStateCache<M extends MailItem & SharedState> implements AbstractItemCache<M> {

    protected Mailbox mbox;
    protected AbstractItemCache<M> localCache;
    protected RedissonClient client;
    private KnownItemIds knownItemIds;
    private TransactionCacheTracker tracker;
    private CachedObjectRegistry cachedObjects;

    public RedisSharedStateCache(Mailbox mbox, AbstractItemCache<M> localCache, TransactionCacheTracker cacheTracker) {
        this.mbox = mbox;
        this.localCache = localCache;
        this.client = RedissonClientHolder.getInstance().getRedissonClient();
        this.tracker = cacheTracker;
        this.knownItemIds = new KnownItemIds();
        this.cachedObjects = cacheTracker.getCachedObjects();
        mbox.addTransactionListener(knownItemIds);
    }

    protected abstract M construct(int id, Map<String, Object> map);

    protected abstract Integer getIdForStringKey(String key);

    protected RMap<String, Object> getMap(M item) {
        return getMap(mbox.getAccountId(), item.getId());
    }

    protected abstract String getMapName(String accountId, int itemId);

    protected RMap<String, Object> getMap(String accountId, int itemId) {
        return client.getMap(getMapName(accountId, itemId));
    }

    protected M lookup(String accountId, int itemId) {
        RMap<String, Object> stateMap = getMap(accountId, itemId);
        if (stateMap.isExists()) {
            M item = construct(itemId, stateMap.readAllMap());
            if (item != null) {
                item.attach(new RedisSharedState(stateMap, tracker, item.getId()));
                put(item, false);
                return item;
            }
        }
        return null;
    }

    private void put(M item, boolean persistInRedis) {
        localCache.put(item);
        if (persistInRedis) {
            persist(item);
            knownItemIds.addIdAddedInThisTransaction(item.getId());
        }
    }

    private void persist(M item) {
        if (!item.isAttached()) {
            RedisSharedState sharedState = new RedisSharedState(getMap(item), tracker, item.getId());
            item.attach(sharedState);
            item.sync();
        }
    }

    @Override
    public void put(M item) {
        put(item, true);
    }

    @Override
    public M get(int id) {
        M item = localCache.get(id);
        if (item == null) {
            //item may not be in local cache yet, check Redis
            item = lookup(mbox.getAccountId(), id);
        } else {
            //check that this item hasn't been deleted by a different mbox worker
            if (!knownItemIds.contains(item.getId())) {
                ZimbraLog.cache.info("%s was deleted remotely, removing from local cache", item);
                localCache.remove(item.getId());
                item = null;
            }
        }
        return item;
    }

    @Override
    public M get(String key) {
        M item = localCache.get(key);
        if (item == null) {
            //item may not be in local cache yet, check Redis
            Integer id = getIdForStringKey(key);
            if (id != null) {
                item = lookup(mbox.getAccountId(), id);
            }
        }
        return item;
    }

    @Override
    public M remove(int id) {
        M removed = localCache.remove(id);
        if (removed != null) {
            removed.detatch();
            knownItemIds.removeIdRemovedInThisTransaction(id);
        }
        return removed;
    }

    protected abstract Collection<Integer> getAllIds();

    @Override
    public Collection<M> values() {
        //validate in-memory cache against redis map in case another node has added/deleted a folder/tag
        Set<Integer> allIds = new HashSet<>(knownItemIds.getThreadLocalIds());
        Collection<M> values = new ArrayList<>();
        Set<Integer> localIds = new HashSet<>();
        for (M item: localCache.values()) {
            if (allIds.contains(item.getId())) {
                values.add(item);
                localIds.add(item.getId());
            } else {
                ZimbraLog.cache.trace("deleting remotely-deleted item %s from local cache", item.getId());
                localCache.remove(item.getId());
            }
        }
        for (Integer id: Sets.difference(allIds, localIds)) {
            M missingLocally = lookup(mbox.getAccountId(), id);
            if (missingLocally != null) {
                values.add(missingLocally);
                ZimbraLog.cache.trace("adding locally-missing item %s to local cache", id);
                put(missingLocally, false);
            }
        }
        return values;
    }

    @Override
    public int size() {
        return localCache.size();
    }

    protected String stringVal(Map<String, Object> map, String fieldName) {
        return stringVal(map, fieldName, null);
    }

    protected String stringVal(Map<String, Object> map, String fieldName, String defaultVal) {
        String val = (String) map.get(fieldName);
        return val == null ? defaultVal : val;
    }

    protected Integer intVal(Map<String, Object> map, String fieldName) {
        return (Integer) map.get(fieldName);
    }

    protected Long longVal(Map<String, Object> map, String fieldName) {
        return (Long) map.get(fieldName);
    }

    protected Boolean boolVal(Map<String, Object> map, String fieldName) {
        return (Boolean) map.get(fieldName);
    }

    protected UnderlyingData mapToUnderlyingData(int id, Map<String, Object> map) {
        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.of(stringVal(map, MailItemState.F_TYPE)).toByte();
        data.name = stringVal(map, MailItemState.F_NAME);
        data.uuid = stringVal(map, MailItemState.F_UUID);
        data.setSubject(stringVal(map, MailItemState.F_SUBJECT));
        data.setTags(stringVal(map, MailItemState.F_TAGS, "").split(","));
        data.setSmartFolders(stringVal(map, MailItemState.F_SMARTFOLDERS, "").split(","));
        data.setFlags(intVal(map, MailItemState.F_FLAGS));
        data.parentId = intVal(map, MailItemState.F_PARENT_ID);
        data.folderId = intVal(map, MailItemState.F_FOLDER_ID);
        data.imapId = intVal(map, MailItemState.F_IMAP_ID);
        data.indexId = intVal(map, MailItemState.F_INDEX_ID);
        data.size = longVal(map, MailItemState.F_SIZE);
        data.unreadCount = intVal(map, MailItemState.F_UNREAD_COUNT);
        data.date = intVal(map, MailItemState.F_DATE);
        data.dateChanged = intVal(map, MailItemState.F_DATE_CHANGED);
        data.modMetadata = intVal(map, MailItemState.F_MOD_METADATA);
        data.modContent = intVal(map, MailItemState.F_MOD_CONTENT);
        data.metadata = stringVal(map, MailItemState.F_METADATA);
        data.setPrevFolders(stringVal(map, MailItemState.F_PREV_FOLDERS));
        data.locator = stringVal(map, MailItemState.F_LOCATOR);
        data.setBlobDigest(stringVal(map, MailItemState.F_BLOB_DIGEST));
        return data;
    }

    private class MailItemGetter<K, V> extends GreedyMapGetter<K, V> {

        private int mailItemId;

        public MailItemGetter(String objectName, MapLoader<K, V> loader, int mailItemId) {
            super(objectName, CachePolicy.SINGLE_VALUE, loader);
            this.mailItemId = mailItemId;
        }

        @Override
        protected Map<K, V> loadObject() {
            Map<K, V> mapSnapshot = super.loadObject();
            CachedObjectKey key = new CachedObjectKey(CachedObjectKeyType.MAILITEM, mailItemId);
            cachedObjects.addObject(key, this);
            return mapSnapshot;
        }


    }
    protected class RedisSharedState extends RedisBackedMap<String, Object> implements SharedStateAccessor {

        public RedisSharedState(RMap<String, Object> map, TransactionCacheTracker tracker, int mailItemId) {
            super(map, tracker, new MailItemGetter<>(map.getName(), () -> map.readAllMap(), mailItemId),
                    ReadPolicy.ANYTIME, WritePolicy.TRANSACTION_ONLY);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String fieldName) {
            return (T) super.get(fieldName);
        }

        @Override
        public void unset(String fieldName) {
            remove(fieldName);
        }

        @Override
        public void delete() {
            clear();
        }

        @Override
        public void set(String fieldName, Object value) {
            put(fieldName, value);
        }
    }

    private class KnownItemIds implements TransactionListener {

        private ThreadLocal<Set<Integer>> allItemIds;

        public KnownItemIds() {
            allItemIds = new ThreadLocal<>();
        }

        @Override
        public void transactionBegin(boolean startChange) {
            if (startChange) {
                allItemIds.set(null);
            }
        }

        @Override
        public void transactionEnd(boolean success, boolean endChange) {
            if (endChange) {
                allItemIds.set(null);
            }
        }

        @Override
        public void commitCache(boolean endChange) {}

        @Override
        public void rollbackCache() {}

        private Set<Integer> getIdsFromRedis() {
            Set<Integer> ids = new HashSet<>(getAllIds());
            ZimbraLog.cache.trace("KnownItemIds: getting item ids for account %s (%s): %s", mbox.getAccountId(), RedisSharedStateCache.this.getClass().getSimpleName(), ids);
            return ids;
        }

        private Set<Integer> getThreadLocalIds() {
            Set<Integer> ids = allItemIds.get();
            if (ids == null) {
                ids = getIdsFromRedis();
                allItemIds.set(ids);
            }
            return ids;
        }

        public boolean contains(int itemId) {
            return getThreadLocalIds().contains(itemId);
        }

        private void removeIdRemovedInThisTransaction(int id) {
            getThreadLocalIds().remove(id);
        }

        public void addIdAddedInThisTransaction(int id) {
            getThreadLocalIds().add(id);
        }
    }
}
