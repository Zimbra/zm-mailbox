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

import java.util.Vector;

import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;

public class WikiWord {
	
	private String mWikiWord;
	private Vector<WikiId> mWikiIds;
	
	WikiWord(String wikiWord) throws ServiceException {
		mWikiWord = wikiWord;
		mWikiIds = new Vector<WikiId>();
	}

	public String getWikiWord() {
		return mWikiWord;
	}

	public void addWikiItem(WikiItem newItem) throws ServiceException {
		mWikiIds.add(new WikiId(newItem));
	}

	public int lastRevision() {
		return mWikiIds.size();
	}

	public WikiItem getWikiItem(OperationContext octxt) throws ServiceException {
		return mWikiIds.lastElement().getWikiItem(octxt);
	}

	public WikiItem getWikiItem(OperationContext octxt, int rev) throws ServiceException {
		if (rev >= mWikiIds.size()) {
			throw new IllegalArgumentException();
		}
		return mWikiIds.get(rev).getWikiItem(octxt);
	}
}
