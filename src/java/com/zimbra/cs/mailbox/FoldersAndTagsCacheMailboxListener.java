/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;


// A MailboxListener capable of keeping a FoldersAndTags cache up to date.
public class FoldersAndTagsCacheMailboxListener implements MailboxListener {
    static final Set<Type> ITEM_TYPES = EnumSet.of(Type.FOLDER, Type.TAG);
    protected @Autowired MailboxManager mailboxManager;
    protected FoldersAndTagsCache cache;

    public FoldersAndTagsCacheMailboxListener(FoldersAndTagsCache cache) {
        this.cache = cache;
    }

    @Override
    public Set<MailItem.Type> notifyForItemTypes() {
        return ITEM_TYPES;
    }

    @Override
    public void notify(ChangeNotification notification) {
        Set<String> accountsToInvalidate = new HashSet<>();

        if (notification.mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : notification.mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Folder || item instanceof Tag) {
                    accountsToInvalidate.add(item.getAccountId());
                }
            }
        }
        if (notification.mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.modified.entrySet()) {
                Change change = entry.getValue();
                if (change.what instanceof Folder || change.what instanceof Tag) {
                    accountsToInvalidate.add(entry.getKey().getAccountId());
                }
            }
        }
        if (notification.mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : notification.mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.FOLDER || type == MailItem.Type.TAG) {
                    accountsToInvalidate.add(entry.getKey().getAccountId());
                }
            }
        }

        try {
            for (String accountId: accountsToInvalidate) {
                Mailbox mbox = mailboxManager.getMailboxByAccountId(accountId);
                cache.remove(mbox);
            }
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to invalidate FoldersAndTags cache. Some cached data has now become stale", e);
        }
    }
}
