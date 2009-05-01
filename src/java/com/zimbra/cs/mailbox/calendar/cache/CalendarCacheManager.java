/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.session.PendingModifications;

public class CalendarCacheManager {

    // for appointment summary caching (primarily for ZWC)
    private CalSummaryCache mSummaryCache;

    // for CalDAV ctag caching
    private CalListCache mCalListCache;
    private CtagInfoCache mCtagCache;
    private CtagResponseCache mCtagResponseCache;

    private static CalendarCacheManager sInstance = new CalendarCacheManager();
    public static CalendarCacheManager getInstance() { return sInstance; }

    private CalendarCacheManager() {
        int summaryLRUSize = 0;
        if (LC.calendar_cache_enabled.booleanValue())
            summaryLRUSize = LC.calendar_cache_lru_size.intValue();
        mSummaryCache = new CalSummaryCache(summaryLRUSize);
        mCtagCache = new CtagInfoCache();
        mCalListCache = new CalListCache();
        mCtagResponseCache = new CtagResponseCache();
    }

    public void notifyCommittedChanges(PendingModifications mods, int changeId) {
        if (!LC.calendar_cache_enabled.booleanValue())
            return;
        mSummaryCache.notifyCommittedChanges(mods, changeId);
        mCalListCache.notifyCommittedChanges(mods, changeId);
        mCtagCache.notifyCommittedChanges(mods, changeId);
    }

    public void removeMailbox(Mailbox mbox) {
        mSummaryCache.removeMailbox(mbox);
    }

    CtagInfoCache getCtagCache() { return mCtagCache; }
    public CalSummaryCache getSummaryCache() { return mSummaryCache; }
    public CtagResponseCache getCtagResponseCache() { return mCtagResponseCache; }

    public AccountCtags getCtags(AccountKey key) throws ServiceException {
        CalList calList = mCalListCache.get(key);
        if (calList == null) return null;
        Collection<Integer> calendarIds = calList.getCalendars();
        List<CalendarKey> calKeys = new ArrayList<CalendarKey>(calendarIds.size());
        String accountId = key.getAccountId();
        for (int calFolderId : calendarIds) {
            calKeys.add(new CalendarKey(accountId, calFolderId));
        }
        List<CtagInfo> ctags = mCtagCache.gets(calKeys);
        AccountCtags acctCtags = new AccountCtags(calList, ctags);
        return acctCtags;
    }
}
