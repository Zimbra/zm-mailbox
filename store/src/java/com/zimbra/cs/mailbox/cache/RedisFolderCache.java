package com.zimbra.cs.mailbox.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.redisson.api.RLiveObjectService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;

public class RedisFolderCache extends LocalFolderCache {

    private RLiveObjectService service;
    private RMap<String, Integer> uuid2IdMap;

    public RedisFolderCache(Mailbox mbox, RLiveObjectService service) {
        super(mbox);
        this.service = service;
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        uuid2IdMap = client.getMap(String.format("FOLDER_UUID2ID:%s", mbox.getAccountId()));
    }

    @Override
    public void put(Folder folder) {
        put(folder, true);
    }

    private void put(Folder folder, boolean persistLiveObject) {
        super.put(folder);
        if (persistLiveObject) {
            persistFolder(folder);
        }
    }

    @Override
    protected void putByUuid(String uuid, Folder folder) {
        super.putByUuid(uuid, folder);
        uuid2IdMap.fastPut(uuid, folder.getId());
    }

    private Folder constructFromLiveObject(SharedFolderData data) {
        UnderlyingData ud = data.toUnderlyingData();
        try {
            Folder folder = (Folder) MailItem.constructItem(mbox, ud, true);
            folder.attach(data);
            //cache locally
            put(folder, false);
            folder.setSize(data.getSize(), data.getDeletedCount(), data.getTotalSize(), data.getDeletedUnreadCount());
            return folder;
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("unable to reconstruct Folder from Redis Live Object cache", e);
            return null;
        }
    }

    @Override
    public Folder get(int id) {
        Folder folder = super.get(id);
        if (folder == null) {
            //folder may not be in local cache yet, check live object service
            SharedFolderData liveObjectData = lookupFolderData(mbox.getAccountId(), id);
            if (liveObjectData != null) {
                folder = constructFromLiveObject(liveObjectData);
            }
        }
        return folder;
    }

    @Override
    public Folder get(String uuid) {
        Folder folder = super.get(uuid);
        if (folder == null) {
            //folder may not be in local cache yet, check live object service
            Integer id = uuid2IdMap.get(uuid);
            if (id != null && id > 0) {
                SharedFolderData liveObjectData = lookupFolderData(mbox.getAccountId(), id);
                if (liveObjectData != null) {
                    folder = constructFromLiveObject(liveObjectData);
                }
            }
        }
        return folder;
    }

    @Override
    public void remove(Folder folder) {
        super.remove(folder);
        SharedFolderData sharedData = folder.toLiveObject();
        if (folder.getUuid() != null) {
            uuid2IdMap.fastRemove(folder.getUuid());
        }
        if (sharedData != null) {
            service.delete(sharedData);
        }
    }

    @Override
    public Collection<Folder> values() {
        //validate in-memory cache against redis map in case another node has deleted a folder
        Set<Integer> allIds = new HashSet<>(uuid2IdMap.readAllValues());
        Collection<Folder> folders = new ArrayList<>();
        for (Folder f: super.values()) {
            if (allIds.contains(f.getId())) {
                folders.add(f);
            } else {
                //delete from in-memory cache
                super.remove(f);
            }
        }
        return folders;
    }

    @Override
    public int size() {
        return uuid2IdMap.size();
    }

    public SharedFolderData lookupFolderData(String accountId, int folderId) {
        String id = String.format("%s:%d", accountId, folderId);
        return service.get(SharedFolderData.class, id);
    }

    private void persistFolder(Folder folder) {
        SharedFolderData proxy = service.merge(folder.toLiveObject());
        folder.attach(proxy);
    }

}
