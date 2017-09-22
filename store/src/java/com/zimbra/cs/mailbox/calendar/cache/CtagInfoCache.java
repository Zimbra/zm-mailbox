/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.AccountUtil;

public class CtagInfoCache {

    private final MemcachedMap<CalendarKey, CtagInfo> mMemcachedLookup;

    CtagInfoCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        CtagInfoSerializer serializer = new CtagInfoSerializer();
        mMemcachedLookup = new MemcachedMap<CalendarKey, CtagInfo>(memcachedClient, serializer);
    }

    private static class CtagInfoSerializer implements MemcachedSerializer<CtagInfo> {

        public CtagInfoSerializer() { }

        @Override
        public Object serialize(CtagInfo value) {
            return value.encodeMetadata().toString();
        }

        @Override
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

    void notifyCommittedChanges(PendingLocalModifications mods, int changeId) {
        int inboxFolder = Mailbox.ID_FOLDER_INBOX;
        Set<CalendarKey> keysToInvalidate = new HashSet<CalendarKey>();
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, BaseItemInfo> entry : mods.created.entrySet()) {
                BaseItemInfo item = entry.getValue();
                if (item instanceof Message) {
                    Message msg = (Message) item;
                    if (msg.hasCalendarItemInfos() && msg.getFolderId() == inboxFolder) {
                        CalendarKey key = new CalendarKey(msg.getAccountId(), inboxFolder);
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
                    MailItem.Type viewType = folder.getDefaultView();
                    if (viewType == MailItem.Type.APPOINTMENT || viewType == MailItem.Type.TASK) {
                        CalendarKey key = new CalendarKey(folder.getAccountId(), folder.getId());
                        keysToInvalidate.add(key);
                    }
                } else if (whatChanged instanceof Message) {
                    Message msg = (Message) whatChanged;
                    if (msg.hasCalendarItemInfos()) {
                        if (msg.getFolderId() == inboxFolder || (change.why & Change.FOLDER) != 0) {
                            // If message was moved, we don't know which folder it was moved from.
                            // Just invalidate the Inbox because that's the only message folder we care
                            // about in calendaring.
                            CalendarKey key = new CalendarKey(msg.getAccountId(), inboxFolder);
                            keysToInvalidate.add(key);
                        }
                    }
                }
            }
        }
        if (mods.deleted != null) {
            for (Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                Type type = (Type) entry.getValue().what;
                if (type == MailItem.Type.FOLDER) {
                    // We only have item id.  Assume it's a folder id and issue a delete.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null)
                        continue;  // just to be safe
                    CalendarKey key = new CalendarKey(acctId, entry.getKey().getItemId());
                    keysToInvalidate.add(key);
                }
                // Let's not worry about hard deletes of invite/reply emails.  It has no practical benefit.
            }
        }
        try {
            mMemcachedLookup.removeMulti(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify ctag info cache.  Some cached data may become stale.", e);
        }
    }
}
