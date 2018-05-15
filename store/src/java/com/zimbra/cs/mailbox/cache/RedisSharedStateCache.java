package com.zimbra.cs.mailbox.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailItemState;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;

public abstract class RedisSharedStateCache<M extends MailItem & SharedState> implements AbstractItemCache<M> {

    protected Mailbox mbox;
    protected AbstractItemCache<M> localCache;
    protected RedissonClient client;

    public RedisSharedStateCache(Mailbox mbox, AbstractItemCache<M> localCache) {
        this.mbox = mbox;
        this.localCache = localCache;
        this.client = RedissonClientHolder.getInstance().getRedissonClient();
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
            item.attach(new RedisSharedState(stateMap));
            put(item, false);
            return item;
        } else {
            return null;
        }
    }

    private void put(M item, boolean persistInRedis) {
        localCache.put(item);
        if (persistInRedis) {
            persist(item);
        }
    }

    private void persist(M item) {
        if (!item.isAttached()) {
            RedisSharedState sharedState = new RedisSharedState(getMap(item));
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

    protected class RedisSharedState implements SharedStateAccessor {

        private RMap<String, Object> map;

        public RedisSharedState(RMap<String, Object> map) {
            this.map = map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String fieldName) {
            return (T) map.get(fieldName);
        }

        @Override
        public <T> void set(String fieldName, T value) {
            map.put(fieldName, value);
        }

        @Override
        public void delete() {
            map.delete();
        }

    }
}
