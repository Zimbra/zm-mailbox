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
package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class GetImapFolderOperation extends Operation {
	String mFolderName;
	boolean mWritable;
	ImapFolder mResult;
	
	public ImapFolder getResult() { return mResult; }
	public boolean getWritable()   { return mWritable; }
	
	private final static int BASE_LOAD = 1000;
	
	public GetImapFolderOperation(Session session, OperationContext oc, Mailbox mbox, String folderName, boolean writable) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), BASE_LOAD);
		
		mFolderName = folderName;
		mWritable = writable;
		
		schedule();
	}
	
	protected void callback() throws ServiceException 
	{
		synchronized(mMailbox) {
			mResult = new ImapFolder(mFolderName, mWritable, mMailbox, mOpCtxt);
			mWritable = mResult.isWritable();
		}
	}
	
	public String toString() {
		return super.toString() + " Folder="+mFolderName+" Writable="+mWritable;
	}
}
