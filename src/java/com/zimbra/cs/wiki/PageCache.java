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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.wiki.WikiTemplate.Context;

/**
 * Cache for the composited Wiki / Notebook pages.  Each pages are fully
 * rendered with all the Template components.  Each pages are keyed off by
 * the list of dependent pages.  If the requestor does not have full
 * privilege to all the included pages to compose the fully rendered page,
 * then a subset of the pages which the requestor has access to will be
 * used to compose the partial page.  The partial page is then cached and
 * used for subsequent request by the same requestor, or another requestor
 * with the same set of privileges.
 * 
 * The size of the page cache is set by config variable 
 * <code>zimbraNotebookPageCacheSize</code>, either globally or by each server.
 * 
 * @author jylee
 *
 */
public class PageCache {
	private static final int DEFAULT_CACHE_SIZE = 10240;
	private LRUMap mCache;
	private static final String SEP = ":";
	
	public PageCache() {
		Provisioning prov = Provisioning.getInstance();
		int cacheSize;
		try {
			Server localServer = prov.getLocalServer();
			cacheSize = localServer.getIntAttr(Provisioning.A_zimbraNotebookPageCacheSize, DEFAULT_CACHE_SIZE);
		} catch (ServiceException se) {
			cacheSize = DEFAULT_CACHE_SIZE;
		}
		mCache = new LRUMap(cacheSize);
	}
	public PageCache(int cacheSize) {
		mCache = new LRUMap(cacheSize);
	}
	
	public synchronized void addPage(Context ctxt, WikiTemplate template, String page) {
		String key = generateKey(ctxt, template);
		mCache.put(key, page);
	}
	
	public synchronized String getPage(Context ctxt, WikiTemplate template) {
		String key = generateKey(ctxt, template);
		return (String)mCache.get(key);
	}
	
	private String generateKey(Context ctxt, WikiTemplate template) {
		List<WikiTemplate> inclusions = new ArrayList<WikiTemplate>();
		template.getInclusions(ctxt, inclusions);
		Collections.sort(inclusions);
		StringBuilder name = new StringBuilder();
		name.append(template.getId()).append(SEP);
		for (WikiTemplate t : inclusions) {
			name.append(t.getId()).append(SEP);
		}
		return name.toString();
	}
}
