package com.zimbra.cs.mailbox.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailItemState;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionListener;

public abstract class RedisSharedStateCache<M extends MailItem & SharedState> implements AbstractItemCache<M> {

    protected Mailbox mbox;
    protected AbstractItemCache<M> localCache;
    protected RedissonClient client;
    private RedisCacheTracker tracker;
    private KnownItemIds knownItemIds;

    public RedisSharedStateCache(Mailbox mbox, AbstractItemCache<M> localCache) {
        this.mbox = mbox;
        this.localCache = localCache;
        this.client = RedissonClientHolder.getInstance().getRedissonClient();
        this.tracker = new RedisCacheTracker(mbox);
        this.knownItemIds = new KnownItemIds();
        mbox.addTransactionListener(tracker);
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
                item.attach(new RedisSharedState(stateMap, tracker));
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
            RedisSharedState sharedState = new RedisSharedState(getMap(item), tracker);
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
        }
        return removed;
    }

    protected abstract Collection<Integer> getAllIds();

    @Override
    public Collection<M> values() {
        //validate in-memory cache against redis map in case another node has added/deleted a folder/tag
        Set<Integer> allIds = new HashSet<>(getAllIds());
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

    protected static class RedisSharedState implements SharedStateAccessor {

        private final RMap<String, Object> map;
        private ThreadLocal<Map<String, Object>> localCache = null;
        private Map<String, Object> valueChanges = null;
        private Set<String> keyDeletions = null;
        private RedisCacheTracker tracker;

        public RedisSharedState(RMap<String, Object> map, RedisCacheTracker tracker) {
            this.map = map;
            this.tracker = tracker;
            localCache = new ThreadLocal<>();
        }

        private Map<String, Object> getLocalCache() {
            return localCache.get();
        }
        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String fieldName) {
            if (getLocalCache() == null) {
                initLocalCache();
            }
            T val = (T) getLocalCache().get(fieldName);
            ZimbraLog.cache.trace("RedisSharedState.get(%s=%s), %s", fieldName, val, map.getName());
            return val;
        }

        @Override
        public <T> void set(String fieldName, T value) {
            ZimbraLog.cache.trace("RedisSharedState.set(%s=%s). %s", fieldName, value, map.getName());
            addChange(fieldName, value);
        }

        private <T> void addChange(String fieldName, T value) {

            if (getLocalCache() == null) {
                initLocalCache();
            }
            getLocalCache().put(fieldName, value);
            valueChanges.put(fieldName, value);
            //if this key was previously deleted in this transaction, remove it from the deletions list!
            if (keyDeletions.remove(fieldName)) {
                ZimbraLog.cache.trace("RedisSharedState: removed %s from keyDeletions set", fieldName);
            }
        }

        @Override
        public void delete() {
            ZimbraLog.cache.trace("RedisSharedState.delete() %s", this);
            map.delete();
            localCache.set(null);
            valueChanges = null;
            keyDeletions = null;
        }

        @Override
        public void unset(String fieldName) {
            ZimbraLog.cache.trace("RedisSharedState.unset(%s) %s", fieldName, this);
            if (getLocalCache() == null) {
                initLocalCache();
            }
            getLocalCache().remove(fieldName);
            keyDeletions.add(fieldName);

        }

        private void initLocalCache() {
            ZimbraLog.cache.trace("initializing local cache for map %s in thread %s", map.getName(), Thread.currentThread().getName());
            valueChanges = new HashMap<>();
            keyDeletions = new HashSet<>();
            localCache.set(map.readAllMap());
            tracker.addToTracker(this);
        }

        private void addChangesToBatch(RBatch batch) {
            if (valueChanges != null && !valueChanges.isEmpty()) {
                ZimbraLog.cache.trace("adding %s changes to redis map %s via batch: %s", valueChanges.size(), map.getName(), valueChanges);
                batch.getMap(map.getName()).putAllAsync(valueChanges);
                valueChanges = null;
            }
            if (keyDeletions != null && !keyDeletions.isEmpty()) {
                int numDeletions = keyDeletions.size();
                ZimbraLog.cache.trace("pushing %s deletions to redis map %s: %s", numDeletions, map.getName(), Joiner.on(",").join(keyDeletions));
                batch.getMap(map.getName()).fastRemoveAsync(keyDeletions.toArray());
                keyDeletions = null;
            }
        }

        private void clearLocalCache() {
            ZimbraLog.cache.trace("clearing cache of redis map %s", map.getName());
            localCache.set(null);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).omitNullValues()
                    .add("map", localCache == null ? null : getLocalCache().entrySet())
                    .add("map.name", map == null ? null : map.getName())
                    .add("map.hashCode", System.identityHashCode(map))
                    .toString();
        }
    }

    private static class RedisCacheTracker implements TransactionListener {

        private Mailbox mbox;
        private ThreadLocal<Set<RedisSharedState>> touchedItems;

        public RedisCacheTracker(Mailbox mbox) {
            this.mbox = mbox;
            touchedItems = ThreadLocal.withInitial(() -> new HashSet<>());
        }

        public void addToTracker(RedisSharedState item) {
            ZimbraLog.cache.trace("adding %s to RedisCacheTracker from thread %s", item.map.getName(), Thread.currentThread().getName());
            touchedItems.get().add(item);
        }

        private RBatch createBatch() {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            return client.createBatch(BatchOptions.defaults());
        }

        @Override
        public void transactionEnd(boolean success, boolean endChange) {
            ZimbraLog.cache.trace("RedisCacheTracker.transactionEnd(): success=%s, endChange=%s", success, endChange);
        }

        @Override
        public void transactionBegin(boolean startChange) {
            ZimbraLog.cache.trace("RedisCacheTracker.transactionBegin(): startChange=%s", startChange);
            if (startChange) {
                //clear out any caches built by getters invoked *outside* of a transaction
                clearTouchedItems();
            }
        }

        @Override
        public void commitCache() {
            Set<RedisSharedState> touchedByThisThread = touchedItems.get();
            ZimbraLog.cache.trace("RedisCacheTracker.commitCache(): flushing changes to %s redis maps for account %s", touchedByThisThread.size(), mbox.getAccountId());
            RBatch batch = createBatch();
            touchedByThisThread.stream().forEach(item -> item.addChangesToBatch(batch));
            batch.execute();
            clearTouchedItems();
        }

        @Override
        public void rollbackCache() {
            clearTouchedItems();
        }

        private void clearTouchedItems() {
            Set<RedisSharedState> touchedByThisThread = touchedItems.get();
            if (touchedByThisThread.isEmpty()) {
                return;
            }
            if (ZimbraLog.cache.isTraceEnabled()) {
                List<String> touched = touchedByThisThread.stream().map(s -> s.map.getName()).sorted().collect(Collectors.toList());
                ZimbraLog.cache.trace("RedisCacheTracker.clearTouchedItems(): clearing touched items: %s", Joiner.on(", ").join(touched));
            }
            touchedByThisThread.stream().forEach(item -> item.clearLocalCache());
            touchedByThisThread.clear();
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
        public void commitCache() {}

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

        public void addIdAddedInThisTransaction(int id) {
            getThreadLocalIds().add(id);
        }
    }
}
