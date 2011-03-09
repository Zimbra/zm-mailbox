/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.filter.FilterListener;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.alerts.CalItemReminderService;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.ZimbraApplication;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public abstract class MailboxListener {
    
    public static class ChangeNotification {
        public Account mailboxAccount;
        public OperationContext ctxt;
        public int lastChangeId;
        public PendingModifications mods;
        public long timestamp;
        
        public ChangeNotification(Account account, PendingModifications mods, OperationContext ctxt, int lastChangeId, long timestamp) {
            this.mailboxAccount = account;
            this.mods = mods;
            this.ctxt = ctxt;
            this.lastChangeId = lastChangeId;
            this.timestamp = timestamp;
        }
    }

    public abstract void notify(ChangeNotification notification);
    public abstract Set<MailItem.Type> registerForItemTypes();

    private static final HashSet<MailboxListener> sListeners;

    static {
        sListeners = new HashSet<MailboxListener>();
        if (ZimbraApplication.getInstance().supports(CalItemReminderService.class) && !DebugConfig.disableCalendarReminderEmail) {
            register(new CalItemReminderService());
        }
        register(new FilterListener());
        register(new MemcachedCacheManager());
        register(new FreeBusyProvider.Listener());
    }


    public static void register(MailboxListener listener) {
        synchronized (sListeners) {
            sListeners.add(listener);
        }
    }

    public static void notifyListeners(ChangeNotification notification) {
        for (MailboxListener l : sListeners) {
            if (!Collections.disjoint(notification.mods.changedTypes, l.registerForItemTypes())) {
                l.notify(notification);
            }
        }
    }
}
