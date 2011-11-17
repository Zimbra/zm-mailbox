/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public class CalListCache {

    private MemcachedMap<AccountKey, CalList> mMemcachedLookup;

    CalListCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        CalListSerializer serializer = new CalListSerializer();
        mMemcachedLookup = new MemcachedMap<AccountKey, CalList>(memcachedClient, serializer);
    }

    private static class CalListSerializer implements MemcachedSerializer<CalList> {
        CalListSerializer() { }

        @Override
        public Object serialize(CalList value) {
            return value.encodeMetadata().toString();
        }

        @Override
        public CalList deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return new CalList(meta);
        }
    }

    public CalList get(AccountKey key) throws ServiceException{
        CalList list = mMemcachedLookup.get(key);
        if (list != null) return list;

        // Not currently in the cache.  Get it the hard way.
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
        list = new CalList(idset);
        mMemcachedLookup.put(key, list);
        return list;
    }

    void purgeMailbox(Mailbox mbox) throws ServiceException {
        AccountKey key = new AccountKey(mbox.getAccountId());
        mMemcachedLookup.remove(key);
    }

    private static class ChangedFolders {
        public Set<Integer> created = new HashSet<Integer>();
        public Set<Integer> modified = new HashSet<Integer>();
        public Set<Integer> deleted = new HashSet<Integer>();

        public boolean isEmpty() {
            return created.isEmpty() && modified.isEmpty() && deleted.isEmpty();
        }
    }

    @SuppressWarnings("serial")
    private static class ChangeMap extends HashMap<String /* account id */, ChangedFolders> {
        public ChangeMap(int capacity) {
            super(capacity);
        }

        public ChangedFolders getAccount(String accountId) {
            ChangedFolders cf = get(accountId);
            if (cf == null) {
                cf = new ChangedFolders();
                put(accountId, cf);
            }
            return cf;
        }
    }

    void notifyCommittedChanges(PendingModifications mods, int changeId) {
        ChangeMap changeMap = new ChangeMap(1);
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.created.entrySet()) {
                MailItem item = (MailItem) entry.getValue().what;
                if (item instanceof Folder) {
                    Folder folder = (Folder) item;
                    MailItem.Type viewType = folder.getDefaultView();
                    if (viewType == MailItem.Type.APPOINTMENT || viewType == MailItem.Type.TASK) {
                        ChangedFolders changedFolders = changeMap.getAccount(entry.getKey().getAccountId());
                        changedFolders.created.add(folder.getId());
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
                        ChangedFolders changedFolders = changeMap.getAccount(entry.getKey().getAccountId());
                        int folderId = folder.getId();
                        if ((change.why & Change.FOLDER) != 0) {
                            // moving the calendar folder to another parent folder
                            int parentFolder = folder.getFolderId();
                            changedFolders.created.add(folderId);
                            if (parentFolder == Mailbox.ID_FOLDER_TRASH) {
                                changedFolders.deleted.add(folderId);
                            } else {
                                changedFolders.created.add(folderId);
                            }
                        } else {
                            // not a folder move, but something else changed, either calendar's metadata
                            // or a child item (appointment/task)
                            changedFolders.modified.add(folderId);
                        }
                    }
                } else if (whatChanged instanceof Message) {
                    Message msg = (Message) whatChanged;
                    if (msg.hasCalendarItemInfos()) {
                        if (msg.getFolderId() == Mailbox.ID_FOLDER_INBOX || (change.why & Change.FOLDER) != 0) {
                            // If message was moved, we don't know which folder it was moved from.
                            // Just invalidate the Inbox because that's the only message folder we care
                            // about in calendaring.
                            ChangedFolders changedFolders = changeMap.getAccount(entry.getKey().getAccountId());
                            changedFolders.modified.add(Mailbox.ID_FOLDER_INBOX);
                        }
                    }
                }
            }
        }
        if (mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.FOLDER) {
                    // We only have item id.  Let's just assume it's a calendar folder id and check
                    // against the cached list.
                    ChangedFolders changedFolders = changeMap.getAccount(entry.getKey().getAccountId());
                    changedFolders.deleted.add(entry.getKey().getItemId());
                }
                // Let's not worry about hard deletes of invite/reply emails.  It has no practical benefit.
            }
        }

        try {
            for (Map.Entry<String, ChangedFolders> entry : changeMap.entrySet()) {
                ChangedFolders changedFolders = entry.getValue();
                if (changedFolders.isEmpty()) continue;
                String accountId = entry.getKey();
                AccountKey key = new AccountKey(accountId);
                CalList list = mMemcachedLookup.get(key);
                if (list != null) {
                    boolean updated = false;
                    CalList newList = new CalList(list);
                    for (Integer folderId : changedFolders.created) {
                        if (!list.contains(folderId)) {
                            updated = true;
                            newList.add(folderId);
                        }
                    }
                    for (Integer folderId : changedFolders.modified) {
                        if (list.contains(folderId))
                            updated = true;
                    }
                    for (Integer folderId : changedFolders.deleted) {
                        if (list.contains(folderId)) {
                            updated = true;
                            newList.remove(folderId);
                        }
                    }
                    // There was a change.  Increment the version and put back to cache.
                    if (updated) {
                        newList.incrementSeq();
                        mMemcachedLookup.put(key, newList);
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify calendar list cache.  Some cached data may become stale.", e);
        }
    }
}
