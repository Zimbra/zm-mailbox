package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;

import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.LocalCachedMapOptions.SyncStrategy;
import org.redisson.api.RBucket;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
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

public class RedisItemCache extends MapItemCache<String> {

    private RBucket<String> folderTagBucket;

    public RedisItemCache(Mailbox mbox, Map<Integer, String> itemMap, Map<String, Integer> uuidMap) {
        super(mbox, itemMap, uuidMap);
        initFolderTagBucket();
    }

    private void initFolderTagBucket() {
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        folderTagBucket = client.getBucket(String.format("FOLDERS_TAGS:%s", mbox.getAccountId()));
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

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<String> getAllValues() {
        return ((RMap<Integer, String>) mapById).readAllValues();
    }

    @Override
    protected void putItem(int id, String value) {
        ((RMap<Integer, String>) mapById).fastPut(id, value);
    }

    @Override
    protected void putUuid(String uuid, int id) {
        ((RMap<String, Integer>) uuid2id).fastPut(uuid, id);
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
    protected void cacheFoldersTagsMeta(Metadata folderTagMeta) {
        folderTagBucket.set(folderTagMeta.toString());
    }

    public static class Factory implements ItemCache.Factory {

        private RLiveObjectService service;

        public Factory() {
            service = RedissonClientHolder.getInstance().getRedissonClient().getLiveObjectService();
        }

        @Override
        public ItemCache getItemCache(Mailbox mbox) {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            String itemMapName = String.format("ITEMS_BY_ID:%s", mbox.getAccountId());
            String uuidMapName = String.format("ITEMS_BY_UUID:%s", mbox.getAccountId());
            LocalCachedMapOptions<Integer, String> opts = LocalCachedMapOptions.defaults();
            opts.cacheSize(LC.zimbra_mailbox_active_cache.intValue()).syncStrategy(SyncStrategy.INVALIDATE);
            RLocalCachedMap<Integer, String> itemMap = client.getLocalCachedMap(itemMapName, opts);
            RMap<String, Integer> uuidMap = client.getMap(uuidMapName);
            return new RedisItemCache(mbox, itemMap, uuidMap);
        }

        @Override
        public FolderCache getFolderCache(Mailbox mbox) {
            return new RedisFolderCache(mbox, service);
        }
    }
}
