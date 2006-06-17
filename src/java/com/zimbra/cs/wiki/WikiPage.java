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

import com.zimbra.cs.client.LmcDocument;
import com.zimbra.cs.client.LmcWiki;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.wiki.Wiki.WikiContext;

public abstract class WikiPage {

	public static WikiPage create(String wikiWord) {
		return new LocalWikiPage(wikiWord);
	}

	public static WikiPage create(String wikiWord, String restUrl) {
		return new RemoteWikiPage(wikiWord, restUrl);
	}
	
	public static WikiPage create(LmcDocument wiki) throws ServiceException {
		return new RemoteWikiPage(wiki);
	}
	
	protected String mWikiWord;
	protected int mId;
	protected int mFolderId;
	protected int mRevision;
	protected long mCreatedDate;
	protected long mModifiedDate;
	protected String mCreator;
	protected String mModifiedBy;

	public abstract void addWikiItem(WikiContext ctxt,
										String acctid,
										int fid,
										String wikiWord,
										String mimeType,
										String author,
										byte[] data,
										byte type) throws ServiceException;
	public abstract void addWikiItem(Document item) throws ServiceException;
	public abstract void deleteAllRevisions(WikiContext ctxt) throws ServiceException;
	public abstract Document getWikiItem(WikiContext ctxt) throws ServiceException;
	public abstract String getContents(WikiContext ctxt) throws ServiceException;
	
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

	public int getFolderId() {
		return mFolderId;
	}

	public int getId() {
		return mId;
	}
	
	public static class LocalWikiPage extends WikiPage {
		private Mailbox mMailbox;
		private boolean mEmpty = true;

		private LocalWikiPage(String wikiWord) {
			mWikiWord = wikiWord;
		}

		public void addWikiItem(WikiContext ctxt, 
								String acctid, 
								int fid, 
								String wikiWord, 
								String mime, 
								String author, 
								byte[] data, 
								byte type) throws ServiceException {
			Mailbox mbox = Mailbox.getMailboxByAccountId(acctid);
			if (mEmpty) {
				addWikiItem(mbox.createDocument(ctxt.octxt, fid, wikiWord, mime, author, data, null, type));
			} else {
				addWikiItem(mbox.addDocumentRevision(ctxt.octxt, getWikiItem(ctxt), data, author));
			}
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
			mEmpty = false;
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
	}
	
	public static class RemoteWikiPage extends WikiPage {
		private String mRestUrl;
		private String mContents;
		
		private RemoteWikiPage(String name, String restUrl) {
			mWikiWord = name;
			mRestUrl = restUrl + name;
		}
		
		private RemoteWikiPage(LmcDocument wiki) throws ServiceException {
			ItemId iid = new ItemId(wiki.getID(), null);
			ItemId fid = new ItemId(wiki.getFolder(), null);
			mWikiWord = wiki.getName();
			mId = iid.getId();
			mRevision = Integer.parseInt(wiki.getRev());
			mModifiedDate = Long.parseLong(wiki.getLastModifiedDate());
			mModifiedBy = wiki.getLastEditor();
			mFolderId = fid.getId();
			mRestUrl = wiki.getRestUrl();
			if (wiki instanceof LmcWiki)
				mContents = ((LmcWiki)wiki).getContents();
		}
		
		public String getContents(WikiContext ctxt) throws ServiceException {
			if (mContents == null) {
				try {
					URL url = new URL(mRestUrl + "?fmt=native");
					URLConnection uconn = url.openConnection();
					uconn.addRequestProperty("cookie", "ZM_AUTH_TOKEN=" + ctxt.auth);
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
		
		public void addWikiItem(WikiContext ctxt, 
								String acctid, 
								int fid, 
								String wikiWord, 
								String mime, 
								String author, 
								byte[] data, 
								byte type) throws ServiceException {
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
	}
}
