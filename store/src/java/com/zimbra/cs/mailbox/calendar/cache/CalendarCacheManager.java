/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingLocalModifications;

public class CalendarCacheManager {

    // for appointment summary caching (primarily for ZWC)
    private boolean mSummaryCacheEnabled;
    private CalSummaryCache mSummaryCache;

    // for CalDAV ctag caching
    private CalListCache mCalListCache;
    private CtagInfoCache mCtagCache;
    private CtagResponseCache mCtagResponseCache;

    private static CalendarCacheManager sInstance = new CalendarCacheManager();

    public static CalendarCacheManager getInstance() { return sInstance; }

    private CalendarCacheManager() {
        mCtagCache = new CtagInfoCache();
        mCalListCache = new CalListCache();
        mCtagResponseCache = new CtagResponseCache();

        int summaryLRUSize = 0;
        mSummaryCacheEnabled = LC.calendar_cache_enabled.booleanValue();
        if (mSummaryCacheEnabled)
            summaryLRUSize = LC.calendar_cache_lru_size.intValue();
        mSummaryCache = new CalSummaryCache(summaryLRUSize);
    }

    public void notifyCommittedChanges(PendingLocalModifications mods, int changeId){
        if (mSummaryCacheEnabled)
            mSummaryCache.notifyCommittedChanges(mods, changeId);
        if (MemcachedConnector.isConnected()) {
            mCalListCache.notifyCommittedChanges(mods, changeId);
            mCtagCache.notifyCommittedChanges(mods, changeId);
        }
    }

    public void purgeMailbox(Mailbox mbox) throws ServiceException {
        mSummaryCache.purgeMailbox(mbox);
        if (MemcachedConnector.isConnected()) {
            mCalListCache.purgeMailbox(mbox);
            mCtagCache.purgeMailbox(mbox);
        }
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
        Map<CalendarKey, CtagInfo> ctagsMap = mCtagCache.getMulti(calKeys);
        AccountCtags acctCtags = new AccountCtags(calList, ctagsMap.values());
        return acctCtags;
    }
}
