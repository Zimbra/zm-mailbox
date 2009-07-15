/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.session.PendingModifications;

public class MemcachedCacheManager {

    public static void purgeMailbox(Mailbox mbox) throws ServiceException {
        CalendarCacheManager.getInstance().purgeMailbox(mbox);
    }

    public static void notifyCommittedChanges(PendingModifications mods, int changeId) {
        CalendarCacheManager.getInstance().notifyCommittedChanges(mods, changeId);
    }
}
