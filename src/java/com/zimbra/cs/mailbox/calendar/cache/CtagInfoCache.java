/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class CtagInfoCache {

    private MemcachedMap<CalendarKey, CtagInfo> mMemcachedLookup;

    CtagInfoCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        CtagInfoSerializer serializer = new CtagInfoSerializer();
        mMemcachedLookup = new MemcachedMap<CalendarKey, CtagInfo>(memcachedClient, serializer); 
    }

    private static class CtagInfoSerializer implements MemcachedSerializer<CtagInfo> {
        
        public Object serialize(CtagInfo value) {
            return value.encodeMetadata().toString();
        }

        public CtagInfo deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return new CtagInfo(meta);
        }
    }

    public boolean containsKey(CalendarKey key) throws ServiceException {
        return mMemcachedLookup.get(key) != null;
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
        CtagInfo info = mMemcachedLookup.get(key);
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
                mMemcachedLookup.put(key, info);
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
        Map<CalendarKey, CtagInfo> result = mMemcachedLookup.getMulti(keys);

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
        mMemcachedLookup.putMulti(toPut);

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
            ZAuthToken zat = AuthProvider.getAdminAuthToken().toZAuthToken();
            ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(acct));
            zoptions.setNoSession(true);
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

    void purgeMailbox(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getCalendarFolders(null, SortBy.NONE);
        List<CalendarKey> keys = new ArrayList<CalendarKey>(folders.size());
        for (Folder folder : folders) {
            CalendarKey key = new CalendarKey(accountId, folder.getId());
            keys.add(key);
        }
        mMemcachedLookup.removeMulti(keys);
    }

    void notifyCommittedChanges(PendingModifications mods, int changeId) {
        int inboxFolder = Mailbox.ID_FOLDER_INBOX;
        Set<CalendarKey> keysToInvalidate = new HashSet<CalendarKey>();
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Message) {
                    Message msg = (Message) item;
                    if (msg.hasCalendarItemInfos() && msg.getFolderId() == inboxFolder) {
                        CalendarKey key = new CalendarKey(msg.getMailbox().getAccountId(), inboxFolder);
                        keysToInvalidate.add(key);
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
                        keysToInvalidate.add(key);
                    }
                } else if (whatChanged instanceof Message) {
                    Message msg = (Message) whatChanged;
                    if (msg.hasCalendarItemInfos()) {
                        if (msg.getFolderId() == inboxFolder || (change.why & Change.MODIFIED_FOLDER) != 0) {
                            // If message was moved, we don't know which folder it was moved from.
                            // Just invalidate the Inbox because that's the only message folder we care
                            // about in calendaring.
                            CalendarKey key = new CalendarKey(msg.getMailbox().getAccountId(), inboxFolder);
                            keysToInvalidate.add(key);
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
                        keysToInvalidate.add(key);
                    }
                } else if (deletedObj instanceof Integer) {
                    // We only have item id.  Assume it's a folder id and issue a delete.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) continue;  // just to be safe
                    int itemId = ((Integer) deletedObj).intValue();
                    CalendarKey key = new CalendarKey(acctId, itemId);
                    keysToInvalidate.add(key);
                }
                // Let's not worry about hard deletes of invite/reply emails.  It has no practical benefit.
                // Besides, when deletedObj is an Integer, we can't tell if it's a calendaring Message.
            }
        }
        try {
            mMemcachedLookup.removeMulti(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify ctag info cache.  Some cached data may become stale.", e);
        }
    }
}
