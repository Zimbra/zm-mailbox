package com.zimbra.cs.mailbox.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
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

    public RedisSharedStateCache(Mailbox mbox, AbstractItemCache<M> localCache) {
        this.mbox = mbox;
        this.localCache = localCache;
        this.client = RedissonClientHolder.getInstance().getRedissonClient();
        this.tracker = new RedisCacheTracker(mbox);
        mbox.addTransactionListener(tracker);
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
        //validate in-memory cache against redis map in case another node has deleted a folder/tag
        Set<Integer> allIds = new HashSet<>(getAllIds());
        Collection<M> values = new ArrayList<>();
        for (M item: localCache.values()) {
            if (allIds.contains(item.getId())) {
                values.add(item);
            } else {
                //delete from in-memory cache
                localCache.remove(item.getId());
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
        private Map<String, Object> localCache = null;
        private Map<String, Object> valueChanges = null;
        private Set<String> keyDeletions = null;
        private RedisCacheTracker tracker;

        public RedisSharedState(RMap<String, Object> map, RedisCacheTracker tracker) {
            this.map = map;
            this.tracker = tracker;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String fieldName) {
            if (localCache == null) {
                initLocalCache();
            }
            T val = (T) localCache.get(fieldName);
            ZimbraLog.cache.trace("RedisSharedState.get(%s=%s). %s", fieldName, val, map.getName());
            return val;
        }

        @Override
        public <T> void set(String fieldName, T value) {
            ZimbraLog.cache.trace("RedisSharedState.set(%s=%s). %s", fieldName, value, map.getName());
            addChange(fieldName, value);
        }

        private <T> void addChange(String fieldName, T value) {
            if (localCache == null) {
                initLocalCache();
            }
            localCache.put(fieldName, value);
            valueChanges.put(fieldName, value);
        }

        @Override
        public void delete() {
            ZimbraLog.cache.trace("RedisSharedState.delete() %s", this);
            map.delete();
            localCache = null;
            valueChanges = null;
            keyDeletions = null;
        }

        @Override
        public void unset(String fieldName) {
            ZimbraLog.cache.trace("RedisSharedState.unset(%s) %s", fieldName, this);
            if (keyDeletions == null) {
                initLocalCache();
            }
            localCache.remove(fieldName);
            keyDeletions.add(fieldName);

        }

        private void initLocalCache() {
            ZimbraLog.cache.trace("initializing local cache for map %s", map.getName());
            valueChanges = new HashMap<>();
            keyDeletions = new HashSet<>();
            localCache = map.readAllMap();
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
            localCache = null;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).omitNullValues()
                    .add("map", localCache == null ? null : localCache.entrySet())
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
            ZimbraLog.cache.trace("adding %s to RedisCacheTracker", item.map.getName());
            touchedItems.get().add(item);
        }

        private RBatch createBatch() {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            return client.createBatch(BatchOptions.defaults());
        }

        @Override
        public void transactionEnd(boolean success) {
            Set<RedisSharedState> touchedByThisThread = touchedItems.get();
            if (touchedByThisThread.isEmpty()) {
                return;
            }
            ZimbraLog.cache.trace("end transaction: flushing changes to %s redis folder/tag maps for account %s", touchedByThisThread.size(), mbox.getAccountId());
            RBatch batch = createBatch();
            for (RedisSharedState state: touchedByThisThread) {
                state.addChangesToBatch(batch);
                state.clearLocalCache();
            }
            batch.execute();
            touchedByThisThread.clear();
        }

        @Override
        public void transactionBegin() {
            Set<RedisSharedState> touchedByThisThread = touchedItems.get();
            if (touchedByThisThread.isEmpty()) {
                return;
            }
            ZimbraLog.cache.trace("new transaction: clearing caches of %s redis folder/tag maps for account %s", touchedItems.get().size(), mbox.getAccountId());
            touchedByThisThread.stream().forEach(item -> item.clearLocalCache());
            touchedByThisThread.clear();
        }
    }
}
