/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.Header;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;

public abstract class WikiPage {

	public static class WikiContext {
		OperationContext octxt;
		AuthToken        auth;
		String           view;
		Locale 			 locale;
		
		public WikiContext(OperationContext o, AuthToken a) {
			octxt = o; auth = a; view = null;
		}
		public WikiContext(OperationContext o, AuthToken a, String v) {
			octxt = o; auth = a; view = v;
		}
		public WikiContext(OperationContext o, AuthToken a, String v, Locale l) {
			octxt = o; auth = a; view = v; locale = l;
		}
	}

	public static WikiPage create(Document page) {
		return new LocalWikiPage(page);
	}
	
	public static WikiPage create(String accountId, String path) throws ServiceException {
		return new RemoteWikiPage(accountId, path);
	}
	
	public WikiPage() {
		mTimestamp = System.currentTimeMillis();
	}
	
	protected long mTimestamp;
	protected String mAccountId;
	protected String mWikiWord;
	protected int mId;
	protected int mFolderId;
	protected int mVersion;
	protected long mCreatedDate;
	protected long mModifiedDate;
	protected String mCreator;
	protected String mModifiedBy;
	protected String mFragment;
	protected String mContents;

	public abstract String getContents(WikiPage.WikiContext ctxt) throws ServiceException;
	public abstract String getFolderKey();
	
	public synchronized WikiTemplate getTemplate(WikiPage.WikiContext ctxt) throws ServiceException {
		return new WikiTemplate(getContents(ctxt), mAccountId, getFolderKey(), mWikiWord);
	}
	public long getTimestamp() {
		return mTimestamp;
	}
	public String getAccountId() {
		return mAccountId;
	}
	public String getWikiWord() {
		return mWikiWord;
	}

	public long getLastVersion() {
		return mVersion;
	}

	public long getCreatedDate() {
		return mCreatedDate;
	}

	public long getModifiedDate() {
		return mModifiedDate;
	}

	public String getCreator() {
		return mCreator;
	}

	public String getLastEditor() {
		return mModifiedBy;
	}

	public String getFolderId() {
		ItemId iid = new ItemId(mAccountId, mFolderId);
		return iid.toString((String)null);
	}

	public String getId() {
		ItemId iid = new ItemId(mAccountId, mId);
		return iid.toString((String) null);
	}
	
	public String getFragment() {
		return mFragment;
	}
	
	public static WikiPage findPage(WikiPage.WikiContext ctxt, String accountId, int id) throws ServiceException {
		Account account = Provisioning.getInstance().get(Provisioning.AccountBy.id, accountId, ctxt.auth);
		if (account == null)
		    throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
		
		if (!Provisioning.onLocalServer(account)) {
			throw new WikiServiceException.NoSuchWikiException("not on local host");
		}
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		MailItem item = mbox.getItemById(ctxt.octxt, id, MailItem.TYPE_UNKNOWN);
		if (item.getType() != MailItem.TYPE_DOCUMENT &&
				item.getType() != MailItem.TYPE_WIKI)
			throw WikiServiceException.NOT_WIKI_ITEM("not a wiki item");
		return create((Document)item);
	}

	private static LRUMap sPageCache = new LRUMap(1024);
	private static long TTL = 10 * 60 * 1000;  // 10 mins
	
	private static WikiPage getCachedTemplate(Domain domain, String template) {
        long now = System.currentTimeMillis();
        String key = domain.getId() + template;
		WikiPage page = null;
		synchronized (sPageCache) {
			page = (WikiPage)sPageCache.get(key);
			if (page != null && page.getTimestamp() + TTL < now) {
				sPageCache.remove(key);
				page = null;
			}
		}
		return page;
	}
	
	private static void cacheTemplate(Domain domain, String template, WikiPage page) {
		if (page == null)
			return;
        String key = domain.getId() + template;
		synchronized (sPageCache) {
			sPageCache.put(key, page);
		}
	}
	public static WikiPage findTemplate(WikiPage.WikiContext ctxt, String accountId, String template) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(Provisioning.AccountBy.id, accountId, ctxt.auth);
        Domain domain = prov.getDomain(acct);
		template = "/Template/" + template;

		WikiPage page = null;
		if (domain != null) {
		    page = getCachedTemplate(domain, template);
		    if (page != null)
		        return page;
        
		    String domainWiki = domain.getAttr(Provisioning.A_zimbraNotebookAccount, null);
		    if (domainWiki != null)
		        page = WikiPage.findTemplatePage(ctxt, domainWiki, template);
		}
		
		if (page == null) {
			String defaultWiki = prov.getConfig().getAttr(Provisioning.A_zimbraNotebookAccount, null);
			if (defaultWiki != null)
				page = WikiPage.findTemplatePage(ctxt, defaultWiki, template);
		}
		
		if (domain != null)
		    cacheTemplate(domain, template, page);
		
		return page;
	}
	
	public static WikiPage missingPage(String template) {
		return new ErrorPage(template);
	}
	private static WikiPage findTemplatePage(WikiPage.WikiContext ctxt, String wikiAccountName, String template) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(Provisioning.AccountBy.name, wikiAccountName);
		
		if (acct == null)
		    throw ServiceException.FAILURE("wiki account " + wikiAccountName + " does not exist, please check " + 
		            Provisioning.A_zimbraNotebookAccount + " on the domain or global config", null);
		
		WikiPage page = null;
		if (Provisioning.onLocalServer(acct)) {
	        try {
		        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
	            MailItem item = mbox.getItemByPath(ctxt.octxt, template);
	            if (item instanceof Document)
	            	page = WikiPage.create((Document)item);
	        } catch (ServiceException se) {
	        }
		} else {
			page = WikiPage.create(acct.getId(), template);
			try {
				page.getContents(ctxt);
			} catch (ServiceException se) {
				page = null;
			}
		}
		return page;
	}
	public static class LocalWikiPage extends WikiPage {

		LocalWikiPage(Document doc) {
			addWikiItem(doc);
		}
		
        private void addWikiItem(Document newItem) {
            mWikiWord = newItem.getName();
            mId = newItem.getId();
            mVersion = newItem.getVersion();
            mModifiedDate = newItem.getDate();
            mModifiedBy = newItem.getCreator();
            mCreatedDate = newItem.getDate();
            mCreator = newItem.getCreator();
            mFolderId = newItem.getFolderId();
            mFragment = newItem.getFragment();
            mContents = null;
            mAccountId = newItem.getMailbox().getAccountId();
        }
        
        public Document getWikiRevision(WikiPage.WikiContext ctxt, int version) throws ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
            return (Document) mbox.getItemRevision(ctxt.octxt, mId, MailItem.TYPE_DOCUMENT, version);
        }

		public String getContents(WikiPage.WikiContext ctxt) throws ServiceException {
			if (mContents == null) {
				try {
					byte[] raw = getWikiRevision(ctxt, mVersion).getContent();
					mContents = new String(raw, "UTF-8");
				} catch (IOException ioe) {
					throw WikiServiceException.ERROR("can't get contents", ioe);
				}
			}
			return mContents;
		}
		
		public String getFolderKey() {
			return Integer.toString(mFolderId);
		}
	}
	
	public static class RemoteWikiPage extends WikiPage {
		private String mPath;
		private String mRestUrl;
		private String mContents;
		
		RemoteWikiPage(String accountId, String path) throws ServiceException {
			Provisioning prov = Provisioning.getInstance();
			Account acct = prov.getAccountById(accountId);
			mRestUrl = UserServlet.getRestUrl(acct) + path;
		}
		
		public String getContents(WikiPage.WikiContext ctxt) throws ServiceException {
			if (mContents != null)
				return mContents;
			
			AuthToken auth;
			if (ctxt != null && ctxt.auth != null)
				auth = ctxt.auth;
			else {
				try {
					auth = AuthProvider.getAdminAuthToken();
				} catch (Exception ate) {
					auth = null;
				}
			}
			String url = mRestUrl + "?fmt=native&disp=attachment";
			Pair<Header[], byte[]> resource = UserServlet.getRemoteResource(auth.toZAuthToken(), url);
			int status = 0;
			for (Header h : resource.getFirst())
				if (h.getName().compareTo("X-Zimbra-Http-Status") == 0)
					status = Integer.parseInt(h.getValue());
			if (status != 200)
	            throw ServiceException.RESOURCE_UNREACHABLE("http error "+status, null);
			try {
				mContents = new String(resource.getSecond(), "UTF-8");
			} catch (IOException ioe) {
	            throw ServiceException.RESOURCE_UNREACHABLE("invalid url", ioe);
			}
			return mContents;
		}
		
		public String getFolderKey() {
			return mPath;
		}
	}
	private static class ErrorPage extends WikiPage {
		String template;
		ErrorPage(String template) {
			this.template = template;
		}
		public String getContents(WikiContext ctxt) {
			return "<!-- missing template"+template+" -->";
		}
		public String getFolderKey() {
			return "";
		}
	}

}
