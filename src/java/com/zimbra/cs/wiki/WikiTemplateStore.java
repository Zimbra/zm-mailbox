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

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException.NoSuchWikiException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Pair;
import com.zimbra.cs.wiki.Wiki.WikiContext;
//import com.zimbra.cs.util.ZimbraLog;

public class WikiTemplateStore {
	
	private static Map<Pair<String,String>,WikiTemplateStore> sTemplates;
	
	static {
		sTemplates = new HashMap<Pair<String,String>,WikiTemplateStore>();
	}
	
	public static WikiTemplate getDefaultTOC() {
		return new WikiTemplate("{{TOC}}");
	}
	
	public static WikiTemplateStore getInstance(MailItem item) throws ServiceException {
		return WikiTemplateStore.getInstance(item.getMailbox().getAccount().getId(), Integer.toString(item.getFolderId()));
	}
	
	public static WikiTemplateStore getCentralAccountInstance(WikiContext ctxt, String accountId) throws ServiceException {
		Wiki w = Wiki.getTemplateStore(ctxt, accountId);
		if (w == null)
			return null;
		return WikiTemplateStore.getInstance(w.getWikiAccount(), w.getKey());
	}
	
	public static WikiTemplateStore getInstance(String account, String k) {
		Pair<String,String> key = Pair.get(account, k);
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

	private static long TTL = 600000;  // 10 minutes
	
	private long   mExpiration;
	private String mAccountId;
	private String mKey;
	private Map<String,WikiTemplate> mTemplateMap;
	
	private WikiTemplateStore(Pair<String,String> key) {
		mAccountId  = key.getFirst();
		mKey = key.getSecond();
		mTemplateMap = new HashMap<String,WikiTemplate>();
		mExpiration = System.currentTimeMillis() + TTL;
	}
	
	public WikiTemplate getTemplate(WikiContext ctxt, String name) throws ServiceException, IOException {
		boolean checkParents = name.startsWith("_");
		try {
			return getTemplate(ctxt, name, checkParents);
		} catch (ServiceException e) {
			if (!checkParents)
				throw e;
			WikiTemplateStore defaultStore = getCentralAccountInstance(ctxt, mAccountId);
			if (defaultStore == null)
				return new WikiTemplate("<!-- missing template "+name+" -->");
			return defaultStore.getTemplate(ctxt, name);
		}
	}
	private WikiTemplate getTemplate(WikiContext ctxt, String name, boolean checkParents) throws ServiceException, IOException {
		if (name.indexOf('/') != -1) {
			return getTemplateByPath(ctxt, name);
		}
		WikiTemplate template = mTemplateMap.get(name);
		if (template != null) {
			//ZimbraLog.wiki.debug("found " + name + " from template cache");
			return template;
		}
		
		Wiki wiki = Wiki.getInstance(ctxt, mAccountId, mKey);
		WikiPage ww = wiki.lookupWiki(name);
		
		if (ww != null) {
			template = new WikiTemplate(ww.getContents(ctxt));
			mTemplateMap.put(name, template);
			return template;
		}
		
		if (!checkParents)
			return new WikiTemplate("<!-- missing template "+name+" -->");

		String parentKey = wiki.getParentKey();
		if (parentKey == null) {
			throw new NoSuchWikiException(name);
		}
		WikiTemplateStore parentStore = WikiTemplateStore.getInstance(mAccountId, parentKey);
		return parentStore.getTemplate(ctxt, name);
	}
	private WikiTemplate getTemplateByPath(WikiContext ctxt, String path) throws ServiceException {
		MailItem item = Wiki.findWikiByPath(ctxt, mAccountId, 0, path);
		if (item instanceof Folder) {
			return getDefaultTOC();
		} else if (item instanceof WikiItem) {
			WikiItem wiki = (WikiItem) item;
			try {
				byte[] raw = ByteUtil.getContent(wiki.getLastRevision().getContent(), 0);
				// XXX cache the template
				return new WikiTemplate(new String(raw, "UTF-8"));
			} catch (Exception e) {
				throw WikiServiceException.ERROR("can't get contents of "+path, e);
			}
		}
		throw WikiServiceException.NOT_WIKI_ITEM(path);
	}
	public void expireTemplate(String name) {
		//ZimbraLog.wiki.debug("removing " + name + " from template cache");
		mTemplateMap.remove(name);
		Wiki.remove(mAccountId, mKey);
	}
}
