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

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Caching facade for any high-level operations that want visibility to multiple other cache delegates.
 */
public class CacheManager {
    protected static CacheManager singleton;
    @Autowired protected CalendarCacheManager calendarCacheManager;
    @Autowired protected EffectiveACLCache effectiveACLCache;
    @Autowired protected FoldersAndTagsCache foldersAndTagsCache;

    public static CacheManager getInstance() {
        if (singleton == null) {
            singleton = Zimbra.getAppContext().getBean(CacheManager.class);
        }
        return singleton;
    }

    public void purgeMailbox(Mailbox mbox) throws ServiceException {
        calendarCacheManager.purgeMailbox(mbox);
        effectiveACLCache.remove(mbox);
        foldersAndTagsCache.remove(mbox);
    }
}
