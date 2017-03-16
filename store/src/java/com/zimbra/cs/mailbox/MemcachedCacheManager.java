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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingLocalModifications;

public class MemcachedCacheManager extends MailboxListener {

    public static void purgeMailbox(Mailbox mbox) throws ServiceException {
        CalendarCacheManager.getInstance().purgeMailbox(mbox);
        EffectiveACLCache.getInstance().purgeMailbox(mbox);
        FoldersTagsCache.getInstance().purgeMailbox(mbox);
    }

    @Override
    public void notify(ChangeNotification notification) {
        PendingLocalModifications mods = notification.mods;
        int changeId = notification.lastChangeId;
        // We have to notify calendar cache before checking memcached connectedness
        // because a portion of calendar cache is not memcached-based.
        CalendarCacheManager.getInstance().notifyCommittedChanges(mods, changeId);
        if (MemcachedConnector.isConnected()) {
            EffectiveACLCache.getInstance().notifyCommittedChanges(mods, changeId);
        }
    }
}
