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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.wiki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.session.WikiSession;
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
	
	private static final String WIKI_FOLDER     = "notebook";
	private static final String TEMPLATE_FOLDER = "template";
	
	private static int sTemplateFolderId;
	
	static {
		wikiMap = new HashMap<Pair<String,String>,Wiki>();
	}
	
	static class WikiUrl {
		public WikiUrl(String url) {
			// url must be in absolute form
			this(url, -1);
		}
		public WikiUrl(String url, int currentPos) {
			// url can be in absolute or relative form.
			mUrl = url;
			mId = currentPos;
			parse();
		}
		private int mId;
		private String mUrl;
		private List<String> mTokens;
		private String mFilename;
		
		private void parse() {
			mTokens = new ArrayList<String>();
			int begin = 0, end = 0;
			if (mUrl.startsWith("//")) {
				begin = 2;
				end = mUrl.indexOf('/', begin);
				mTokens.add("//");
				mTokens.add(mUrl.substring(begin, end));
				begin = end;
			} else if (mUrl.startsWith("/")) {
				mTokens.add("/");
			} else {
				if (mId == -1)
					throw new IllegalArgumentException("not absolute url: " + mUrl);
				mTokens.add(Integer.toString(mId));
			}
			mTokens.add(mUrl.substring(begin));
			begin = mUrl.lastIndexOf("/");
			if (begin > 0)
				mFilename = mUrl.substring(begin+1);
			else
				mFilename = mUrl;
		}
		public Folder getFolder(OperationContext octxt, String accountId) throws ServiceException {
			return (Folder)getItemByPath(octxt, accountId, true);
		}
		public MailItem getItem(OperationContext octxt, String accountId) throws ServiceException {
			return getItemByPath(octxt, accountId, false);
		}
		public MailItem getItemByPath(OperationContext octxt, String accountId, boolean getFolder) throws ServiceException {
			Iterator<String> iter = mTokens.listIterator();
			int fid = Mailbox.ID_FOLDER_USER_ROOT;
			String root = iter.next();
			if (root.equals("//")) {
				Account acct = Provisioning.getInstance().get(AccountBy.NAME, iter.next());
				accountId = acct.getId();
			} else if (!root.equals("/")) {
				fid = Integer.parseInt(root);
			}
			
			Mailbox mbox = Mailbox.getMailboxByAccountId(accountId);
			return mbox.getItemByPath(octxt, iter.next(), fid, getFolder);
		}
		public boolean isAbsolute() {
			return (mTokens != null &&
					mTokens.get(0).startsWith("/"));
		}
		public boolean isRemote() {
			return (mTokens != null &&
					mTokens.get(0).equals("//"));
		}
		public String getToken(int pos) {
			return mTokens.get(pos);
		}
		public String getFilename() {
			return mFilename;
		}
	}
	
	public static int getDefaultFolderId(Account acct) throws ServiceException {
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		OperationContext octxt = new OperationContext(acct);
		Folder f = mbox.getFolderByPath(octxt, WIKI_FOLDER);
		return f.getId();
	}
	
	public static Account getDefaultWikiAccount() throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Config globalConfig = prov.getConfig();
		String defaultAcct = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount);
		if (defaultAcct == null)
			throw WikiServiceException.ERROR("empty config variable " + Provisioning.A_zimbraNotebookAccount);
		Account acct = prov.get(AccountBy.NAME, defaultAcct);
		if (acct == null)
			throw WikiServiceException.ERROR("no such account " + Provisioning.A_zimbraNotebookAccount);
		
		return acct;
	}
	
	public static Wiki getInstance() throws ServiceException {
		return getInstance(Wiki.getDefaultWikiAccount());
	}
	
	public static Wiki getTemplateStore() throws ServiceException {
		synchronized (Wiki.class) {
			if (sTemplateFolderId == 0) {
				Account acct = getDefaultWikiAccount();
				OperationContext octxt = new OperationContext(acct);
				WikiUrl wurl = new WikiUrl("//wiki/template");
				Folder f = wurl.getFolder(octxt, acct.getId());
				sTemplateFolderId = f.getId();
			}
		}
		return getInstance(Wiki.getDefaultWikiAccount(), sTemplateFolderId);
	}
	
	public static MailItem findWikiByPathTraverse(OperationContext octxt, String accountId, int fid, String path) throws ServiceException {
		WikiUrl url = new WikiUrl(path, fid);
		Folder f = url.getFolder(octxt, accountId);
		String name = url.getFilename();

		MailItem item = findWikiByNameTraverse(octxt, accountId, f.getId(), name);
		if (item == null) {
			Wiki w = getTemplateStore();
			item = findWikiByNameTraverse(octxt, w.getWikiAccount(), w.getWikiFolderId(), name);
		}
		return item;
	}
	
	public static MailItem findWikiByNameTraverse(OperationContext octxt, String accountId, int fid, String name) throws ServiceException {
		Mailbox mbox = Mailbox.getMailboxByAccountId(accountId);
		Wiki w = getInstance(accountId, fid);
		while (w != null) {
			WikiWord ww = w.lookupWiki(name);
			if (ww != null)
				return ww.getWikiItem(octxt);
			if (fid == Mailbox.ID_FOLDER_USER_ROOT)
				break;
			Folder f = mbox.getFolderById(octxt, fid);
			fid = f.getFolderId();
			w = getInstance(accountId, fid);
		}
		return null;
	}
	
	public static MailItem findWikiByPath(OperationContext octxt, String accountId, int fid, String path, boolean traverse) throws ServiceException {
		if (traverse)
			return findWikiByPathTraverse(octxt, accountId, fid, path);
		
		return findWikiByPath(octxt, accountId, fid, path);
	}
	
	public static MailItem findWikiByPath(OperationContext octxt, String accountId, int fid, String path) throws ServiceException {
		WikiUrl url = new WikiUrl(path, fid);
		return url.getItem(octxt, accountId);
	}
	
	public static Wiki getInstance(String acct, int folderId) throws ServiceException {
		return getInstance(Provisioning.getInstance().get(AccountBy.ID, acct), folderId);
	}
	
	public static Wiki getInstance(Account acct) throws ServiceException {
		return getInstance(acct, getDefaultFolderId(acct));
	}

	public static Wiki getInstance(Account acct, int folderId) throws ServiceException {
		Wiki w;
		if (acct == null || folderId < 1)
			throw new WikiServiceException.NoSuchWikiException("no such account");
		synchronized (wikiMap) {
			Pair<String,String> key = Pair.get(acct.getId(), Integer.toString(folderId));
			w = wikiMap.get(key);
			if (w == null) {
				w = new Wiki(acct, folderId);
				wikiMap.put(key, w);
			}
		}
		return w;
	}
	
	public static void remove(String acctId, int folderId) {
		Pair<String,String> key = Pair.get(acctId, Integer.toString(folderId));
		wikiMap.remove(key);
	}
	
	private Wiki(Account acct, int fid) throws ServiceException {
		if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
			throw WikiServiceException.NOT_ENABLED();
		
		mWikiWords = new HashMap<String,WikiWord>();
		
		mWikiAccount = acct.getId();
		mFolderId = fid;
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		if (mbox == null)
			throw WikiServiceException.ERROR("wiki account mailbox not found");
		OperationContext octxt = new OperationContext(acct);
		loadWiki(octxt, mbox);
		mbox.addListener(WikiSession.getInstance());
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
