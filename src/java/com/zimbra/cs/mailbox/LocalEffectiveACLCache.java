package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;

/** An in-process EffectiveACLCache implementation, can be used by unit tests. */
public class LocalEffectiveACLCache implements EffectiveACLCache {
    protected Map<Key, ACL> map = new ConcurrentHashMap<>();

    @VisibleForTesting
    void flush() {
        map.clear();
    }

    protected String key(Mailbox mbox) {
        return mbox.getAccountId();
    }

    @Override
    public ACL get(Key key) throws ServiceException {
        return map.get(key);
    }

    @Override
    public void put(Key key, ACL acl) throws ServiceException {
        map.put(key, acl);
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        List<Folder> folders = mbox.getFolderList(null, SortBy.NONE);
        Set<Key> keys = new HashSet<>(folders.size());
        for (Folder folder : folders) {
            keys.add(new Key(mbox.getAccountId(), folder.getId()));
        }
        remove(keys);
    }

    public void remove(Set<Key> keys) throws ServiceException {
        for (Key key: keys) {
            map.remove(key);
        }
    }
}
