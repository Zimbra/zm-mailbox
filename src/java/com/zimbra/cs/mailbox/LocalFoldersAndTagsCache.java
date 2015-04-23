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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;

/** An in-process FoldersAndTagsCache implementation, can be used by unit tests. */
public class LocalFoldersAndTagsCache implements FoldersAndTagsCache {
    protected Map<String, String> map = new ConcurrentHashMap<>();

    @VisibleForTesting
    void flush() {
        map.clear();
    }

    @Override
    public FoldersAndTags get(Mailbox mbox) throws ServiceException {
        String value = map.get(key(mbox));
        if (value == null) {
            return null;
        }
        Metadata meta = new Metadata(value);
        return FoldersAndTags.decode(meta);
    }

    @Override
    public void put(Mailbox mbox, FoldersAndTags foldersAndTags) throws ServiceException {
        map.put(key(mbox), foldersAndTags.encode().toString());
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        map.remove(key(mbox));
    }

    protected String key(Mailbox mbox) {
        return mbox.getAccountId();
    }
}
