package com.zimbra.cs.mailbox.cache;

import java.util.Collection;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public abstract class ItemCache {

    private static Factory factory;
    static {
        setFactory(new LocalItemCache.Factory());
    }
    protected final Mailbox mbox;

    public ItemCache(Mailbox mbox) {
        this.mbox = mbox;
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

    public abstract boolean contains(MailItem item);

    public abstract Collection<MailItem> values();

    public abstract int size();

    public abstract void clear();

    public static interface Factory {
        public ItemCache getItemCache(Mailbox mbox);
    }
}