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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;

public class Wiki {
	private String mWikiAccount;
	private String mWikiAccountId;
	private int    mFolderId;
	
	private Map<String,WikiWord>    mWikiWords;
	
	private static Map<String,Wiki> wikiMap;
	
	private static final String WIKI_FOLDER =  "inbox";
	
	static {
		wikiMap = new HashMap<String,Wiki>();
	}
	
	public static Wiki getInstance() throws ServiceException {
		if (!LC.wiki_enabled.booleanValue()) {
			throw ServiceException.FAILURE("wiki disabled", null);
		}
		return getInstance(LC.wiki_user.value());
	}
	
	public static Wiki getInstance(String acct) throws ServiceException {
		Wiki w;
		synchronized (wikiMap) {
			w = wikiMap.get(acct);
			if (w == null) {
				w = new Wiki(acct);
				wikiMap.put(acct, w);
			}
		}
		return w;
	}
	
	private Wiki(String wikiAcct) throws ServiceException {
		mWikiWords = new HashMap<String,WikiWord>();
		
		mWikiAccount = wikiAcct;
		
		Account acct = Provisioning.getInstance().getAccountByName(mWikiAccount);
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		OperationContext octxt = new OperationContext(acct);
		Folder f = mbox.getFolderByPath(octxt, WIKI_FOLDER);
		mWikiAccountId = acct.getId();
		mFolderId = f.getId();
		loadWiki(octxt, mbox);
	}
	
	private void loadWiki(OperationContext octxt, Mailbox mbox) throws ServiceException {
	    List<MailItem> wikiList = mbox.getItemList(octxt, MailItem.TYPE_WIKI);
	    // this is List, so it'd better be in the natural chronological order of MailItem in the folder.

	    for (MailItem item : wikiList) {
	    	assert(item instanceof WikiItem);
	    	WikiItem witem = (WikiItem) item;
	    	addWiki(witem);
	    }
	}
	
	public String getWikiAccount() {
		return mWikiAccount;
	}
	
	public String getWikiAccountId() {
		return mWikiAccountId;
	}
	
	public String getWikiFolder() {
		return WIKI_FOLDER;
	}
	
	public int getWikiFolderId() {
		return mFolderId;
	}
	
	public Set<String> listWiki() {
		return mWikiWords.keySet();
	}
	public WikiWord lookupWiki(String wikiWord) {
		return mWikiWords.get(wikiWord);
	}
	
	public void addWiki(WikiItem wikiItem) throws ServiceException {
		String wikiStr = wikiItem.getWikiWord();
		WikiWord w;
		w = mWikiWords.get(wikiStr);
		if (w == null) {
			w = new WikiWord(wikiStr);
			mWikiWords.put(wikiStr, w);
		}
		w.addWikiItem(wikiItem);
	}
	
	public void deleteWiki(OperationContext octxt, String wikiWord) throws ServiceException {
		WikiWord w = mWikiWords.remove(wikiWord);
		if (w != null) {
			w.deleteAllRevisions(octxt);
		}
	}
}
