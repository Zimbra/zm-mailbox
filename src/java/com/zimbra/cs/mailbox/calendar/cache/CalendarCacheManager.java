/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;

public class CalendarCacheManager {

    // for appointment summary caching (primarily for ZWC)
    protected boolean mSummaryCacheEnabled;
    protected CalSummaryCache mSummaryCache;

    // for CalDAV ctag caching
    protected CalListCache mCalListCache;
    protected CtagInfoCache mCtagCache;
    protected CtagResponseCache mCtagResponseCache;


    public CalendarCacheManager() {
    }

    @PostConstruct
    public void init() {
        mCtagCache = new MemcachedCtagInfoCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mCtagCache);

        mCalListCache = new MemcachedCalListCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mCalListCache);

        mCtagResponseCache = new MemcachedCtagResponseCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mCtagResponseCache);

        int summaryLRUSize = 0;
        try {
            mSummaryCacheEnabled = Provisioning.getInstance().getLocalServer().isCalendarCacheEnabled();
        } catch (ServiceException e) {
            ZimbraLog.cache.error("Error while fetching the attribute calendarCacheEnabled");
        }
        if (mSummaryCacheEnabled) {
            try {
                summaryLRUSize = Provisioning.getInstance().getLocalServer().getCalendarCacheLRUSize();
            } catch (ServiceException e) {
                ZimbraLog.cache.error("Error while fetching attribute calendarCacheLRUSize");
            }
        }
        mSummaryCache = new CalSummaryCache(summaryLRUSize);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mSummaryCache);

        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mCtagCache, "ctagInfoCache");
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mCalListCache, "calListCache");
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mCtagResponseCache, "ctagResponseCache");
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mSummaryCache, "calSummaryCache");
    }

    public void notifyCommittedChanges(PendingModifications mods, int changeId) throws ServiceException {
        if (mSummaryCacheEnabled) {
            mSummaryCache.notifyCommittedChanges(mods, changeId);
        }
        new CalListCacheMailboxListener(mCalListCache).notifyCommittedChanges(mods, changeId);
        new CtagInfoCacheMailboxListener(mCtagCache).notifyCommittedChanges(mods, changeId);
    }

    public void purgeMailbox(Mailbox mbox) throws ServiceException {
        mSummaryCache.purgeMailbox(mbox);
        mCalListCache.remove(mbox);
        mCtagCache.remove(mbox);
    }

    CtagInfoCache getCtagCache() { return mCtagCache; }
    public CalSummaryCache getSummaryCache() { return mSummaryCache; }
    public CtagResponseCache getCtagResponseCache() { return mCtagResponseCache; }

    protected CtagInfo getFolder(String accountId, int folderId) throws ServiceException {
        CtagInfo calInfo = null;
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.id, accountId);
        if (acct == null) {
            ZimbraLog.calendar.warn("Invalid account %s during cache lookup", accountId);
            return null;
        }
        if (Provisioning.onLocalServer(acct)) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            Folder folder = mbox.getFolderById(null, folderId);
            if (folder != null)
                calInfo = new CtagInfo(folder);
            else
                ZimbraLog.calendar.warn("Invalid folder %d in account %s during cache lookup", folderId, accountId);
        } else {
            ZAuthToken zat = AuthProvider.getAdminAuthToken().toZAuthToken();
            ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(acct));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(acct.getId());
            zoptions.setTargetAccountBy(Key.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            ItemId iidFolder = new ItemId(accountId, folderId);
            ZFolder zfolder = zmbx.getFolderById(iidFolder.toString());
            if (zfolder != null)
                calInfo = new CtagInfo(zfolder);
            else
                ZimbraLog.calendar.warn("Invalid folder %d in account %s during cache lookup", folderId, accountId);
        }
        return calInfo;
    }

    public AccountCtags getCtags(AccountKey key) throws ServiceException {
        CalList calList = mCalListCache.get(key.getAccountId());
        if (calList == null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(key.getAccountId());
            if (mbox == null) {
                ZimbraLog.calendar.warn("Invalid account %s during cache lookup", key.getAccountId());
                return null;
            }
            List<Folder> calFolders = mbox.getCalendarFolders(null, SortBy.NONE);
            Set<Integer> idset = new HashSet<Integer>(calFolders.size());
            idset.add(Mailbox.ID_FOLDER_INBOX);  // Inbox is always included for scheduling support.
            for (Folder calFolder : calFolders) {
                idset.add(calFolder.getId());
            }
            calList = new CalList(idset);
            mCalListCache.put(key.getAccountId(), calList);
        }

        Collection<Integer> calendarIds = calList.getCalendars();
        List<Pair<String,Integer>> calKeys = new ArrayList<>(calendarIds.size());
        String accountId = key.getAccountId();
        for (int calFolderId : calendarIds) {
            calKeys.add(new Pair<>(accountId, calFolderId));
        }

        Map<Pair<String,Integer>, CtagInfo> ctagsMap = mCtagCache.get(calKeys);

        // Resolve cache misses from DB as necessary, and add missing entries to cache
        Map<Pair<String,Integer>, CtagInfo> toPut = new HashMap<>();
        for (Map.Entry<Pair<String,Integer>, CtagInfo> entry : ctagsMap.entrySet()) {
            Pair<String,Integer> pair = entry.getKey();
            CtagInfo info = entry.getValue();
            boolean needToPut = false;
            if (info == null) {
                info = getFolder(pair.getFirst(), pair.getSecond());
                needToPut = true;
            }
            if (info != null) {
                if (info.isMountpoint()) {
                    // no multi-get for mountpoint resolution
                    CtagInfo target = mCtagCache.get(info.getRemoteAccount(), info.getRemoteId());
                    if (target != null) {
                        // Mountpoint inherits ctag from the target.
                        String remoteCtag = target.getCtag();
                        if (!remoteCtag.equals(info.getCtag())) {
                            info.setCtag(remoteCtag);
                            needToPut = true;
                        }
                    }
                }
                if (needToPut) {
                    toPut.put(pair, info);
                    entry.setValue(info);
                }
            }
        }
        mCtagCache.put(toPut);

        AccountCtags acctCtags = new AccountCtags(calList, ctagsMap.values());
        return acctCtags;
    }
}
