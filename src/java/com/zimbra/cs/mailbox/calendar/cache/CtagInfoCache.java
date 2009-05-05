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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.ZimbraMemcachedClient;
import com.zimbra.common.util.ZimbraMemcachedClient.KeyPrefix;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class CtagInfoCache {

    private static final KeyPrefix MEMCACHED_PREFIX = MemcachedKeyPrefix.CTAGINFO;
    private ZimbraMemcachedClient mMemcachedClient;

    private CtagInfo cacheGet(CalendarKey key) throws ServiceException {
        Object value = mMemcachedClient.get(MEMCACHED_PREFIX, key.getKeyString());
        if (value == null) return null;

        String encoded = (String) value;
        Metadata meta = new Metadata(encoded);
        return new CtagInfo(meta);
    }

    private Map<CalendarKey, CtagInfo> cacheGetMulti(List<CalendarKey> keys) throws ServiceException {
        int size = keys.size();
        Map<CalendarKey, CtagInfo> result = new HashMap<CalendarKey, CtagInfo>(size);
        Map<String, CalendarKey> keymap = new HashMap<String, CalendarKey>(size);
        List<String> keystrs = new ArrayList<String>(size);
        for (CalendarKey key : keys) {
            result.put(key, null);
            String kval = key.getKeyString();
            keymap.put(kval, key);
            keystrs.add(kval);
        }
        Map<String, Object> values = mMemcachedClient.getMulti(MEMCACHED_PREFIX, keystrs);
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String kval = entry.getKey();
                CalendarKey ckey = keymap.get(kval);
                if (ckey != null) {
                    CtagInfo info = null;
                    String encoded = (String) entry.getValue();
                    if (encoded != null) {
                        Metadata meta = new Metadata(encoded);
                        info = new CtagInfo(meta);
                    }
                    result.put(ckey, info);
                }
            }
        }
        return result;
    }

    private void cacheRemove(CalendarKey key) {
        mMemcachedClient.remove(MEMCACHED_PREFIX, key.getKeyString());
    }

    private boolean cacheContains(CalendarKey key) {
        return mMemcachedClient.contains(MEMCACHED_PREFIX, key.getKeyString());
    }

    private void cachePut(CalendarKey key, CtagInfo value) {
        String encoded = value.encodeMetadata().toString();
        mMemcachedClient.put(MEMCACHED_PREFIX, key.getKeyString(), encoded);
    }

    CtagInfoCache() {
        mMemcachedClient = MemcachedConnector.getClient();
    }

    public CtagInfo get(CalendarKey key) throws ServiceException {
        return get(key, false);
//        CtagInfo info = cacheGet(key);
//        if (info == null || info.isMountpoint())
//            info = resolve(key, info, false);
//        return info;
    }

    /**
     * @param key
     * @param wasMountpoint If true, this call is being made to resolve a mountpoint.  This information
     *                      is used to detect a mountpoint to a mountpoint, which is not allowed.
     * @return
     * @throws ServiceException
     */
    private CtagInfo get(CalendarKey key, boolean wasMountpoint) throws ServiceException {
        CtagInfo info = cacheGet(key);
        // Always resolve mountpoint to actual calendar folder.
        if (info != null && !info.isMountpoint())
            return info;

        boolean needToPut = false;
        if (info == null) {
            // Not currently in the cache.  Get it the hard way.
            info = getFolder(key);
            needToPut = true;
        }
        if (info != null) {
            // Don't follow mountpoint of mountpoint.  Only one level of indirection is allowed.
            if (info.isMountpoint() && !wasMountpoint) {
                CalendarKey keyTarget = new CalendarKey(info.getRemoteAccount(), info.getRemoteId());
                CtagInfo infoTarget = get(keyTarget, true);  // recurse
                if (infoTarget != null) {
                    // Mountpoint inherits ctag from the target.
                    String remoteCtag = infoTarget.getCtag();
                    if (!remoteCtag.equals(info.getCtag())) {
                        info.setCtag(remoteCtag);
                        needToPut = true;
                    }
                } else {
                    ZimbraLog.calendar.warn("Mounpoint target %s:%d not found during cache lookup",
                            keyTarget.getAccountId(), keyTarget.getFolderId());
                }
            }
            if (needToPut)
                cachePut(key, info);
        }
        return info;
    }

    public Map<CalendarKey, CtagInfo> getMultiSerial(List<CalendarKey> keys) throws ServiceException {
        Map<CalendarKey, CtagInfo> result = new HashMap<CalendarKey, CtagInfo>(keys.size());
        for (CalendarKey key : keys) {
            CtagInfo value = get(key);
            result.put(key, value);  // Null values are added too.
        }
        return result;
    }

    public Map<CalendarKey, CtagInfo> getMulti(List<CalendarKey> keys) throws ServiceException {
        // entries to put back to cache
        Map<CalendarKey, CtagInfo> toPut = new HashMap<CalendarKey, CtagInfo>(keys.size());

        // Use multi-get from cache.
        Map<CalendarKey, CtagInfo> result = cacheGetMulti(keys);

        // Resolve as necessary.
        for (Map.Entry<CalendarKey, CtagInfo> entry : result.entrySet()) {
            CalendarKey key = entry.getKey();
            CtagInfo info = entry.getValue();
            boolean needToPut = false;
            if (info == null) {
                info = getFolder(key);
                needToPut = true;
            }
            if (info != null) {
                if (info.isMountpoint()) {
                    CalendarKey keyTarget = new CalendarKey(info.getRemoteAccount(), info.getRemoteId());
                    CtagInfo target = get(keyTarget, true);  // no multi-get for mountpoint resolution
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
                    toPut.put(key, info);
                    entry.setValue(info);
                }
            }
        }

        // Add new entries to cache.
        for (Map.Entry<CalendarKey, CtagInfo> entry : toPut.entrySet()) {
            CalendarKey key = entry.getKey();
            CtagInfo info = entry.getValue();
            cachePut(key, info);
        }

        return result;
    }

    private CtagInfo getFolder(CalendarKey key) throws ServiceException {
        String accountId = key.getAccountId();
        int folderId = key.getFolderId();
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
        return cacheContains(key);
    }

    void purgeMailbox(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getCalendarFolders(null, SortBy.NONE);
        for (Folder folder : folders) {
            CalendarKey key = new CalendarKey(accountId, folder.getId());
            cacheRemove(key);
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
                        cacheRemove(key);
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
                        cacheRemove(key);
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
                        cacheRemove(key);
                    }
                } else if (deletedObj instanceof Integer) {
                    // We only have item id.  Assume it's a folder id and issue a delete.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) continue;  // just to be safe
                    int itemId = ((Integer) deletedObj).intValue();
                    CalendarKey key = new CalendarKey(acctId, itemId);
                    cacheRemove(key);
                }
            }
        }
    }
}
