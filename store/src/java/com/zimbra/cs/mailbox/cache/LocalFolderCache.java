package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

public class LocalFolderCache extends FolderCache {

    private final Map<Integer, Folder> mapById;
    private final Map<String, Folder> mapByUuid;

    public LocalFolderCache(Mailbox mbox) {
        super(mbox);
        mapById = new ConcurrentHashMap<Integer, Folder>();
        mapByUuid = new ConcurrentHashMap<String, Folder>();
    }

    @Override
    public void put(Folder folder) {
        mapById.put(folder.getId(), folder);
        if (folder.getUuid() != null) {
            putByUuid(folder.getUuid(), folder);
        }
    }

    protected void putByUuid(String uuid, Folder folder) {
        mapByUuid.put(folder.getUuid(), folder);
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
    public void remove(Folder folder) {
        Folder removed = mapById.remove(folder.getId());
        if (removed != null) {
            String uuid = removed.getUuid();
            if (uuid != null) {
                mapByUuid.remove(uuid);
            }
        }
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
