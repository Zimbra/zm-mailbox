/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

    public synchronized void registerAllowedThread(Thread t) {
        allowedThreads.add(t);
    }

    synchronized boolean canAccess() {
        Thread curr = Thread.currentThread();
        for (Thread t : allowedThreads) {
            if (curr == t)
                return true;
        }
        return false;
    }

    synchronized void markUnavailable()  {
        mailbox = null;
        allowedThreads.clear();
    }
}
