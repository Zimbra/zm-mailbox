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

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;

public class WikiWord {
	
	private String mWikiWord;
	private Mailbox mMailbox;
	private int mId;
	private int mRevision;
	private int mModifiedDate;
	private String mModifiedBy;
	private String mCreator;
	private int mFolderId;
	
	WikiWord(String wikiWord) throws ServiceException {
		mWikiWord = wikiWord;
	}

	public String getWikiWord() {
		return mWikiWord;
	}

	public void addWikiItem(WikiItem newItem) throws ServiceException {
		Document.DocumentRevision rev = newItem.getLastRevision();
		mMailbox = newItem.getMailbox();
		mId = newItem.getId();
		mRevision = newItem.getVersion();
		mModifiedDate = rev.getRevDate();
		mModifiedBy = rev.getCreator();
		mCreator = newItem.getCreator();
		mFolderId = newItem.getFolderId();
	}
	
	public void deleteAllRevisions(OperationContext octxt) throws ServiceException {
		mMailbox.delete(octxt, mId, MailItem.TYPE_WIKI);
	}

	public long lastRevision() {
		return mRevision;
	}

	public long getCreatedDate() {
		return mRevision;
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
	
	public WikiItem getWikiItem(OperationContext octxt) throws ServiceException {
		return mMailbox.getWikiById(octxt, mId);
	}

}
