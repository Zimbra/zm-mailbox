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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Pair;

public class WikiTemplateStore {
	
	private static Map<Pair,WikiTemplateStore> sTemplates;
	
	static {
		sTemplates = new HashMap<Pair,WikiTemplateStore>();
	}
	
	public static String getDefaultTOC() {
		return "{{TOC}}";
	}
	
	public static WikiTemplateStore getInstance(String account, int folderId) {
		Pair<String,String> key = Pair.get(account, Integer.toString(folderId));
		WikiTemplateStore store = sTemplates.get(key);
		long now = System.currentTimeMillis();
		
		if (store != null && store.mExpiration < now) {
			sTemplates.remove(key);
			store = null;
		}
		if (store == null) {
			store = new WikiTemplateStore(key);
			sTemplates.put(key, store);
		}
		
		return store;
	}
	
	private static long TTL = 3600000;  // 1 hour
	
	private long   mExpiration;
	private String mAccount;
	private int    mFolderId;
	private Map<String,String> mTemplateMap;
	
	private WikiTemplateStore(Pair<String,String> key) {
		mAccount  = key.getFirst();
		mFolderId = Integer.parseInt(key.getSecond());
		mTemplateMap = new HashMap<String,String>();
		mExpiration = System.currentTimeMillis() + TTL;
	}
	
	public String getTemplate(OperationContext octxt, String name) throws ServiceException, IOException {
		String template = mTemplateMap.get(name);
		if (template != null)
			return template;
		
		Wiki wiki = Wiki.getInstance(mAccount, mFolderId);
		WikiWord ww = wiki.lookupWiki(name);
		
		if (ww != null) {
			Document item = ww.getWikiItem(octxt);
			template = new String(ByteUtil.getContent(item.getRawDocument(), 0), "UTF-8");
			mTemplateMap.put(name, template);
			return template;
		}
		
		Mailbox mbox = Mailbox.getMailboxByAccountId(wiki.getWikiAccount());
		Folder f = mbox.getFolderById(octxt, mFolderId);
		int parentId = f.getFolderId();  // mountpoint should return its logical parent.
		
		WikiTemplateStore parentStore = WikiTemplateStore.getInstance(mAccount, parentId);
		return parentStore.getTemplate(octxt, name);
	}
}
