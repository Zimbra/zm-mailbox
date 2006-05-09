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
package com.zimbra.cs.operation;

import java.util.List;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class GetItemListOperation extends Operation {

	private static int LOAD = 5;
	static {
		Operation.Config c = loadConfig(GetItemListOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private byte mType;
	private int mFolderId;
	private byte mSort = -1;
	
	private List<? extends MailItem> mResult;

	public GetItemListOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, 
				byte type, int folderId, byte sort) {
		super(session, oc, mbox, req, LOAD);

		mType = type;
		mFolderId = folderId;
		mSort = sort;
	}
	
	public GetItemListOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, 
				byte type, int folderId) {
		this(session, oc, mbox, req, type, folderId, DbMailItem.SORT_NONE);
	}
	
	public GetItemListOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, 
				byte type) {
		this(session, oc, mbox, req, type, -1, DbMailItem.SORT_NONE);
	}
	
	public String toString() {
		StringBuilder toRet = new StringBuilder("GetItemList(");
		toRet.append("type=").append(mType);
		toRet.append(" folder=").append(mFolderId);
		toRet.append(" sort=").append(mSort);
		toRet.append(")");
		return toRet.toString();
	}
	
	protected void callback() throws ServiceException {
		mResult = getMailbox().getItemList(getOpCtxt(), mType, mFolderId, mSort);
	}
	
	public List<? extends MailItem> getResults() { return mResult; }
}
