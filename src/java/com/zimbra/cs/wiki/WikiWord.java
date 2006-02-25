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

import java.util.TreeSet;

import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;

public class WikiWord {
	
	private String mWikiWord;
	private TreeSet<WikiId> mWikiIds;
	
	WikiWord(String wikiWord) throws ServiceException {
		mWikiWord = wikiWord;
		mWikiIds = new TreeSet<WikiId>();
	}

	public String getWikiWord() {
		return mWikiWord;
	}

	public void addWikiItem(WikiItem newItem) throws ServiceException {
		mWikiIds.add(new WikiId(newItem));
	}
	
	public void deleteAllRevisions(OperationContext octxt) throws ServiceException {
		for (WikiId wiki : mWikiIds) {
			wiki.deleteWikiItem(octxt);
		}
		mWikiIds.clear();
	}

	public long lastRevision() {
		return mWikiIds.last().getVersion();
	}

	public long getCreatedDate() {
		return mWikiIds.first().getCreatedDate();
	}
	
	public long getModifiedDate() {
		return mWikiIds.last().getCreatedDate();
	}
	
	public String getCreator() {
		return mWikiIds.first().getCreator();
	}
	
	public String getLastEditor() {
		return mWikiIds.last().getCreator();
	}
	
	public int getFolderId() {
		return mWikiIds.last().getFolderId();
	}
	
	public WikiItem getWikiItem(OperationContext octxt) throws ServiceException {
		return mWikiIds.last().getWikiItem(octxt);
	}

	public WikiItem getWikiItem(OperationContext octxt, long rev) throws ServiceException {
		if (rev > 0 && rev <= lastRevision()) {
			WikiId item = mWikiIds.tailSet(WikiId.getWikiId(rev)).first();
			if (item != null && item.mVersion == rev) {
				return item.getWikiItem(octxt);
			}
		}
		throw ServiceException.FAILURE("no such item", null);
	}
}
