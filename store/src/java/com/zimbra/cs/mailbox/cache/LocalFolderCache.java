package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;

public class LocalFolderCache implements FolderCache {

    private final Map<Integer, Folder> mapById;
    private final Map<String, Folder> mapByUuid;

    public LocalFolderCache() {
        mapById = new ConcurrentHashMap<Integer, Folder>();
        mapByUuid = new ConcurrentHashMap<String, Folder>();
    }

    @Override
    public void put(Folder folder) {
        mapById.put(folder.getId(), folder);
        if (folder.getUuid() != null) {
            mapByUuid.put(folder.getUuid(), folder);
        }
    }

    @Override
    public Folder get(int id) {
        return mapById.get(id);
    }

    @Override
    public Folder get(String uuid) {
        return mapByUuid.get(uuid);
    }

    @Override
    public Folder remove(int folderId) {
        Folder removed = mapById.remove(folderId);
        if (removed != null) {
            String uuid = removed.getUuid();
            if (uuid != null) {
                mapByUuid.remove(uuid);
            }
        }
        return removed;
    }

    @Override
    public Collection<Folder> values() {
        return mapById.values();
    }

    @Override
    public int size() {
        return mapById.size();
    }
}
