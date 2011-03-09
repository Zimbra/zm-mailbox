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

import java.util.EnumSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingModifications;

public class MemcachedCacheManager extends MailboxListener {

    public static void purgeMailbox(Mailbox mbox) throws ServiceException {
        CalendarCacheManager.getInstance().purgeMailbox(mbox);
        EffectiveACLCache.getInstance().purgeMailbox(mbox);
        FoldersTagsCache.getInstance().purgeMailbox(mbox);
    }

    @Override
    public void notify(ChangeNotification notification) {
        PendingModifications mods = notification.mods;
        int changeId = notification.lastChangeId;
        // We have to notify calendar cache before checking memcached connectedness
        // because a portion of calendar cache is not memcached-based.
        CalendarCacheManager.getInstance().notifyCommittedChanges(mods, changeId);
        if (MemcachedConnector.isConnected()) {
            EffectiveACLCache.getInstance().notifyCommittedChanges(mods, changeId);
            FoldersTagsCache.getInstance().notifyCommittedChanges(mods, changeId);
        }
    }

    private static final EnumSet<Type> TYPES = EnumSet.allOf(Type.class);
    
    @Override
    public Set<Type> registerForItemTypes() {
        return TYPES;
    }
}
