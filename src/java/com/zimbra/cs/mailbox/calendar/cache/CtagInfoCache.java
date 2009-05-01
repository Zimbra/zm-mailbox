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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class CtagInfoCache {

    @SuppressWarnings("serial")
    private static class InfoLRU extends LinkedHashMap<CalendarKey, CtagInfo> {
        private int mMaxAllowed;

        private InfoLRU(int capacity) {
            super(capacity + 1, 1.0f, true);
            mMaxAllowed = Math.max(capacity, 1);
        }

        @Override
        public void clear() {
            super.clear();
        }

        @Override
        public CtagInfo get(Object key) {
            return super.get(key);
        }

        @Override
        public CtagInfo put(CalendarKey key, CtagInfo value) {
            CtagInfo prevVal = super.put(key, value);
            return prevVal;
        }

        @Override
        public void putAll(Map<? extends CalendarKey, ? extends CtagInfo> t) {
            super.putAll(t);
        }

        @Override
        public CtagInfo remove(Object key) {
            CtagInfo prevVal = super.remove(key);
            return prevVal;
        }        

        @Override
        protected boolean removeEldestEntry(Map.Entry<CalendarKey, CtagInfo> eldest) {
            boolean remove = size() > mMaxAllowed;
            return remove;
        }
    }

    private InfoLRU mInfoLRU;

    CtagInfoCache() {
        // TODO: Use memcached instead of LRU on heap.
        int lruSize = 0;
        if (LC.calendar_cache_enabled.booleanValue())
            lruSize = LC.calendar_cache_lru_size.intValue();
        mInfoLRU = new InfoLRU(lruSize);
    }

    public CtagInfo get(CalendarKey key) throws ServiceException {
        return get(key, false);
    }
    
    /**
     * @param key
     * @param wasMountpoint If true, this call is being made to resolve a mountpoint.  This information
     *                      is used to detect a mountpoint to a mountpoint, which is not allowed.
     * @return
     * @throws ServiceException
     */
    private CtagInfo get(CalendarKey key, boolean wasMountpoint) throws ServiceException {
        CtagInfo info = null;
        synchronized (mInfoLRU) {
            info = mInfoLRU.get(key);
        }
        // Always resolve mountpoint to actual calendar folder.
        if (info != null && !info.isMountpoint())
            return info;

        // Not currently in the cache.  Get it the hard way.
        if (info == null)
            info = getFolder(key.getAccountId(), key.getFolderId());
        if (info != null) {
            // Don't follow mountpoint of mountpoint.  Only one level of indirection is allowed.
            if (info.isMountpoint() && !wasMountpoint) {
                CalendarKey keyTarget = new CalendarKey(info.getRemoteAccount(), info.getRemoteId());
                CtagInfo infoTarget = get(keyTarget, true);  // recurse
                if (infoTarget != null) {
                    // Mountpoint inherits ctag from the target.
                    info.setCtag(infoTarget.getCtag());
                } else {
                    ZimbraLog.calendar.warn("Mounpoint target %s:%d not found during cache lookup",
                            keyTarget.getAccountId(), keyTarget.getFolderId());
                }
            }
            synchronized (mInfoLRU) {
                mInfoLRU.put(key, info);
            }
        }
        return info;
    }

    public List<CtagInfo> gets(List<CalendarKey> keys) throws ServiceException {
        List<CtagInfo> values = new ArrayList<CtagInfo>(keys.size());
        for (CalendarKey key : keys) {
            CtagInfo value = get(key);
            values.add(value);  // Null values are added too.
        }
        return values;
    }

    private CtagInfo getFolder(String accountId, int folderId) throws ServiceException {
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
            ZAuthToken zat = AuthToken.getZimbraAdminAuthToken().toZAuthToken();
            ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(acct));
            zoptions.setNoSession(true);
            zoptions.setResponseProtocol(SoapProtocol.SoapJS);
            zoptions.setTargetAccount(acct.getId());
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
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

    public boolean containsKey(CalendarKey key) {
        synchronized (mInfoLRU) {
            return mInfoLRU.containsKey(key);
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
                        CalendarKey key = new CalendarKey(folder.getMailbox().getAccountId(), folder.getId());
                        synchronized (mInfoLRU) {
                            mInfoLRU.remove(key);
                        }
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
                        CalendarKey key = new CalendarKey(folder.getMailbox().getAccountId(), folder.getId());
                        synchronized (mInfoLRU) {
                            mInfoLRU.remove(key);
                        }
                    }
                }
            }
        }
        if (mods.deleted != null) {
            // This code gets called even for non-calendar items, for example it's called for every email
            // being emptied from Trash.  But there's no way to short circuit out of here because the delete
            // notification doesn't tell us the item type of what's being deleted.  Oh well.
            for (Map.Entry<ModificationKey, Object> entry : mods.deleted.entrySet()) {
                Object deletedObj = entry.getValue();
                if (deletedObj instanceof Folder) {
                    Folder folder = (Folder) deletedObj;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        CalendarKey key = new CalendarKey(folder.getMailbox().getAccountId(), folder.getId());
                        synchronized (mInfoLRU) {
                            mInfoLRU.remove(key);
                        }
                    }
                } else if (deletedObj instanceof Integer) {
                    // We only have item id.  Assume it's a folder id and issue a delete.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) continue;  // just to be safe
                    int itemId = ((Integer) deletedObj).intValue();
                    CalendarKey key = new CalendarKey(acctId, itemId);
                    synchronized (mInfoLRU) {
                        mInfoLRU.remove(key);
                    }
                }
            }
        }
    }
}
