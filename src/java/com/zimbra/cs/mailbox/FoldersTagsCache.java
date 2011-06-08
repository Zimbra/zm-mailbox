/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.Zimbra;

/**
 * Memcached-based cache of folders and tags of mailboxes.  Loading folders/tags from database is expensive,
 * so we cache them in memcached.  The cached data must be kept up to date as changes occur to a folder or
 * a tag.  Folder changes occur very frequently because creating/deleting an item in a folder updates the
 * folder state.  The cache changes are queued up and pushed to memcached periodically.  This reduces the
 * number of memcached puts because rapid changes to the same mailbox are aggregated to a single cache
 * update.  Thus the cache won't always have the up to date data.  This is okay because stale data is
 * ignored during mailbox load and correct data is retrieved from the database.  We're able to reduce the
 * database calls for folder/tag loading, while limiting the number of memcached writes.
 */
public class FoldersTagsCache {

    private static final long SWEEP_INTERVAL_MSEC = 10 * Constants.MILLIS_PER_SECOND;

    private static FoldersTagsCache sTheInstance = new FoldersTagsCache();

    private MemcachedMap<FoldersTagsCacheKey, FoldersTags> mMemcachedLookup;
    Map<Integer, Mailbox> mDirtyMailboxes;

    public static FoldersTagsCache getInstance() { return sTheInstance; }

    FoldersTagsCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        FoldersTagsSerializer serializer = new FoldersTagsSerializer();
        mMemcachedLookup = new MemcachedMap<FoldersTagsCacheKey, FoldersTags>(memcachedClient, serializer, false);
        mDirtyMailboxes = new HashMap<Integer, Mailbox>(100);
        if (!DebugConfig.disableFoldersTagsCache) {
            Zimbra.sTimer.schedule(new DirtyMailboxesTask(), SWEEP_INTERVAL_MSEC, SWEEP_INTERVAL_MSEC);
        }
    }

    static class FoldersTags {
        private static final int DATA_VERSION = 1;

        private MetadataList mFolders;
        private MetadataList mTags;

        public FoldersTags(List<Folder> folders, List<Tag> tags) {
            mFolders = new MetadataList();
            for (Folder f : folders) {
                mFolders.add(f.serializeUnderlyingData());
            }
            mTags = new MetadataList();
            for (Tag t : tags) {
                mTags.add(t.serializeUnderlyingData());
            }
        }

        private FoldersTags(MetadataList folders, MetadataList tags) {
            mFolders = folders;
            mTags = tags;
        }

        private static final String FN_DATA_VERSION = "dv";
        private static final String FN_FOLDERS = "folders";
        private static final String FN_TAGS = "tags";

        public Metadata encode() {
            Metadata meta = new Metadata();
            meta.put(FN_DATA_VERSION, DATA_VERSION);
            meta.put(FN_FOLDERS, mFolders);
            meta.put(FN_TAGS, mTags);
            return meta;
        }

        public static FoldersTags decode(Metadata meta) throws ServiceException {
            int ver = (int) meta.getLong(FN_DATA_VERSION, 0);
            if (ver != DATA_VERSION) {
                ZimbraLog.mailbox.info("Ignoring cached folders/tags with stale data version");
                return null;
            }
            MetadataList folders = meta.getList(FN_FOLDERS);
            MetadataList tags = meta.getList(FN_TAGS);
            return new FoldersTags(folders, tags);
        }

        public List<Metadata> getFolders() {
            List<Metadata> toRet = new ArrayList<Metadata>();
            List list = mFolders.asList();
            for (Object obj : list) {
                if (obj instanceof Metadata)
                    toRet.add((Metadata) obj);
            }
            return toRet;
        }

        public List<Metadata> getTags() {
            List<Metadata> toRet = new ArrayList<Metadata>();
            List list = mTags.asList();
            for (Object obj : list) {
                if (obj instanceof Metadata)
                    toRet.add((Metadata) obj);
            }
            return toRet;
        }
    }

    private static class FoldersTagsSerializer implements MemcachedSerializer<FoldersTags> {
        FoldersTagsSerializer() { }

        @Override
        public Object serialize(FoldersTags value) {
            return value.encode().toString();
        }

        @Override
        public FoldersTags deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return FoldersTags.decode(meta);
        }
    }

    public FoldersTags get(Mailbox mbox) throws ServiceException {
        FoldersTagsCacheKey key = new FoldersTagsCacheKey(mbox.getAccountId(), mbox.getLastChangeID());
        return mMemcachedLookup.get(key);
    }

    public void put(Mailbox mbox, FoldersTags foldersTags) throws ServiceException {
        if (DebugConfig.disableFoldersTagsCache)
            return;

        FoldersTagsCacheKey key = new FoldersTagsCacheKey(mbox.getAccountId(), mbox.getLastChangeID());
        mMemcachedLookup.put(key, foldersTags);
    }

    public void purgeMailbox(Mailbox mbox) {
        // nothing to do
    }

    private static Set<MailItem.Type> FOLDER_AND_TAG_TYPES = EnumSet.of(MailItem.Type.TAG, MailItem.Type.FOLDER, MailItem.Type.SEARCHFOLDER, MailItem.Type.MOUNTPOINT);

    public void notifyCommittedChanges(PendingModifications mods, int changeId) {
        if (DebugConfig.disableFoldersTagsCache)
            return;

        Map<String /* account id */, Mailbox> mboxesToUpdate = new HashMap<String, Mailbox>();
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Folder || item instanceof Tag) {
                    Mailbox mbox = item.getMailbox();
                    mboxesToUpdate.put(mbox.getAccountId(), mbox);
                }
            }
        }
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                if (whatChanged instanceof Folder || whatChanged instanceof Tag) {
                    MailItem mi = (MailItem) whatChanged;
                    Mailbox mbox = mi.getMailbox();
                    mboxesToUpdate.put(mbox.getAccountId(), mbox);
                }
            }
        }
        if (mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                //noinspection RedundantCast
                if (FOLDER_AND_TAG_TYPES.contains((MailItem.Type) entry.getValue().what)) {
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null)
                        continue;  // just to be safe
                    if (!mboxesToUpdate.containsKey(acctId)) {
                        mboxesToUpdate.put(acctId, null);  // Look up Mailbox later.
                    }
                }
            }
        }
        try {
            for (Map.Entry<String, Mailbox> entry : mboxesToUpdate.entrySet()) {
                String acctId = entry.getKey();
                Mailbox mbox = entry.getValue();
                if (mbox == null) {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(acctId, false);
                }
                if (mbox != null) {
                    // Don't update memcached yet.  Just queue it so we can aggregate memcached puts for
                    // the same mailbox.
                    synchronized (mDirtyMailboxes) {
                        mDirtyMailboxes.put(mbox.getId(), mbox);
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify folders/tags cache", e);
        }
    }

    // periodic task that updates memcached for modified mailboxes
    private final class DirtyMailboxesTask extends TimerTask {
        DirtyMailboxesTask() { }

        @Override
        public void run() {
            try {
                List<Mailbox> dirty;
                synchronized (mDirtyMailboxes) {
                    dirty = new ArrayList<Mailbox>(mDirtyMailboxes.values());
                    mDirtyMailboxes.clear();
                }
                ZimbraLog.mailbox.debug("Saving folders/tags to memcached for " + dirty.size() + " mailboxes");
                for (Mailbox mbox : dirty) {
                    try {
                        mbox.cacheFoldersTagsToMemcached();
                    } catch (Throwable e) {
                        if (e instanceof OutOfMemoryError)
                            Zimbra.halt("Caught out of memory error", e);
                        ZimbraLog.mailbox.warn("Caught exception in FolersTagsCache timer", e);
                    }
                }
            } catch (Throwable e) { //don't let exceptions kill the timer
                if (e instanceof OutOfMemoryError)
                    Zimbra.halt("Caught out of memory error", e);
                ZimbraLog.mailbox.warn("Caught exception in FolersTagsCache timer", e);
            }
        }
    }
}
