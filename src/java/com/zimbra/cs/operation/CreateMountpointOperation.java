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
package com.zimbra.cs.operation;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;


public class CreateMountpointOperation extends Operation {
	
	private static final int LOAD = 10;
	
	private ItemId mIidParent;
	private String mName;
	private String mOwnerId;
	private int mRemoteId;
	private String mView;
	private boolean mFetchIfExists;
	
	private Mountpoint mMpt;

	public CreateMountpointOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iidParent, String name, String ownerId, int remoteId, String view, boolean fetchIfExists) {
		super(session, oc, mbox, req, LOAD);
		
		mIidParent = iidParent;
		mName = name;
		mOwnerId = ownerId;
		mRemoteId = remoteId;
		mView = view;
		mFetchIfExists = fetchIfExists;
	}
	
	public String toString() {
		StringBuilder toRet = new StringBuilder("CreateMountpoint(");

		toRet.append("name=").append(mName);
		toRet.append(" parent=").append(mIidParent);
		if (mView != null) 
			toRet.append(" view=").append(mView);
		
		toRet.append(" ownerId=").append(mOwnerId);
		toRet.append(" remoteId=").append(mRemoteId);
		
		toRet.append(")");
 		
		return toRet.toString();
	}
	
	protected void callback() throws ServiceException {
		try {
			mMpt = getMailbox().createMountpoint(getOpCtxt(), mIidParent.getId(), mName, mOwnerId, mRemoteId, MailItem.getTypeForName(mView));
		} catch (ServiceException se) {
			if (se.getCode() == MailServiceException.ALREADY_EXISTS && mFetchIfExists) {
				Folder folder = getMailbox().getFolderByName(this.getOpCtxt(), mIidParent.getId(), mName);
				if (folder instanceof Mountpoint)
					mMpt = (Mountpoint) folder;
				else
					throw se;
			} else
				throw se;
		}
	}
	
	public Mountpoint getMountpoint() { return mMpt; }

}
