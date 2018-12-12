package com.zimbra.cs.mailbox.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionCacheTracker;
import com.zimbra.cs.mailbox.redis.RedisBackedMap;
import com.zimbra.cs.mailbox.redis.RedisCacheTracker;
import com.zimbra.cs.mailbox.redis.RedisUtils;

public class RedisItemCache extends MapItemCache<String> {

    private RBucket<String> folderTagBucket;

    //cache that stores items locally over the course of a transaction, so multiple requests
    //for the same item ID will yield the same object
    private Map<Integer, MailItem> localCache;

    public RedisItemCache(Mailbox mbox, Map<Integer, String> itemMap, Map<String, Integer> uuidMap) {
        super(mbox, itemMap, uuidMap);
        initFolderTagBucket();
        localCache = new ConcurrentHashMap<>();
    }

    private void initFolderTagBucket() {
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        folderTagBucket = client.getBucket(RedisUtils.createAccountRoutedKey(mbox.getAccountId(), "FOLDERS_TAGS"));
    }

    @Override
    public MailItem get(int id) {
        MailItem item = localCache.get(id);
        if (item == null) {
            item = super.get(id);
            if (item != null) {
                localCache.put(id, item);
            }
        }
        return item;
    }

    @Override
    protected String toCacheValue(MailItem item) {
        return item.serializeUnderlyingData().toString();
    }

    @Override
    protected MailItem fromCacheValue(String value) {
        UnderlyingData data = new UnderlyingData();
        try {
            data.deserialize(new Metadata(value));
            return MailItem.constructItem(mbox, data, true);
        } catch (ServiceException e) {
            ZimbraLog.cache.error("unable to get MailItem from Redis cache for account %s", mbox.getAccountId());
            return null;
        }
    }

    @Override
    protected Metadata getCachedTagsAndFolders() {
        String encoded = folderTagBucket.get();
        if (Strings.isNullOrEmpty(encoded)) {
            return null;
        }
        try {
            return new Metadata(encoded);
        } catch (ServiceException e) {
            ZimbraLog.cache.error("unable to deserialize Folder/Tag metadata from Redis", e);
            return null;
        }
    }

    @Override
    public void flush() {
        localCache.clear();
    }

    @Override
    protected void cacheFoldersTagsMeta(Metadata folderTagMeta) {
        folderTagBucket.set(folderTagMeta.toString());
    }

    public static class Factory implements ItemCache.Factory {

        @Override
        public ItemCache getItemCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            String accountId = mbox.getAccountId();
            String itemMapName = RedisUtils.createAccountRoutedKey(accountId, String.format("ITEMS_BY_ID"));
            String uuidMapName = RedisUtils.createAccountRoutedKey(accountId, String.format("ITEMS_BY_UUID"));
            RedisBackedMap<Integer, String> itemMap = new RedisBackedMap<>(client.getMap(itemMapName), cacheTracker, false);
            RedisBackedMap<String, Integer> uuidMap = new RedisBackedMap<>(client.getMap(uuidMapName), cacheTracker, false);
            return new RedisItemCache(mbox, itemMap, uuidMap);
        }

        @Override
        public FolderCache getFolderCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            return new RedisFolderCache(mbox, cacheTracker);
        }

        @Override
        public TagCache getTagCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            return new RedisTagCache(mbox, cacheTracker);
        }

        @Override
        public RedisCacheTracker getTransactionCacheTracker(Mailbox mbox) {
            return new RedisCacheTracker(mbox);
        }
    }
}
