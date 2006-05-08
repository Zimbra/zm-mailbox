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

import java.io.IOException;

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class AddMsgOperation extends Operation {

	private static final int LOAD = 20;
	
	long mDate;
	String mTagsStr;
	Folder mFolder;
	String mFlagsStr;
	boolean mNoICal;
	MimeMessage mMm;
	int mMessageId = 0;
	

	public AddMsgOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				long date, String tags, Folder folder, String flags, boolean noIcal, MimeMessage mm)
	{
		super(session, oc, mbox, req, LOAD);

		mDate = date;
		mTagsStr = tags;
		mFolder = folder;
		mFlagsStr = flags;
		mNoICal = noIcal;
		mMm = mm;
	}
	
	protected void callback() throws ServiceException {
		try {
			ParsedMessage pm = new ParsedMessage(mMm, mDate, getMailbox().attachmentsIndexingEnabled());
			Message msg = getMailbox().addMessage(getOpCtxt(), pm, mFolder.getId(), mNoICal, Flag.flagsToBitmask(mFlagsStr), mTagsStr);
			if (msg != null)
				mMessageId = msg.getId();
		} catch(IOException ioe) {
			throw ServiceException.FAILURE("Error While Delivering Message", ioe);
		}
	}
	
	public int getMessageId() { return mMessageId; }
	
	public String toString() {
		StringBuffer toRet = new StringBuffer("<AddMsg ");
		if (mTagsStr != null)
			toRet.append(" tags=\"").append(mTagsStr).append("\"");
		if (mFolder!= null)
			toRet.append(" folder=\"").append(mFolder.getName()).append("\"");
		toRet.append(">");
		return toRet.toString();
	}
}
