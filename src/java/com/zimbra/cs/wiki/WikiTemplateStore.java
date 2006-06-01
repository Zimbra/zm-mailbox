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
package com.zimbra.cs.wiki;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException.NoSuchWikiException;
import com.zimbra.cs.util.Pair;
import com.zimbra.cs.util.ZimbraLog;

public class WikiTemplateStore {
	
	private static Map<Pair<String,String>,WikiTemplateStore> sTemplates;
	
	static {
		sTemplates = new HashMap<Pair<String,String>,WikiTemplateStore>();
	}
	
	public static WikiTemplate getDefaultTOC() {
		return new WikiTemplate("{{TOC}}");
	}
	
	public static WikiTemplateStore getInstance(MailItem item) throws ServiceException {
		return WikiTemplateStore.getInstance(item.getMailbox().getAccount().getId(), item.getFolderId());
	}
	
	public static WikiTemplateStore getInstance() throws ServiceException {
		Wiki w = Wiki.getTemplateStore();
		return WikiTemplateStore.getInstance(w.getWikiAccount(), w.getWikiFolderId());
	}
	
	public static WikiTemplateStore getInstance(String account, int folderId) {
		Pair<String,String> key = Pair.get(account, Integer.toString(folderId));
		WikiTemplateStore store;
		long now = System.currentTimeMillis();
		
		synchronized (WikiTemplateStore.class) {
			store = sTemplates.get(key);
			if (store != null && store.mExpiration < now) {
				sTemplates.remove(key);
				store = null;
			}
			if (store == null) {
				store = new WikiTemplateStore(key);
				sTemplates.put(key, store);
			}
		}
		return store;
	}

	private static long TTL = 3600000;  // 1 hour
	
	private long   mExpiration;
	private String mAccountId;
	private int    mFolderId;
	private int    mParentFolderId;
	private Map<String,WikiTemplate> mTemplateMap;
	
	private WikiTemplateStore(Pair<String,String> key) {
		mAccountId  = key.getFirst();
		mFolderId = Integer.parseInt(key.getSecond());
		mTemplateMap = new HashMap<String,WikiTemplate>();
		mExpiration = System.currentTimeMillis() + TTL;
	}
	
	public WikiTemplate getTemplate(OperationContext octxt, String name) throws ServiceException, IOException {
		boolean checkParents = name.startsWith("_");
		try {
			return getTemplate(octxt, name, checkParents);
		} catch (ServiceException e) {
			if (!checkParents)
				throw e;
			WikiTemplateStore defaultStore = getInstance();
			if (defaultStore.mAccountId.equals(mAccountId))
				return new WikiTemplate("<!-- missing template "+name+" -->");
			return defaultStore.getTemplate(octxt, name, checkParents);
		}
	}
	public WikiTemplate getTemplate(OperationContext octxt, String name, boolean checkParents) throws ServiceException, IOException {
		if (name.indexOf('/') != -1) {
			return getTemplateByPath(octxt, name);
		}
		WikiTemplate template = mTemplateMap.get(name);
		if (template != null) {
			ZimbraLog.wiki.debug("found " + name + " from template cache");
			return template;
		}
		
		Wiki wiki = Wiki.getInstance(mAccountId, mFolderId);
		WikiWord ww = wiki.lookupWiki(name);
		
		if (ww != null) {
			Document item = ww.getWikiItem(octxt);
			if (!(item instanceof WikiItem))
				throw WikiServiceException.NOT_WIKI_ITEM(item.getFilename());
			template = new WikiTemplate((WikiItem)item);
			mTemplateMap.put(name, template);
			return template;
		}
		
		if (!checkParents)
			return new WikiTemplate("<!-- missing template "+name+" -->");
		
		if (mParentFolderId == 0) {
			Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccount());
			Folder f = mbox.getFolderById(octxt, mFolderId);
			mParentFolderId = f.getFolderId();  // mountpoint should return its logical parent.
		}
		
		if (mParentFolderId == mFolderId) {
			throw new NoSuchWikiException(name);
		}
		WikiTemplateStore parentStore = WikiTemplateStore.getInstance(mAccountId, mParentFolderId);
		return parentStore.getTemplate(octxt, name);
	}
	public WikiTemplate getTemplateByPath(OperationContext octxt, String path) throws ServiceException {
		MailItem item = Wiki.findWikiByPath(octxt, mAccountId, mFolderId, path);
		if (item instanceof Folder) {
			return getDefaultTOC();
		} else if (item instanceof WikiItem) {
			WikiItem wiki = (WikiItem) item;
			// XXX cache the template
			return new WikiTemplate(wiki);
		}
		throw WikiServiceException.NOT_WIKI_ITEM(path);
	}
	public void expireTemplate(String name) {
		ZimbraLog.wiki.debug("removing " + name + " from template cache");
		mTemplateMap.remove(name);
		Wiki.remove(mAccountId, mFolderId);
	}
}
