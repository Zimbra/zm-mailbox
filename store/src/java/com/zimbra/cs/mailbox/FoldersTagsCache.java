/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedConnector;

/**
 * Memcached-based cache of folders and tags of mailboxes.  Loading folders/tags from database is expensive,
 * so we cache them in memcached.  The cached data must be kept up to date as changes occur to a folder or
 * a tag.  Folder changes occur very frequently because creating/deleting an item in a folder updates the
 * folder state.
 */
public class FoldersTagsCache {
    private static FoldersTagsCache sTheInstance = new FoldersTagsCache();

    private MemcachedMap<FoldersTagsCacheKey, FoldersTags> mMemcachedLookup;

    public static FoldersTagsCache getInstance() { return sTheInstance; }

    FoldersTagsCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        FoldersTagsSerializer serializer = new FoldersTagsSerializer();
        mMemcachedLookup = new MemcachedMap<FoldersTagsCacheKey, FoldersTags>(memcachedClient, serializer, false);
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
        FoldersTagsCacheKey key = new FoldersTagsCacheKey(mbox.getAccountId());
        return mMemcachedLookup.get(key);
    }

    public void put(Mailbox mbox, FoldersTags foldersTags) throws ServiceException {
        if (DebugConfig.disableFoldersTagsCache)
            return;

        FoldersTagsCacheKey key = new FoldersTagsCacheKey(mbox.getAccountId());
        mMemcachedLookup.put(key, foldersTags);
    }

    public void purgeMailbox(Mailbox mbox) throws ServiceException {
        if (DebugConfig.disableFoldersTagsCache)
            return;

        FoldersTagsCacheKey key = new FoldersTagsCacheKey(mbox.getAccountId());
        mMemcachedLookup.remove(key);
    }
}
