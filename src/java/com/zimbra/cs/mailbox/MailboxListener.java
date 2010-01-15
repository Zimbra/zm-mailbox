/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashSet;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;


public abstract class MailboxListener {

	
	public abstract void handleMailboxChange(String accountId, int changedTypes, OperationContext octxt);
	public abstract int registerForItemTypes();
	
	
	private static HashSet<MailboxListener> sListeners;
	
	static {
		sListeners = new HashSet<MailboxListener>();
	}
	
	
	public static void register(MailboxListener listener) {

		
		
		synchronized (sListeners) {
			sListeners.add(listener);
		}
	}
	
	public static void mailboxChanged(String accountId, int changedTypes, OperationContext octxt) {
	
		for (MailboxListener l : sListeners) {
			if ((changedTypes & l.registerForItemTypes()) > 0) {
				l.handleMailboxChange(accountId, changedTypes, octxt);
			}
		}
	}



	
	
}
