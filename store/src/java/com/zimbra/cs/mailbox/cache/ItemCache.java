package com.zimbra.cs.mailbox.cache;

import java.util.Collection;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public abstract class ItemCache {

    private static Factory factory;
    static {
        setFactory(new RedisItemCache.Factory());
    }
    protected final Mailbox mbox;

    public ItemCache(Mailbox mbox) {
        this.mbox = mbox;
        ZimbraLog.mailbox.info("Using %s for Mailbox item cache", this.getClass().getSimpleName());
    }

    public static Factory getFactory() {
        return factory;
    }

    public static void setFactory(Factory factory) {
        ItemCache.factory = factory;
    }

    public abstract void put(MailItem item);

    public abstract MailItem get(int id);

    public abstract MailItem get(String uuid);

    public MailItem remove(MailItem item) {
        return remove(item.getId());
    }

    public abstract MailItem remove(int id);

    public abstract Collection<MailItem> values();

    public abstract void clear();

    public void flush() {};

    public abstract void trim(int numItems);

    public abstract int size();

    public abstract void uncacheChildren(MailItem parent);

    public static interface Factory {
        public ItemCache getItemCache(Mailbox mbox, TransactionCacheTracker cacheTracker);
        public FolderCache getFolderCache(Mailbox mbox, TransactionCacheTracker cacheTracker);
        public TagCache getTagCache(Mailbox mbox, TransactionCacheTracker cacheTracker);
        public FolderTagSnapshotCache getFolderTagSnapshotCache(Mailbox mbox);
        public TransactionCacheTracker getTransactionCacheTracker(Mailbox mbox);
    }
}