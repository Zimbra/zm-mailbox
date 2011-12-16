/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * IMAP cache using Ehcache's DiskStore.
 *
 * @author ysasaki
 */
final class EhcacheImapCache implements ImapSessionManager.Cache {
    private final Ehcache ehcache;

    EhcacheImapCache(String name) {
        ehcache = CacheManager.getInstance().getEhcache(name);
    }

    @Override
    public void put(String key, ImapFolder folder) {
        if (ehcache.isKeyInCache(key)) {
            if (ImapSessionManager.isActiveKey(key)) {
                //isKeyInCache() can return expired elements, and does not update lastAccessTime
                //for example session paged out, then paged back in, then invokes a few commands, then paged out again
                //lastAccessTime is only set when paged back in, but actual session timeout based on execution of commands
                //for active cache need to do a get() so lastAccessTime is updated on page out, even if already exists in cache
                if (ehcache.get(key) != null) {
                    return;
                }
            } else {
                //no concerns with lastAccessTime for inactive; it is LRU'd instead of expired based on timeToIdle
                return;
            }
        }
        Element el = new Element(key, folder);
        ehcache.put(el);
    }

    @Override
    public ImapFolder get(String key) {
        Element el = ehcache.get(key);
        return el != null ? (ImapFolder) el.getValue() : null;
    }

    @Override
    public void remove(String key) {
        ehcache.remove(key);
    }
}
