package com.zimbra.cs.mailbox.cache;

import java.util.Collection;

import com.zimbra.cs.mailbox.MailItem;

public interface AbstractItemCache<T extends MailItem>{

    public abstract void put(T item);

    public abstract T get(int itemId);

    public abstract T get(String key);

    public abstract T remove(int itemId);

    public abstract Collection<T> values();

    public abstract int size();
}
