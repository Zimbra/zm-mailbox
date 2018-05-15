package com.zimbra.cs.mailbox.cache;

import com.zimbra.cs.mailbox.Tag;

public interface TagCache extends AbstractItemCache<Tag> {

    public abstract Tag remove(String tagName);

    public abstract boolean contains(String tagName);
}
