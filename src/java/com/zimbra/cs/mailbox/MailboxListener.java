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

import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.mailbox.alerts.CalItemReminderService;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.ZimbraApplication;

import java.util.HashSet;


public abstract class MailboxListener {

	
	public abstract void handleMailboxChange(String accountId, PendingModifications mods, OperationContext octxt, int lastChangeId);
	public abstract int registerForItemTypes();
	
	
	private static final HashSet<MailboxListener> sListeners;
	
	static {
		sListeners = new HashSet<MailboxListener>();
        if (ZimbraApplication.getInstance().supports(CalItemReminderService.class)) {
            register(new CalItemReminderService());
        }
    }
	
	
	public static void register(MailboxListener listener) {
		synchronized (sListeners) {
			sListeners.add(listener);
		}
	}
	
    public static void mailboxChanged(String accountId, PendingModifications mods, OperationContext octxt, int lastChangeId) {
        // if the calendar items has changed in the mailbox,
        // recalculate the free/busy for the user and propogate to
        // other system.
        FreeBusyProvider.mailboxChanged(accountId, mods.changedTypes);

        MemcachedCacheManager.notifyCommittedChanges(mods, lastChangeId);

        for (MailboxListener l : sListeners) {
            if ((mods.changedTypes & l.registerForItemTypes()) > 0) {
                l.handleMailboxChange(accountId, mods, octxt, lastChangeId);
            }
        }
    }
}
