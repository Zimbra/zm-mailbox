package com.zimbra.cs.mailbox.cache;

import java.util.Collection;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

public abstract class FolderCache {

    protected Mailbox mbox;

    public FolderCache(Mailbox mbox) {
        this.mbox = mbox;
    }

    public abstract void put(Folder folder);

    public abstract Folder get(int id);

    public abstract Folder get(String uuid);

    public abstract void remove(Folder folder);

    public abstract Collection<Folder> values();

    public abstract int size();
}
