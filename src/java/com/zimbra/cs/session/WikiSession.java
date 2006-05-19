/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiTemplateStore;

public class WikiSession extends Session {

	public static WikiSession getInstance() throws ServiceException {
		if (sSession == null) {
			sSession = new WikiSession(Wiki.getDefaultWikiAccount().getId());
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
		if (obj != null && obj instanceof WikiItem) {
			try {
				WikiItem item = (WikiItem) obj;
				WikiTemplateStore wiki = WikiTemplateStore.getInstance(item);
				wiki.expireTemplate(item.getWikiWord());
			} catch (ServiceException se) {
				ZimbraLog.wiki.error("can't expire WikiItem", se);
			}
		}
	}
}
