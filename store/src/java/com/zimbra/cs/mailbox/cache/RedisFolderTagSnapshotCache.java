package com.zimbra.cs.mailbox.cache;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.redis.RedisUtils;

public class RedisFolderTagSnapshotCache extends FolderTagSnapshotCache {

    private RBucket<String> folderTagBucket;

    public RedisFolderTagSnapshotCache(Mailbox mbox) {
        super(mbox);
        initFolderTagBucket();
    }

    private void initFolderTagBucket() {
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        folderTagBucket = client.getBucket(RedisUtils.createAccountRoutedKey(mbox.getAccountId(), "FOLDERS_TAGS"));
    }

    @Override
    protected Metadata getCachedTagsAndFolders() {
        if (!LC.redis_cache_synchronize_folder_tag_snapshot.booleanValue()) {
            return null;
        }
        String encoded = folderTagBucket.get();
        if (encoded == null || !encoded.startsWith(RedisItemCache.CURR_VERSION_PREFIX)) {
            return null;
        }
        try {
            return new Metadata(encoded.substring(RedisItemCache.CURR_VERSION_PREFIX.length()));
        } catch (ServiceException e) {
            ZimbraLog.cache.error("unable to deserialize Folder/Tag metadata from Redis", e);
            return null;
        }
    }

    @Override
    protected void cacheFoldersTagsMeta(Metadata folderTagMeta) {
        if (LC.redis_cache_synchronize_folder_tag_snapshot.booleanValue()) {
            folderTagBucket.set(RedisItemCache.CURR_VERSION_PREFIX + folderTagMeta.toString());
        }
    }
}
