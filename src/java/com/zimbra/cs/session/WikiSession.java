/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
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
package com.zimbra.cs.session;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.wiki.Wiki;

/*
 * WikiSession acts as a conduit between the Mailboxes and the wiki caches,
 * so the dirty cache data can be invalidated when there is a Mailbox change.
 * It doesn't require registration and cleanup as login and activity based
 * conventional session objects.  When a Mailbox it listens to gets unloaded,
 * it will re-register as the listener the next time the Mailbox is accessed 
 * by wiki.
 */
public class WikiSession extends Session {

	public static WikiSession getInstance() throws ServiceException {
		synchronized (WikiSession.class) {
			if (sSession == null) {
				sSession = new WikiSession(Wiki.getDefaultWikiAccount().getId());
			}
		}
		return sSession;
	}
	private static WikiSession sSession;
	
    private WikiSession(String accountId) {
    	super(accountId, Session.Type.WIKI);
    }

    @Override
    protected boolean isMailboxListener() {
        return false;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return false;
    }

    @Override
    public Session register() {
        return this;
    }
    
	@Override
	protected void cleanup() {
	}

	@Override
	protected long getSessionIdleLifetime() {
		return 0;
	}

	@Override
	public void notifyPendingChanges(PendingModifications pns, int changeId, Session source) {
		
		if (pns.modified != null && pns.modified.size() > 0) {
			for (PendingModifications.Change value : pns.modified.values()) {
				expireItem(value.what);
			}
		}
		
		if (pns.deleted != null && pns.deleted.size() > 0) {
			for (Object value : pns.deleted.values()) {
				expireItem(value);
			}
		}
	}

	private void expireItem(Object obj) {
		if (obj == null)
			return;
		if (obj instanceof Document) {
			Wiki.expireTemplate((Document) obj);
		}
		if (obj instanceof Folder) {
			Wiki.expireNotebook((Folder) obj);
		}
		if (obj instanceof Mailbox) {
			Wiki.expireAll();
		}
	}
    
    public void addMailboxForNotification(Mailbox mbox) throws ServiceException {
        if (mbox != null)
            mbox.addListener(this);
    }
}
