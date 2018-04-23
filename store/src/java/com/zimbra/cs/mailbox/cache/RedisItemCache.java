package com.zimbra.cs.mailbox.cache;

import java.util.Map;

import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.google.common.base.Strings;
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
        folderTagBucket = client.getBucket(String.format("%s:FOLDERS_TAGS", mbox.getAccountId()));
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
    protected void cacheFoldersTagsMeta(Metadata folderTagMeta) {
        folderTagBucket.set(folderTagMeta.toString());
    }

    public static class Factory implements ItemCache.Factory {

        @Override
        public ItemCache getItemCache(Mailbox mbox) {
            RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
            String itemMapName = String.format("%s:id", mbox.getAccountId());
            String uuidMapName = String.format("%s:uuid", mbox.getAccountId());
            RMap<Integer, String> itemMap = client.getMap(itemMapName);
            RMap<String, Integer> uuidMap = client.getMap(uuidMapName);
            return new RedisItemCache(mbox, itemMap, uuidMap);
        }
    }

}
