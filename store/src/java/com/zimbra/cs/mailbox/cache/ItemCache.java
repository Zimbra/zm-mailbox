package com.zimbra.cs.mailbox.cache;

import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.TransactionCacheTracker;

public abstract class ItemCache {

    private static Factory factory;
    static {
        setFactory(new RedisItemCache.Factory());
    }
    protected final Mailbox mbox;

    private static final String KEY_FOLDERS = "folders";
    private static final String KEY_TAGS = "tags";

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

    protected abstract Metadata getCachedTagsAndFolders();

    protected abstract void cacheFoldersTagsMeta(Metadata folderTagMeta);

    public abstract void trim(int numItems);

    public abstract int size();

    public abstract void uncacheChildren(MailItem parent);

    public CachedTagsAndFolders getTagsAndFolders() {
        Metadata folderTagMeta = getCachedTagsAndFolders();

        if (folderTagMeta == null) {
            return null;
        }
        List<Metadata> folderMeta;
        try {
            folderMeta = folderTagMeta.getList(KEY_FOLDERS).asList();
            List<Metadata> tagMeta = folderTagMeta.getList(KEY_TAGS).asList();
            return new CachedTagsAndFolders(folderMeta, tagMeta);
        } catch (ServiceException e) {
            ZimbraLog.cache.error("error fetching folder and tags from cache", e);
            return null;
        }
    }

    public void cacheTagsAndFolders(List<Folder> folders, List<Tag> tags) {
        MetadataList folderMeta = new MetadataList();
        for (Folder f : folders) {
            folderMeta.add(f.serializeUnderlyingData());
        }
        MetadataList tagMeta = new MetadataList();
        for (Tag t : tags) {
            tagMeta.add(t.serializeUnderlyingData());
        }
        Metadata snapshot = new Metadata();
        snapshot.put(KEY_FOLDERS, folderMeta);
        snapshot.put(KEY_TAGS, tagMeta);
        cacheFoldersTagsMeta(snapshot);
    }

    public static class CachedTagsAndFolders extends Pair<List<Metadata>, List<Metadata>>{

        public CachedTagsAndFolders(List<Metadata> folders, List<Metadata> tags) {
            super(folders, tags);
        }

        public List<Metadata> getFolders() {
            return getFirst();
        }

        public List<Metadata> getTags() {
            return getSecond();
        }
    }

    public static interface Factory {
        public ItemCache getItemCache(Mailbox mbox, TransactionCacheTracker cacheTracker);
        public FolderCache getFolderCache(Mailbox mbox, TransactionCacheTracker cacheTracker);
        public TagCache getTagCache(Mailbox mbox, TransactionCacheTracker cacheTracker);
        public TransactionCacheTracker getTransactionCacheTracker(Mailbox mbox);
    }
}