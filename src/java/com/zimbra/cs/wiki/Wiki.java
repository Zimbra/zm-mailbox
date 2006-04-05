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
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Pair;

/**
 * This class represents a Wiki notebook.
 * 
 * @author jylee
 *
 */
public class Wiki {
	private String mWikiAccount;
	private int    mFolderId;
	
	private Map<String,WikiWord>    mWikiWords;
	
	private static Map<Pair<String,String>,Wiki> wikiMap;
	
	//private static final String WIKI_FOLDER =  "wiki";
	private static final String WIKI_FOLDER =  "inbox";
	
	static {
		wikiMap = new HashMap<Pair<String,String>,Wiki>();
	}
	
	public static int getDefaultFolderId(Account acct) throws ServiceException {
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		OperationContext octxt = new OperationContext(acct);
		Folder f;
		try {
			f = mbox.getFolderByPath(octxt, WIKI_FOLDER);
		} catch (ServiceException se) {
			f = mbox.createFolder(octxt, WIKI_FOLDER, Mailbox.ID_FOLDER_USER_ROOT, MailItem.TYPE_WIKI, null);
		}
		return f.getId();
	}
	
	public static Wiki getInstance() throws ServiceException {
		if (!LC.wiki_enabled.booleanValue()) {
			throw ServiceException.FAILURE("wiki disabled", null);
		}
		return getInstance(LC.wiki_user.value());
	}
	
	public static Wiki getInstance(String acct) throws ServiceException {
		return getInstance(Provisioning.getInstance().getAccountByName(acct));
	}
	
	public static Wiki getInstance(Account acct) throws ServiceException {
		return getInstance(acct, getDefaultFolderId(acct));
	}
	
	public static Wiki getInstance(Account acct, int folderId) throws ServiceException {
		Wiki w;
		synchronized (wikiMap) {
			Pair<String,String> key = Pair.get(acct.getName(), Integer.toString(folderId));
			w = wikiMap.get(key);
			if (w == null) {
				w = new Wiki(acct, folderId);
				// XXX should be able to handle delete before start caching.
				//wikiMap.put(key, w);
			}
		}
		return w;
	}
	
	private Wiki(Account acct, int fid) throws ServiceException {
		mWikiWords = new HashMap<String,WikiWord>();
		
		mWikiAccount = acct.getId();
		mFolderId = fid;
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		OperationContext octxt = new OperationContext(acct);
		loadWiki(octxt, mbox);
	}
	
	private void loadWiki(OperationContext octxt, Mailbox mbox) throws ServiceException {
	    List<Document> wikiList = mbox.getWikiList(octxt, mFolderId);
	    for (Document item : wikiList) {
	    	addDoc(item);
	    }
	}
	
	public String getWikiAccount() {
		return mWikiAccount;
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
	
	public void addDoc(Document doc) throws ServiceException {
		if (doc instanceof WikiItem) 
			addWiki((WikiItem)doc);
		else
			addDocImpl(doc.getFilename(), doc);
	}
	
	public void addWiki(WikiItem wikiItem) throws ServiceException {
		addDocImpl(wikiItem.getWikiWord(), wikiItem);
	}
	
	public void addDocImpl(String wikiStr, Document doc) throws ServiceException {
		WikiWord w;
		w = mWikiWords.get(wikiStr);
		if (w == null) {
			w = new WikiWord(wikiStr);
			mWikiWords.put(wikiStr, w);
		}
		w.addWikiItem(doc);
	}
	
	public synchronized WikiWord createWiki(OperationContext octxt, String wikiword, String author, byte[] data) throws ServiceException {
		return createDocument(octxt, WikiItem.WIKI_CONTENT_TYPE, wikiword, author, data, MailItem.TYPE_WIKI);
	}
	
	public synchronized WikiWord createDocument(OperationContext octxt, String ct, String filename, String author, byte[] data) throws ServiceException {
		return createDocument(octxt, ct, filename, author, data, MailItem.TYPE_DOCUMENT);
	}
	
	public synchronized WikiWord createDocument(OperationContext octxt, String ct, String filename, String author, byte[] data, byte type) throws ServiceException {
		WikiWord ww = lookupWiki(filename);
		
		if (ww == null) {
			ww = new WikiWord(filename);
			mWikiWords.put(filename, ww);
		}
		ww.addWikiItem(octxt, mWikiAccount, mFolderId, filename, ct, author, data, type);
		return ww;
	}
	
	public void deleteWiki(OperationContext octxt, String wikiWord) throws ServiceException {
		WikiWord w = mWikiWords.remove(wikiWord);
		if (w != null) {
			w.deleteAllRevisions(octxt);
		}
	}
}
