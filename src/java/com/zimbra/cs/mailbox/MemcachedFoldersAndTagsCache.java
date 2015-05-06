package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.StringBasedMemcachedKey;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedFoldersAndTagsCache implements FoldersAndTagsCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<MemcachedKey, FoldersAndTags> mMemcachedLookup;

    /** Constructor */
    public MemcachedFoldersAndTagsCache() {
    }

    @PostConstruct
    public void init() {
        mMemcachedLookup = new MemcachedMap<>(memcachedClient, new Serializer(), false);
    }

    protected MemcachedKey key(Mailbox mbox) {
        return new StringBasedMemcachedKey(MemcachedKeyPrefix.MBOX_FOLDERS_TAGS, mbox.getAccountId());
    }

    /** Returns cached list of all folders and tags for a given mailbox */
    @Override
    public FoldersAndTags get(Mailbox mbox) throws ServiceException {
        return mMemcachedLookup.get(key(mbox));
    }

    /** Caches list of all folders and tags for a given mailbox */
    @Override
    public void put(Mailbox mbox, FoldersAndTags foldersAndTags) throws ServiceException {
        mMemcachedLookup.put(key(mbox), foldersAndTags);
    }

    /** Clears cache of folders and tags for a given mailbox */
    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        mMemcachedLookup.remove(key(mbox));
    }


    private static class Serializer implements MemcachedSerializer<FoldersAndTags> {
        Serializer() { }

        @Override
        public Object serialize(FoldersAndTags value) {
            return value.encode().toString();
        }

        @Override
        public FoldersAndTags deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return FoldersAndTags.decode(meta);
        }
    }
}
