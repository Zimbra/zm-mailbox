/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Mailbox maintenance context.
 */
public final class MailboxMaintenance {

    private final String accountId;
    private final int mailboxId;
    private Mailbox mailbox;
    private List<Thread> allowedThreads;
    private boolean nestedAllowed = false;
    private boolean inner = false;

    MailboxMaintenance(String acct, int id) {
        this(acct, id, null);
    }

    MailboxMaintenance(String acct, int id, Mailbox mbox) {
        accountId = acct.toLowerCase();
        mailboxId = id;
        mailbox = mbox;
        allowedThreads = new ArrayList<Thread>();
        allowedThreads.add(Thread.currentThread());
    }

    public String getAccountId() {
        return accountId;
    }

    int getMailboxId() {
        return mailboxId;
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    void setMailbox(Mailbox mbox) {
        if (mbox.getId() == mailboxId && mbox.getAccountId().equalsIgnoreCase(accountId)) {
            mailbox = mbox;
        }
    }

    synchronized void registerOuterAllowedThread(Thread t) throws MailServiceException {
        if (inner) {
            throw MailServiceException.MAINTENANCE(mailboxId, "cannot add new maintenance thread when inner maintenance is already started");
        } else if (!nestedAllowed) {
            throw MailServiceException.MAINTENANCE(mailboxId, "cannot add outer maintenance thread when nested is not enabled");
        } else if (allowedThreads.size() > 0) {
            throw MailServiceException.MAINTENANCE(mailboxId, "cannot add multiple outer maintenance threads");
        }
        registerAllowedThread(t);
    }

    public synchronized void registerAllowedThread(Thread t) {
        allowedThreads.add(t);
    }

    synchronized void removeAllowedThread(Thread t) {
        allowedThreads.remove(t);
    }

    synchronized void setNestedAllowed(boolean allowed) {
        nestedAllowed = allowed;
    }

    synchronized void startInnerMaintenance() throws MailServiceException {
        if (inner) {
            throw MailServiceException.MAINTENANCE(mailboxId, "attempted to nest maintenance when already nested");
        } else if (!nestedAllowed || !canAccess()) {
            throw MailServiceException.MAINTENANCE(mailboxId, "attempted to nest maintenance when not allowed");
        }
        inner = true;
    }

    synchronized boolean endInnerMaintenance() {
        boolean set = inner;
        assert(nestedAllowed || !set);
        inner = false;
        return set;
    }

    synchronized boolean isNestedAllowed() {
        return nestedAllowed;
    }


    synchronized boolean canAccess() {
        return allowedThreads.contains(Thread.currentThread());
    }

    synchronized void markUnavailable()  {
        mailbox = null;
        inner = false;
        nestedAllowed = false;
        allowedThreads.clear();
    }

    void cacheMailbox(Mailbox mbox) {
        if (mbox.getId() == mailboxId && mbox.getAccountId().equalsIgnoreCase(accountId))
            mailbox = mbox;
    }
}
