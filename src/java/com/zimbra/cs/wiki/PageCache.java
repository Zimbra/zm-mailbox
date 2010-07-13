/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.wiki;

import java.util.Map;

import com.zimbra.common.util.MapUtil;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;

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
	private static final int TTL = 10 * 60 * 1000;  // 10 min
	private Map mCache;
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
		mCache = MapUtil.newLruMap(cacheSize);
	}
	public PageCache(int cacheSize) {
		mCache = MapUtil.newLruMap(cacheSize);
	}
	
	public synchronized void addPage(String key, String page) {
		Pair<Long,String> v = new Pair<Long,String>(getExpirationTime(), page);
		mCache.put(key, v);
	}
	
	private long getExpirationTime() {
		return System.currentTimeMillis() + TTL;
	}
	
	public synchronized String getPage(String key) {
		Pair v = (Pair)mCache.get(key);
		if (v != null && (Long)v.getFirst() < System.currentTimeMillis()) {
			mCache.remove(key);
			v = null;
		}
		if (v == null)
			return null;
		return (String)v.getSecond();
	}
	
	public synchronized void removePage(String key) {
		Pair v = (Pair)mCache.get(key);
		if (v != null)
			mCache.remove(key);
	}
	
	public String generateKey(Account user, MailItem item) {
		String auth;
		if (user == null)
			auth = "guest";
		else
			auth = user.getId();
		StringBuilder buf = new StringBuilder();
		buf.append(auth).append(SEP).append(item.getMailboxId()).append(SEP).append(item.getId());
		return buf.toString();
	}
}
