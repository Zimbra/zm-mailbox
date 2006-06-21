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
import java.util.StringTokenizer;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.client.LmcDocument;
import com.zimbra.cs.client.LmcFolder;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcGetFolderRequest;
import com.zimbra.cs.client.soap.LmcGetFolderResponse;
import com.zimbra.cs.client.soap.LmcSearchRequest;
import com.zimbra.cs.client.soap.LmcSearchResponse;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.session.WikiSession;
import com.zimbra.cs.util.Pair;

/**
 * This class represents a Wiki notebook.  A notebook corresponds to a folder
 * where the Wiki pages and other uploaded documents are stored.  The name
 * (wiki word for a Wiki page, or filename for uploaded documents) is enforced
 * to be unique within the notebook.
 * 
 * A Wiki can be constructed with the Id of the account holder, and the path
 * to the folder / notebook.  When target mailbox resides on a remote machine,
 * then the path in REST url to the folder is used as the key.  For local
 * mailboxes, folderId is used for the path.
 * 
 * There is a cache of each Wik instances to help speed up a page lookup
 * by name / subject.  The maximum cache size is set by the attribute
 * <code>zimbraNotebookFolderCacheSize</code>.  The cache is connected to
 * each mailbox via <code>WikiSession</code> which sends notification for
 * changes in the Wiki pages and documents in the cache.
 * 
 * @author jylee
 *
 */
public abstract class Wiki {
	protected String mWikiAccount;
	
	protected Map<String,WikiPage> mWikiWords;
	
	private static final int DEFAULT_CACHE_SIZE = 1024;
	protected static LRUMap sWikiNotebookCache;

	protected static final String WIKI_FOLDER     = "notebook";
	protected static final String TEMPLATE_FOLDER = "/template";
	protected static final int    WIKI_FOLDER_ID  = 12;
	
	static {
		Provisioning prov = Provisioning.getInstance();
		int cacheSize;
		try {
			Server localServer = prov.getLocalServer();
			cacheSize = localServer.getIntAttr(Provisioning.A_zimbraNotebookFolderCacheSize, DEFAULT_CACHE_SIZE);
		} catch (ServiceException se) {
			cacheSize = DEFAULT_CACHE_SIZE;
		}
		sWikiNotebookCache = new LRUMap(cacheSize);
	}
	
	public static class WikiContext {
		OperationContext octxt;
		String           auth;
		
		public WikiContext(OperationContext o, String a) {
			octxt = o; auth = a;
		}
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
		public Folder getFolder(WikiContext ctxt, String accountId) throws ServiceException {
			return (Folder)getItemByPath(ctxt, accountId, true);
		}
		public MailItem getItem(WikiContext ctxt, String accountId) throws ServiceException {
			return getItemByPath(ctxt, accountId, false);
		}
		public MailItem getItemByPath(WikiContext ctxt, String accountId, boolean getFolder) throws ServiceException {
			Iterator<String> iter = mTokens.listIterator();
			int fid = Mailbox.ID_FOLDER_USER_ROOT;
			String root = iter.next();
			if (root.equals("//")) {
				Account acct = Provisioning.getInstance().get(AccountBy.name, iter.next());
				accountId = acct.getId();
			} else if (!root.equals("/")) {
				fid = Integer.parseInt(root);
			}
			
			Mailbox mbox = Mailbox.getMailboxByAccountId(accountId);
			return mbox.getItemByPath(ctxt.octxt, iter.next(), fid, getFolder);
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
	
	/*
	 * Mainly used for local accounts.  It's easier to
	 * manage and navigate folders with folder ID.
	 */
	public static class WikiById extends Wiki {
		protected int    mFolderId;
		protected int    mParentFolderId;
		
		private WikiById(WikiContext ctxt, Account acct, int fid) throws ServiceException {
			if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
				throw WikiServiceException.NOT_ENABLED();
			
			mWikiWords = new HashMap<String,WikiPage>();
			mWikiAccount = acct.getId();
			mFolderId = fid;
			
			Server acctServer = Provisioning.getInstance().getServer(acct);
			Server localServer = Provisioning.getInstance().getLocalServer();
			if (acctServer.getId().equals(localServer.getId())) {
				Mailbox mbox = Mailbox.getMailboxByAccount(acct);
				if (mbox == null)
					throw WikiServiceException.ERROR("wiki account mailbox not found");
				Folder f = mbox.getFolderById(ctxt.octxt, mFolderId);
				mParentFolderId = f.getFolderId();
				loadWiki(ctxt, mbox);
				mbox.addListener(WikiSession.getInstance());
			} else {
				// for some reason if the account is not local, but we have folder id.
				loadRemoteWiki(ctxt, acct, acctServer);
			}
		}
		
		public WikiPage lookupWiki(String wikiWord) {
			return mWikiWords.get(wikiWord);
		}
		
		public String getKey() {
			return Integer.toString(mFolderId);
		}
		
		public String getParentKey() {
			if (mParentFolderId == Mailbox.ID_FOLDER_ROOT) {
				return null;
			}
			return Integer.toString(mParentFolderId);
		}

		/*
		 * populate the wiki list from the MailItem's in the Mailbox.
		 */
		private void loadWiki(WikiContext ctxt, Mailbox mbox) throws ServiceException {
		    List<Document> wikiList = mbox.getWikiList(ctxt.octxt, mFolderId);
		    for (Document item : wikiList) {
		    	addDoc(item);
		    }
		}

		/*
		 * populate the wiki list from the remote machine.
		 */
		private void loadRemoteWiki(WikiContext ctxt, Account acct, Server remoteServer) throws ServiceException {
			String auth = ctxt.auth;
			try {
				if (auth == null)
					auth = AuthToken.getZimbraAdminAuthToken().getEncoded();

				String url = URLUtil.getMailURL(remoteServer, ZimbraServlet.USER_SERVICE_URI, false);

				LmcSession session = new LmcSession(auth, null);

				LmcFolder folder = getRemoteFolder(ctxt, acct, remoteServer, mFolderId);
				ItemId pfid = new ItemId(folder.getParentID(), null);
		        mParentFolderId = pfid.getId();
				
				LmcSearchRequest sreq = new LmcSearchRequest();
				sreq.setRequestedAccountId(acct.getId());
				sreq.setSession(session);
				sreq.setTypes("wiki,document");
				sreq.setLimit("100");
				sreq.setOffset("0");
				sreq.setQuery("inid:" + mFolderId);

				LmcSearchResponse sresp = (LmcSearchResponse)sreq.invoke(url);

				List ls = sresp.getResults();
				for (Object obj : ls) {
					if (obj instanceof LmcDocument) {
						WikiPage wp = WikiPage.create((LmcDocument)obj);
						mWikiWords.put(wp.getWikiWord(), wp);
					} else {
						// unhandled item
					}
				}
			} catch (Exception e) {
				throw WikiServiceException.ERROR("can't load remote wiki", e);
			}
		}
		
		public synchronized WikiPage createDocument(WikiContext ctxt, String ct, String filename, String author, byte[] data, byte type) throws ServiceException {
			WikiPage ww = lookupWiki(filename);
			
			if (ww == null) {
				ww = WikiPage.create(filename);
				mWikiWords.put(filename, ww);
			}
			ww.addWikiItem(ctxt, mWikiAccount, mFolderId, filename, ct, author, data, type);
			return ww;
		}
		
		public int getWikiFolderId() {
			return mFolderId;
		}
		
	}
	
	/*
	 * This is for remote accounts.  Since folder ID is not readily available,
	 * key off the path of the item.
	 */
	public static class WikiByPath extends Wiki {
		private String mPath;
		private String mRestUrl;
		
		private WikiByPath(WikiContext ctxt, Account acct, String path) throws ServiceException {
			if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
				throw WikiServiceException.NOT_ENABLED();
			
			mWikiWords = new HashMap<String,WikiPage>();
			mWikiAccount = acct.getId();
			mPath = path;
			StringBuilder buf = new StringBuilder();
			buf.append(UserServlet.getRestUrl(acct, false));
			if (!path.startsWith("/"))
				buf.append("/");
			buf.append(path);
			if (!path.endsWith("/"))
				buf.append("/");
			mRestUrl = buf.toString();
		}

		public String getKey() {
			return mPath;
		}
		
		public String getParentKey() {
			int pos = mPath.lastIndexOf('/');
			if (pos < 0)
				return null;
			return mPath.substring(0, pos);
		}
		
		public WikiPage lookupWiki(String wikiWord) {
			WikiPage page = mWikiWords.get(wikiWord);
			if (page == null) {
				// if we have a cache of Wiki pages whose mailbox is on another machine,
				// the cache may not be complete.  create an instance of WikiPage and
				// set the REST url for retrieval.
				page = WikiPage.create(wikiWord, mRestUrl);
			}			
			return page;
		}
		
		public synchronized WikiPage createDocument(WikiContext ctxt, String ct, String filename, String author, byte[] data, byte type) throws ServiceException {
			throw WikiServiceException.ERROR("createDocument on a remote wiki: not implemented");
		}
		public int getWikiFolderId() throws ServiceException {
			throw WikiServiceException.ERROR("createDocument on a remote wiki: not implemented");
		}
	}
	
	public static Account getDefaultWikiAccount() throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Config globalConfig = prov.getConfig();
		String defaultAcct = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount);
		if (defaultAcct == null)
			throw WikiServiceException.ERROR("empty config variable " + Provisioning.A_zimbraNotebookAccount);
		Account acct = prov.get(AccountBy.name, defaultAcct);
		if (acct == null)
			throw WikiServiceException.ERROR("no such account " + Provisioning.A_zimbraNotebookAccount);
		
		return acct;
	}
	
	public static Wiki getTemplateStore(WikiContext ctxt, String accountId) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(Provisioning.AccountBy.id, accountId);
		Domain dom = prov.getDomain(acct);
		Config globalConfig = prov.getConfig();
		
		String domainWiki = dom.getAttr(Provisioning.A_zimbraNotebookAccount, null);
		String defaultWiki = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount, null);
		
		Account target = null;
		if (domainWiki != null) {
			target = prov.get(Provisioning.AccountBy.name, domainWiki);
			if (target.getId() == accountId)
				target = null;
		}
		if (defaultWiki != null) {
			Account defaultAccount = prov.get(Provisioning.AccountBy.name, defaultWiki);
			if (defaultAccount.getId() == accountId)
				return null;
			if (target == null)
				target = defaultAccount;
		}
		if (target == null)
			return null;
		WikiUrl wurl = new WikiUrl("//" + target.getName() + TEMPLATE_FOLDER);
			
		return getInstance(ctxt, wurl);
	}
	
	public static MailItem findWikiByPathTraverse(WikiContext ctxt, String accountId, int fid, String path) throws ServiceException {
		WikiUrl url = new WikiUrl(path, fid);
		Folder f = url.getFolder(ctxt, accountId);
		String name = url.getFilename();
		
		String targetAccountId = accountId;
		int targetFolderId = f.getId();

		MailItem item = findWikiByNameTraverse(ctxt, targetAccountId, targetFolderId, name);
		while (item == null) {
			Wiki w = getTemplateStore(ctxt, targetAccountId);
			if (w == null)
				return null;
			targetAccountId = w.getWikiAccount();
			targetFolderId = w.getWikiFolderId();
			item = findWikiByNameTraverse(ctxt, targetAccountId, targetFolderId, name);
		}
		return item;
	}
	
	public static MailItem findWikiByNameTraverse(WikiContext ctxt, String accountId, int fid, String name) throws ServiceException {
		Mailbox mbox = Mailbox.getMailboxByAccountId(accountId);
		Wiki w = getInstance(ctxt, accountId, fid);
		while (w != null) {
			WikiPage ww = w.lookupWiki(name);
			if (ww != null)
				return ww.getWikiItem(ctxt);
			if (fid == Mailbox.ID_FOLDER_USER_ROOT)
				break;
			Folder f = mbox.getFolderById(ctxt.octxt, fid);
			fid = f.getFolderId();
			w = getInstance(ctxt, accountId, fid);
		}
		return null;
	}
	
	public static MailItem findWikiByPath(WikiContext ctxt, String accountId, int fid, String path, boolean traverse) throws ServiceException {
		if (traverse)
			return findWikiByPathTraverse(ctxt, accountId, fid, path);
		
		return findWikiByPath(ctxt, accountId, fid, path);
	}
	
	public static MailItem findWikiByPath(WikiContext ctxt, String accountId, int fid, String path) throws ServiceException {
		WikiUrl url = new WikiUrl(path, fid);
		return url.getItem(ctxt, accountId);
	}
	
	public static Wiki getInstance(WikiContext ctxt, String acct) throws ServiceException {
		return getInstance(ctxt, acct, WIKI_FOLDER_ID);
	}

	public static Wiki getInstance(WikiContext ctxt, String acct, int folderId) throws ServiceException {
		return getInstance(ctxt, Provisioning.getInstance().get(AccountBy.id, acct), folderId);
	}
	
	public static Wiki getInstance(WikiContext ctxt, Account acct, int folderId) throws ServiceException {
		if (acct == null || folderId < 1)
			throw new WikiServiceException.NoSuchWikiException("no such account");
		
		// check the folder for access
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		if (mbox == null)
			throw WikiServiceException.ERROR("wiki account mailbox not found");
		Folder f = mbox.getFolderById(ctxt.octxt, folderId);

		Pair<String,String> key = Pair.get(acct.getId(), Integer.toString(folderId));
		Wiki wiki = (Wiki)sWikiNotebookCache.get(key);
		if (wiki == null) {
			wiki = new WikiById(ctxt, acct, folderId);
			Wiki.add(wiki, key);
		}
		return wiki;
	}

	public static Wiki getInstance(WikiContext ctxt, WikiUrl url) throws ServiceException {
		if (!url.isRemote()) {
			throw WikiServiceException.ERROR("no account name in the path");
		}
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(Provisioning.AccountBy.name, url.getToken(1));
		if (account == null) {
			throw WikiServiceException.ERROR("no such account: " + url.getToken(1));
		}
		return getInstance(ctxt, account.getId(), url.getToken(2));
	}
	
	public static Wiki getInstance(WikiContext ctxt, String acct, String key) throws ServiceException {
		try {
			int fid = Integer.parseInt(key);
			return getInstance(ctxt, acct, fid);
		} catch (Exception e) {
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(Provisioning.AccountBy.id, acct);
			Server acctServer = Provisioning.getInstance().getServer(account);
			Server localServer = Provisioning.getInstance().getLocalServer();
			if (acctServer.getId().equals(localServer.getId())) {
				Mailbox mbox = Mailbox.getMailboxByAccount(account);
				int fid = mbox.getFolderByPath(ctxt.octxt, key).getId();
				return getInstance(ctxt, acct, fid);
			}
			Pair<String,String> k = Pair.get(account.getId(), key);
			Wiki wiki = (Wiki)sWikiNotebookCache.get(k);
			if (wiki == null) {
				wiki = new WikiByPath(ctxt, account, key);
				Wiki.add(wiki, k);
			}
			return wiki;
		}
	}
	
	public static void add(Wiki wiki, Pair<String,String> key) {
		synchronized (sWikiNotebookCache) {
			Wiki w = (Wiki)sWikiNotebookCache.get(key);
			if (w == null) {
				sWikiNotebookCache.put(key, wiki);
			}
		}
	}
	
	public static void remove(String acctId, String k) {
		synchronized (sWikiNotebookCache) {
			Pair<String,String> key = Pair.get(acctId, k);
			sWikiNotebookCache.remove(key);
		}
	}

	private static LmcFolder findFolder(LmcFolder folder, String path) throws ServiceException {
		if (folder == null || path == null)
			return null;
		StringTokenizer tok = new StringTokenizer(path, "/");
		String lastToken = null;
		while (tok.hasMoreTokens()) {
			lastToken = tok.nextToken();
			LmcFolder[] sub = folder.getSubFolders();
			if (sub == null || sub.length == 0)
				return null;
			for (int i = 0; i < sub.length; i++) {
				if (sub[i].getName().equalsIgnoreCase(lastToken)) {
					folder = sub[i];
					break;
				}
			}
			if (!folder.getName().equalsIgnoreCase(lastToken))
				return null;
		}
		if (!folder.getName().equalsIgnoreCase(lastToken))
			return null;
		return folder;
	}
	
	private static LmcFolder getRemoteFolder(WikiContext ctxt, Account acct, Server remoteServer, int fid) throws ServiceException {
		String auth = ctxt.auth;
		try {
			if (fid <= 0)
				fid = Mailbox.ID_FOLDER_USER_ROOT;
			if (auth == null)
				auth = AuthToken.getZimbraAdminAuthToken().getEncoded();

			String url = URLUtil.getMailURL(remoteServer, ZimbraServlet.USER_SERVICE_URI, false);

			LmcSession session = new LmcSession(auth, null);
			LmcGetFolderRequest gfreq = new LmcGetFolderRequest();
			gfreq.setRequestedAccountId(acct.getId());
			gfreq.setSession(session);
			gfreq.setFolderToGet(Integer.toString(fid));
			LmcGetFolderResponse gfresp = (LmcGetFolderResponse) gfreq.invoke(url);
			return gfresp.getRootFolder();
		} catch (Exception e) {
			throw WikiServiceException.ERROR("can't load remote wiki", e);
		}
	}
	
	public String getWikiAccount() {
		return mWikiAccount;
	}
	
	public abstract String getKey();
	public abstract String getParentKey() throws ServiceException;
	public abstract int getWikiFolderId() throws ServiceException;
	
	public Set<String> listWiki() {
		return mWikiWords.keySet();
	}
	public abstract WikiPage lookupWiki(String wikiWord);
	
	public void addDoc(Document doc) throws ServiceException {
		String wikiWord;
		if (doc instanceof WikiItem) 
			wikiWord = ((WikiItem)doc).getWikiWord();
		else
			wikiWord = doc.getFilename();
		WikiPage w = mWikiWords.get(wikiWord);
		if (w == null) {
			w = WikiPage.create(wikiWord);
			mWikiWords.put(wikiWord, w);
		}
		w.addWikiItem(doc);
	}
	
	public synchronized WikiPage createWiki(WikiContext ctxt, String wikiword, String author, byte[] data) throws ServiceException {
		return createDocument(ctxt, WikiItem.WIKI_CONTENT_TYPE, wikiword, author, data, MailItem.TYPE_WIKI);
	}
	
	public synchronized WikiPage createDocument(WikiContext ctxt, String ct, String filename, String author, byte[] data) throws ServiceException {
		return createDocument(ctxt, ct, filename, author, data, MailItem.TYPE_DOCUMENT);
	}

	public abstract WikiPage createDocument(WikiContext ctxt, String ct, String filename, String author, byte[] data, byte type) throws ServiceException;
	
	public void deleteWiki(WikiContext ctxt, String wikiWord) throws ServiceException {
		WikiPage w = mWikiWords.remove(wikiWord);
		if (w != null) {
			w.deleteAllRevisions(ctxt);
		}
	}
}
