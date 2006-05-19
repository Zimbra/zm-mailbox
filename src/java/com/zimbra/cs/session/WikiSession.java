package com.zimbra.cs.session;

import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.wiki.WikiTemplateStore;

public class WikiSession extends Session {

    public WikiSession(String accountId, String sessionId, int type) throws ServiceException {
    	super(accountId, sessionId, type);
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
