package com.zimbra.cs.mailbox;

import java.util.HashSet;



public abstract class MailboxListener {

	
	public abstract void handleMailboxChange(String accountId, int changedTypes);
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
	
	public static void mailboxChanged(String accountId, int changedTypes) {
	
		for (MailboxListener l : sListeners) {
			if ((changedTypes & l.registerForItemTypes()) > 0) {
				l.handleMailboxChange(accountId, changedTypes);
			}
		}
	}



	
	
}
