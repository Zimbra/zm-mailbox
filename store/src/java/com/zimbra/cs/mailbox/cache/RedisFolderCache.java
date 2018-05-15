package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;

public class RedisFolderCache extends RedisSharedStateCache<Folder> implements FolderCache {

    private RMap<String, Integer> uuid2IdMap;

    public RedisFolderCache(Mailbox mbox) {
        super(mbox, new LocalFolderCache());
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        uuid2IdMap = client.getMap(String.format("FOLDER_UUID2ID:%s", mbox.getAccountId()));
    }

    @Override
    public void put(Folder folder) {
        super.put(folder);
        if (folder.getUuid() != null) {
            uuid2IdMap.fastPut(folder.getUuid(), folder.getId());
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
        if (removed != null && removed.getUuid() != null) {
            uuid2IdMap.fastRemove(removed.getUuid());
        }
        return removed;
    }

    @Override
    public int size() {
        return uuid2IdMap.size();
    }

    @Override
    protected Integer getIdForStringKey(String uuid) {
        return uuid2IdMap.get(uuid);
    }

    @Override
    protected String getMapName(String accountId, int itemId) {
        return String.format("%s:FOLDER:%d", accountId, itemId);
    }

    @Override
    protected Collection<Integer> getAllIds() {
        return uuid2IdMap.readAllValues();
    }
}
