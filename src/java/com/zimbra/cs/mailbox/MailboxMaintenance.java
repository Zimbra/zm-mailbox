/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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

    String getAccountId() {
        return accountId;
    }

    int getMailboxId() {
        return mailboxId;
    }

    Mailbox getMailbox() {
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
