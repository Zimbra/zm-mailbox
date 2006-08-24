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
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.client.LmcDocument;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcSearchRequest;
import com.zimbra.cs.client.soap.LmcSearchResponse;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
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
 * There is a cache of each Wiki instances to help speed up a page lookup
 * by name / subject.  The maximum cache size is set by the attribute
 * <code>zimbraNotebookFolderCacheSize</code>.  The cache is connected to
 * each mailbox via <code>WikiSession</code> which sends notification for
 * changes in the Wiki pages and documents in the cache.
 * 
 * Each Wiki instances have a cache of Wiki pages, in order to minimize
 * the search in the mailbox after each page lookup.  The size of
 * Wiki page cache is set by the attribute 
 * <code>zimbraNotebookMaxCachedTemplatesPerFolder</code>.
 * 
 * @author jylee
 *
 */
public abstract class Wiki {
	protected String mWikiAccount;
	
	private static final long TEMPLATE_TTL = 10 * 60 * 1000;  // 10 minutes
	private static final int  DEFAULT_CACHE_SIZE = 1024;
	protected static LRUMap sWikiNotebookCache;

	protected static final String TEMPLATE_FOLDER_NAME = "/template";
	protected static final int    WIKI_FOLDER_ID  = 12;
	protected static final int    TEMPLATE_CACHE_SIZE = 256;
	
	protected long   mExpiration;
	protected LRUMap mWikiPages;
	
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
		String           view;
		
		public WikiContext(OperationContext o, String a) {
			octxt = o; auth = a; view = null;
		}
		public WikiContext(OperationContext o, String a, String v) {
			octxt = o; auth = a; view = v;
		}
	}
	
	static class WikiUrl {
		public WikiUrl(MailItem item) {
			this(item.getSubject(), item.getFolderId());
			if (item instanceof Folder)
				mIsFolder = true;
		}
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
		private boolean mIsFolder;
		
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
			if (mIsFolder)
				newPath.append("/");
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
		
		WikiById(WikiContext ctxt, Account acct, int fid) throws ServiceException {
			if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
				throw WikiServiceException.NOT_ENABLED();
			
			mWikiAccount = acct.getId();
			mFolderId = fid;
			
			if (Provisioning.onLocalServer(acct)) {
				Mailbox mbox = Mailbox.getMailboxByAccount(acct);
				if (mbox == null)
					throw WikiServiceException.ERROR("wiki account mailbox not found");
				mbox.addListener(WikiSession.getInstance());
			} else {
				// for some reason if the account is not local, but we have folder id.
				throw WikiServiceException.ERROR("cannot access remote account "+acct.getName());
			}
		}
		
		private String createQueryString(String wikiWord) {
			StringBuilder buf = new StringBuilder();
			buf.append("inid:");
			buf.append(mFolderId);
			buf.append(" filename:");
			buf.append(wikiWord);
			return buf.toString();
		}
		
		private static byte[] sQueryTypes = { MailItem.TYPE_WIKI, MailItem.TYPE_DOCUMENT };
		
		public WikiPage lookupWiki(WikiContext ctxt, String wikiWord) throws ServiceException {
			wikiWord = wikiWord.toLowerCase();
			WikiPage page = (WikiPage)mWikiPages.get(wikiWord);
			if (page != null)
				return page;
			Mailbox mbox = Mailbox.getMailboxByAccountId(mWikiAccount);
			ZimbraQueryResults results = null;
			try {
				results = mbox.search(ctxt.octxt, createQueryString(wikiWord), sQueryTypes, SortBy.SUBJ_ASCENDING, 10);
				while (results.hasNext()) {
					MailItem item = results.getNext().getMailItem();
					if (item.getFolderId() == mFolderId &&
							(item.getType() == MailItem.TYPE_DOCUMENT ||
							 item.getType() == MailItem.TYPE_WIKI)) {
						page = WikiPage.create((Document) item);
						synchronized (mWikiPages) {
							if (!mWikiPages.containsKey(wikiWord))
								mWikiPages.put(wikiWord, page);
						}
						return page;
					}
				}
				return null;
			} catch (ParseException pe) {
				throw WikiServiceException.ERROR("error searching for "+wikiWord, pe);
			} catch (IOException ioe) {
				throw WikiServiceException.ERROR("error searching for "+wikiWord, ioe);
			} finally {
				if (results != null)
					results.doneWithSearchResults();
			}
		}
		
		public synchronized void renameDocument(WikiContext ctxt, int id, String newName, String author) throws ServiceException {
			WikiPage p = lookupWiki(ctxt, newName);
			if (p != null)
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
		
		WikiByPath(Account acct, String path) throws ServiceException {
			if (!acct.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
				throw WikiServiceException.NOT_ENABLED();
			
			mWikiAccount = acct.getId();
			mPath = path;
		}

		private String createQueryString(String wikiWord) {
			StringBuilder buf = new StringBuilder();
			buf.append("in:");
			buf.append(mPath);
			buf.append(" filename:");
			buf.append(wikiWord);
			return buf.toString();
		}
		
		private synchronized WikiPage loadRemotePage(String page) throws ServiceException {
			try {
				Provisioning prov = Provisioning.getInstance();
				Account acct = prov.get(AccountBy.id, mWikiAccount);
				Server remoteServer = prov.getServer(acct);
				String auth = AuthToken.getZimbraAdminAuthToken().getEncoded();

				String url = URLUtil.getMailURL(remoteServer, ZimbraServlet.USER_SERVICE_URI, false);

				LmcSession session = new LmcSession(auth, null);

				LmcSearchRequest sreq = new LmcSearchRequest();
				sreq.setRequestedAccountId(acct.getId());
				sreq.setSession(session);
				sreq.setTypes("wiki,document");
				sreq.setLimit("1");
				sreq.setOffset("0");
				sreq.setQuery(createQueryString(page));

				LmcSearchResponse sresp = (LmcSearchResponse)sreq.invoke(url);

				@SuppressWarnings("unchecked")
				List<Object> ls = (List<Object>)sresp.getResults();
				for (Object obj : ls) {
					if (obj instanceof LmcDocument) {
						WikiPage wp = WikiPage.create(mWikiAccount, mPath, (LmcDocument)obj);
						return wp;
					} else {
						// unhandled item
					}
				}
			} catch (Exception e) {
				throw WikiServiceException.ERROR("can't load remote wiki", e);
			}
			return null;
		}
		
		public synchronized WikiPage lookupWiki(WikiContext ctxt, String wikiWord) throws ServiceException{
			wikiWord = wikiWord.toLowerCase();
			WikiPage page = (WikiPage)mWikiPages.get(wikiWord);
			if (page != null)
				return page;
			synchronized (mWikiPages) {
				page = (WikiPage)mWikiPages.get(wikiWord);
				if (page == null) {
					page = loadRemotePage(wikiWord);
					if (page != null)
						mWikiPages.put(wikiWord, page);
				}
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
	
	protected Wiki() {
		Provisioning prov = Provisioning.getInstance();
		int cacheSize;
		try {
			Server localServer = prov.getLocalServer();
			cacheSize = localServer.getIntAttr(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, TEMPLATE_CACHE_SIZE);
		} catch (ServiceException se) {
			cacheSize = TEMPLATE_CACHE_SIZE;
		}
		
		mWikiPages = new LRUMap(cacheSize);
		mExpiration = System.currentTimeMillis() + TEMPLATE_TTL;
	}
	
	public static Account getDefaultWikiAccount() throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Config globalConfig = prov.getConfig();
		String defaultAcct = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount);
		if (defaultAcct == null)
			throw WikiServiceException.ERROR("empty config variable " + Provisioning.A_zimbraNotebookAccount);
		Account acct = prov.get(AccountBy.name, defaultAcct);
		if (acct == null)
			throw WikiServiceException.ERROR("no such account " + defaultAcct);
		
		return acct;
	}
	
	public static WikiPage findWikiPageByPath(WikiContext ctxt, String accountId, int fid, String path, boolean traverse) throws ServiceException {
		return findWikiPageByPath(ctxt, accountId, new WikiUrl(path, fid), traverse);
	}
	
	public static WikiPage getChrome(WikiContext ctxt, String pageName, String accountId) throws ServiceException {
		Wiki wiki;
		WikiPage page;
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(Provisioning.AccountBy.id, accountId);
		String domainWiki = prov.getDomain(acct).getAttr(Provisioning.A_zimbraNotebookAccount, null);
		
		if (domainWiki != null) {
			acct = prov.get(Provisioning.AccountBy.name, domainWiki);
			wiki = getInstance(ctxt, acct.getId(), TEMPLATE_FOLDER_NAME);
			page = wiki.lookupWiki(ctxt, pageName);
			if (page != null)
				return page;
		}
		String defaultWiki = prov.getConfig().getAttr(Provisioning.A_zimbraNotebookAccount, null);
		if (defaultWiki != null) {
			acct = prov.get(Provisioning.AccountBy.name, defaultWiki);
			wiki = getInstance(ctxt, acct.getId(), TEMPLATE_FOLDER_NAME);
			page = wiki.lookupWiki(ctxt, pageName);
			if (page != null)
				return page;
		}
		throw new WikiServiceException.NoSuchWikiException(pageName);
	}
	
	public static WikiPage findWikiPageByPath(WikiContext ctxt, String accountId, WikiUrl url, boolean traverse) throws ServiceException {
		Wiki wiki = url.findWiki(ctxt, accountId);
		String pageName = url.getFilename();
		WikiPage page = wiki.lookupWiki(ctxt, pageName);
		
		if (page != null)
			return page;

		if (!traverse)
			throw new WikiServiceException.NoSuchWikiException(url.getUrl());

		return getChrome(ctxt, pageName, accountId);
	}
	
	public static Wiki getInstance(WikiContext ctxt, MailItem item) throws ServiceException {
		int folderId = (item instanceof Folder) ? item.getId() : item.getFolderId();
		return getInstance(ctxt, item.getAccount().getId(), folderId);
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

		Pair<String,String> key = new Pair<String,String>(acctId, Integer.toString(folderId));
		Wiki wiki;
		synchronized (sWikiNotebookCache) {
			wiki = get(key);
			if (wiki == null) {
				wiki = new WikiById(ctxt, acct, folderId);
				sWikiNotebookCache.put(key, wiki);
			}
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
			Pair<String,String> k = new Pair<String,String>(account.getId(), key);
			Wiki wiki;
			synchronized (sWikiNotebookCache) {
				wiki = get(k);
				if (wiki == null) {
					wiki = new WikiByPath(account, key);
					sWikiNotebookCache.put(k, wiki);
				}
			}
			return wiki;
		}
	}
	
	public static Wiki get(String acctId, String k) {
		return get(new Pair<String,String>(acctId, k));
	}

	private static Wiki get(Pair<String,String> key) {
		Wiki wiki;
		synchronized (sWikiNotebookCache) {
			wiki = (Wiki)sWikiNotebookCache.get(key);
			if (wiki != null) {
				long now = System.currentTimeMillis();
				if (wiki.mExpiration < now) {
					sWikiNotebookCache.remove(key);
					wiki = null;
				}
			}
		}
		return wiki;
	}
	
	public static void remove(String acctId, String k) {
		synchronized (sWikiNotebookCache) {
			Pair<String,String> key = new Pair<String,String>(acctId, k);
			sWikiNotebookCache.remove(key);
		}
	}

	public abstract int getWikiFolderId() throws ServiceException;
	
	public abstract WikiPage lookupWiki(WikiContext ctxt, String wikiWord) throws ServiceException;
	
	public static WikiPage findPage(WikiContext ctxt, String accountId, int id) throws ServiceException {
		Account account = Provisioning.getInstance().get(Provisioning.AccountBy.id, accountId);
		if (!Provisioning.onLocalServer(account)) {
			throw new WikiServiceException.NoSuchWikiException("not on local host");
		}
		Mailbox mbox = Mailbox.getMailboxByAccount(account);
		MailItem item = mbox.getItemById(ctxt.octxt, id, MailItem.TYPE_UNKNOWN);
		if (item.getType() != MailItem.TYPE_DOCUMENT &&
				item.getType() != MailItem.TYPE_WIKI)
			throw WikiServiceException.NOT_WIKI_ITEM("not a wiki item");
		return WikiPage.create((Document)item);
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
			Wiki w = getInstance(ctxt, account, fid);
			synchronized (w) {
				WikiPage pg = w.lookupWiki(ctxt, wikiWord);
				if (pg != null)
					throw MailServiceException.ALREADY_EXISTS("wiki word "+wikiWord+" in folder "+fid,
							new Argument(MailService.A_NAME, wikiWord, Argument.Type.STR),
							new Argument(MailService.A_ID, pg.getId(), Argument.Type.IID),
							new Argument(MailService.A_VERSION, pg.getLastRevision(), Argument.Type.NUM));

				// create a new page
				page.create(ctxt, w);
			}
		} else {
			// add a new revision
			WikiPage oldPage = findPage(ctxt, account, id);
			if (oldPage == null)
				throw new WikiServiceException.NoSuchWikiException("page id="+id+" not found");
			if (oldPage.getLastRevision() != ver) {
				throw MailServiceException.MODIFY_CONFLICT(
						new Argument(MailService.A_NAME, wikiWord, Argument.Type.STR),
						new Argument(MailService.A_ID, oldPage.getId(), Argument.Type.IID),
						new Argument(MailService.A_VERSION, oldPage.getLastRevision(), Argument.Type.NUM));
			}
			oldPage.add(ctxt, page);
		}
	}
	
	public abstract void renameDocument(WikiContext ctxt, int id, String newName, String author) throws ServiceException;
	
	public WikiTemplate getTemplate(WikiContext ctxt, String name) throws ServiceException, IOException {
		// check if the request is for the chrome.
		WikiPage page;
		if (name.startsWith("_")) {
			try {
				page = getChrome(ctxt, name, mWikiAccount);
				if (page != null)
					return page.getTemplate(ctxt);
			} catch (ServiceException se) {
				return new WikiTemplate("<!-- missing template "+name+" -->");
			}
		}

		// check if the request is local to the same folder.
		page = lookupWiki(ctxt, name);

		if (page != null)
			return page.getTemplate(ctxt);

		// find the page by its full path.
		page = findWikiPageByPath(ctxt, mWikiAccount, new WikiUrl(name, getWikiFolderId()), false);
		if (page != null)
			return page.getTemplate(ctxt);

		// the page hasn't been found.
		return new WikiTemplate("<!-- missing template "+name+" -->");
	}
	
	public static void expireTemplate(Document item) {
		Pair<String,String> key = new Pair<String,String>(item.getMailbox().getAccountId(), 
															Integer.toString(item.getFolderId()));
		Wiki wiki = (Wiki)sWikiNotebookCache.get(key);
		if (wiki != null) {
			String wikiWord = item.getSubject().toLowerCase();
			synchronized (wiki.mWikiPages) {
				wiki.mWikiPages.remove(wikiWord);
			}
		}
	}
}
