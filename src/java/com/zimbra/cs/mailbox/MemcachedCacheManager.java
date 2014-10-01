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

package com.zimbra.cs.mailbox;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.util.Zimbra;

public class MemcachedCacheManager extends MailboxListener {

    public static void purgeMailbox(Mailbox mbox) throws ServiceException {
        CalendarCacheManager.getInstance().purgeMailbox(mbox);

        MailboxManager mm = Zimbra.getAppContext().getBean(MailboxManager.class);
        mm.getEffectiveACLCache().remove(mbox);
        mm.getFoldersAndTagsCache().remove(mbox);
    }

    @Override
    public void notify(ChangeNotification notification) {
        PendingModifications mods = notification.mods;
        int changeId = notification.lastChangeId;

        // Invalidate CalendarCache.
        // We have to notify calendar cache before checking memcached connectedness
        // because a portion of calendar cache is not memcached-based.
        CalendarCacheManager.getInstance().notifyCommittedChanges(mods, changeId);

        // Invalidate EffectiveACLCache
        Set<Pair<String,Integer>> keysToInvalidate = new HashSet<>();
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                // We only need to pay attention to modified folders whose modification involves
                // permission change or move to a new parent folder.
                if (whatChanged instanceof Folder && (change.why & (Change.ACL | Change.FOLDER)) != 0) {
                    Folder folder = (Folder) whatChanged;
                    // Invalidate all child folders because their inherited ACL will need to be recomputed.
                    String acctId = folder.getMailbox().getAccountId();
                    List<Folder> subfolders = folder.getSubfolderHierarchy();  // includes "folder" folder
                    for (Folder subf : subfolders) {
                        keysToInvalidate.add(new Pair<>(acctId, subf.getId()));
                    }
                }
            }
        }
        if (mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.FOLDER) {
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null)
                        continue;  // just to be safe
                    keysToInvalidate.add(new Pair<>(acctId, entry.getKey().getItemId()));
                }
            }
        }

        MailboxManager mm = Zimbra.getAppContext().getBean(MailboxManager.class);
        try {
            mm.getEffectiveACLCache().remove(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify folder acl cache.  Some cached data may become stale.", e);
        }
    }

}
