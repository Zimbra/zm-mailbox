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

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class GetMsgOperation extends Operation {

	private static final int LOAD = 5;
	
	protected void callback() throws ServiceException {
		if (!mIId.hasSubpart()) {
			mMsg = mMailbox.getMessageById(mOpCtxt, mIId.getId());
            
			if (mRead && mMsg.isUnread() && !RedoLogProvider.getInstance().isSlave())
				mMailbox.alterTag(mOpCtxt, mIId.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, false);
		} else {
			mAppt = mMailbox.getAppointmentById(mOpCtxt, mIId.getId());
        }
	}
	
	public String toString() {
		String toRet = super.toString() + " IID="+mIId.toString()+" mRaw="+mRaw+" mRead="+mRead;
		if (mPart != null) {
			toRet = toRet + " Part="+mPart;
		}
		return toRet;
	}

	public GetMsgOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iid, boolean raw, boolean read, String part) throws ServiceException {
		super(session, oc, mbox, req, LOAD);
		
		mIId = iid;
		mRaw = raw;
		mRead = read;
		mPart = part;
		
		schedule();
	}
	
	public Message getMsg() { return mMsg; }
	public Appointment getAppt() { return mAppt; }

	ItemId mIId;
	boolean mRaw;
	boolean mRead;
	String mPart;
	Message mMsg;
	Appointment mAppt;
}
