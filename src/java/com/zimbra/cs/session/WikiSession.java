/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.wiki.Wiki;

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
	
    private WikiSession(String accountId) throws ServiceException {
    	super(accountId, "wiki-session", SessionCache.SESSION_WIKI);
    }
    
	@Override
	protected void cleanup() {
	}

	@Override
	protected long getSessionIdleLifetime() {
		return 0;
	}

	@Override
	public void notifyIM(IMNotification imn) {
	}

	@Override
	public void notifyPendingChanges(PendingModifications pns) {
		
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

	@Override
	protected boolean shouldRegisterWithIM() {
		return false;
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
}
