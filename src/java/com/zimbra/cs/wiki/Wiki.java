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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcSearchRequest;
import com.zimbra.cs.client.soap.LmcSearchResponse;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.ServiceException.Argument;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.session.WikiSession;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Pair;
import com.zimbra.cs.util.ZimbraLog;

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

	protected static final String WIKI_FOLDER_NAME     = "notebook";
	protected static final String TEMPLATE_FOLDER_NAME = "/template/";
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
		private String mFilename;
		private List<String> mTokens;
		
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
		public Wiki findWiki(WikiContext ctxt, String referenceAccount) throws ServiceException {
			Account ownerAccount = getOwnerAccount(referenceAccount);
			return Wiki.getInstance(ctxt, ownerAccount.getId(), getFolderPath(ctxt, referenceAccount));
		}
		public String getFullUrl(WikiContext ctxt, String referenceAccount) throws ServiceException {
			Account ownerAccount = getOwnerAccount(referenceAccount);
			return UserServlet.getRestUrl(ownerAccount)
							+ getPath(ctxt, ownerAccount);
		}
		public Account getOwnerAccount(String referenceAccount) throws ServiceException {
			return (inAnotherMailbox()) ? 
					Provisioning.getInstance().get(AccountBy.name, mTokens.get(1))
					: (referenceAccount == null) ? null : Provisioning.getInstance().get(AccountBy.id, referenceAccount);
		}
		public String getPath(WikiContext ctxt, Account acct) throws ServiceException {
			// sanity check
			if (!isAbsolute() && mId < 1) {
				throw WikiServiceException.INVALID_PATH(mUrl);
			}
			
			StringBuilder p = new StringBuilder();
			if (inAnotherMailbox() || acct == null) {
				// take the absolute url in the path.
				p.append(mTokens.get(2));
			} else if (isAbsolute()) {
				// take the path as is.
				p.append(mUrl);
			} else if (Provisioning.onLocalServer(acct)) {
				// calculate absolute path based on current location
				Mailbox mbox = Mailbox.getMailboxByAccount(acct);
				Folder f = mbox.getFolderById(ctxt.octxt, mId);
				p.append(f.getPath());
				if (p.charAt(p.length() - 1) != '/')
					p.append("/");
				p.append(mUrl);
			} else {
				// we know the account, and remote folder id, and relative path
				// from the remote folder.  we can do something like getFolder
				// and then calculate REST from the result.  for now treat
				// this case as unreachable.
				throw WikiServiceException.INVALID_PATH(mUrl);
			}
			return normalizePath(p.toString());
		}
		public String getFolderPath(WikiContext ctxt, String referenceAccount) throws ServiceException {
			Account acct = getOwnerAccount(referenceAccount);
			String url = getPath(ctxt, acct);
			int index = url.lastIndexOf('/');
			if (index > 0) {
				return url.substring(0, index);
			}
			return "/";
		}
		/*
		 * get rid of /./ and /../ in the path.
		 */
		private String normalizePath(String path) throws ServiceException {
			List<String> tokens = new ArrayList<String>();
			StringTokenizer tok = new StringTokenizer(path, "/");
			while (tok.hasMoreElements()) {
				String token = tok.nextToken();
				if (token.equals("."))
					continue;
				else if (token.equals("..")) {
					if (tokens.isEmpty()) {
						throw WikiServiceException.INVALID_PATH(path);
					}
					tokens.remove(tokens.size() - 1);
				} else
					tokens.add(token);
			}
			if (tokens.isEmpty()) {
				return "/";
			}
			if (path.endsWith("/"))
				tokens.add("");
			StringBuilder newPath = new StringBuilder();
			for (String token : tokens) {
				newPath.append("/").append(token);
			}
			return newPath.toString();
		}
		public boolean isAbsolute() {
			return (mTokens != null &&
					mTokens.get(0).startsWith("/"));
		}
		public boolean inAnotherMailbox() {
			return (mTokens != null &&
					mTokens.get(0).equals("//"));
		}
		public boolean onLocalMachine() throws ServiceException {
			if (inAnotherMailbox()) {
				Account account = Provisioning.getInstance().get(AccountBy.name, mTokens.get(1));
				return Provisioning.onLocalServer(account);
			}
			return true;
		}
		public String getToken(int pos) {
			return mTokens.get(pos);
		}
		public String getFilename() {
			return mFilename;
		}
		public String getUrl() {
			return mUrl;
		}
		public String toString() {
			return "wikiUrl: " + mUrl + " in folderId" + mId;
		}
	}
	
	/*
	 * Mainly used for local accounts.  It's easier to
	 * manage and navigate folders with folder ID.
	 */
	public static class WikiById extends Wiki {
		protected int mFolderId;
		protected int mParentFolderId;
		
		private WikiById(WikiContext ctxt, Account acct, int fid) throws ServiceException {
			if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
				throw WikiServiceException.NOT_ENABLED();
			
			mWikiWords = new HashMap<String,WikiPage>();
			mWikiAccount = acct.getId();
			mFolderId = fid;
			
			if (Provisioning.onLocalServer(acct)) {
				Mailbox mbox = Mailbox.getMailboxByAccount(acct);
				if (mbox == null)
					throw WikiServiceException.ERROR("wiki account mailbox not found");
				Folder f = mbox.getFolderById(ctxt.octxt, mFolderId);
				mParentFolderId = f.getFolderId();
				loadWiki(ctxt, mbox);
				mbox.addListener(WikiSession.getInstance());
			} else {
				// for some reason if the account is not local, but we have folder id.
				throw WikiServiceException.ERROR("cannot access remote account "+acct.getName());
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
		private synchronized void loadWiki(WikiContext ctxt, Mailbox mbox) throws ServiceException {
		    List<Document> wikiList = mbox.getWikiList(ctxt.octxt, mFolderId);
		    for (Document item : wikiList) {
		    	addDoc(item);
		    }
		}

		public synchronized void renameDocument(WikiContext ctxt, int id, String newName, String author) throws ServiceException {
			WikiPage page = lookupWiki(newName);
			if (page != null)
				throw MailServiceException.MODIFY_CONFLICT(
						new Argument(MailService.A_NAME, newName, Argument.Type.STR));
			Mailbox mbox = Mailbox.getMailboxByAccountId(mWikiAccount);
			MailItem item = mbox.getItemById(ctxt.octxt, id, MailItem.TYPE_UNKNOWN);
			if (item.getType() != MailItem.TYPE_DOCUMENT && item.getType() != MailItem.TYPE_WIKI)
				throw WikiServiceException.NOT_WIKI_ITEM("MailItem id " +id+ " is not a wiki item or a document");
			Document doc = (Document) item;
			doc.rename(newName);
			byte[] contents;
        	try {
        		contents = ByteUtil.getContent(doc.getRawDocument(), 0);
        	} catch (IOException ioe) {
        		ZimbraLog.wiki.error("cannot read the item body", ioe);
        		throw WikiServiceException.CANNOT_READ(doc.getSubject());
        	}
        	mbox.addDocumentRevision(ctxt.octxt, doc, contents, author);
        	Wiki.remove(mWikiAccount, Integer.toString(mFolderId));
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
		
		private WikiByPath(Account acct, String path) throws ServiceException {
			if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
				throw WikiServiceException.NOT_ENABLED();
			
			mWikiWords = new HashMap<String,WikiPage>();
			mWikiAccount = acct.getId();
			mPath = path;
			StringBuilder buf = new StringBuilder();
			buf.append(UserServlet.getRestUrl(acct));
			if (!path.startsWith("/"))
				buf.append("/");
			buf.append(path);
			if (!path.endsWith("/"))
				buf.append("/");
			mRestUrl = buf.toString();
			loadRemoteWiki(acct, Provisioning.getInstance().getServer(acct));
		}

		public String getKey() {
			return mPath;
		}
		
		public String getParentKey() {
			int pos = mPath.lastIndexOf('/');
			if (pos < 1)
				return null;
			return mPath.substring(0, pos);
		}
		
		/*
		 * populate the wiki list from the remote machine.
		 */
		private synchronized void loadRemoteWiki(Account acct, Server remoteServer) throws ServiceException {
			try {
				String auth = AuthToken.getZimbraAdminAuthToken().getEncoded();

				String url = URLUtil.getMailURL(remoteServer, ZimbraServlet.USER_SERVICE_URI, false);

				LmcSession session = new LmcSession(auth, null);

				LmcSearchRequest sreq = new LmcSearchRequest();
				sreq.setRequestedAccountId(acct.getId());
				sreq.setSession(session);
				sreq.setTypes("wiki,document");
				sreq.setLimit("100");  // XXX revisit the limit of items in a folder.
				sreq.setOffset("0");
				sreq.setQuery("in:" + mPath);

				LmcSearchResponse sresp = (LmcSearchResponse)sreq.invoke(url);

				@SuppressWarnings("unchecked")
				List<Object> ls = (List<Object>)sresp.getResults();
				for (Object obj : ls) {
					if (obj instanceof LmcDocument) {
						WikiPage wp = WikiPage.create(mWikiAccount, (LmcDocument)obj);
						mWikiWords.put(wp.getWikiWord(), wp);
					} else {
						// unhandled item
					}
				}
			} catch (Exception e) {
				throw WikiServiceException.ERROR("can't load remote wiki", e);
			}
		}
		
		public synchronized WikiPage lookupWiki(String wikiWord) {
			WikiPage page = mWikiWords.get(wikiWord);
			if (page == null) {
				// if we have a cache of Wiki pages whose mailbox is on another machine,
				// the cache may not be complete.  create an instance of WikiPage and
				// set the REST url for retrieval.
				page = WikiPage.create(mWikiAccount, wikiWord, mRestUrl);
				try {
					page.getContents(null);
				} catch (ServiceException se) {
					return null;
				}
				mWikiWords.put(page.getWikiWord(), page);
			}			
			return page;
		}
		
		public void renameDocument(WikiContext ctxt, int id, String newName, String author) throws ServiceException {
			throw WikiServiceException.ERROR("createDocument on a remote wiki: not implemented");
		}
		public int getWikiFolderId() throws ServiceException {
			throw WikiServiceException.ERROR("getWikiFolderId on a remote wiki: not implemented");
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
	
	public static Wiki getDefaultStore(WikiContext ctxt, String accountId) throws ServiceException {
		return getByUrl(ctxt, getDefaultStoreUrl(accountId));
	}
	public static WikiUrl getDefaultStoreUrl(String accountId) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(Provisioning.AccountBy.id, accountId);
		Domain dom = prov.getDomain(acct);
		Config globalConfig = prov.getConfig();
		
		String domainWiki = dom.getAttr(Provisioning.A_zimbraNotebookAccount, null);
		String defaultWiki = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount, null);

		//ZimbraLog.wiki.debug("template store for " + accountId);
		Account target = null;
		if (domainWiki != null) {
			//ZimbraLog.wiki.debug("domainWiki " + domainWiki);
			target = prov.get(Provisioning.AccountBy.name, domainWiki);
			if (target.getId().equals(accountId))
				target = null;
		}
		if (defaultWiki != null) {
			//ZimbraLog.wiki.debug("defaultWiki " + defaultWiki);
			Account defaultAccount = prov.get(Provisioning.AccountBy.name, defaultWiki);
			if (defaultAccount.getId().equals(accountId))
				return null;
			if (target == null)
				target = defaultAccount;
		}
		if (target == null)
			return null;
		WikiUrl wurl = new WikiUrl("//" + target.getName() + TEMPLATE_FOLDER_NAME);
		//ZimbraLog.wiki.debug("wikiUrl for template: " + wurl.getFullUrl(ctxt, accountId));

		return wurl;
	}
	
	public static Wiki findWikiByPath(WikiContext ctxt, String accountId, int fid, String path, boolean traverse) throws ServiceException {
		WikiUrl url = new WikiUrl(path, fid);
		return findWikiByPath(ctxt, accountId, url, traverse);
	}
	public static Wiki findWikiByPath(WikiContext ctxt, String accountId, WikiUrl url, boolean traverse) throws ServiceException {
		Wiki wiki = url.findWiki(ctxt, accountId);
		String pageName = url.getFilename();
		WikiPage page = wiki.lookupWiki(pageName);
		
		if (page != null)
			return wiki;

		if (!traverse)
			throw new WikiServiceException.NoSuchWikiException(url.getUrl());
		
		String actualAccount = wiki.getWikiAccount();
		String key = wiki.getParentKey();
		while (wiki != null) {
			actualAccount = wiki.getWikiAccount();
			key = wiki.getKey();
			while (wiki != null && actualAccount != null && key != null) {
				try {
					wiki = Wiki.getInstance(ctxt, actualAccount, key);
				} catch (ServiceException se) {
					wiki = null;
				}
				if (wiki != null) {
					page = wiki.lookupWiki(pageName);
					if (page != null) {
						return wiki;
					}
					key = wiki.getParentKey();
				}
			}
			try {
				wiki = getDefaultStore(ctxt, actualAccount);
			} catch (ServiceException se) {
				wiki = null;
			}
		}
		throw new WikiServiceException.NoSuchWikiException(url.getUrl());
	}
	public static WikiPage findWikiPageByPath(WikiContext ctxt, String accountId, int fid, String path, boolean traverse) throws ServiceException {
		WikiUrl url = new WikiUrl(path, fid);
		Wiki wiki = findWikiByPath(ctxt, accountId, url, traverse);
		String pageName = url.getFilename();
		return wiki.lookupWiki(pageName);
	}
	public static Wiki getInstance(WikiContext ctxt, String acct) throws ServiceException {
		return getInstance(ctxt, acct, WIKI_FOLDER_ID);
	}

	public static Wiki getInstance(WikiContext ctxt, String acctId, int folderId) throws ServiceException {
		Account acct = Provisioning.getInstance().get(AccountBy.id, acctId);
		if (acct == null || folderId < 1)
			throw new WikiServiceException.NoSuchWikiException("no such account");
		
		// check the folder for access
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		if (mbox == null)
			throw WikiServiceException.ERROR("wiki account mailbox not found");
		mbox.getFolderById(ctxt.octxt, folderId);

		Pair<String,String> key = Pair.get(acctId, Integer.toString(folderId));
		Wiki wiki = (Wiki)sWikiNotebookCache.get(key);
		if (wiki == null) {
			wiki = Wiki.add(new WikiById(ctxt, acct, folderId), key);
		}
		return wiki;
	}

	public static Wiki getInstance(WikiContext ctxt, String acct, String key) throws ServiceException {
		int fid;
		try {
			fid = Integer.parseInt(key);
			return getInstance(ctxt, acct, fid);
		} catch (NumberFormatException e) {
			Account account = Provisioning.getInstance().get(Provisioning.AccountBy.id, acct);
			if (Provisioning.onLocalServer(account)) {
				Mailbox mbox = Mailbox.getMailboxByAccount(account);
				if (key.equals("/"))
					fid = Mailbox.ID_FOLDER_USER_ROOT;
				else {
					MailItem item = mbox.getItemByPath(ctxt.octxt, key, 0, true);
					if (item instanceof Folder)
						fid = item.getId();
					else
						fid = item.getFolderId();
				}
				return getInstance(ctxt, acct, fid);
			}
			Pair<String,String> k = Pair.get(account.getId(), key);
			Wiki wiki = (Wiki)sWikiNotebookCache.get(k);
			if (wiki == null) {
				wiki = Wiki.add(new WikiByPath(account, key), k);
			}
			return wiki;
		}
	}
	
	public static Wiki getByUrl(WikiContext ctxt, WikiUrl url) throws ServiceException {
		if (url == null) {
			throw WikiServiceException.ERROR("empty url");
		}
		if (!url.inAnotherMailbox()) {
			throw WikiServiceException.ERROR("no account name in the path");
		}
		Account account = url.getOwnerAccount(null);
		if (account == null) {
			throw WikiServiceException.INVALID_PATH("no account in : " + url.getFullUrl(ctxt, null));
		}
		return getInstance(ctxt, account.getId(), url.getFolderPath(ctxt, null));
	}
	
	public static Wiki add(Wiki wiki, Pair<String,String> key) {
		synchronized (sWikiNotebookCache) {
			Wiki w = (Wiki)sWikiNotebookCache.get(key);
			if (w == null) {
				sWikiNotebookCache.put(key, wiki);
			}
		}
		return wiki;
	}
	
	public static void remove(String acctId, String k) {
		synchronized (sWikiNotebookCache) {
			Pair<String,String> key = Pair.get(acctId, k);
			sWikiNotebookCache.remove(key);
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
	
	public synchronized void addDoc(Document doc) throws ServiceException {
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
	
	public static WikiPage findPage(WikiContext ctxt, String accountId, int id) throws ServiceException {
		Account account = Provisioning.getInstance().get(Provisioning.AccountBy.id, accountId);
		if (!Provisioning.onLocalServer(account)) {
			throw new WikiServiceException.NoSuchWikiException("not on local host");
		}
		Mailbox mbox = Mailbox.getMailboxByAccount(account);
		MailItem item = mbox.getItemById(ctxt.octxt, id, MailItem.TYPE_UNKNOWN);
		String subject = item.getSubject();
		int folderId = item.getFolderId();
		Wiki w = Wiki.getInstance(ctxt, accountId, folderId);
		return w.lookupWiki(subject);
	}
	
	public static void addPage(WikiContext ctxt, WikiPage page, int id, int ver, ItemId folder) throws ServiceException {
		String wikiWord = page.getWikiWord();
		String account;
		int fid;
		if (folder == null) {
			fid = WIKI_FOLDER_ID;
			Account acct = Provisioning.getInstance().get(AccountBy.name, page.getCreator());
			account = acct.getId();
		} else {
			fid = folder.getId();
			account = folder.getAccountId();
		}
		
		if (id == 0) {
			// absent id means new document.
			// make sure another page with the same name does not exist.
			Wiki w = Wiki.getInstance(ctxt, account, fid);
			synchronized (w) {
				if (w.lookupWiki(page.getWikiWord()) != null)
					throw MailServiceException.ALREADY_EXISTS("wiki word "+wikiWord+" in folder "+folder,
							new Argument(MailService.A_ID, id, Argument.Type.IID),
							new Argument(MailService.A_VERSION, ver, Argument.Type.NUM));

				// create a new page
				page.create(ctxt, w);
				w.mWikiWords.put(page.getWikiWord(), page);
			}
		} else {
			// add a new revision
			WikiPage oldPage = findPage(ctxt, account, id);
			if (oldPage == null)
				throw new WikiServiceException.NoSuchWikiException("page id="+id+" not found");
			oldPage.add(ctxt, page);
		}
	}
	
	public abstract void renameDocument(WikiContext ctxt, int id, String newName, String author) throws ServiceException;
	
	public synchronized void deleteWiki(WikiContext ctxt, String wikiWord) throws ServiceException {
		WikiPage w = mWikiWords.remove(wikiWord);
		if (w != null) {
			w.deleteAllRevisions(ctxt);
		}
	}
}
