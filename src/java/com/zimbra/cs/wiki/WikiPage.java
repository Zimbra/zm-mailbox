/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.wiki;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.httpclient.Header;

import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.session.WikiSession;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.wiki.Wiki.WikiContext;

public abstract class WikiPage {

	public static WikiPage create(Document page) {
		return new LocalWikiPage(page);
	}
	
    public static WikiPage create(Document page, int rev, WikiContext ctxt) throws ServiceException {
        return new LocalWikiPage(page, rev, ctxt);
    }
    
	public static WikiPage create(String wikiWord, String author, byte[] data) {
		return new LocalWikiPage(wikiWord, WikiItem.WIKI_CONTENT_TYPE, author, data, MailItem.TYPE_WIKI);
	}

	public static WikiPage create(String wikiWord, String author, String ctype, byte[] data) {
		return new LocalWikiPage(wikiWord, ctype, author, data, MailItem.TYPE_DOCUMENT);
	}

	public static WikiPage create(String accountId, String path, Element elem) throws ServiceException {
		return new RemoteWikiPage(accountId, path, elem);
	}
	
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
	
	public static class LocalWikiPage extends WikiPage {
		private String  mMimeType;
		private byte[]  mData;
		private byte    mType;

		LocalWikiPage(Document doc) {
			addWikiItem(doc);
		}
		
        LocalWikiPage(Document doc, int rev, WikiContext ctxt) throws ServiceException {
            addWikiItem(doc, rev, ctxt);
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
			WikiSession.getInstance().addMailboxForNotification(mbox);
			addWikiItem(mbox.createDocument(ctxt.octxt, where.getWikiFolderId(), mWikiWord, mMimeType, mCreator, mData, mType));
		}

		public void add(WikiContext ctxt, WikiPage p) throws ServiceException {
			if (!(p instanceof LocalWikiPage))
				throw WikiServiceException.ERROR("cannot add revision", null);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
			WikiSession.getInstance().addMailboxForNotification(mbox);

			LocalWikiPage newRev = (LocalWikiPage) p;
			Document doc = getWikiItem(ctxt);
			if (newRev.mWikiWord != null && !newRev.mWikiWord.equals(mWikiWord))
                mbox.rename(ctxt.octxt, doc.getId(), MailItem.TYPE_DOCUMENT, newRev.mWikiWord);
			doc = mbox.addDocumentRevision(ctxt.octxt, doc.getId(), doc.getType(), newRev.mData, newRev.mCreator);
			addWikiItem(doc);
			newRev.addWikiItem(doc);
		}

        private void addWikiItem(Document newItem, int version, WikiContext ctxt) throws ServiceException {
            Document revision = (Document) newItem.getMailbox().getItemRevision(ctxt.octxt, newItem.getId(), newItem.getType(), version);
            if (revision == null)
                throw MailServiceException.NO_SUCH_REVISION(newItem.getId(), version);
            addWikiItem(revision);
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
        
		public void deleteAllRevisions(WikiContext ctxt) throws ServiceException {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
			mbox.delete(ctxt.octxt, mId, MailItem.TYPE_DOCUMENT);
		}

        public Document getWikiItem(WikiContext ctxt) throws ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
            return (Document) mbox.getItemById(ctxt.octxt, mId, MailItem.TYPE_DOCUMENT);
        }

        public Document getWikiRevision(WikiContext ctxt, int version) throws ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
            return (Document) mbox.getItemRevision(ctxt.octxt, mId, MailItem.TYPE_DOCUMENT, version);
        }

		public String getContents(WikiContext ctxt) throws ServiceException {
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
		private String mContent;
		
		RemoteWikiPage(String accountId, String path, Element elem) throws ServiceException {
			mAccountId = accountId;
			ItemId iid = new ItemId(elem.getAttribute(MailConstants.A_ID), (String) null);
			ItemId fid = new ItemId(elem.getAttribute(MailConstants.A_FOLDER), (String) null);
			mId = iid.getId();
			mFolderId = fid.getId();
			mWikiWord = elem.getAttribute(MailConstants.A_NAME);
			mVersion = (int) elem.getAttributeLong(MailConstants.A_VERSION);
			mModifiedDate = elem.getAttributeLong(MailConstants.A_MODIFIED_DATE);
			mModifiedBy = elem.getAttribute(MailConstants.A_LAST_EDITED_BY);
			mRestUrl = elem.getAttribute(MailConstants.A_REST_URL);
			mCreatedDate = elem.getAttributeLong(MailConstants.A_DATE);
			mCreator = elem.getAttribute(MailConstants.A_CREATOR);
			Iterator<Element> iter = elem.elementIterator();
			while (iter.hasNext()) {
				Element e = iter.next();
				if (e.getName().equals(MailConstants.E_FRAG))
					mFragment = e.getText();
				else if (e.getName().equals(MailConstants.A_BODY))
					mContent = e.getText();
			}
		}
		
		public String getContents(WikiContext ctxt) throws ServiceException {
			if (mContent != null)
				return mContent;
			
			AuthToken auth;
			if (ctxt != null && ctxt.auth != null)
				auth = ctxt.auth;
			else {
				try {
				    // AP-TODO-10: this will not work for Yahoo Y&T
					auth = AuthToken.getZimbraAdminAuthToken();
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
