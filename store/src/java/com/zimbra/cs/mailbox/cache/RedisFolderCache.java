package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;

import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.TransactionCacheTracker;
import com.zimbra.cs.mailbox.redis.RedisBackedMap;
import com.zimbra.cs.mailbox.redis.RedisBackedSet;
import com.zimbra.cs.mailbox.redis.RedisUtils;

public class RedisFolderCache extends RedisSharedStateCache<Folder> implements FolderCache {

    private RedisBackedMap<String, Integer> uuid2IdMap;
    private RedisBackedSet<Integer> idSet;

    public RedisFolderCache(Mailbox mbox, TransactionCacheTracker cacheTracker) {
        super(mbox, new LocalFolderCache(), cacheTracker);
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        String uuid2IdMapName = RedisUtils.createAccountRoutedKey(mbox.getAccountId(), "FOLDER_UUID2ID");
        RMap<String, Integer> rmap  = client.getMap(uuid2IdMapName);
        uuid2IdMap = new RedisBackedMap<>(rmap, cacheTracker);
        String idSetName = RedisUtils.createAccountRoutedKey(mbox.getAccountId(), "FOLDER_IDS");
        RSet<Integer> rset = client.getSet(idSetName);
        idSet = new RedisBackedSet<>(rset, cacheTracker);
    }

    @Override
    public void put(Folder folder) {
        super.put(folder);
        idSet.add(folder.getId());
        if (folder.getUuid() != null) {
            uuid2IdMap.put(folder.getUuid(), folder.getId());
        }
    }

    @Override
    protected Folder construct(int itemId, Map<String, Object> map) {
        UnderlyingData ud = mapToUnderlyingData(itemId, map);
        try {
            return (Folder) MailItem.constructItem(mbox, ud, true);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("unable to reconstruct Folder from Redis cache for id %d", itemId, e);
            return null;
        }
    }

    @Override
    public Folder remove(int folderId) {
        Folder removed = super.remove(folderId);
        if (removed != null) {
            idSet.remove(folderId);
            if (removed.getUuid() != null) {
                uuid2IdMap.remove(removed.getUuid());
            }
        }
        return removed;
    }

    @Override
    public int size() {
        return idSet.size();
    }

    @Override
    protected Integer getIdForStringKey(String uuid) {
        return uuid2IdMap.get(uuid);
    }

    @Override
    protected String getMapName(String accountId, int itemId) {
        return RedisUtils.createAccountRoutedKey(accountId, String.format("FOLDER:%d", itemId));
    }

    @Override
    protected Collection<Integer> getAllIds() {
        return idSet.values();
    }
}
