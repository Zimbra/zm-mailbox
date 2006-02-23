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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;

public class WikiId implements Comparable {
	Account mAcct;
	int     mId;
	long    mCreatedDate;
	String  mCreator;
	long    mVersion;
	
	public static WikiId getWikiId(long ver) {
		return new WikiId(ver);
	}
	
	private WikiId(long ver) {
		mVersion = ver;
	}
	public WikiId(WikiItem wikiItem) throws ServiceException {
		Mailbox mbox = wikiItem.getMailbox();
		mAcct = mbox.getAccount();
		mId = wikiItem.getId();
		mCreatedDate = wikiItem.getCreatedTime();
		mCreator = wikiItem.getCreator();
		mVersion = wikiItem.getVersion();
	}
	
	public WikiItem getWikiItem(OperationContext octxt) throws ServiceException {
		Mailbox mbox = Mailbox.getMailboxByAccount(mAcct);
		WikiItem wiki = mbox.getWikiById(octxt, mId);
		return wiki;
	}
	
	public void deleteWikiItem(OperationContext octxt) throws ServiceException {
		Mailbox mbox = Mailbox.getMailboxByAccount(mAcct);
		mbox.move(octxt, mId, MailItem.TYPE_WIKI, Mailbox.ID_FOLDER_TRASH);
	}
	
	public long getCreatedDate() {
		return mCreatedDate;
	}
	
	public String getCreator() {
		return mCreator;
	}
	
	public long getVersion() {
		return mVersion;
	}
	
	public int compareTo(Object obj) {
		if (obj instanceof WikiId) {
			WikiId that = (WikiId) obj;
			return (int) (mVersion - that.mVersion);
		}
		return 0;
	}
}
