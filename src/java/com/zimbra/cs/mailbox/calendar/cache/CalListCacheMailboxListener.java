/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public class CalListCacheMailboxListener implements MailboxListener {
    protected CalListCache cache;

    public CalListCacheMailboxListener(CalListCache cache) {
        this.cache = cache;
    }

    @Override
    public Set<MailItem.Type> notifyForItemTypes() {
        return MailboxListener.ALL_ITEM_TYPES;
    }

    @Override
    public void notify(ChangeNotification notification) {
        ChangeMap changeMap = new ChangeMap(1);
        if (notification.mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : notification.mods.created.entrySet()) {
                MailItem item = entry.getValue();
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
        if (notification.mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.modified.entrySet()) {
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
        if (notification.mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.deleted.entrySet()) {
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
                CalList list = cache.get(accountId);
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
                        cache.put(accountId, newList);
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify calendar list cache.  Some cached data may become stale.", e);
        }
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
}
