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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.client.LmcDocument;
import com.zimbra.cs.client.LmcWiki;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.wiki.Wiki.WikiContext;

public abstract class WikiPage {

	public static WikiPage create(String wikiWord) {
		return new LocalWikiPage(wikiWord);
	}
	
	public static WikiPage create(String wikiWord, String author, byte[] data) {
		return new LocalWikiPage(wikiWord, WikiItem.WIKI_CONTENT_TYPE, author, data, MailItem.TYPE_WIKI);
	}

	public static WikiPage create(String wikiWord, String author, String ctype, byte[] data) {
		return new LocalWikiPage(wikiWord, ctype, author, data, MailItem.TYPE_DOCUMENT);
	}

	public static WikiPage create(String accountId, String wikiWord, String restUrl) {
		return new RemoteWikiPage(accountId, wikiWord, restUrl);
	}
	
	public static WikiPage create(String accountId, LmcDocument wiki) throws ServiceException {
		return new RemoteWikiPage(accountId, wiki);
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

	public abstract void create(WikiContext ctxt, Wiki where) throws ServiceException;
	public abstract void add(WikiContext ctxt, WikiPage page) throws ServiceException;
	public abstract void addWikiItem(Document item) throws ServiceException;
	public abstract void deleteAllRevisions(WikiContext ctxt) throws ServiceException;
	public abstract Document getWikiItem(WikiContext ctxt) throws ServiceException;
	public abstract String getContents(WikiContext ctxt) throws ServiceException;
	public abstract String getRestUrl();
	
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
		private Mailbox mMailbox;
		private String  mMimeType;
		private byte[]  mData;
		private byte    mType;

		private LocalWikiPage(String wikiWord) {
			mWikiWord = wikiWord;
		}
		
		private LocalWikiPage(String wikiWord, String mime, String author, byte[] data, byte type) {
			mWikiWord = wikiWord;
			mCreator = author;
			mMimeType = mime;
			mData = data;
			mType = type;
		}

		public void create(WikiContext ctxt, Wiki where) throws ServiceException {
			if (mMimeType == null || mData == null || mType == 0)
				throw WikiServiceException.ERROR("cannot create", null);
			Mailbox mbox = Mailbox.getMailboxByAccountId(where.mWikiAccount);
			addWikiItem(mbox.createDocument(ctxt.octxt, where.getWikiFolderId(), mWikiWord, mMimeType, mCreator, mData, null, mType));
		}

		public void add(WikiContext ctxt, WikiPage p) throws ServiceException {
			if (!(p instanceof LocalWikiPage))
				throw WikiServiceException.ERROR("cannot add revision", null);
			Mailbox mbox = Mailbox.getMailboxByAccountId(mAccountId);

			LocalWikiPage newRev = (LocalWikiPage) p;
			Document doc = getWikiItem(ctxt);
			if (newRev.mWikiWord != null && !newRev.mWikiWord.equals(mWikiWord))
				doc.rename(newRev.mWikiWord);
			doc = mbox.addDocumentRevision(ctxt.octxt, doc, newRev.mData, newRev.mCreator);
			addWikiItem(doc);
			newRev.addWikiItem(doc);
		}
		
		public void addWikiItem(Document newItem) throws ServiceException {
			Document.DocumentRevision rev = newItem.getLastRevision();
			mMailbox = newItem.getMailbox();
			mId = newItem.getId();
			mRevision = newItem.getVersion();
			mModifiedDate = rev.getRevDate();
			mModifiedBy = rev.getCreator();
			mCreatedDate = newItem.getFirstRevision().getRevDate();
			mCreator = newItem.getFirstRevision().getCreator();
			mFolderId = newItem.getFolderId();
			mFragment = newItem.getFragment();
			mAccountId = newItem.getMailbox().getAccountId();
		}

		public void deleteAllRevisions(WikiContext ctxt) throws ServiceException {
			mMailbox.delete(ctxt.octxt, mId, MailItem.TYPE_UNKNOWN);
		}

		public Document getWikiItem(WikiContext ctxt) throws ServiceException {
			return (Document)mMailbox.getItemById(ctxt.octxt, mId, MailItem.TYPE_UNKNOWN);
		}
		
		public String getContents(WikiContext ctxt) throws ServiceException {
			try {
				byte[] raw = ByteUtil.getContent(getWikiItem(ctxt).getLastRevision().getContent(), 0);
				return new String(raw, "UTF-8");
			} catch (IOException ioe) {
				throw WikiServiceException.ERROR("can't get contents", ioe);
			}
		}
		public String getRestUrl() {
			return "";
		}
	}
	
	public static class RemoteWikiPage extends WikiPage {
		private String mRestUrl;
		private String mContents;
		
		private RemoteWikiPage(String accountId, String name, String restUrl) {
			mAccountId = accountId;
			mWikiWord = name;
			mRestUrl = restUrl + name;
		}
		
		private RemoteWikiPage(String accountId, LmcDocument wiki) throws ServiceException {
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
			if (wiki instanceof LmcWiki)
				mContents = ((LmcWiki)wiki).getContents();
		}
		
		public String getContents(WikiContext ctxt) throws ServiceException {
			String auth;
			if (ctxt != null && ctxt.auth != null)
				auth = ctxt.auth;
			else
				try {
					auth = AuthToken.getZimbraAdminAuthToken().getEncoded();
				} catch (Exception ate) {
					auth = null;
				}
			if (mContents == null) {
				try {
					URL url = new URL(mRestUrl + "?fmt=native");
					URLConnection uconn = url.openConnection();
					uconn.addRequestProperty("cookie", "ZM_AUTH_TOKEN=" + auth);
					Object obj = uconn.getContent();
					if (obj instanceof InputStream) {
						mContents = new String(ByteUtil.getContent((InputStream)obj, 0), "UTF-8");
					}
				} catch (Exception e) {
					throw WikiServiceException.ERROR("can't get contents", e);
				}
			}
			return mContents;
		}
		
		public void create(WikiContext ctxt, Wiki where) throws ServiceException {
			throw WikiServiceException.ERROR("creating remote wiki page: not implemented");
		}
		
		public void add(WikiContext ctxt, WikiPage page) throws ServiceException {
			throw WikiServiceException.ERROR("updating remote wiki page: not implemented");
		}
		
		public void addWikiItem(Document newItem) throws ServiceException {
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
	}
}
