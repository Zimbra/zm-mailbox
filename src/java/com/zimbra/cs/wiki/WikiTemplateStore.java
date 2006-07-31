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

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Pair;
import com.zimbra.cs.wiki.Wiki.WikiContext;
//import com.zimbra.cs.util.ZimbraLog;

/**
 * Cache store for parsed WikiTemplates.  The size of the cache is set by
 * attributes <code>zimbraNotebookFolderCacheSize</code> and
 * <code>zimbraNotebookMaxCachedTemplatesPerFolder</code>.  Each cached
 * templates are invalidated when an update is detected via <code>Wiki</code>
 * and <code>WikiSession</code>.  WikiTemplate cache is organized by
 * the folder / notebook, the same way Wiki is organized.
 * 
 * @author jylee
 * 
 */
public class WikiTemplateStore {
	
	private static final long TTL = 10  * 60 * 1000;  // 10 minutes
	private static final int DEFAULT_CACHE_SIZE = 1024;
	private static LRUMap sTemplateStoreCache;
	
	static {
		Provisioning prov = Provisioning.getInstance();
		int cacheSize;
		try {
			Server localServer = prov.getLocalServer();
			cacheSize = localServer.getIntAttr(Provisioning.A_zimbraNotebookFolderCacheSize, DEFAULT_CACHE_SIZE);
		} catch (ServiceException se) {
			cacheSize = DEFAULT_CACHE_SIZE;
		}
		sTemplateStoreCache = new LRUMap(cacheSize);
	}
	
	public static WikiTemplate getDefaultTOC() {
		return new WikiTemplate("{{TOC}}");
	}
	
	public static WikiTemplateStore getInstance(MailItem item) throws ServiceException {
		int id = (item instanceof Folder) ? item.getId() : item.getFolderId();
		return WikiTemplateStore.getInstance(item.getMailbox().getAccount().getId(), 
				Integer.toString(id));
	}
	
	public static WikiTemplateStore getInstance(String account, String k) {
		Pair<String,String> key = new Pair<String,String>(account, k);
		WikiTemplateStore store;
		long now = System.currentTimeMillis();
		
		synchronized (sTemplateStoreCache) {
			store = (WikiTemplateStore)sTemplateStoreCache.get(key);
			if (store != null && store.isExpired(now)) {
				sTemplateStoreCache.remove(key);
				store = null;
			}
			if (store == null) {
				store = new WikiTemplateStore(key);
				sTemplateStoreCache.put(key, store);
			}
		}
		return store;
	}

	
	private long   mExpiration;
	private String mAccountId;
	private String mKey;
	private LRUMap mTemplateMap;
	private static final int TEMPLATE_CACHE_SIZE = 256;
	
	private WikiTemplateStore(Pair<String,String> key) {
		mAccountId  = key.getFirst();
		mKey = key.getSecond();
		
		Provisioning prov = Provisioning.getInstance();
		int cacheSize;
		try {
			Server localServer = prov.getLocalServer();
			cacheSize = localServer.getIntAttr(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, TEMPLATE_CACHE_SIZE);
		} catch (ServiceException se) {
			cacheSize = TEMPLATE_CACHE_SIZE;
		}
		
		mTemplateMap = new LRUMap(cacheSize);
		mExpiration = System.currentTimeMillis() + TTL;
	}

	private boolean isExpired(long now) {
		return mExpiration < now;
	}
	
	public WikiTemplate getTemplate(WikiContext ctxt, String name) throws ServiceException, IOException {
		WikiTemplate template = (WikiTemplate)mTemplateMap.get(name);
		if (template != null) {
			//ZimbraLog.wiki.debug("found " + name + " from template cache");
			return template;
		}
		
		Wiki wiki = Wiki.getInstance(ctxt, mAccountId, mKey);
		WikiPage ww = wiki.lookupWiki(name);
		
		if (ww != null) {
			template = new WikiTemplate(ww.getContents(ctxt), mAccountId, mKey, name);
			mTemplateMap.put(name, template);
			return template;
		}
		
		boolean checkParents = name.startsWith("_");
		try {
			int fid = 0;
			try {
				fid = Integer.parseInt(wiki.getKey());
			} catch (Exception e) {
			}
			wiki = Wiki.findWikiByPath(ctxt, mAccountId, fid, name, checkParents);
			WikiTemplateStore store = getInstance(wiki.getWikiAccount(), wiki.getKey());
			return store.getTemplate(ctxt, name);
		} catch (ServiceException se) {
			return new WikiTemplate("<!-- missing template "+name+" -->");
		}
	}
	
	public void expireTemplate(String name) {
		//ZimbraLog.wiki.debug("removing " + name + " from template cache");
		mTemplateMap.remove(name);
		Wiki.remove(mAccountId, mKey);
	}
}
