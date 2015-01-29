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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public class CtagInfoCacheMailboxListener {
    protected CtagInfoCache cache;

    CtagInfoCacheMailboxListener(CtagInfoCache cache) {
        this.cache = cache;
    }

    public void notifyCommittedChanges(PendingModifications mods, int changeId) {
        int inboxFolder = Mailbox.ID_FOLDER_INBOX;
        List<Pair<String,Integer>> keysToInvalidate = new ArrayList<>();
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Message) {
                    Message msg = (Message) item;
                    if (msg.hasCalendarItemInfos() && msg.getFolderId() == inboxFolder) {
                        keysToInvalidate.add(new Pair<>(msg.getAccountId(), inboxFolder));
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
                        keysToInvalidate.add(new Pair<>(folder.getAccountId(), folder.getId()));
                    }
                } else if (whatChanged instanceof Message) {
                    Message msg = (Message) whatChanged;
                    if (msg.hasCalendarItemInfos()) {
                        if (msg.getFolderId() == inboxFolder || (change.why & Change.FOLDER) != 0) {
                            // If message was moved, we don't know which folder it was moved from.
                            // Just invalidate the Inbox because that's the only message folder we care
                            // about in calendaring.
                            keysToInvalidate.add(new Pair<>(msg.getAccountId(), inboxFolder));
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
                    keysToInvalidate.add(new Pair<>(acctId, entry.getKey().getItemId()));
                }
                // Let's not worry about hard deletes of invite/reply emails.  It has no practical benefit.
            }
        }

        try {
            cache.remove(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify ctag info cache.  Some cached data may become stale.", e);
        }
    }
}
