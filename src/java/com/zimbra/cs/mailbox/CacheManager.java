/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.util.Set;

import org.springframework.beans.BeansException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.acl.EffectiveACLCacheMailboxListener;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.Zimbra;

public class CacheManager implements MailboxListener {

    public static void purgeMailbox(Mailbox mbox) throws ServiceException {
        Zimbra.getAppContext().getBean(CalendarCacheManager.class).purgeMailbox(mbox);
        Zimbra.getAppContext().getBean(EffectiveACLCache.class).remove(mbox);
        Zimbra.getAppContext().getBean(FoldersAndTagsCache.class).remove(mbox);
    }

    @Override
    public Set<MailItem.Type> notifyForItemTypes() {
        return MailboxListener.ALL_ITEM_TYPES;
    }

    @Override
    public void notify(ChangeNotification notification) throws BeansException, ServiceException {
        PendingModifications mods = notification.mods;
        int changeId = notification.lastChangeId;

        // Invalidate caches managed by CalendarCacheManager
        Zimbra.getAppContext().getBean(CalendarCacheManager.class).notifyCommittedChanges(mods, changeId);

        // Invalidate EffectiveACLCache
        new EffectiveACLCacheMailboxListener(Zimbra.getAppContext().getBean(EffectiveACLCache.class)).notify(notification);
    }
}
