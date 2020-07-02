/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.mailbox.event;

import java.util.Collection;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.LruMap;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.event.MailboxEvent.EventFilter;

public class EventLogger {

    private static EventLogger instance;
    public synchronized static EventLogger getInstance() {
        if (instance == null) {
            instance = new EventLogger();
        }
        return instance;
    }

    public ItemEventLog getLog(MailItem item) throws ServiceException {
        return new DbEventLog(item);
    }

    public ItemEventLog getLog(String sessionId) {
        synchronized (logs) {
            return logs.get(sessionId);
        }
    }

    public ItemEventLog getLog(String accountId, int mailboxId, Collection<Integer> itemIds, EventFilter filter) throws ServiceException {
        DbEventLog log = null;
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
        log = new DbEventLog(mbox, itemIds, filter);
        synchronized (logs) {
            logs.put(log.getId(), log);
        }
        return log;
    }

    public ItemEventLog getLog(String accountId, Mountpoint mp, EventFilter filter) {
        String owner = mp.getOwnerId();
        int folderId = mp.getRemoteId();
        Provisioning prov = Provisioning.getInstance();
        try {
            Account authAccount = prov.getAccountById(accountId);
            Account ownerAccount = prov.getAccountById(owner);
            return getRemoteItemLog(authAccount, ownerAccount, folderId);
        } catch (ServiceException se) {
            // skip this log if it is unavailable (stale mountpoint, etc)
            return null;
        }
    }

    public ItemEventLog getLog(String accountId, String ownerAccountId, Collection<Integer> itemIds, EventFilter filter) throws ServiceException {
        MergedEventLog log = new MergedEventLog();
        Provisioning prov = Provisioning.getInstance();
        Account ownerAccount = prov.getAccountById(ownerAccountId);
        if (ownerAccount == null) {
            return log;
        }
        if (Provisioning.onLocalServer(ownerAccount)) {
            Mailbox ownerMbox = MailboxManager.getInstance().getMailboxByAccount(ownerAccount);
            return getLog(accountId, ownerMbox.getId(), itemIds, filter);
        }
        Account authAccount = prov.getAccountById(accountId);
        if (itemIds.size() == 1) {
            return getRemoteItemLog(authAccount, ownerAccount, itemIds.iterator().next());
        }
        for (int itemId : itemIds) {
            log.merge(getRemoteItemLog(authAccount, ownerAccount, itemId));
        }
        return log;
    }

    private ItemEventLog getRemoteItemLog(Account authAccount, Account targetAccount, int itemId) throws ServiceException {
        return new RemoteEventLog(authAccount, targetAccount, itemId);
    }

    private final LruMap<String,DbEventLog> logs;

    public EventLogger() {
        logs = new LruMap<String,DbEventLog>(1024);
    }
}
