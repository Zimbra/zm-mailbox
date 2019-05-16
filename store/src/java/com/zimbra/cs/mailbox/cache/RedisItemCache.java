package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionAware.CachePolicy;
import com.zimbra.cs.mailbox.TransactionAware.ReadPolicy;
import com.zimbra.cs.mailbox.TransactionAware.WritePolicy;
import com.zimbra.cs.mailbox.TransactionCacheTracker;
import com.zimbra.cs.mailbox.redis.RedisBackedLRUItemCache;
import com.zimbra.cs.mailbox.redis.RedisBackedMap;
import com.zimbra.cs.mailbox.redis.RedisCacheTracker;
import com.zimbra.cs.mailbox.redis.RedisUtils;

public class RedisItemCache extends MapItemCache<String> {

    private RBucket<String> folderTagBucket;

    //cache that stores items locally over the course of a transaction, so multiple requests
    //for the same item ID will yield the same object
    private Map<Integer, MailItem> localCache;
    private RedisBackedLRUItemCache lruCache;
    private RedissonClient client;

    public RedisItemCache(Mailbox mbox, Map<Integer, String> itemMap, Map<String, Integer> uuidMap, RedisBackedLRUItemCache lruCache) {
        super(mbox, itemMap, uuidMap);
        this.client = RedissonClientHolder.getInstance().getRedissonClient();
        this.localCache = new ConcurrentHashMap<>();
        this.lruCache = lruCache;
        initFolderTagBucket();
    }

    private void initFolderTagBucket() {
        folderTagBucket = client.getBucket(RedisUtils.createAccountRoutedKey(mbox.getAccountId(), "FOLDERS_TAGS"));
    }

    private void markItemAccessed(int id) {
        lruCache.markAccessed(id);
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
        if (item != null) {
            markItemAccessed(id);
        }
        return item;
    }

    @Override
    public void put(MailItem item) {
        super.put(item);
        markItemAccessed(item.getId());
    }

    /* Change this if item serialization algorithm changes. */
    private String currVersionPrefix = "%v1;";

    @Override
    protected String toCacheValue(MailItem item) {
        return currVersionPrefix + item.serializeUnderlyingData().toString();
    }

    @Override
    protected MailItem fromCacheValue(String value) {
        if (value == null || !value.startsWith(currVersionPrefix)) {
            ZimbraLog.cache.debug("Dropping old cache value for mail item '%s'", value);
            return null;
        }
        UnderlyingData data = new UnderlyingData();
        try {
            data.deserialize(new Metadata(value.substring(currVersionPrefix.length())));
            return MailItem.constructItem(mbox, data, true);
        } catch (ServiceException e) {
            ZimbraLog.cache.error("unable to get MailItem from Redis cache for account %s", mbox.getAccountId());
            return null;
        }
    }

    @Override
    protected Metadata getCachedTagsAndFolders() {
        if (!LC.redis_cache_synchronize_folder_tag_snapshot.booleanValue()) {
            return null;
        }
        String encoded = folderTagBucket.get();
        if (encoded == null || !encoded.startsWith(currVersionPrefix)) {
            return null;
        }
        try {
            return new Metadata(encoded.substring(currVersionPrefix.length()));
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
    public void trim(int numItemsToKeep) {
        Collection<Integer> removed = lruCache.trimCache(numItemsToKeep);
        if (!removed.isEmpty()) {
            if (ZimbraLog.cache.isDebugEnabled()) {
                ZimbraLog.cache.debug("trimmed %d items from the redis cache", removed.size());
            } else if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.debug("trimmed %d items from the redis cache: %s", removed.size(), removed);
            }
        }
    }

    @Override
    public MailItem remove(int id) {
        lruCache.remove(id);
        return super.remove(id);
    }

    @Override
    public void clear() {
        if (ZimbraLog.cache.isTraceEnabled()) {
            ZimbraLog.cache.trace("clearing RedisItemCache");
        }
        super.clear();
        lruCache.clear();
    }

    @Override
    protected void cacheFoldersTagsMeta(Metadata folderTagMeta) {
        if (LC.redis_cache_synchronize_folder_tag_snapshot.booleanValue()) {
            folderTagBucket.set(currVersionPrefix + folderTagMeta.toString());
        }
    }

    public static class Factory extends LocalItemCache.Factory {

        @Override
        public ItemCache getItemCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            if (!LC.redis_cache_synchronize_item_cache.booleanValue()) {
                return super.getItemCache(mbox, cacheTracker);
            }
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            String accountId = mbox.getAccountId();
            String itemMapName = RedisUtils.createAccountRoutedKey(accountId, String.format("ITEMS_BY_ID"));
            String uuidMapName = RedisUtils.createAccountRoutedKey(accountId, String.format("ITEMS_BY_UUID"));
            String itemSetName = RedisUtils.createAccountRoutedKey(accountId, String.format("ITEMS_LRU_SET"));
            RScoredSortedSet<Integer> rLruItemSet = client.getScoredSortedSet(itemSetName);
            RMap<Integer, String> rItemMap = client.getMap(itemMapName);
            RMap<String, Integer> rUuidMap = client.getMap(uuidMapName);
            RedisBackedMap<Integer, String> itemMap = new RedisBackedMap<>(rItemMap, cacheTracker,
                    ReadPolicy.TRANSACTION_ONLY,
                    WritePolicy.TRANSACTION_ONLY,
                    false, //lazy loading values
                    CachePolicy.THREAD_LOCAL);
            RedisBackedMap<String, Integer> uuidMap = new RedisBackedMap<>(rUuidMap, cacheTracker,
                    ReadPolicy.TRANSACTION_ONLY,
                    WritePolicy.TRANSACTION_ONLY,
                    false, CachePolicy.THREAD_LOCAL);
            RedisBackedLRUItemCache lruItemSet = new RedisBackedLRUItemCache(rLruItemSet, rItemMap, cacheTracker);
            return new RedisItemCache(mbox, itemMap, uuidMap, lruItemSet);
        }

        @Override
        public FolderCache getFolderCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            if (LC.redis_cache_synchronize_folders_tags.booleanValue()) {
                return new RedisFolderCache(mbox, cacheTracker);
            } else {
                return super.getFolderCache(mbox, cacheTracker);
            }
        }

        @Override
        public TagCache getTagCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
            if (LC.redis_cache_synchronize_folders_tags.booleanValue()) {
                return new RedisTagCache(mbox, cacheTracker);
            } else {
                return super.getTagCache(mbox, cacheTracker);
            }
        }

        @Override
        public RedisCacheTracker getTransactionCacheTracker(Mailbox mbox) {
            return new RedisCacheTracker(mbox);
        }
    }
}
