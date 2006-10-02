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

import java.io.IOException;
import java.net.URL;

import org.apache.commons.httpclient.Header;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.client.LmcDocument;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.wiki.Wiki.WikiContext;

public abstract class WikiPage {

	public static WikiPage create(Document page) throws ServiceException {
		return new LocalWikiPage(page);
	}
	
	public static WikiPage create(String wikiWord, String author, byte[] data) {
		return new LocalWikiPage(wikiWord, WikiItem.WIKI_CONTENT_TYPE, author, data, MailItem.TYPE_WIKI);
	}

	public static WikiPage create(String wikiWord, String author, String ctype, byte[] data) {
		return new LocalWikiPage(wikiWord, ctype, author, data, MailItem.TYPE_DOCUMENT);
	}

	public static WikiPage create(String accountId, String path, LmcDocument doc) throws ServiceException {
		return new RemoteWikiPage(accountId, path, doc);
	}
	
	protected String mAccountId;
	protected String mWikiWord;
	protected int mId;
	protected int mFolderId;
	protected int mRevision;
	protected long mCreatedDate;
	protected long mModifiedDate;
	protected String mCreator;
	protected String mModifiedBy;
	protected String mFragment;
	protected String mContents;

	public abstract void create(WikiContext ctxt, Wiki where) throws ServiceException;
	public abstract void add(WikiContext ctxt, WikiPage page) throws ServiceException;
	public abstract void deleteAllRevisions(WikiContext ctxt) throws ServiceException;
	public abstract Document getWikiItem(WikiContext ctxt) throws ServiceException;
	public abstract String getContents(WikiContext ctxt) throws ServiceException;
	public abstract String getRestUrl();
	public abstract String getFolderKey();
	
	public synchronized WikiTemplate getTemplate(WikiContext ctxt) throws ServiceException {
		return new WikiTemplate(getContents(ctxt), mAccountId, getFolderKey(), mWikiWord);
	}
	public String getAccountId() {
		return mAccountId;
	}
	public String getWikiWord() {
		return mWikiWord;
	}

	public long getLastRevision() {
		return mRevision;
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
		return iid.toString((String)null);
	}
	
	public String getFragment() {
		return mFragment;
	}
	
	public static class LocalWikiPage extends WikiPage {
		private String  mMimeType;
		private byte[]  mData;
		private byte    mType;

		LocalWikiPage(Document doc) throws ServiceException {
			addWikiItem(doc);
		}
		
		LocalWikiPage(String wikiWord, String mime, String author, byte[] data, byte type) {
			mWikiWord = wikiWord;
			mCreator = author;
			mMimeType = mime;
			mData = data;
			mType = type;
		}

		public void create(WikiContext ctxt, Wiki where) throws ServiceException {
			if (mMimeType == null || mData == null || mType == 0)
				throw WikiServiceException.ERROR("cannot create", null);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(where.mWikiAccount);
			addWikiItem(mbox.createDocument(ctxt.octxt, where.getWikiFolderId(), mWikiWord, mMimeType, mCreator, mData, null, mType));
		}

		public void add(WikiContext ctxt, WikiPage p) throws ServiceException {
			if (!(p instanceof LocalWikiPage))
				throw WikiServiceException.ERROR("cannot add revision", null);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);

			LocalWikiPage newRev = (LocalWikiPage) p;
			Document doc = getWikiItem(ctxt);
			if (newRev.mWikiWord != null && !newRev.mWikiWord.equals(mWikiWord))
				doc.rename(newRev.mWikiWord);
			doc = mbox.addDocumentRevision(ctxt.octxt, doc, newRev.mData, newRev.mCreator);
			addWikiItem(doc);
			newRev.addWikiItem(doc);
		}
		
		private void addWikiItem(Document newItem) throws ServiceException {
			Document.DocumentRevision rev = newItem.getLastRevision();
			mWikiWord = newItem.getSubject();
			mId = newItem.getId();
			mRevision = newItem.getVersion();
			mModifiedDate = rev.getRevDate();
			mModifiedBy = rev.getCreator();
			mCreatedDate = newItem.getFirstRevision().getRevDate();
			mCreator = newItem.getFirstRevision().getCreator();
			mFolderId = newItem.getFolderId();
			mFragment = newItem.getFragment();
			mAccountId = newItem.getMailbox().getAccountId();
			mContents = null;
		}

		public void deleteAllRevisions(WikiContext ctxt) throws ServiceException {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
			mbox.delete(ctxt.octxt, mId, MailItem.TYPE_UNKNOWN);
		}

		public Document getWikiItem(WikiContext ctxt) throws ServiceException {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
			return (Document)mbox.getItemById(ctxt.octxt, mId, MailItem.TYPE_UNKNOWN);
		}
		
		public String getContents(WikiContext ctxt) throws ServiceException {
			if (mContents == null) {
				try {
					byte[] raw = ByteUtil.getContent(getWikiItem(ctxt).getLastRevision().getContent(), 0);
					mContents = new String(raw, "UTF-8");
				} catch (IOException ioe) {
					throw WikiServiceException.ERROR("can't get contents", ioe);
				}
			}
			return mContents;
		}
		public String getRestUrl() {
			return "";
		}
		public String getFolderKey() {
			return Integer.toString(mFolderId);
		}
	}
	
	public static class RemoteWikiPage extends WikiPage {
		private String mPath;
		private String mRestUrl;
		
		RemoteWikiPage(String accountId, String path, LmcDocument wiki) throws ServiceException {
			ItemId iid = new ItemId(wiki.getID(), null);
			ItemId fid = new ItemId(wiki.getFolder(), null);
			mAccountId = accountId;
			mWikiWord = wiki.getName();
			mId = iid.getId();
			mRevision = Integer.parseInt(wiki.getRev());
			mModifiedDate = Long.parseLong(wiki.getLastModifiedDate());
			mModifiedBy = wiki.getLastEditor();
			mFolderId = fid.getId();
			mRestUrl = wiki.getRestUrl();
			mCreatedDate = Long.parseLong(wiki.getCreateDate());
			mCreator = wiki.getCreator();
			mFragment = wiki.getFragment();
			mPath = path;
		}
		
		public String getContents(WikiContext ctxt) throws ServiceException {
			String auth;
			if (ctxt != null && ctxt.auth != null)
				auth = ctxt.auth;
			else {
				try {
					auth = AuthToken.getZimbraAdminAuthToken().getEncoded();
				} catch (Exception ate) {
					auth = null;
				}
			}
			String hostname;
			try {
				hostname = new URL(mRestUrl).getHost();
			} catch (java.net.MalformedURLException mue) {
	            throw ServiceException.RESOURCE_UNREACHABLE("invalid url", mue);
			}
			String url = mRestUrl + "?fmt=native";
			Pair<Header[], byte[]> resource = UserServlet.getRemoteResource(auth, hostname, url);
			try {
				return new String(resource.getSecond(), "UTF-8");
			} catch (IOException ioe) {
	            throw ServiceException.RESOURCE_UNREACHABLE("invalid url", ioe);
			}
		}
		
		public void create(WikiContext ctxt, Wiki where) throws ServiceException {
			throw WikiServiceException.ERROR("creating remote wiki page: not implemented");
		}
		
		public void add(WikiContext ctxt, WikiPage page) throws ServiceException {
			throw WikiServiceException.ERROR("updating remote wiki page: not implemented");
		}
		
		public void deleteAllRevisions(WikiContext ctxt) throws ServiceException {
			throw WikiServiceException.ERROR("updating remote wiki page: not implemented");
		}

		public Document getWikiItem(WikiContext ctxt) throws ServiceException {
			throw WikiServiceException.ERROR("not implemented");
		}
		public String getRestUrl() {
			return mRestUrl;
		}
		public String getFolderKey() {
			return mPath;
		}
	}
}
