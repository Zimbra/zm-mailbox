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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public class CalListCache {

    @SuppressWarnings("serial")
    private static class ListLRU extends LinkedHashMap<AccountKey, CalList> {
        private int mMaxAllowed;

        private ListLRU(int capacity) {
            super(capacity + 1, 1.0f, true);
            mMaxAllowed = Math.max(capacity, 1);
        }

        @Override
        public void clear() {
            super.clear();
        }

        @Override
        public CalList get(Object key) {
            return super.get(key);
        }

        @Override
        public CalList put(AccountKey key, CalList value) {
            CalList prevVal = super.put(key, value);
            return prevVal;
        }

        @Override
        public void putAll(Map<? extends AccountKey, ? extends CalList> t) {
            super.putAll(t);
        }

        @Override
        public CalList remove(Object key) {
            CalList prevVal = super.remove(key);
            return prevVal;
        }        

        @Override
        protected boolean removeEldestEntry(Map.Entry<AccountKey, CalList> eldest) {
            boolean remove = size() > mMaxAllowed;
            return remove;
        }
    }

    private ListLRU mListLRU;

    CalListCache() {
        // TODO: Use memcached instead of LRU on heap.
        int lruSize = 0;
        if (LC.calendar_cache_enabled.booleanValue())
            lruSize = LC.calendar_cache_lru_size.intValue();
        mListLRU = new ListLRU(lruSize);
    }

    public CalList get(AccountKey key) throws ServiceException{
        CalList list = null;
        synchronized (mListLRU) {
            list = mListLRU.get(key);
        }
        if (list != null) return list;

        // Not currently in the cache.  Get it the hard way.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(key.getAccountId());
        if (mbox == null) {
            ZimbraLog.calendar.warn("Invalid account %s during cache lookup", key.getAccountId());
            return null;
        }
        List<Folder> calFolders = mbox.getCalendarFolders(null, SortBy.NONE);
        Set<Integer> idset = new HashSet<Integer>(calFolders.size());
        for (Folder calFolder : calFolders) {
            idset.add(calFolder.getId());
        }
        list = new CalList(idset);
        synchronized (mListLRU) {
            mListLRU.put(key, list);
        }
        return list;
    }

    private void addCalendar(AccountKey key, int calFolderId) {
        synchronized (mListLRU) {
            CalList list = mListLRU.get(key);
            if (list != null && !list.contains(calFolderId)) {
                CalList newList = new CalList(list);
                newList.add(calFolderId);
                mListLRU.put(key, newList);
            }
        }
    }

    // Remove a calendar from account's calendar list.
    private void removeCalendar(AccountKey key, int calFolderId) {
        synchronized (mListLRU) {
            CalList list = mListLRU.get(key);
            if (list != null && list.contains(calFolderId)) {
                CalList newList = new CalList(list);
                newList.remove(calFolderId);
                mListLRU.put(key, newList);
            }
        }
    }

    private void touchCalendar(AccountKey key, int calFolderId) {
        synchronized (mListLRU) {
            CalList list = mListLRU.get(key);
            if (list != null && list.contains(calFolderId)) {
                CalList newList = new CalList(list);
                newList.incrementSeq();
                mListLRU.put(key, newList);
            }
        }
    }

    void notifyCommittedChanges(PendingModifications mods, int changeId) {
        if (mods == null)
            return;
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Folder) {
                    Folder folder = (Folder) item;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        AccountKey key = new AccountKey(folder.getMailbox().getAccountId());
                        addCalendar(key, folder.getId());
                    }
                }
            }
        }
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                if (whatChanged instanceof Folder) {
                    Folder folder = (Folder) whatChanged;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        AccountKey key = new AccountKey(folder.getMailbox().getAccountId());
                        int folderId = folder.getId();
                        if ((change.why & Change.MODIFIED_FOLDER) != 0) {
                            // moving the calendar folder to another parent folder
                            int parentFolder = folder.getFolderId();
                            if (parentFolder == Mailbox.ID_FOLDER_TRASH)
                                removeCalendar(key, folderId);
                            else
                                addCalendar(key, folderId);
                        } else {
                            // not a folder move, but something else changed, either calendar's metadata
                            // or a child item (appointment/task)
                            touchCalendar(key, folderId);
                        }
                    }
                }
            }
        }
        if (mods.deleted != null) {
            // This code gets called even for non-calendar items, for example it's called for every email
            // being emptied from Trash.  But there's no way to short circuit out of here because the delete
            // notification doesn't tell us the item type of what's being deleted.  Oh well.
            CtagInfoCache infoCache = CalendarCacheManager.getInstance().getCtagCache();
            for (Map.Entry<ModificationKey, Object> entry : mods.deleted.entrySet()) {
                Object deletedObj = entry.getValue();
                if (deletedObj instanceof Folder) {
                    Folder folder = (Folder) deletedObj;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        AccountKey key = new AccountKey(folder.getMailbox().getAccountId());
                        removeCalendar(key, folder.getId());
                    }
                } else if (deletedObj instanceof Integer) {
                    // We only have item id.  Consult the calendar info cache to see if we're dealing with
                    // a calendar folder.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) continue;  // just to be safe
                    int itemId = ((Integer) deletedObj).intValue();
                    CalendarKey calkey = new CalendarKey(acctId, itemId);
                    if (infoCache.containsKey(calkey)) {
                        AccountKey key = new AccountKey(acctId);
                        removeCalendar(key, itemId);
                    }
                }
            }
        }
    }
}
